package no.ntnu.acp142.rdt;

import java.util.ArrayList;

import no.ntnu.acp142.Log;
import no.ntnu.acp142.pdu.DataPdu;


/*
 * Copyright (c) 2013, Karl Mardoff Kittilsen, Bjørn Tungesvik
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
 * This list allows DataPdus to be entered at any position in an array. It is
 * used to store the dataPdus in correct order based on their sequence number
 * 
 * @author Karl Mardoff Kittilsen, Bjørn Tungesvik
 * 
 */
public class DataPduList extends ArrayList<DataPdu> {

    private static final long serialVersionUID = 1L;

    /**
	 * Add a dataPdu at a specific position in the structure. Note: Under normal
	 * use the index position is the same as the sequence number of the PDU.
	 * 
	 * If there is a dataPdu at the position, this method will replace
	 * the old one with the new one silently.
	 * 
	 * @param index the position of the added dataPdu.
	 * @param p the dataPdu to add
	 */
	public void add(int index, DataPdu p) {
		Log.writeLine(Log.LOG_LEVEL_DEBUG,
				"Adding pdu seq: " + p.getSequenceNumber() + " to position "
						+ index);
		if (index > numberOfPduSpots()) {
			ensureNumberOfPduSpots(index);
			super.set(index, p);
		} else {
			super.set(index, p);
		}
	}

	/**
	 * Ensure the capacity for the structure.
	 * 
	 * @param numberOfPdus number of pdus to ensure spots are available for.
	 */
	private void ensureNumberOfPduSpots(int numberOfPdus) {
		Log.writeLine(Log.LOG_LEVEL_DEBUG, "Making sure we got room for "
				+ numberOfPdus + " PDUs.");
		for (int i = numberOfPduSpots(); i < numberOfPdus; i++) {
			this.add(null);
		}
	}

	/**
	 * Returns all the DataPdus.
	 * 
	 * @return All data pdus.
	 */
	public ArrayList<DataPdu> getPdus() {
		ArrayList<DataPdu> pdus = new ArrayList<DataPdu>();
		for (DataPdu pdu : this) {
			if (pdu != null) {
				pdus.add(pdu);
			}
		}
		return pdus;
	}

	private int numberOfPduSpots() {
		return super.size() - 1;
	}
	
	 /**
     * Returns the number of dataPdus currently residing in the structure.
     * 
     * @return pdus number of pdus.
     */
    public int getNumberOfDataPdus() {
        int size = 0;
        for (DataPdu pdu : this) {
            if (pdu != null) {
                size++;
            }
        }
        return size;
    }

	/**
	 * Returns a list of sequence numbers of missing DataPdus. Note: Only useful
	 * when receiving.
	 * 
	 * @param highestSequenceNumber
	 *            expected sequence number
	 * @return missing sequence numbers
	 */
	public ArrayList<Integer> getMissing(int highestSequenceNumber) {
		Log.writeLine(Log.LOG_LEVEL_DEBUG, "HighestSequenceNumber: "
				+ highestSequenceNumber);
		Log.writeLine(Log.LOG_LEVEL_DEBUG, "printing dataPduList.");
		for (DataPdu pdu : this) {
			if (pdu != null) {
				Log.writeLine(Log.LOG_LEVEL_DEBUG,
						"DataPdu: " + pdu.getSequenceNumber());
			} else {
				Log.writeLine(Log.LOG_LEVEL_DEBUG, "DataPdu: " + pdu);
			}
		}

		if (highestSequenceNumber >= super.size()) {
			// The array list is to small, expand it.
			this.ensureNumberOfPduSpots(highestSequenceNumber);
		}

		ArrayList<Integer> missing = new ArrayList<Integer>();
		for (int i = 1; i <= highestSequenceNumber; i++) {
			if (this.get(i) == null) {
				missing.add(i);
			}
			if (this.get(i) != null) {
				Log.writeLine(Log.LOG_LEVEL_DEBUG, "DataPdu1: "
						+ this.get(i).getSequenceNumber());
			} else {
				Log.writeLine(Log.LOG_LEVEL_DEBUG, "DataPdu1: " + this.get(i));
			}

		}
		return missing;
	}

	/**
	 * Get the highest missing sequence number. Note: This structure store all
	 * incoming as well as outgoing messages (Depends if the message are to be
	 * sent or received). This method is only useful when receiving.
	 * 
	 * @return the highest missing sequence number
	 */
	public int getHighestSequenceNumber() {
		int highestSequenceNumber = 1;
		for (DataPdu dataPdu : this) {
			if (dataPdu != null
					&& dataPdu.getSequenceNumber() > highestSequenceNumber) {
				highestSequenceNumber = dataPdu.getSequenceNumber();
			}
		}
		return highestSequenceNumber;
	}
}
