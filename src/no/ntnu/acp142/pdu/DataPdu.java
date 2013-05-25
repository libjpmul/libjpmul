package no.ntnu.acp142.pdu;

import java.util.ArrayList;

import no.ntnu.acp142.Configuration;
import no.ntnu.acp142.Log;

/*
 * Copyright (c) 2013, Thomas Martin Schmid, Erik Lothe, Bjørn Tungesvik, Karl Mardoff Kittilsen
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
 * The DataPdu represents a fragment of data.
 * 
 * @author Thomas Martin Schmid, Erik Lothe, Bjørn Tungesvik, Karl Mardoff Kittilsen
 * 
 */
public class DataPdu extends Pdu {

    // --- OCTET PLACEMENTS ------------------------------------------------ //
    private static final int SEQUENCE_NUMBER_OFFSET                  = 4;
    private static final int MSID_OFFSET                            = 12;
    private static final int DATA_FRAGMENT_OFFSET                   = 16;
    // --------------------------------------------------------------------- //

    // --- CONSTANTS ------------------------------------------------------- //
    /**
     * The size of a DataPdu excluding all data fragments
     */
    public static final int DATA_PDU_BASE_SIZE                     = 16;
    // --------------------------------------------------------------------- //

    /**
     * Creates an Address_PDU from binary
     * 
     * @param binary
     *          Byte array representing the binary form of PDU
     */
    protected DataPdu(byte[] binary) {
        this.binary = binary;
    }

    /**
     * Creates all DataPdus for a message
     * 
     * @param priority
     *          P_Mul priority
     * @param sourceId
     *          Source ID
     * @param messageId
     *          Message ID
     * @param data
     *          Byte array representing all data to be transmitted
     * @return a list of fragmented DataPdus
     */
    public static ArrayList<DataPdu> create( int priority, int sourceId, int messageId, byte[] data ) {

        ArrayList<DataPdu> pduList = new ArrayList<DataPdu>();
        int pduMaxDataSize = Configuration.getPduMaxSize() - DATA_PDU_BASE_SIZE;

        int count;
        int remainingData;
        int sequenceNumber = 1;

        // For all data
        for (int offset = 0; offset < data.length; offset += count) {
            // Add as much data as can fit
            remainingData = data.length - offset;
            if ( remainingData < pduMaxDataSize ) {
                count = remainingData;
            } else {
                count = pduMaxDataSize;
            }
            // Add PDU to list with count of data
            try {
                pduList.add(new DataPdu(priority, sequenceNumber++, sourceId, messageId, data, offset, count));
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.writeLine(Log.LOG_LEVEL_NORMAL, "Data segment does not fit into binary of packet in method create in class DataPdu");
            }
        }
        return pduList;
    }

    /**
     * Initializes a single DataPdu.
     * 
     * @param priority
     *          P_Mul priority
     * @param sequenceNumber
     *          Sequence number of this DataPdu
     * @param sourceId
     *          Source ID
     * @param messageId
     *          Message ID
     * @param data
     *            the data segment of this packet
     * @param offset
     *            offset into the data array to start this fragment
     * @param count
     *            bytes of data to add to this DataPdu
     * @throws ArrayIndexOutOfBoundsException 
     *            if data segment can not fit into binary
     */
    private DataPdu( int priority, int sequenceNumber, int sourceId, int messageId, byte[] data, 
            int offset, int count ) throws ArrayIndexOutOfBoundsException {
        
        byte pduType = Data_PDU;
        int lengthOfPDU = (DATA_PDU_BASE_SIZE + count);

        try {
            initCommonBinary(lengthOfPDU, priority, (byte) 0, pduType, sourceId);
        } catch (IllegalArgumentException e) {
            Log.writeLine(Log.LOG_LEVEL_NORMAL, e.getMessage());
        }
        
        try {
            addToBinary(SEQUENCE_NUMBER_OFFSET, (short) sequenceNumber);
            addToBinary(MSID_OFFSET,            (int)   messageId);
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.writeLine(Log.LOG_LEVEL_NORMAL, "Packet data could not fit into binary in constructor in class " +
            		"DataPdu");
        }
        // Add data up to count, without exceeding length
        // Ignore if data array is shorter than count
        addToBinary(DATA_FRAGMENT_OFFSET, data, offset, count);

        // Finally calculate checksum over entire PDU
        setChecksum();
    }

    /**
     * Returns a value that "specifies the order of the message fragment
     * within the original message, starting from 1.
     * 
     * @return sequence number
     */
    public int getSequenceNumber( ) {
        return unsigned( concatenateBytes(binary[SEQUENCE_NUMBER_OFFSET],
                                           binary[SEQUENCE_NUMBER_OFFSET + 1]));
    }

    /**
     * Returns the unique identifier created within the scope of Source_ID by
     * the transmitter
     * 
     * @return message ID
     */
    public int getMessageId( ) {
        return concatenateBytes(binary[MSID_OFFSET],
                                 binary[MSID_OFFSET + 1], 
                                 binary[MSID_OFFSET + 2],
                                 binary[MSID_OFFSET + 3]);
    }

    /**
     * Returns the data fragment in a byte array.
     * 
     * @return data fragment
     */
    public byte[] getDataFragment( ) {
        int length = getLengthOfPDU();
        byte[] data = new byte[length - DATA_FRAGMENT_OFFSET];
        for (int i = 0, j = DATA_FRAGMENT_OFFSET; j < length; ++j, ++i) {
            data[i] = this.binary[j];
        }
        return data;
    }

    /**
     * Writes this data fragment to given byte array starting from given offset
     * 
     * @param data
     *            data array to write this data fragment to
     * @param offset
     *            offset in data to write to
     * @return number of bytes written to array
     */
    public int getDataFragment( byte[] data, int offset ) {
        int length = getLengthOfPDU();
        if ( offset + length - DATA_PDU_BASE_SIZE > data.length ) {
            throw new RuntimeException("Attempting to write to an insufficiently large data array:\n"
                    + "in getDataFragment in class DataPdu, data array size: " + data.length + ", offset " + offset
                    + " and length " + (length - DATA_PDU_BASE_SIZE));
        }
        if ( offset < 0 ) {
            throw new RuntimeException("Negative offset");
        }
        int i, j;
        for (i = offset, j = DATA_PDU_BASE_SIZE; j < length; ++j, ++i) {
            data[i] = this.binary[j];
        }
        return i - offset;
    }

    /**
     * Helper method that returns the length of the data fragment.
     * 
     * @return length
     */
    public int getLengthOfDataFragment( ) {
        return getLengthOfPDU() - DATA_PDU_BASE_SIZE;
    }
}
