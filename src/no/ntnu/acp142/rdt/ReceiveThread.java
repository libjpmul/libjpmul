package no.ntnu.acp142.rdt;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import no.ntnu.acp142.Libjpmul;
import no.ntnu.acp142.Configuration;
import no.ntnu.acp142.Log;
import no.ntnu.acp142.pdu.AckPdu;
import no.ntnu.acp142.pdu.AckPdu.AckInfoEntry;
import no.ntnu.acp142.pdu.AddressPdu;
import no.ntnu.acp142.pdu.AddressPdu.DestinationEntry;
import no.ntnu.acp142.pdu.AnnouncePdu;
import no.ntnu.acp142.pdu.DataPdu;
import no.ntnu.acp142.pdu.DiscardMessagePdu;
import no.ntnu.acp142.pdu.Pdu;
import no.ntnu.acp142.pdu.RejectPdu;
import no.ntnu.acp142.pdu.ReleasePdu;
import no.ntnu.acp142.pdu.RequestRejectReleasePdu;
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
 * This class contains all functionality required to receive P_MUL packets.
 * 
 * @author Bjørn Tungesvik, Karl Mardoff Kittilsen
 * 
 */

public class ReceiveThread extends Thread {

	/**
	 * Reference to the ReliableTransferClass
	 */
	private ReliableDataTransfer rdt;
    
	/**
	 * Contains the the mapping from <messageId, sourceId> to InetAddresses currently active
	 */
	ConcurrentHashMap<HashValue, InetAddress> multicastAddressesInUse;
	
	/**
	 * Creates an instance of the ReceiveThread
	 * 
	 * @param rdt reference to ReliableDataTransfer
	 */
	public ReceiveThread(ReliableDataTransfer rdt) {
		this.rdt = rdt;
		multicastAddressesInUse = new ConcurrentHashMap<HashValue, InetAddress>();
	}

	/**
	 * The main thread loop. Get packets from the UDPLayer and processes them.
	 * 
	 */
	@Override
	public void run() {
		Log.writeLine(Log.LOG_LEVEL_DEBUG, "Starting receive thread");
		while (true) {
			// blocking = No need to sleep
			try {
				Tuple<InetAddress, byte[]> tmp = rdt.receive();
				Pdu packet = Pdu.parsePDU(tmp.t2);
				if (packet == null) {
				    Log.writeLine(Log.LOG_LEVEL_DEBUG, "Packet is null");
					continue;
				}
				Tuple<InetAddress, Pdu> data = new Tuple<InetAddress, Pdu>(
						tmp.t1, packet);
				// handler
				Log.writeLine(Log.LOG_LEVEL_DEBUG, "Received packet");
				receiveHandler(data);
			} catch (InterruptedException e) {

			}

		}
	}

    /**
     * Handles PDU packets and applies the appropriate operation depending on
     * its type
     * 
     * @param data
     *            consisting of the source address and Pdu, respectively.
     * 
     * @throws InterruptedException
     *             if we fail to put a complete message in the handleAddressPdu
     *             or handleDataPdu method, into the completeMessages queue of
     *             Libjpmul.
     */

	private void receiveHandler(Tuple<InetAddress, Pdu> data)
			throws InterruptedException {
		byte PDUType = data.t2.getPduType();
		Log.writeLine(Log.LOG_LEVEL_DEBUG, "Entering receive handler");
		switch (PDUType) {
		case Pdu.Ack_PDU:
			Log.writeLine(Log.LOG_LEVEL_DEBUG, "Handle received AckPdu");
			handleAckPdu(data);
			break;
		case Pdu.Address_PDU:
			Log.writeLine(Log.LOG_LEVEL_DEBUG, "Handle received AddressPdu");
			handleAddressPdu(data);
			break;
		case Pdu.Announce_PDU:
			Log.writeLine(Log.LOG_LEVEL_DEBUG, "Handle received AnnouncePdu");
			handleAnnouncePdu(data);
			break;
		case Pdu.Data_PDU:
			Log.writeLine(Log.LOG_LEVEL_DEBUG, "Handle received DataPdu");
			handleDataPdu(data);
			break;
		case Pdu.Discard_Message_PDU:
			Log.writeLine(Log.LOG_LEVEL_DEBUG, "Handle received DiscardMessagePdu");
			handleDiscardMessagePdu(data);
			break;
		case Pdu.Reject_PDU:
			Log.writeLine(Log.LOG_LEVEL_DEBUG, "Handle received RejectPdu");
			handleRejectMessagePdu(data);
			break;
		case Pdu.Release_PDU:
			Log.writeLine(Log.LOG_LEVEL_DEBUG, "Handle received ReleasePdu");
			handleReleasePdu(data);
			break;
		case Pdu.Request_PDU:
			Log.writeLine(Log.LOG_LEVEL_DEBUG, "Handle received RequestPdu");
			handleRequestPdu(data);
			break;
		}

	}

	/**
	 * Check if we exist in the recipients list of the AddressPdu or AnnouncePdu
	 * 
	 * @param pdu address or announce pdu to check
	 * @return true if the client is in recipients, and false otherwise
	 */
	private boolean isDestination(Pdu pdu) {
		switch (pdu.getPduType()) {
		case Pdu.Address_PDU:
			AddressPdu addressPdu = (AddressPdu) pdu;
			ArrayList<DestinationEntry> destinationEntries = addressPdu
					.getDestinationEntries();
			for (DestinationEntry destinationEntry : destinationEntries) {
				if (destinationEntry.getDestinationID() == Configuration
						.getNodeId()) {
					return true;
				}
			}
			return false;

		case Pdu.Announce_PDU:
			AnnouncePdu announcePdu = (AnnouncePdu) pdu;
			int[] destinationIds = announcePdu.getDestinationIds();
			for (int i : destinationIds) {
				if (i == Configuration.getNodeId()) {
					return true;
				}
			}
			return false;

		}

		return false;

	}

	/**
	 * Handle a received AddressPdu<br><br>
	 * 
	 * a) On receipt of an Address_PDU the receiving node shall first check
	 * whether the Address_PDU with the same tuple "Source_ID, MSID" has already
	 * been received.<br><br>
	 * 
	 * b) If such an Address_PDU has already been received the receiving node
	 * shall check whether it has previously sent a message complete Ack_PDU for
	 * this message.<br><br>
	 * 
	 * (1)
	 * 
	 * (a) if its own ID is not in the list of Destination_Entries, it knows
	 * that its own Ack_PDU has been successfully received by the transmitting
	 * node. Consequently, the receiving node can release all information about
	 * this message and its membership of the dynamically created multicast
	 * group, and can then discard the Address_PDU, or<br><br>
	 * 
	 * (b) if its own ID exists in the list of Destination_Entries, re-transmit
	 * the message complete Ack_PDU, and discard the Address_PDU, or<br><br>
	 * 
	 * (2) if it has not sent a message complete, Ack_PDU, discard the
	 * Address_PDU and wait for remaining Data_PDUs.<br><br>
	 * 
	 * 
	 * c) If the Address_PDU has not been previously received the receiving node
	 * shall either:<br><br>
	 * 
	 * (1) if its own ID is not in the list of Destination_Entries, the
	 * receiving node shall check whether it has previously received any
	 * Data_PDUs associated with this Address_PDU (ie same Source_ID and
	 * Message_ID) and then discard the Address_PDU. If there are Data_PDUs
	 * associated with this Address_PDU, the receiving node shall discard these
	 * Data_PDUs, or<br><br>
	 * 
	 * (2) if its own ID is in the list of Destination_Entries, determine
	 * whether it has previously received any Data_PDUs associated with this
	 * Address_PDU.<br><br>
	 * 
	 * (a) If there are no Data_PDUs associated with this Address_PDU, the
	 * receiving node shall create a message entry and wait transmission of
	 * associated Data-PDUs.<br><br>
	 * 
	 * (b) If there are Data_PDUs associated with this Address_PDU, the
	 * receiving node shall update the status of the Data_PDU entry (see para
	 * 313) to a message entry. The receiving node shall stop the
	 * "Unidentified_Data_PDU_Validity_Timer" (see para 314), and initialise the
	 * "Receiver Expiry_Time Timer". The receiving node shall determine whether
	 * to enter the "Acknowledgement of a Message" mode (see para 315).<br><br>
	 * 
	 * @param packet packet tuple containing the source address for this packet, and actual packet.
	 * @throws InterruptedException if interrupted while waiting to put the complete message
	 * in the completedMessages queue of Libjpmul.
	 */

	private void handleAddressPdu(Tuple<InetAddress, Pdu> packet)
			throws InterruptedException {
		AddressPdu addressPdu = (AddressPdu) packet.t2;
		HashValue key = new HashValue(addressPdu.getMessageId(),
				addressPdu.getSourceID());

		// Calculate ack delay
		Random random = new Random();
		long endTime = random.nextInt((int) Configuration
				.getAckDelayUpperBound());

		if (!isDestination(addressPdu) && addressPdu.getSourceID() != Configuration.getNodeId()) {
			//We are not supposed to get this message
			
			//Check if we have previously received any dataPdu associated with this addressPdu
			if(rdt.inMessages.containsKey(key)){
				MessageEntry currentEntry = rdt.inMessages.get(key);
				rdt.stopTimer(TimerType.UNIDENTIFIED_DATA_DPU_VALIDITY_TIMER, currentEntry);
				rdt.inMessages.remove(key);
			}
			return;
		}
		
		
		if(rdt.inMessages.containsKey(key)){
			Log.writeLine(Log.LOG_LEVEL_DEBUG, "Got a address PDU, and we already got some data PDUs.");
			MessageEntry currentEntry = rdt.inMessages.get(key);
			currentEntry.setAckAddress(packet.t1);
			
			if (currentEntry.getAddressPdu() == null) {
				// We already have dataPdus for this message
				currentEntry.addAddressPdu(addressPdu);
				rdt.stopTimer(TimerType.UNIDENTIFIED_DATA_DPU_VALIDITY_TIMER,
						currentEntry);
				
				
				if(rdt.EMCON.get() == false){
					rdt.initializeTimer(endTime, TimerType.ACK_DELAY, currentEntry, null);
					
					if(currentEntry.dataReady()){
						rdt.initializeTimer(endTime, TimerType.COMPLETE_ACK_TIMER, currentEntry, null);
						currentEntry.setState(States.COMPLETE);
						Libjpmul.completedMessages.put(currentEntry);
						rdt.stopTimer(TimerType.ACK_TIMER, currentEntry);
					}
				
				}
				//Nothing more to do
				return;

			} else {
				// AddressPDU already received
	            if (isDuplicateAddressPdu(addressPdu)){
	                currentEntry.setState(States.RE_TRANSMITTING);
					if (currentEntry.dataReady()) {
						// Already sent ack
						if(rdt.EMCON.get() == false){
							rdt.initializeTimer(endTime, TimerType.COMPLETE_ACK_TIMER,
									currentEntry, null);
						}
						return;
					} else {
						// Ack not sent, message not complete
						return;
					}
				}

			}
		
		}else {
			Log.writeLine(Log.LOG_LEVEL_DEBUG, "Received the addressPdu for the first time");
			MessageEntry newMessageEntry = new MessageEntry();
			newMessageEntry.addAddressPdu(addressPdu);
			newMessageEntry.setState(States.RECEIVING);
			rdt.inMessages.put(key, newMessageEntry);
			long currentUnixTime = System.currentTimeMillis()/1000;
			
			if (currentUnixTime > addressPdu.getExpiryTime()){
				Log.writeLine(Log.LOG_LEVEL_DEBUG, "Discarded because it has expired");
				return;
			}else {
				long diff = addressPdu.getExpiryTime() - currentUnixTime;
				rdt.initializeTimer(diff * 1000L, TimerType.EXPIRY_TIMER_RECEIVE, newMessageEntry, null);
				
			}
		}
		
	}

	/**
	 * Check if the AddressPdu is already received.
	 * 
	 * @param addressPdu to check if already received
	 * @return true if duplicate, and false otherwise.
	 */
	private boolean isDuplicateAddressPdu(AddressPdu addressPdu) {
		HashValue key = new HashValue(addressPdu.getMessageId(), addressPdu.getSourceID());
		
		MessageEntry entry = rdt.inMessages.get(key);
		return (entry != null && entry.getAddressPdu() != null);
	}

	/**
	 * Check if we have already received this data PDU. 
	 * 
	 * @param currentEntry Entry to check if contains the data pdu
	 * @param dataPdu to search for
	 * @return true if duplicate, and false otherwise
	 */
	private boolean isDataPduReceived(MessageEntry currentEntry, DataPdu dataPdu) {
		ArrayList<Pdu> pdus = currentEntry.getPdus();
		for (Pdu pdu : pdus) {
			if (pdu == null) {
				continue;
			}
			if (pdu.getPduType() == Pdu.Address_PDU) {
				continue;
			}
			DataPdu d = (DataPdu) pdu;
			
			if (d.equals(dataPdu)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Handle a received DataPDU.
	 * 
	 * a) First check whether this PDU has already been received
	 * 
	 * b) If the Data_PDU has already been received the receiving node shall
	 * either:
	 * 
	 * (1) if the receiving node has not received a duplicate of the associated
	 * Address_PDU (see para 311) and it has previously sent a
	 * "message complete" Ack_PDU for this message, re-transmit the
	 * "message complete" Ack_PDU and discard the Data_PDU, or
	 * 
	 * (2) otherwise, discard the Data_PDU.
	 * 
	 * c) If the Data_PDU has not been previously received, the receiving node
	 * shall check whether it has received the associated Address_PDU.
	 * 
	 * (1) If the associated Address_PDU has been received and a message entry
	 * exists, the receiving node shall update the status of the message entry
	 * and determine whether to enter the "Acknowledgement of a Message" mode
	 * (see para 315). If the associated Address_PDU has been received but no
	 * message entry exists, the receiving node shall discard the Data_PDU.
	 * 
	 * (2) If the associated Address_PDU has not yet been received the receiving
	 * node shall check whether there is a Data_PDU entry associated with the
	 * Source_ID and Message_ID contents of the received Data_PDU. If there is
	 * no Data_PDU entry associated with this Data_PDU, the receiving node shall
	 * create a Data_PDU entry and await transmission of the associated
	 * Address_PDU. In addition, the receiving node shall initialise a
	 * "Unidentified_Data_PDUValidity_Timer". If there is a Data_PDU entry
	 * associated with this Data_PDU, the receiving node shall update the status
	 * of the Data_PDU entry and await transmission of the associated
	 * Address_PDU.
	 * 
	 * @param packet packet tuple containing the source address for this packet, and actual packet.
	 * @throws InterruptedException if interrupted while waiting to put the complete message
     * in the completedMessages queue of Libjpmul.
	 */

	private void handleDataPdu(Tuple<InetAddress, Pdu> packet)
			throws InterruptedException {

		DataPdu dataPdu = (DataPdu) packet.t2;
		HashValue key = new HashValue(dataPdu.getMessageId(),
				dataPdu.getSourceID());
		MessageEntry currentEntry;

		if (rdt.inMessages.containsKey(key)) {
			// The entry exists
			Log.writeLine(Log.LOG_LEVEL_DEBUG, "Entry exists");
			currentEntry = rdt.inMessages.get(key);
			// Set ack address
			currentEntry.setAckAddress(packet.t1);
			if (isDataPduReceived(currentEntry, dataPdu)) {
				// This is a duplicate -> discard
				Log.writeLine(Log.LOG_LEVEL_DEBUG, "Received duplicate");
				if(currentEntry.getAddressPdu() != null && currentEntry.getMissingDataSequenceNumbers().size() > 0){
					Random random = new Random();
					long endTime = random.nextInt((int)Configuration.getAckDelayUpperBound());
					rdt.initializeTimer(endTime, TimerType.ACK_DELAY, currentEntry, null);
				}
				currentEntry.setState(States.RE_TRANSMITTING);
				return;
			} else {
				// Not received before
				if (currentEntry.getAddressPdu() == null) {
					// Message entry exists, but contain no addressPdu
					Log.writeLine(Log.LOG_LEVEL_DEBUG,
							"Contains no address pdu");
					currentEntry.addDataPdu(dataPdu);
					
					Random random = new Random();
					long endTime = random.nextInt((int)Configuration.getAckDelayUpperBound());
					rdt.initializeTimer(endTime, TimerType.ACK_DELAY, currentEntry, null);
					
					
				} else {
					
					ArrayList<Integer> missing = currentEntry.getMissingDataSequenceNumbers();
					Integer highestMissing = 0;
					for (Integer integer : missing) {
						if (integer > highestMissing){
							highestMissing = integer;
						}
						
						Log.writeLine(Log.LOG_LEVEL_DEBUG ,"Missing " + integer);
						
					}
					Log.writeLine(Log.LOG_LEVEL_DEBUG, "Highest missing: " + highestMissing);
					currentEntry.setHighestMissingSequenceNumber(highestMissing);
					
					// There is an addressPdu
					Log.writeLine(Log.LOG_LEVEL_DEBUG, "Have an AddressPdu");
					currentEntry.addDataPdu(dataPdu);
					if (currentEntry.dataReady()) {
						Log.writeLine(Log.LOG_LEVEL_DEBUG, "Data is ready");
						Libjpmul.completedMessages.put(currentEntry);
						currentEntry.setState(States.COMPLETE);
						rdt.stopTimer(TimerType.ACK_TIMER, currentEntry);
						
		                Random random = new Random();
		                long endTime = random.nextInt((int)Configuration.getAckDelayUpperBound());
		                if (!rdt.EMCON.get()) {
		                    rdt.initializeTimer(endTime, TimerType.COMPLETE_ACK_TIMER, currentEntry, null);
		                }	
					}
					
					Random random = new Random();
					long endTime = random.nextInt((int)Configuration.getAckDelayUpperBound());
					
					if(!rdt.EMCON.get() && currentEntry.getState() != States.COMPLETE){
						rdt.initializeTimer(endTime, TimerType.ACK_DELAY,
								currentEntry, null);
					}else if (rdt.EMCON.get()) {
						/*
						 *In EMCON. Store the reference of the entry.
						 *It will be used for later when leaving EMCON 
						 */
						rdt.readyToAckEmcon.put(key, currentEntry);
					}
				}
			}
			currentEntry.setLastReceivedDataPdu(dataPdu);

		} else {
			// Message entry for this dataPdu does not exist
			Log.writeLine(Log.LOG_LEVEL_DEBUG,
					"We did not find any entry with this sourceId and Msid");
			currentEntry = new MessageEntry();
			currentEntry.addDataPdu(dataPdu);
			
			currentEntry.setLastReceivedDataPdu(dataPdu);
			currentEntry.setState(States.RECEIVING);
			
			rdt.inMessages.put(key, currentEntry);
			rdt.initializeTimer(
					Configuration.getUndefinedPduExpiryTime(),
					TimerType.UNIDENTIFIED_DATA_DPU_VALIDITY_TIMER,
					currentEntry, null);
		}

	}

	/**
	 * Handle a received AckPdu. The entries in the AckPdu is acknowledgment if
	 * inMessages contain an entry with this message, otherwise they are
	 * ignored.
	 * 
	 * @param packet packet tuple containing the source address for this packet, and actual packet.
	 */
	private void handleAckPdu(Tuple<InetAddress, Pdu> packet) {
		Log.writeLine(Log.LOG_LEVEL_DEBUG, "Handle received ack");
		AckPdu ackPdu = (AckPdu) packet.t2;
		// int sourceId = ackPdu.getSourceID();
		int sourceId = Configuration.getNodeId();
		MessageEntry currentEntry = null;
		AckInfoEntry[] allAckInfoEntries = ackPdu.getAckInfoEntries();

		ArrayList <Integer> emconIndexes = new ArrayList<Integer>();
		
		for (AckInfoEntry ackInfoEntry : allAckInfoEntries) {
			HashValue key = new HashValue(ackInfoEntry.getMessageID(), sourceId);
			if (rdt.inMessages.containsKey(key)) {
				currentEntry = rdt.inMessages.get(key);
				currentEntry.setAcked(ackInfoEntry);
			}
		}

		if (currentEntry == null) {
		    // This is an ack to some message we have discarded
		    // or otherwise don't know about, in any case, ignore it.
		    return;
		}
		
		//Find every position in the ack table affected by nodes in EMCON
		ArrayList <Integer> recipients = currentEntry.getRecipients();
		for(int i = 0; i < recipients.size(); i++){
			if(rdt.libjpmul.isEmcon(recipients.get(i))){
				emconIndexes.add(i);
			}
		}
		
		boolean emcon = false;
		// Check if everything is acked
		boolean complete = true;
        boolean[][] acked = currentEntry.getAckedMatrix();
        for (int i = 0; i < acked.length; i++) {
            //If this node is in EMCON, we ignore it
            if(emconIndexes.contains(i)){
                Log.writeLine(Log.LOG_LEVEL_DEBUG, i + " is in emcon.");
                emcon = true;
                continue;
            }

            for (int j = 1; j < acked[i].length; j++) {
                if (acked[i][j] == false) {
                    complete = false;
                }
            }
        }

        //if we received an ack from a source, and no other sources are in EMCON we can safely remove the timer
        if(emconIndexes.isEmpty()){
            rdt.stopTimer(TimerType.EMCON_RETRANSMISSION_TIMER, currentEntry);
        }

        rdt.initializeTimer(Configuration.getAckRetransmissionTime(), TimerType.RETRANSMISSION_TIMER, currentEntry, null);

        // Stop re-transmission timer if we are complete
        if (complete) {
            Log.writeLine(Log.LOG_LEVEL_DEBUG, "Stop re-transmission timer.");
            rdt.stopTimer(TimerType.RETRANSMISSION_TIMER, currentEntry);
        }

        CopyOnWriteArraySet<MessageEntry> entries = null;
        if (currentEntry.getMulticastAddress() != null && rdt.persistentGroupSessions.get(currentEntry.getMulticastAddress()) != null) {
             entries = rdt.persistentGroupSessions.get(currentEntry.getMulticastAddress()).t1;
        }

        if(entries != null && entries.size() == 1 && complete){
            //We are the last message using this multicast group
            if(currentEntry.getMulticastAddress() != null && rdt.persistentGroupSessions.get(currentEntry.getMulticastAddress()).t2.get() == false){
                prepareReleasePdu(currentEntry.getMulticastAddress(), currentEntry.getAddressPdu().getMessageId());
                rdt.persistentGroupSessions.remove(currentEntry.getMulticastAddress());
                rdt.persistentGroups.remove(currentEntry.getRecipients().hashCode());
                System.err.println("We where the last to use this, and persistent where false.");
                return;
            } else {
                entries.remove(currentEntry);
            }

        }else if(entries != null && entries.size() > 1 && complete){
            entries.remove(currentEntry);
            currentEntry.setPersistentMulticastGroup(true);
            System.err.println("more ppl is using this, or we still set persistent true.");
        }

        // send release group when message is complete
        if (currentEntry.usesDynamicGroup() && complete
                && currentEntry.isPersistentMulticastGroups() == false && !emcon) {
                prepareReleasePdu(currentEntry.getAnnouncePdu().getInetMulticastGroupAddress(), currentEntry.getAddressPdu().getMessageId());
        }

	}

	/**
	 * As soon as a member of R_Nodes has received the Announce_PDU, it decides
	 * whether it is a member of the announced group, based on whether it is
	 * listed in the list of Destination_IDs. If it is not in the list if
	 * Destination_IDs it can ignore the Announce_PDU; otherwise it must join
	 * the multicast group denoted by the Announce_PDU.
	 * 
	 * @param packet packet tuple containing the source address for this packet, and actual packet.
	 */

	private void handleAnnouncePdu(Tuple<InetAddress, Pdu> packet) {

		AnnouncePdu pdu = (AnnouncePdu) packet.t2;
		InetAddress address = pdu.getInetMulticastGroupAddress();

		if (!isDestination(pdu)) {
			return;
		} else {
			rdt.udpWrapper.joinMulticastGroup(address);
			multicastAddressesInUse.put(new HashValue(pdu.getMessageId(), pdu.getSourceID()), address);
		}
		
		

	}

	/**
	 * If a receiving node receives a Discard_Message_PDU, it shall discard all
	 * the PDUs (ie data and address) associated with the message identified by
	 * the combination of the Source_ID and Message_ID fields in the
	 * Discard_Message_PDU, and stop any associated timers. If a special
	 * multicast group has been dynamically created for this specific message,
	 * the receiving node shall release this group immediately. When using persistent 
	 * groups this method will also check whether is possible to leave the group, but
	 * will not leave it if it is used by some other message.
	 * 
	 * @param packet tuple containing the source address for this packet, and actual packet.
	 */

	private void handleDiscardMessagePdu(Tuple<InetAddress, Pdu> packet) {
		DiscardMessagePdu discardMessagePdu = (DiscardMessagePdu) packet.t2;
		HashValue key = new HashValue(discardMessagePdu.getMessageId(),
				discardMessagePdu.getSourceID());
		if (rdt.inMessages.containsKey(key)) {
			//This is only relevant if we are using dynamic or persistent multicast groups
			InetAddress groupAddress = multicastAddressesInUse.get(key);
			int groupAddressInteger = ByteBuffer.wrap(groupAddress.getAddress()).getInt();
			checkMulticastUsageAndLeave(discardMessagePdu, rdt.udpWrapper.getCurrentMulticastGroups(), groupAddressInteger);
			
			//Remove message 
			rdt.inMessages.remove(key);
		}
	}

	/**
	 * Create a releasePDU and put it in the outMessages queue.
	 *
	 * @param multicastAddress destination of the message.
     * @param messageId of the message to send.
	 */
	protected synchronized void prepareReleasePdu(InetAddress multicastAddress, int messageId) {
	    RequestRejectReleasePdu release = null;
            release = ReleasePdu
                    .create(Configuration.getNodeId(), messageId,
                            ByteBuffer.wrap(multicastAddress.getAddress()).getInt());
		PacketEntry entry = new PacketEntry();
		try {
            entry.addDestinationAddress(Configuration.getGg());
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		entry.addRequestReleaseReject(release);
		rdt.outMessages.add(entry);
	}

	/**
	 * If the requesting node receives a RejectPdu it revokes its request by
	 * transmitting a ReleasePDU, and renew its request with a different
	 * multicast group.
	 * 
	 * @param packet packet tuple containing the source address for this packet, and actual packet.
	 */
	private void handleRejectMessagePdu(Tuple<InetAddress, Pdu> packet) {
		RejectPdu pdu = (RejectPdu) packet.t2;
		
		try {
            prepareReleasePdu(InetAddress.getByAddress(BigInteger.valueOf(pdu.getMulticastGroupAddress()).toByteArray()), pdu.getMessageId());
            } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		HashValue key = new HashValue(pdu.getMessageId(), pdu.getSourceID());
		MessageEntry currentEntry = rdt.inMessages.get(key);
		rdt.stopTimer(TimerType.WAIT_FOR_REJECT_TIME, currentEntry);
		currentEntry.setMulticastAddress(null);
		Log.writeLine(Log.LOG_LEVEL_DEBUG, "The multicast group was rejected");
		// Renew request
		rdt.outMessages.add(currentEntry);

	}

	/**
	 * When getting a ReleasePdu check if we can safely leave the group. 
	 * @param packet packet tuple containing the source address for this packet, and actual packet.
	 */
	private void handleReleasePdu(Tuple<InetAddress, Pdu> packet) {
		ReleasePdu pdu = (ReleasePdu) packet.t2;
		CopyOnWriteArrayList<InetAddress> memberAddresses = rdt.udpWrapper
				.getCurrentMulticastGroups();
		checkMulticastUsageAndLeave(pdu, memberAddresses);

	}

	/**
	 * Check if we can safely leave the multicast group located in the received ReleasePdu.
	 * If we are currently using the group in question, this method will not leave the group.
	 * 
	 * 
	 * @param pdu Contains the release information for the group
	 * @param memberAddresses All multicast addresses we are currently listening on.
	 */
	private void checkMulticastUsageAndLeave(ReleasePdu pdu,
			CopyOnWriteArrayList<InetAddress> memberAddresses) {
		int releasePduMulticastGroup = pdu.getMulticastGroupAddress();
		HashValue key = new HashValue(pdu.getMessageId(), pdu.getSourceID());
		for (InetAddress memberAddress : memberAddresses) {
			int myMulticastGroup = ByteBuffer.wrap(memberAddress.getAddress()).getInt();
			if (myMulticastGroup == releasePduMulticastGroup) {
			    // We are currently using this address, make sure
			    // the release pdu corresponds to a complete message.
                //Check magic number, if magic number is detected; leave group
				if ( pdu.getMessageId() == 0 || rdt.inMessages.containsKey(key) && rdt.inMessages.get(key).dataReady() ) {
                    multicastAddressesInUse.remove(key);
                    if ( !multicastGroupInUse(memberAddress) ) {
                        rdt.leaveMulticastGroup(memberAddress);
                    } else {
                        Log.writeLine(Log.LOG_LEVEL_VERBOSE, "We got a releasePDU for a multicast" +
                        		"group that we are currently using in a message entry. Don't leave it.");
                    }
                }
			}
		}
		
	}
	
	/**
	 * When using persistent groups we need to make sure we can safely leave
	 * the group. If other messages are currently using the group in question, this method will
	 * not leave the group.
	 * 
	 * @param pdu
	 *            DiscardMessagePdu
	 * @param memberAddresses
	 *            All multicast groups we are currently listening to
	 * @param releasePduMulticastGroup
	 *            the we potentially want to leave
	 */
	
	private void checkMulticastUsageAndLeave(DiscardMessagePdu pdu,
			CopyOnWriteArrayList<InetAddress> memberAddresses, int releasePduMulticastGroup) {
		HashValue key = new HashValue(pdu.getMessageId(), pdu.getSourceID());
		for (InetAddress memberAddress : memberAddresses) {
			int myMulticastGroup = ByteBuffer.wrap(memberAddress.getAddress()).getInt();
			if (myMulticastGroup == releasePduMulticastGroup) {
			    // We are currently using this address, make sure
			    // the release pdu corresponds to a complete message.
                //Check magic number, if magic number is detected; leave group
				if ( pdu.getMessageId() == 0 || rdt.inMessages.containsKey(key) && rdt.inMessages.get(key).dataReady() ) {
                    multicastAddressesInUse.remove(key);
                    if ( !multicastGroupInUse(memberAddress) ) {
                        rdt.leaveMulticastGroup(memberAddress);
                    } else {
                        Log.writeLine(Log.LOG_LEVEL_VERBOSE, "We got a discardPDU for a multicast" +
                        		"group that we are currently using in a message entry. Don't leave it.");
                    }
                }
			}
		}
		
	}
	
	

	/**
	 * Iterate through the inMessages and see if we can safely leave this group.
	 * If it returns true, we got message entry/entries that is still in flight on this
	 * multicast group. If no message entry is found, or all that are found have
	 * completed, return false.
	 * @param memberAddress Address to check.
	 * @return true or false
	 */
	private boolean multicastGroupInUse( InetAddress memberAddress ) {
	    Set<Map.Entry<HashValue, MessageEntry>> inMessagesKeySet = rdt.inMessages.entrySet();
        for (Map.Entry<HashValue, MessageEntry> entry : inMessagesKeySet) {
            if (!entry.getValue().dataReady()) {
                if (multicastAddressesInUse.containsValue(memberAddress)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
	 * Prepare a RejectPdu for sending, and put it in the outMessages queue.
	 *
     * @param pdu the reject pdu to send.
	 * @param destination where to send it.
	 */
	private void prepareRejectPdu(Pdu pdu, InetAddress destination) {
		if(rdt.EMCON.get()){
			//We see the conflict, but we can not complain
			return;
		}
		
		BigInteger destinationAddress = new BigInteger(destination.getAddress());
		RequestRejectReleasePdu request = (RequestRejectReleasePdu) pdu;
		RequestRejectReleasePdu reject = RejectPdu
				.create(request.getSourceID(), request.getMessageId(),
						destinationAddress.intValue());
		PacketEntry packetEntry = new PacketEntry();
		packetEntry.addRequestReleaseReject(reject);
		packetEntry.addDestinationAddress(destination);
		rdt.outMessages.add(packetEntry);

	}

	/**
	 * When receiving an RequestPdu determine if we are currently using this group.
	 * If not wait for the announcePdu announcing this group, else send reject to sender.
	 * 
	 * If we are currently in EMCON, this method will do nothing.
	 * @param packet packet tuple containing the source address for this packet, and actual packet.
	 */
	
	private void handleRequestPdu(Tuple<InetAddress, Pdu> packet) {
		RequestRejectReleasePdu pdu = (RequestRejectReleasePdu) packet.t2;
		int multicastAddress = pdu.getMulticastGroupAddress();

		if(rdt.EMCON.get()){
			//We are in EMCON, ignore
			return;
		}
		
		CopyOnWriteArrayList<InetAddress> addresses = rdt.udpWrapper
				.getCurrentMulticastGroups();
		for (InetAddress inetAddress : addresses) {
			BigInteger inGroup = new BigInteger(inetAddress.getAddress());
			if (inGroup.intValue() == multicastAddress) {
				// send reject
				prepareRejectPdu(packet.t2, packet.t1);
				return;
			}
		}

	}

}
