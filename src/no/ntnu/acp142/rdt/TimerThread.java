package no.ntnu.acp142.rdt;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import no.ntnu.acp142.Configuration;
import no.ntnu.acp142.Log;
import no.ntnu.acp142.pdu.AckPdu;
import no.ntnu.acp142.pdu.AckPdu.AckInfoEntry;
import no.ntnu.acp142.pdu.AddressPdu;
import no.ntnu.acp142.pdu.AddressPdu.DestinationEntry;
import no.ntnu.acp142.pdu.AnnouncePdu;
import no.ntnu.acp142.pdu.DataPdu;
import no.ntnu.acp142.pdu.DiscardMessagePdu;
import no.ntnu.acp142.udp.Tuple;

/*
 * Copyright (c) 2013, Bjørn Tungesvik,  Karl Mardoff Kittilsen
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
 * This class handles all logic associated with timers
 * 
 * @author Bjørn Tungesvik, Karl Mardoff Kittilsen
 * 
 */

public class TimerThread extends Thread {

	/**
	 * List of all timers currently active
	 */
	private ConcurrentLinkedDeque<Timer> timerList;


	/**
	 * Reference to ReliableDataTransfer
	 */
	private ReliableDataTransfer rdt;

	/**
	 * Create new instance of TimerThread
	 * @param rdt reference to the RDT layer.
	 */
	public TimerThread(ReliableDataTransfer rdt) {
		timerList = new ConcurrentLinkedDeque<Timer>();
		this.rdt = rdt;
	}

	/**
	 * Advances timers and calls the correct methods when they expire. Waits
	 * until next expiry of any timer, but is notified if a new timer is
	 * created or stopped.
	 */
	@Override
	public void run() {
		Log.writeLine(Log.LOG_LEVEL_DEBUG, "Started timer thread");
		long timestamp;
		long sleep;
		while (true) {
			try {
				Log.writeLine(Log.LOG_LEVEL_DEBUG, "Timer thread running");
				timestamp = System.currentTimeMillis();
				timerExpires(timestamp);
				if (timerList.isEmpty()) {
					Thread.sleep(10000);
				} else {

					sleep = findSleepTime();
					Log.writeLine(Log.LOG_LEVEL_DEBUG, "Sleep time: " + sleep);
					Thread.sleep(sleep);
				}

			} catch (InterruptedException e) {
			   //Intentionally left empty.
			}
		}

	}

	/**
	 * Removes any timer of the given type pertaining to the given MessageEntry and type. Notifies
	 * when the timer is removed. 
	 * 
	 * @param messageEntry
     *            MessageEntry to clear timer type for
	 * @param timerType
	 *            Type of timer to clear
	 */
	public void stopTimer(MessageEntry messageEntry, TimerType timerType) {
		for (Timer timer : timerList) {
		    
		    if (timer.getMsg() == null) {
		        // This timer is not linked do any message, and this makes no sense.
		        // Just drop it.
		        timerList.remove(timer);
		        continue;
		    }
		    
			if (timer.getMsg().equals(messageEntry) && timer.getType() == timerType) {
				Log.writeLine(Log.LOG_LEVEL_DEBUG, "Removing timer+++++++++++++++  " + timer.getType().toString());
				timerList.remove(timer);
				 
			}
		}
		this.interrupt();
	}
	/**
	 * Removes any timer pertaining to the given messageEntry list with the given type. 
	 * Notify when removing the timer.
	 * @param entries of MessageEntry.
	 * @param timerType 
	 *         Type of timer to clear.
	 */
	
	public void stopTimer(ArrayList <MessageEntry> entries, TimerType timerType){
		for(Timer timer : timerList){
			if(timer.getMessageEntryList() == null){
				continue;
			}
			if(timer.getMessageEntryList().hashCode() == entries.hashCode() && timer.getType() == timerType){
				timerList.remove(timer);
			}
		}
		this.interrupt();
	}
	

	/**
	 * Add the timer to the list and notify the thread.
	 * 
	 * @param timer to add.
	 */
	public void addTimer(Timer timer) {
	    Log.writeLine(Log.LOG_LEVEL_DEBUG, "Adding timer+++++++++++++++++++++ " + timer.getType().toString());
		
		timerList.add(timer);
		// Interrupt the sleep function
		this.interrupt();

	}

	/**
	 * Check for timers that have expired.
	 * 
	 * @param timestamp current unix time in milliseconds.
	 * @throws InterruptedException if we get interrupted while calling {@link #timeHandler}.
	 */
	public void timerExpires(long timestamp) throws InterruptedException {
		Log.writeLine(Log.LOG_LEVEL_DEBUG,
				"TimerList size: " + timerList.size());
		for (Timer timer : timerList) {
			long diff = timer.getStartTime() + timer.getEndTime();
			if (diff <= timestamp) {
				Log.writeLine(Log.LOG_LEVEL_DEBUG, "Timer expires");
				timeHandler(timer);
			}
		}
	}

	/**
	 * Goes through every timer and find the time in milliseconds the thread can
	 * sleep before it need to work.
	 * 
	 * @return minimum
	 */
	private long findSleepTime() {
		long min;
		long current;
		long timeStamp = System.currentTimeMillis();
		// First timer
		Timer first = timerList.getFirst();
		min = first.getEndTime() + first.getStartTime() - timeStamp;
		for (Timer timer : timerList) {
			timeStamp = System.currentTimeMillis();
			current = timer.getEndTime() + timer.getStartTime() - timeStamp;
			if (min > current) {
				min = current;
			}
		}

		if (min < 0) {
			return 0;
		}
		Log.writeLine(Log.LOG_LEVEL_DEBUG, "Sleep time: " + min);
		return min;
	}

	/**
	 * Determine timer type and execute the appropriate task.
	 * 
	 * @param timer to handle.
	 * @throws InterruptedException if we get interrupted while putting to {@link no.ntnu.acp142.rdt.ReliableDataTransfer#outMessages}.
	 */
	private void timeHandler(Timer timer) throws InterruptedException {
		switch (timer.getType()) {
		case EXPIRY_TIMER_RECEIVE:
			handleExpiryTimerReceive(timer);
			break;
		case EXPIRY_TIMER_TRANSMIT:
			handleExpiryTimerTransmit(timer);
			break;
		case RETRANSMISSION_TIMER:
			handleReTransmissionTimer(timer);
			break;
		case EMCON_RETRANSMISSION_TIMER:
			handleEmconRetransmissionTimer(timer);
			break;
		case ACK_TIMER:
			handleAckTimer(timer);
			break;
		case UNIDENTIFIED_DATA_DPU_VALIDITY_TIMER:
			handleUnidentifiedDataPduValidityTimer(timer);
			break;
		case WAIT_FOR_REJECT_TIME:
			handleWaitForRejectTimer(timer);
			break;
		case ANNOUNCE_DELAY:
			handleAnnounceDelay(timer);
			break;
		case ACK_DELAY:
			handleAckDelay(timer);
			break;
		case ANNOUNCE_RE_TRANSMISSION_TIMER:
			handleAnnounceReTransmission(timer);
			break;
			
		case COMPLETE_ACK_TIMER:
			//Same handling 
			handleCompleteAck(timer);
			break;
			
		default:
			Log.writeLine(Log.LOG_LEVEL_DEBUG, "Unknown timer");	
			

		}
	}

	/**
	 * Perform necessary tasks associated with the EXPIRY_TIMER_RECEIVE
	 * 
	 * This timer indicates the time remaining before the the contents of the
	 * received message is considered invalid.
	 * 
	 * If this timer expires before the receiving node has received all the
	 * Data_PDUs associated with a message, the receiving node shall discard the
	 * associated Data_PDUs and Address_PDU.
	 * 
	 * @param timer that expired.
	 */
	private void handleExpiryTimerReceive(Timer timer) {
		MessageEntry currentEntry = timer.getMsg();
		if(currentEntry == null){
			Log.writeLine(Log.LOG_LEVEL_DEBUG, "MessageEntry is NULL");
            return;
		}

        AddressPdu apdu = currentEntry.getAddressPdu();
        if ( apdu == null ) {
            Log.writeLine(Log.LOG_LEVEL_DEBUG, "MessageEntry's addressPDU is NULL");
            return;
        }
		int sourceId = apdu.getSourceID();
		int messageId = apdu.getMessageId();
		HashValue key = new HashValue(messageId, sourceId);
		rdt.inMessages.remove(key);
		
		stopTimer(timer.getMsg(), TimerType.EXPIRY_TIMER_RECEIVE);
	}

	/**
	 * Perform necessary tasks associated with the EXPIRY_TIMER_TRANSMIT
	 * 
	 * This timer indicates the time remaining before the contents of the
	 * transmitted message is considered invalid
	 * 
	 * If one or more of the receiving nodes have not acknowledge the receipt of
	 * the complete message when this timer expires the transmitting node will
	 * transmit a Discard_Message_PDU with the Message_ID set equal to the
	 * Message_ID field in the associated Address_PDU and Data_PDUs.
	 * 
	 * @param timer that expired.
	 * @throws InterruptedException if we get interrupted while putting to {@link no.ntnu.acp142.rdt.ReliableDataTransfer#outMessages}.
	 */
	private void handleExpiryTimerTransmit(Timer timer) throws InterruptedException {
        stopTimer(timer.getMsg(), TimerType.RETRANSMISSION_TIMER);
        rdt.persistentGroups.remove(timer.getMsg().getRecipients().hashCode());
        MessageEntry currentEntry = timer.getMsg();
        int sourceId = currentEntry.getAddressPdu().getSourceID();
        int messageId = currentEntry.getAddressPdu().getMessageId();
        DiscardMessagePdu discard = DiscardMessagePdu.create(currentEntry.getPriority(), sourceId, messageId);
        PacketEntry entry = new PacketEntry();
        entry.addDiscardMessagedPdu(discard);
        entry.addDestinationAddress(currentEntry.getMulticastAddress());
        Thread.interrupted();
        rdt.outMessages.put(entry);
        stopTimer(timer.getMsg(), TimerType.EXPIRY_TIMER_TRANSMIT);
        rdt.inMessages.remove(new HashValue(messageId, sourceId));
	}

	/**
	 * Perform necessary tasks associated with the RETRANSMISISON_TIMER
	 *
	 * If the transmitting node has not received all expected AckPDUs from one
	 * or more of the Non-EMCON receiving nodes and this timer expires, the
	 * transmitting node shall update and transmit the Address_PDU, and then
	 * either:
	 *
	 * (1) If it has not received any previous Ack_PDUs from one or more of the
	 * receiving node, it shall re-transmit the complete message and reset the
	 * the timer to a value greater than the initial value by the configurable
	 * BACK-OFF_FACTOR, or
	 *
	 * (2) If it has received previous ACK_PDUs from some receiving nodes, it
	 * shall only re-transmit those unacknowledged DataPDUs, and reset the timer
	 * to a value greater than the initial value by the configurable
	 * BACK-OFF-factor
	 *
	 * @param timer that expired.
	 * @throws InterruptedException if we get interrupted while putting to {@link no.ntnu.acp142.rdt.ReliableDataTransfer#outMessages}.
	 */
    private void handleReTransmissionTimer( Timer timer ) throws InterruptedException {
        Log.writeLine(Log.LOG_LEVEL_DEBUG, "Entered retransmission mode.");
        MessageEntry currentEntry = timer.getMsg();

		boolean[][] ackedMatrix = currentEntry.getAckedMatrix();
		AddressPdu addressPdu = currentEntry.getAddressPdu();

		ArrayList<Integer> recipients = currentEntry.getRecipients();
		ArrayList<DestinationEntry> destinationEntries = addressPdu
				.getDestinationEntries();
		ArrayList<DestinationEntry> modifiedDestinations = new ArrayList<DestinationEntry>();

		int ackedCount = 0;
		boolean completeMessage = false;


		Set<Integer> missingDataPduSequenceNumbers = new TreeSet<Integer>();

		boolean alreadySentAnnounceBeforeRetransmission = false;

		for (int i = 0; i < recipients.size(); i++) {
			for (DestinationEntry destinationEntry : destinationEntries) {
				if (destinationEntry.getDestinationID() == recipients.get(i)) {
					if(rdt.isEmcon(destinationEntry.getDestinationID())){
						continue;
					}

					// We have the index into the ack table (I hope)
					ackedCount = 0;
					for (int j = 1; j < ackedMatrix[i].length; j++) {
						if (ackedMatrix[i][j]) {
							ackedCount++;
						} else {
						    missingDataPduSequenceNumbers.add(j);
						}
					}
                    if (ackedCount == 0) {
                        // We have not received any ack from this client
                        if(currentEntry.getAnnounceCt() <= 0 && currentEntry.usesDynamicGroup()){
                            //send new announce
                            PacketEntry packet = new PacketEntry();
                            packet.addAnnouncePdu(currentEntry.getAnnouncePdu());

                            if (!alreadySentAnnounceBeforeRetransmission) {
                                alreadySentAnnounceBeforeRetransmission = true;
                                rdt.outMessages.put(packet);
                            }
                        }

                        completeMessage = true;
                        modifiedDestinations.add(new DestinationEntry(
                                destinationEntry.getDestinationID(),
                                destinationEntry.getMessageSequenceNumber(),
                                destinationEntry.getReservedField()));
                    } else if (ackedCount == ackedMatrix[i].length-1) {
                        continue;
                    } else {
                        modifiedDestinations.add(new DestinationEntry(
                                destinationEntry.getDestinationID(),
                                destinationEntry.getMessageSequenceNumber(),
                                destinationEntry.getReservedField()));
                    }
				}
			}
		}
		
		
		
		Log.writeLine(Log.LOG_LEVEL_DEBUG, "We need to re-transmit to " + modifiedDestinations.size() + " destinations");
		if (modifiedDestinations.size() == 0) {
		    return;
		}
		
		DestinationEntry[] destinationsArray = new DestinationEntry[modifiedDestinations
				.size()];
		for (int j = 0; j < destinationsArray.length; j++) {
			destinationsArray[j] = modifiedDestinations.get(j);
		}


		ArrayList<DataPdu> allDataPdus = currentEntry.getDataPdus();

		// Preparing for sending and switch to re-transmission state
		MessageEntry messageEntry = new MessageEntry();
		messageEntry.setState(States.RE_TRANSMITTING);
		// Send to same multicast address as previous message
		messageEntry.setMulticastAddress(currentEntry.getMulticastAddress());
		messageEntry.setPriority(currentEntry.getPriority());
		
		short numberOfDataPduToSend = 0;

		if (completeMessage) {
			// Send complete message
			Log.writeLine(Log.LOG_LEVEL_DEBUG, "Re-transmit complete message");
			for (DataPdu dataPdu : allDataPdus) {
				if (dataPdu == null) {
					continue;
				}

				messageEntry.addDataPdu(dataPdu);

			}
			// set highest numbered missing dataPDU
			//messageEntry.setHighestMissingSequenceNumber(addressPdu
				//	.getTotalNumberOfPDUs());
			numberOfDataPduToSend = (short) allDataPdus.size();

		} else {
			// Send only the missing data PDUs
			Log.writeLine(Log.LOG_LEVEL_DEBUG,
					"Re-transmit only missing dataPDUs");
				for (Integer integer : missingDataPduSequenceNumbers) {
				    Log.writeLine(Log.LOG_LEVEL_DEBUG, "Missing  sequence number: " + integer);
				    messageEntry.addDataPdu(currentEntry.getDataPdus().get(integer - 1));
				}
				
				numberOfDataPduToSend = (short) missingDataPduSequenceNumbers.size();
		}

		// Stop timer
		stopTimer(currentEntry, TimerType.RETRANSMISSION_TIMER);
		// Adjusting new timer
		double endTime;
		if(currentEntry.getReTransmissionTime() == 0){
			//First time
			endTime = Configuration.getAckRetransmissionTime()
					* Configuration.getBackoffFactor();
			currentEntry.setReTransmissionTime((long)endTime);
		}else {
			//
			endTime = currentEntry.getReTransmissionTime() * Configuration.getBackoffFactor();
			currentEntry.setReTransmissionTime((long)endTime);
		}
		
		//Re-start timer
		rdt.initializeTimer((int) endTime,
				TimerType.RETRANSMISSION_TIMER, currentEntry, null);
		
	      // Create new addressPdu
        ArrayList<AddressPdu> addressPdus = AddressPdu.create(
                currentEntry.getPriority(),
                numberOfDataPduToSend, addressPdu.getSourceID(),
                addressPdu.getMessageId(), (int) addressPdu.getExpiryTime(),
                destinationsArray, 0);
        
        for (AddressPdu addrPdu : addressPdus) {
            messageEntry.addAddressPdu(addrPdu);
        }
        
        // Clear interrupt vector and queue for sending
        Thread.interrupted();
        rdt.outMessages.put(messageEntry);
	}

	/**
	 * Perform necessary tasks associated with the EMCON_RETRANSMISSION_TIMER
	 * 
	 * (a) If this timer expires, implying that since the last EMCON re-transmission all the receiving
	 * nodes in the EMCON mode have not entered the non-EMCON mode (by sending
	 * Ack_PDUs), and the "EMCON Re-transmission Counter" has not exceeded its maximum, the
	 * transmitting node shall re-transmit the Address_PDU and all Data_PDUs not already
	 * acknowledged. The transmitting node shall re-initialise this timer and increment the
	 * "EMCON Re-transmission Counter". The transmitting node shall wait until either:
	 * 
	 * (1) all the receiving nodes in EMCON mode respond with an Ack_PDU at which
	 * point the transmitting node shall enter the "Non-EMCON Re-transmission"
	 * mode, or
	 * 
	 * (2) the "Transmitter Expiry_Time timer" expires at which point the transmitting
	 * node shall transmit a Discard-Message_PDU.
	 * 
	 * (b) 
	 * the "Transmitter Expiry_Time timer" expires at which point the transmitting
	 * node shall transmit a Discard-Message_PDU.
	 * 
	 * @param timer that expired.
	 * @throws InterruptedException if we get interrupted while putting to {@link no.ntnu.acp142.rdt.ReliableDataTransfer#outMessages}.
	 */
	private void handleEmconRetransmissionTimer(Timer timer) throws InterruptedException {
		MessageEntry currentEntry = timer.getMsg();
		if(currentEntry.getAndDecrementEmconRtc() >= 0){
			//We are still able to re-transmit
			//We need to modify the receiver list. Only re-transmit to those 
			//in EMCON 
			//Create new address PDU for the occasion
			
			int seqNumber = 1; //Sequence number for the destination entries
			byte[] b = new byte[0];
			ArrayList<DestinationEntry> destinationEntries = new ArrayList <DestinationEntry>();
			for(Integer destination: currentEntry.getRecipients()){
				if(rdt.isEmcon(destination)){
					//This node is currently in EMCON
					destinationEntries.add(new DestinationEntry(destination, seqNumber++, b));
					Log.writeLine(Log.LOG_LEVEL_DEBUG, "Adding " + destination + " as an emcon destination.");
				}
			}
			
			//Then we create the modified address pdu (s)
			DestinationEntry[] destinationArray = new DestinationEntry[destinationEntries.size()];
			
			if(destinationEntries.isEmpty()){
				//No one to send to, exit EMCON re-transmission mode
				rdt.stopTimer(TimerType.EMCON_RETRANSMISSION_TIMER, currentEntry);
				rdt.initializeTimer(Configuration.getAckRetransmissionTime(), TimerType.RETRANSMISSION_TIMER, currentEntry, null); 
				return;
			}
			
			for(int i = 0; i < destinationEntries.size(); i++){
				destinationArray[i] = destinationEntries.get(i);
			}
			
			ArrayList <AddressPdu> addressPdus = AddressPdu.create(currentEntry.getPriority(), (short) currentEntry.getDataPdus().size(), Configuration.getNodeId(), currentEntry.getAddressPdu().getMessageId(), currentEntry.getAddressPdu().getExpiryTime(), destinationArray, (byte)0);
			
			MessageEntry readyForSending = new MessageEntry();
			for (AddressPdu addressPdu : addressPdus) {
				readyForSending.addAddressPdu(addressPdu);
			}
			
			for(DataPdu dataPdu : currentEntry.getDataPdus()){
			    if (dataPdu != null) {
			        readyForSending.addDataPdu(dataPdu);
			    }
			}
			
			readyForSending.setMulticastAddress(currentEntry.getMulticastAddress());
			readyForSending.setDynamicGroupTag(currentEntry.usesDynamicGroup());
			readyForSending.setPersistentMulticastGroup(currentEntry.isPersistentMulticastGroups());
			
			readyForSending.setState(States.EMCON_RE_TRANSMISSION);
			
			rdt.outMessages.put(readyForSending);
			
			//Stop timer 
			stopTimer(currentEntry, TimerType.EMCON_RETRANSMISSION_TIMER);
			
			//Restart timer
			rdt.initializeTimer(Configuration.getEmconRti(), TimerType.EMCON_RETRANSMISSION_TIMER, currentEntry, null);
			
			
		} else {
			//We have retransmitted maximum number of times for this message
			stopTimer(currentEntry, TimerType.EMCON_RETRANSMISSION_TIMER);
			
		}
	}

	/**
	 * Perform necessary tasks associated with the ACK_TIMER
	 * 
	 * If the receiving node does not receive a response to the transmitted
	 * Ack_PDU(s) from the transmitting node in the form of the requested
	 * missing Data_PDUs or an Address_PDU, and an "Ack_PDU Timer" expires, the
	 * receiving node shall re-transmit the associated Ack_PDU(s) and
	 * re-initialise the timer.
	 * 
	 * @param timer that expired.
	 * 
	 * @throws InterruptedException if we get interrupted while putting to {@link no.ntnu.acp142.rdt.ReliableDataTransfer#outMessages}.
	 */
	private void handleAckTimer(Timer timer) throws InterruptedException {

		MessageEntry currentEntry = timer.getMsg();
		int sourceID = currentEntry.getAddressPdu().getSourceID();
		ArrayList<Integer> missing = currentEntry
				.getMissingDataSequenceNumbers();

		AckInfoEntry ackInfoEntry = prepareAckInfoEntry(currentEntry, missing, currentEntry.getAckAddress());
		AckInfoEntry[] ackInfoEntries = { ackInfoEntry };
		ArrayList<AckPdu> ackPdus = AckPdu.create(sourceID, currentEntry.getPriority(),
				ackInfoEntries);

		PacketEntry packetEntry = new PacketEntry();
		packetEntry.addAckPdus(ackPdus);

		packetEntry.addDestinationAddress(currentEntry.getAckAddress());
	
		rdt.outMessages.put(packetEntry);
		
		stopTimer(timer.getMsg(), TimerType.ACK_TIMER);

		long endTime = Configuration.getAckPduTime();
		rdt.initializeTimer(endTime, TimerType.ACK_TIMER, currentEntry, null);
	}

	/**
	 * Perform necessary tasks associated with the
	 * UNIDENTIFIED_DATA_DPU_VALIDITY_TIMER
	 * 
	 * This timer indicates the time remaining before the Data_PDUs in a
	 * Data_PDU entry are no longer considered valid and can therefore be
	 * discarded.
	 * 
	 * If the timer expires before the receiving node receives either the
	 * Address_PDU or a Discard_Message_PDU associated with the Data_PDUs in the
	 * Data_PDU entry, the receiving node shall discard all Data_PDUs.
	 * 
	 * @param timer that expired.
	 */
	private void handleUnidentifiedDataPduValidityTimer(Timer timer) {
		MessageEntry currentEntry = timer.getMsg();
		DataPdu tmp = currentEntry.getDataPdus().get(0);
		HashValue key = new HashValue(tmp.getMessageId(), tmp.getSourceID());
		
	
		if (rdt.inMessages.containsKey(key)) {
			// Remove this entry
			rdt.inMessages.remove(key);
			stopTimer(timer.getMsg(), TimerType.UNIDENTIFIED_DATA_DPU_VALIDITY_TIMER);
		} else {
			// Should never happen
		    Log.writeLine(Log.LOG_LEVEL_QUIET, "Something bad happened! Did not find the key associated with the timer!?");
		    throw new RuntimeException("Did not find the key associated with the timer?!. Bad...");
		}

	}

	/**
	 * Perform necessary tasks associated with the WAIT_FOR_REJECT_TIME timer
	 * 
	 * After the transmission of the Request_PDU the requesting node waits a
	 * certain time (WAIT_FOR_REJECT_TIME) to receive Reject_PDUs from members
	 * of T_Nodes. If no Reject_PDU has been received when this time has
	 * expired, the requesting node announces the requested multicast address to
	 * all receiving nodes. Once a multicast group has been announced, the late
	 * reception of a corresponding Reject_PDU will be ignored.
	 * 
	 * @param timer that expired.
	 * @throws InterruptedException if we get interrupted while putting to {@link no.ntnu.acp142.rdt.ReliableDataTransfer#outMessages}.
	 */
	private void handleWaitForRejectTimer(Timer timer) throws InterruptedException {
		Log.writeLine(Log.LOG_LEVEL_DEBUG, "WaitForRejectTime timer expires");
		MessageEntry currentEntry = timer.getMsg();
		if (currentEntry == null) {
			Log.writeLine(Log.LOG_LEVEL_DEBUG, "CurrentEntry is null");
            return;
		}
		// Get announce PDU
		AnnouncePdu announcePdu = currentEntry.getAnnouncePdu();
		if (announcePdu == null) {
			// Should never happen
			Log.writeLine(Log.LOG_LEVEL_DEBUG, "The announcePdu does not exist");
			return;
		}
		
		//This message is supposed to use a persistent group
		if(currentEntry.isPersistentMulticastGroups()){
			//Store it for later use
			InetAddress group = announcePdu.getInetMulticastGroupAddress();
			rdt.persistentGroups.put(currentEntry.getRecipients().hashCode(), group);
			
			if(rdt.persistentGroupSessions.get(group) == null){
			    CopyOnWriteArraySet<MessageEntry> entries = new CopyOnWriteArraySet<MessageEntry>();
			    entries.add(currentEntry);
				rdt.persistentGroupSessions.put(group, new Tuple<CopyOnWriteArraySet<MessageEntry>, AtomicBoolean>(entries, new AtomicBoolean(true)));
				
			}
		}

		Log.writeLine(Log.LOG_LEVEL_DEBUG, "Sending announce PDU");
		PacketEntry packetEntry = new PacketEntry();
		packetEntry.addAnnouncePdu(currentEntry.getAnnouncePdu());

		//sending announce PDU
		long endTime = Configuration.getAnnounceDelay() / Configuration.getAnnounceCt();
		rdt.initializeTimer(endTime, TimerType.ANNOUNCE_RE_TRANSMISSION_TIMER, currentEntry, null);
		
		Thread.interrupted();
		rdt.outMessages.put(packetEntry);
		stopTimer(currentEntry, TimerType.WAIT_FOR_REJECT_TIME);

	    rdt.initializeTimer(Configuration.getAnnounceDelay(), TimerType.ANNOUNCE_DELAY, currentEntry, null);
		

	}

	/**
	 * Perform necessary tasks associated with the ANNOUNCE_DELAY timer
	 * 
	 * The announcing node must wait a certain period of time (ANNOUNCE_DELAY)
	 * until all routers within the multicast tree acquire information about the
	 * group memberships of those nodes in the list of Destination_IDs. After
	 * this time has expired, data transfer may begin.
	 * 
	 * @param timer that expired.
	 * @throws InterruptedException if we get interrupted while putting to {@link no.ntnu.acp142.rdt.ReliableDataTransfer#outMessages}.
	 */

	private void handleAnnounceDelay(Timer timer) throws InterruptedException {
		Log.writeLine(Log.LOG_LEVEL_DEBUG, "ANNOUNCE_DELAY expires");
		MessageEntry currentEntry = timer.getMsg();
		
		
		//If we are going to use persistent groups we add these here
		if(currentEntry.isPersistentMulticastGroups()){
			//Persistent group added
			rdt.setPersistentMulticastGroup(currentEntry.getRecipients(), currentEntry.getMulticastAddress());
		}
		
		rdt.outMessages.put(currentEntry);
		stopTimer(currentEntry, TimerType.ANNOUNCE_DELAY);
		
		

	}

	/**
	 * This timer will expire when the receiver is ready to send acks.
	 * The method is able to ack several messages from the same sender.
	 * 
	 * @param timer that expired.
	 * @throws InterruptedException if we get interrupted while putting to outMessages.
	 */
	private void handleAckDelay(Timer timer) throws InterruptedException {
		MessageEntry currentEntry = timer.getMsg();
		ArrayList <AckInfoEntry> ackInfoEntriesList = new ArrayList<AckInfoEntry>();
		ArrayList <MessageEntry> entries;
		InetAddress destinationAddress = null;
		ArrayList<Integer> missing = null;
		
		if(currentEntry == null){
			entries = timer.getMessageEntryList();
			//Stop timer
			stopTimer(entries, TimerType.ACK_DELAY);
		}else {
			entries = new ArrayList <MessageEntry>();
			entries.add(currentEntry);
			//Stop timer
			stopTimer(currentEntry, TimerType.ACK_DELAY);
		}
				
		
		for(MessageEntry entry : entries) {
			destinationAddress = entry.getAckAddress();
			missing = entry
					.getMissingDataSequenceNumbers();
			
			// This message is part of retransmission
			if (entry.getState() == States.RE_TRANSMITTING && entry.getAddressPdu() != null && entry.getHighestMissingSequenceNumber() == entry
					.getLastReceivedDataPdu().getSequenceNumber()) {
				// Highest numbered previously missing data PDU
					
					ackInfoEntriesList.add(prepareAckInfoEntry(entry, missing, entry.getAckAddress()));
					
					Log.writeLine(Log.LOG_LEVEL_DEBUG,
							"Highest previously missing dataPdu received");
			} else if(entry.getAddressPdu() != null && entry.getAddressPdu().getTotalNumberOfPDUs() == entry.getHighestSequenceNumber()){

				// Received the last dataPDU for the first time
					ackInfoEntriesList.add(prepareAckInfoEntry(entry, missing, entry.getAckAddress()));
					Log.writeLine(Log.LOG_LEVEL_DEBUG, "Last dataPDU received for the first time " + entry.getState().toString());
				
			}else{
				// Missing MM or more pdus
				if (entry.getAddressPdu() != null && missing.size() >= Configuration.getMm()) {
					ackInfoEntriesList.add(prepareAckInfoEntry(entry, missing, entry.getAckAddress()));
					Log.writeLine(Log.LOG_LEVEL_DEBUG, "MM dataPdus missing " + entry.getState().toString());
				}
			}
		
		}
		
		if(destinationAddress == null){
			Log.writeLine(Log.LOG_LEVEL_DEBUG, "No destination specified");
		}
		
		if(!rdt.EMCON.get()){
			//Not in EMCON
			//Prepare ack
			AckInfoEntry[] ackInfoEntries = new AckInfoEntry[ackInfoEntriesList.size()];
			int sourceID = Configuration.getNodeId();
			for(int i = 0; i < ackInfoEntriesList.size(); i++){
				ackInfoEntries[i] = ackInfoEntriesList.get(i);
				
            }

            if ( missing != null && missing.size() != 0 ) {
                ArrayList<AckPdu> ackPdus = AckPdu.create(sourceID, currentEntry.getPriority(), ackInfoEntries);
                PacketEntry entry = new PacketEntry();
                entry.addAckPdus(ackPdus);
                entry.addDestinationAddress(destinationAddress);
                currentEntry.setState(States.RE_TRANSMITTING);

                Thread.interrupted();
                rdt.outMessages.put(entry);
            }else {
            	Log.writeLine(Log.LOG_LEVEL_DEBUG, "No ack to send");
            }
		}
	}
	
	
    /**
     * This timer is created when we return a message to the layer above us,
     * aka the message is complete, so we need to tell the sender. The timer then
     * starts with a random delay, and when it expires we create and send a complete
     * ack.
     * 
     * @param timer The timer that expired.
     * @throws InterruptedException if we get interrupted while waiting to put.
     */	
	private void handleCompleteAck(Timer timer) throws InterruptedException {
	    MessageEntry currentEntry = timer.getMsg();
	    PacketEntry entry = new PacketEntry();
	    
	    AckInfoEntry[] ackInfoEntries = new AckInfoEntry[1];
        ackInfoEntries[0] = prepareAckInfoEntry(currentEntry, new ArrayList<Integer>(), currentEntry.getAckAddress());
	    int sourceID = Configuration.getNodeId();
	    ArrayList<AckPdu> ackPdus = AckPdu.create(sourceID, currentEntry.getPriority(), ackInfoEntries);
	    entry.addAckPdus(ackPdus);
	    
	    entry.addDestinationAddress(currentEntry.getAckAddress());
	    
	    rdt.outMessages.put(entry);
	    stopTimer(currentEntry, TimerType.COMPLETE_ACK_TIMER);
	    
	}

    /**
     * This method will prepare ackInfoEntries given a messageEntry, the dataPdu's we are missing, and the
     * destination from which the entry came from.
     * 
     * @param currentEntry to create AckInfoEntries for.
     * @param missing sequence numbers of the missing data pdus.
     * @param destinationAddress source of the message.
     * @return AckInfoEntry for the missing DataPdus.
     * @throws InterruptedException if we get interrupted while putting to {@link no.ntnu.acp142.rdt.ReliableDataTransfer#readyToAckEmcon}.
     */
	private AckInfoEntry prepareAckInfoEntry(MessageEntry currentEntry,
			ArrayList<Integer> missing, InetAddress destinationAddress)
			throws InterruptedException {

		int sourceID = Configuration.getNodeId();
		int messageID = currentEntry.getAddressPdu().getMessageId();
		short[] m;
		
		if(rdt.EMCON.get()){
			//In EMCON
			HashValue key = new HashValue(messageID, sourceID);
			if(!rdt.readyToAckEmcon.containsKey(key)){
				rdt.readyToAckEmcon.put(key, currentEntry);
			}
		}
		
		if (missing.size() <= Configuration.getMm()) {
			m = new short[missing.size()];
		} else {
			m = new short[Configuration.getMm()];
		}
		
		for (int i = 0; i < missing.size(); i++) {			
			if(i >= Configuration.getMm()){
				continue;
			}
			m[i] = missing.get(i).shortValue();
		}
		return new AckInfoEntry(sourceID, messageID, m);
	}
	
	
	private void handleAnnounceReTransmission(Timer timer){
		MessageEntry currentEntry = timer.getMsg();
		
		if(currentEntry.getAndDecrementAnnounceCt() > 0){
		    Log.writeLine(Log.LOG_LEVEL_DEBUG, currentEntry.getAnnounceCt() + " announces left to send.");
			PacketEntry packet = new PacketEntry();
			packet.addAnnouncePdu(currentEntry.getAnnouncePdu());
			
			Thread.interrupted();
			try {
				rdt.outMessages.put(packet);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			stopTimer(currentEntry, TimerType.ANNOUNCE_RE_TRANSMISSION_TIMER);
			long endTime = Configuration.getAnnounceDelay() / Configuration.getAnnounceCt();
			Thread.interrupted();
			rdt.initializeTimer(endTime, TimerType.ANNOUNCE_RE_TRANSMISSION_TIMER, currentEntry, null);
		}else {
			
			stopTimer(currentEntry, TimerType.ANNOUNCE_RE_TRANSMISSION_TIMER);
		}
	}
}
