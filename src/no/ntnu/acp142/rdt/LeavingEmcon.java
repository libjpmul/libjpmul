package no.ntnu.acp142.rdt;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import no.ntnu.acp142.Configuration;
import no.ntnu.acp142.Log;
import no.ntnu.acp142.pdu.AddressPdu;
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
 * When leaving EMCON the client must acknowledgment every message received
 * during EMCON state. This thread is responsible for sending this
 * acknowledgment packets to their respective recipients
 * 
 * @author Bjørn Tungesvik, Karl Mardoff Kittilsen
 * 
 */

public class LeavingEmcon extends Thread {
	/**
	 * Reference to ReliableDataTransfer
	 */
	private ReliableDataTransfer rdt;

	/**
	 * List of messages to be acknowledgment
	 */
	private ArrayList<Tuple<Integer, ArrayList<MessageEntry>>> ackList;

	/**
	 * Creates a new instance of the leave EMCON thread. This thread is
	 * responsible for preparing acknowledgment packets for every message
	 * received during EMCON mode
	 * 
	 * @param rdt reference to the ReliableDataTransfer class
	 */
	public LeavingEmcon(ReliableDataTransfer rdt) {
		this.rdt = rdt;
	}

	/**
	 * Goes through every message we received during EMCON. Then either schedule
	 * a sending for an complete acknowledgment, or acknowledgment for partial
	 * re-transmission. Both the complete and partial acknowledgment is
	 * scheduled to prevent ack-implosion.
	 */
	public void run() {
		Log.writeLine(Log.LOG_LEVEL_DEBUG, "Starting leaveEmcon thread.");
		ackList = new ArrayList<Tuple<Integer, ArrayList<MessageEntry>>>();
		Set<Map.Entry<HashValue, MessageEntry>> entries = rdt.readyToAckEmcon
				.entrySet();
		Tuple<Integer, ArrayList<MessageEntry>> temp;
		System.err.println("entries length: " + entries.size());
		for (java.util.Map.Entry<HashValue, MessageEntry> entry : entries) {
			MessageEntry currentEntry = entry.getValue();
			AddressPdu addressPdu = currentEntry.getAddressPdu();
			int sourceId = addressPdu.getSourceID();
			int index;
			if ((index = contains(sourceId)) == -1) {
				// We have not seen this source before
				ArrayList<MessageEntry> l = new ArrayList<MessageEntry>();
				l.add(currentEntry);
				temp = new Tuple<Integer, ArrayList<MessageEntry>>(sourceId, l);
				ackList.add(temp);
			} else {
				// Already noticed this
				temp = ackList.get(index);
				temp.t2.add(currentEntry);
			}
		}
		// Start ack delay timer to prevent ack implosion
		for (int i = 0; i < ackList.size(); i++) {
			Tuple<Integer, ArrayList<MessageEntry>> tuple = ackList.get(i);
			Random random = new Random();
			long endTime = random.nextInt((int) Configuration
					.getAckDelayUpperBound());

			ArrayList<MessageEntry> entriesToRemove = new ArrayList<MessageEntry>();
			for (MessageEntry tmp : tuple.t2) {
				// Remove the EMCON re-transmission timer
				rdt.stopTimer(TimerType.EMCON_RETRANSMISSION_TIMER, tmp);

				if (tmp.dataReady()) {
					rdt.initializeTimer(endTime, TimerType.COMPLETE_ACK_TIMER,
                            tmp, null);
					entriesToRemove.add(tmp);
				}
			}
			// Remove the entries we already have sent complete ack for.
			if (!entriesToRemove.isEmpty()
					&& !tuple.t2.removeAll(entriesToRemove)) {
				// We where not able to remove the entries we wanted!?
				Log.writeLine(
						Log.LOG_LEVEL_NORMAL,
						"Already sent acks for these entries, but when we tried"
								+ "to remove them something failed. This might result in acks not being sent or acks"
								+ "being sent twice. Should not happen!");
			}

			for (MessageEntry entry : tuple.t2) {
				rdt.initializeTimer(Configuration.getAckRetransmissionTime(),
						TimerType.ACK_TIMER, entry, null);
			}
		}
		System.err.println("Setting readyToAckEmcon to null.");
		rdt.readyToAckEmcon = new ConcurrentHashMap<HashValue, MessageEntry>();
	}

	/**
	 * Check if the source id is contained in the list
	 * 
	 * @param sourceId to check
	 * @return the position of the id if it is in the list or -1 otherwise.
	 */
	private int contains(int sourceId) {
		for (int i = 0; i < ackList.size(); i++) {
			Tuple<Integer, ArrayList<MessageEntry>> tuple = ackList.get(i);
			if (tuple.t1 == sourceId) {
				return i;
			}
		}
		return -1;
	}

}
