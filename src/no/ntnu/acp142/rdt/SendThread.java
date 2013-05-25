package no.ntnu.acp142.rdt;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import no.ntnu.acp142.Libjpmul;
import no.ntnu.acp142.Configuration;
import no.ntnu.acp142.Log;
import no.ntnu.acp142.MulticastGroup;
import no.ntnu.acp142.pdu.AddressPdu;
import no.ntnu.acp142.pdu.AddressPdu.DestinationEntry;
import no.ntnu.acp142.pdu.AnnouncePdu;
import no.ntnu.acp142.pdu.Pdu;
import no.ntnu.acp142.pdu.RequestPdu;
import no.ntnu.acp142.rdt.Entry.EntryType;
import no.ntnu.acp142.udp.Tuple;

/*
 * Copyright (c) 2013, Bjørn Tungesvik, Karl Mardoff Kittilsen
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     (1) Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer. 
 * 
 *     (2) Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.  
 *     
 *     (3) The name of the author may not be used to
 *     endorse or promote products derived from this software without
 *     specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */


/**
 * 
 * @author Bjørn Tungesvik, Karl Mardoff Kittilsen
 * 
 */

public class SendThread extends Thread {
	/**
	 * Reference to the ReliableDataTransfer class. 
	 */
	private ReliableDataTransfer rdt;

	/**
	 * Create and new instance of the sendThread
	 * 
	 * @param rdt
	 *            instance
	 */
	public SendThread(ReliableDataTransfer rdt) {
		this.rdt = rdt;
	}

	/**
	 * The purpose of this thread is to choose the
	 * appropriate send method for the specific Entry located in the outMessage queue.
	 * 
	 * */
	@Override
	public void run() {
	    Log.writeLine(Log.LOG_LEVEL_DEBUG, "Starting send thread");
		while (true) {
			try {
				// Get a message entry to send

				Entry currentEntry = rdt.outMessages.take();
				Log.writeLine(Log.LOG_LEVEL_DEBUG, "Size of queue after take: "
						+ rdt.outMessages.size());

				sendHandler(currentEntry);
				Log.writeLine(Log.LOG_LEVEL_DEBUG, "Processing send request");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}

	

	/**
	 * This method will detect the type of PDU to be sent, and act depending on
	 * its type. 
	 * 
	 * @param currentEntry Entry the packets to send
	 */
	private void sendHandler(Entry currentEntry) {
		//If some of the receivers is currently in EMCON mode; this flag is set to 
		//true
		boolean atLeastOneInEMCON = false;
		
		if (currentEntry.getType() == EntryType.MESSAGE_ENTRY) {
			MessageEntry tmp = (MessageEntry) currentEntry;
			if (tmp.getState() == States.START_STATE) {
				// set to transmitting state
				tmp.setState(States.TRANSMITTING);
			}
	        Tuple<CopyOnWriteArraySet<MessageEntry>, AtomicBoolean> tuple;
			if (tmp.getMulticastAddress() == null) {
				//Set up a new persistent group if it does not already exist
				//This method will do nothing if we do not use persistent groups
				if(!rdt.usePersistentGroupIfAvailable(tmp)){
					//Does not exist
					Log.writeLine(Log.LOG_LEVEL_DEBUG,
							"Destination address is null");
					prepareRequestPdu(tmp);
					return;
				}  else if ( (tuple = rdt.persistentGroupSessions.get(tmp.getMulticastAddress())) != null
				        && tmp.isPersistentMulticastGroups() == false) {
		            tuple.t2.set(false);
		        }
			}

			//Get the addressPdu
			AddressPdu addressPdu = tmp.getAddressPdu();
			
			if(addressPdu == null){
				Log.writeLine(Log.LOG_LEVEL_DEBUG, "AddressPdu was NULL in mode " + tmp.getState());
			}

			if(tmp.getState() == States.TRANSMITTING){
				/*
				 * Message from the level above. 
				 * 
				 * Store it and start the expiry timer
				 */
				
				long expiryTime = addressPdu.getExpiryTime() - System.currentTimeMillis()/1000;
				rdt.initializeTimer(expiryTime * 1000,
						TimerType.EXPIRY_TIMER_TRANSMIT,
						(MessageEntry) currentEntry, null);
				
				HashValue key = new HashValue(addressPdu.getMessageId(),
						addressPdu.getSourceID());
				tmp.initAckedList();
				rdt.inMessages.put(key, tmp);
				// Initialize ack matrix
				
				System.err.println("Node id creation: " + addressPdu.getSourceID());
				System.err.println("Message id creation: " + addressPdu.getMessageId());
				
			}
			
			//Send pdus
			ArrayList <Pdu> pdus = tmp.getPdus();
			for (Pdu pdu : pdus) {
				if (pdu != null) {
					try {
                        rdt.sendAddressDataPdu(pdu, tmp.getMulticastAddress());
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
				} else {
					// PDU was null, we can not deal with this.
					Log.writeLine(Log.LOG_LEVEL_NORMAL,
							"One of the PDUs in messageEntry was null!");
				}
			}
			

			/*
			 * Check the emcon state of our destinations, and act accordingly.
			 */
            boolean allInEmcon = true;
            for(Integer sourceID : tmp.getRecipients()){
                if(rdt.isEmcon(sourceID)){
                    atLeastOneInEMCON = true;
                } else {
                    allInEmcon = false;
                }
            }

			// Start re_transmission timer as long this message is not part of a
			// re-transmission session
			if (tmp.getState() != States.RE_TRANSMITTING && !allInEmcon) {
			    			    
				Log.writeLine(Log.LOG_LEVEL_DEBUG,
						"Started retransmission timer");
				
				rdt.initializeTimer(
						Configuration.getAckRetransmissionTime(),
						TimerType.RETRANSMISSION_TIMER,
						(MessageEntry) currentEntry, null);
			}
			
			if(atLeastOneInEMCON && tmp.getState() != States.EMCON_RE_TRANSMISSION){
				//One or more nodes are in EMCON
				//Start EMCON re-transmission timer
				rdt.initializeTimer(Configuration.getEmconRti(), TimerType.EMCON_RETRANSMISSION_TIMER, tmp, null);
			}
			
			//Finished transmitting, switch to waiting for ack mode
			tmp.setState(States.WAIT_FOR_ACK);

		} else if (currentEntry.getType() == EntryType.PACKET_ENTRY) {

			ArrayList<Pdu> packets = currentEntry.getPdus();
			for (Pdu pdu : packets) {
				if(pdu == null){
					continue;
				}
				if (pdu.getPduType() == Pdu.Announce_PDU) {
					rdt.sendAnnouncePdu( pdu);
					Log.writeLine(Log.LOG_LEVEL_DEBUG, "Send Announce");
				} else if (pdu.getPduType() == Pdu.Ack_PDU) {
					Log.writeLine(Log.LOG_LEVEL_DEBUG, "Send ack");
			        try {
                        rdt.sendAckPdu( pdu, ((PacketEntry) currentEntry).getDestinationAddress());
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
					//currentEntry = null;
				} else if (pdu.getPduType() == Pdu.Request_PDU) {
					Log.writeLine(Log.LOG_LEVEL_DEBUG, "Send request");
					try {
						rdt.sendRequestRelease( pdu, Configuration.getGg());
					} catch (UnknownHostException e) {
						Log.writeLine(Log.LOG_LEVEL_QUIET, "GG address threw an UnknownHostException");
					}

				} else if (pdu.getPduType() == Pdu.Release_PDU) {
					rdt.sendRequestRelease(pdu, ((PacketEntry) currentEntry).getDestinationAddress());
					Log.writeLine(Log.LOG_LEVEL_DEBUG, "Send Release");
				} else if (pdu.getPduType() == Pdu.Reject_PDU) {
					Log.writeLine(Log.LOG_LEVEL_DEBUG, "Send Reject");
					rdt.sendReject(pdu, ((PacketEntry) currentEntry).getDestinationAddress());

				} else if (pdu.getPduType() == Pdu.Discard_Message_PDU) {
				    Log.writeLine(Log.LOG_LEVEL_DEBUG, "Send Discard");
				    try {
                        rdt.sendDiscard( pdu, ((PacketEntry) currentEntry).getDestinationAddress());
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
				}

			}
			currentEntry = null;
		}
	}
	
	/**
	 * This method will prepare a requestPdu and pass it to the send handler.
	 * 
	 * @param currentEntry the message entry this requestPdu should represent.
	 */
	private void prepareRequestPdu(MessageEntry currentEntry) {
		// Tag the message entry
		currentEntry.setDynamicGroupTag(true);

		// Create request and send
		AddressPdu addressPdu = currentEntry.getAddressPdu();
		int sourceId = addressPdu.getSourceID();
		int messageId = addressPdu.getMessageId();

		InetAddress destination = Libjpmul.getRandomMulticastGroup();
		BigInteger intAddress = new BigInteger(destination.getAddress());
		RequestPdu pdu = RequestPdu.create(
				sourceId, messageId, intAddress.intValue());
		MulticastGroup multicastGroup = new MulticastGroup(destination);
		rdt.dynamicMulticast.put(destination, multicastGroup);
		PacketEntry packetEntry = new PacketEntry();
		packetEntry.addRequestReleaseReject(pdu);
		Log.writeLine(Log.LOG_LEVEL_DEBUG, "Sending request");
		rdt.outMessages.add(packetEntry);

		// Assume that we are going to use this multicast group, so create the
		// announcePdu

		Log.writeLine(Log.LOG_LEVEL_DEBUG, "Creating announcePdu");
		HashValue key = new HashValue(messageId, sourceId);
		// create new AnnouncePdu
		ArrayList<DestinationEntry> dest = addressPdu.getDestinationEntries();
		int[] destinationIds = new int[dest.size()];
		for (int i = 0; i < dest.size(); i++) {
			destinationIds[i] = dest.get(i).getDestinationID();
		}
		int expiryTime = (int) addressPdu.getExpiryTime();
		ArrayList<AnnouncePdu> announcePdus = AnnouncePdu.create(
				addressPdu.getSourceID(), addressPdu.getMessageId(),
				expiryTime, intAddress.intValue(), destinationIds);

		for (AnnouncePdu announcePdu : announcePdus) {
			currentEntry.addAnnouncePdu(announcePdu);
		}
		
		//Set announce ct
		currentEntry.setAnnounceCt(Configuration.getAnnounceCt());

		currentEntry.setMulticastAddress(destination);
		rdt.inMessages.put(key, currentEntry);
		// Stop timer if any
		Log.writeLine(Log.LOG_LEVEL_DEBUG, "Stop timer if any");
		rdt.stopTimer(TimerType.WAIT_FOR_REJECT_TIME, currentEntry);
		// And start timer
		Log.writeLine(Log.LOG_LEVEL_DEBUG,
				"Start timer " + Configuration.getWaitForRejectTime());
		rdt.initializeTimer(Configuration.getWaitForRejectTime(),
				TimerType.WAIT_FOR_REJECT_TIME, currentEntry, null);

	}
}
