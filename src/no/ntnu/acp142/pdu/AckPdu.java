package no.ntnu.acp142.pdu;

import java.util.ArrayList;

import no.ntnu.acp142.Configuration;
import no.ntnu.acp142.Log;

/*
 * Copyright (c) 2013, Thomas Martin Schmid, Erik Lothe
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
 * The ACK_PDU is used by a receiver to acknowledge the reception of one or 
 * more Data_PDUs. Each Data_PDU that is acknowledged is represented in an 
 * AckInfoEntry.
 * @author Thomas Martin Schmid, Erik Lothe
 *
 */
public class AckPdu extends Pdu {

    // --- OCTET PLACEMENTS ------------------------------------------------ //
    private static final int COUNT_OF_ACK_INFO_ENTRIES_OFFSET	       = 12;
    private static final int ACK_INFO_ENTRIES_OFFSET                = 14;
    // --------------------------------------------------------------------- //

    // --- CONSTANTS ------------------------------------------------------- //
    /**
     * The base size of an Ack PDU excluding AckInfoEntries
     */
    public static final int ACK_PDU_BASE_SIZE                      = 14;
    // --------------------------------------------------------------------- //

    /**
     * Creates an Ack_PDU from binary
     * @param binary
     *          The byte array representing the PDU
     */
    protected AckPdu(byte[] binary) {
        this.binary = binary;
    }

    /**
     * Creates an AckPdu
     * @param sourceID
     *          Source ID of this node
     * @param priority
     *          P_Mul priority of this packet
     * @param ackInfoEntries 
     *          All ackInfoEntries to send
     * @return {@code ArrayList<AckPdu>} list of fragmented AckPdus         
     */
    public static ArrayList<AckPdu> create(int sourceID, int priority, AckInfoEntry[] ackInfoEntries) {

        ArrayList<AckPdu> pduList = new ArrayList<AckPdu>();
        int pduMaxSize = Configuration.getPduMaxSize();
        int lengthOfPDU;
        short countOfAckInfoEntries;
        ArrayList<AckInfoEntry> packageAckInfoEntries;

        for (int i=0; i < ackInfoEntries.length; ) {	

            // Add as many ack info entries entries to packet without exceeding max packet size
            packageAckInfoEntries = new ArrayList<AckInfoEntry>();
            lengthOfPDU = ACK_PDU_BASE_SIZE;
            countOfAckInfoEntries = 0;
            while (
                    i < ackInfoEntries.length &&                               // There is more ackInfoEntries
                    lengthOfPDU + ackInfoEntries[i].size() < pduMaxSize &&     // There is room for more
                    !overflows16bit(lengthOfPDU + ackInfoEntries[i].size()) && // New length does not overflow
                    !overflows16bit(countOfAckInfoEntries + 1)                 // New count of ackInfoEntries does not overflow
                    ) {
                lengthOfPDU += ackInfoEntries[i].size();
                packageAckInfoEntries.add(ackInfoEntries[i]);
                i++;
                countOfAckInfoEntries++;
            }


            // Add packet to list
            try {
                pduList.add(new AckPdu(lengthOfPDU, sourceID, priority, countOfAckInfoEntries, packageAckInfoEntries));
            } catch (IllegalArgumentException e) {
                Log.writeLine(Log.LOG_LEVEL_NORMAL, e.getMessage());
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.writeLine(Log.LOG_LEVEL_NORMAL, "Length of PDU field too short to fit all packet data into " +
                		"binary in method create in class AckPdu");
            }
        }

        return pduList;
    }

    /**
     * Initializes a single Ack_PDU.
     * 
     * @param lengthOfPDU
     *          The length of this PDU
     * @param sourceID
     *          Source ID of this node
     * @param priority
     *          P_Mul priority of this packet
     * @param countOfAckInfoEntries
     *          Total amount of AckInfoEntries
     * @param ackInfoEntries
     *          All ackInfoEntries to send in this PDU
     * @throws IllegalArgumentException
     *          Length of PDU field overflows
     * @throws ArrayIndexOutOfBoundsException
     *          Length of PDU field too short to fit all packet data into binary
     */
    private AckPdu(int lengthOfPDU, int sourceID, int priority, short countOfAckInfoEntries,
            ArrayList<AckInfoEntry> ackInfoEntries) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {

        initCommonBinary(lengthOfPDU, priority, (byte) 0, Ack_PDU, sourceID);
        
        addToBinary(COUNT_OF_ACK_INFO_ENTRIES_OFFSET, countOfAckInfoEntries);

        // Adds destination entries 
        int i = ACK_INFO_ENTRIES_OFFSET;
        for (AckInfoEntry ackInfoEntry : ackInfoEntries) {
            byte[] destBinary = ackInfoEntry.getBinary();
            for (int j=0; j < destBinary.length; j++, i++) {
                binary[i] = destBinary[j];
            }
        }

        setChecksum();
    }

    /**
     * Returns how many ack info entries are in this Ack_PDU.
     * @return {@code int} count of ack info entries
     */
    public int getCountOfAckInfoEntries() {
        return unsigned( concatenateBytes( binary[COUNT_OF_ACK_INFO_ENTRIES_OFFSET],
                                            binary[COUNT_OF_ACK_INFO_ENTRIES_OFFSET+1] ) );
    }

    /**
     * Returns a list of all AckInfoEntries in this AckPdu.
     * @return {@code AckInfoEntry[]} list of AckInfoEntries
     */
    public AckInfoEntry[] getAckInfoEntries() {
        int countOfAckInfoEntries = getCountOfAckInfoEntries();
        AckInfoEntry[] ackInfoEntries = new AckInfoEntry[countOfAckInfoEntries]; 

        int length = getLengthOfPDU();
        for ( int i = 0, offset = ACK_INFO_ENTRIES_OFFSET; i < countOfAckInfoEntries && offset < length; i++) {
            try {
                AckInfoEntry e = new AckInfoEntry(this.binary, offset);
                ackInfoEntries[i] = e;
                offset += e.size();
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.writeLine(Log.LOG_LEVEL_NORMAL, "Length field of AckInfoEntry does not exist in binary. " +
                        "Method getAckInfoEntries() in class AckPdu.");
            }
        }
        return ackInfoEntries;
    }

    /**
     * The AckInfoEntry is a part of the AckPdu. It contains data for which
     * sources have acknowledged what data PDUs. 
     * 
     * @author Erik Lothe
     *
     */
    public static class AckInfoEntry { 

        // --- OCTET PLACEMENTS -------------------------------------------- //
        private static final int LENGTH_OF_ACK_ENTRY_OFFSET         =  0;
        private static final int SOURCE_ID_FIELD_OFFSET             =  2;
        private static final int MESSAGE_ID_FIELD_OFFSET            =  6;
        private static final int LIST_OF_MISSING_DATA_PDU_OFFSET    = 10;
        // ----------------------------------------------------------------- //

        // --- CONSTANTS --------------------------------------------------- //
        /**
         * The size of an AckInfoEntry excluding missing sequence numbers
         */
        public static final int  ACK_INFO_ENTRY_BASE_SIZE           = 10;
        // ----------------------------------------------------------------- //

        private byte[] binary;

        /**
         * Creates a AckInfoEntry object
         * @param sourceID 
         *              Source of the message we are Ack'ing
         * @param messageID 
         *              Message id of message we are Ack'ing 
         * @param missing 
         *              Array of all missing data_PDUs' sequence numbers
         * @throws IllegalArgumentException
         *              If input array 'missing' causes the total size to overflow 16 bits
         *          
         */
        public AckInfoEntry(int sourceID, int messageID, short[] missing) throws IllegalArgumentException {
            if ( missing.length > Configuration.getMm() ) {
                throw new IllegalArgumentException("Attempting to add " + missing.length + 
                        " new sequence numbers to the list of missing data_DPUs" +
                        " in an AckInfoEntry, when maximum (ACP142Config.MM) is set to " + 
                        Configuration.getMm() + ".: AckInfoEntry in class AchInfoEntry");
            }
            int length = (ACK_INFO_ENTRY_BASE_SIZE + ( missing.length * 2 ));
            if (overflows16bit(length)) {
                throw new IllegalArgumentException("Length of AckInfoEntry overflows 16 bit");
            }
            binary = new byte[length];

            try {
                binary[LENGTH_OF_ACK_ENTRY_OFFSET]     = (byte) (length >>> 8);
                binary[LENGTH_OF_ACK_ENTRY_OFFSET + 1] = (byte)  length;
                binary[SOURCE_ID_FIELD_OFFSET]         = (byte) (sourceID >>> 24);
                binary[SOURCE_ID_FIELD_OFFSET + 1]     = (byte) (sourceID >>> 16);
                binary[SOURCE_ID_FIELD_OFFSET + 2]     = (byte) (sourceID >>> 8);
                binary[SOURCE_ID_FIELD_OFFSET + 3]     = (byte)  sourceID;
                binary[MESSAGE_ID_FIELD_OFFSET]        = (byte) (messageID >>> 24);
                binary[MESSAGE_ID_FIELD_OFFSET + 1]    = (byte) (messageID >>> 16);
                binary[MESSAGE_ID_FIELD_OFFSET + 2]    = (byte) (messageID >>> 8);
                binary[MESSAGE_ID_FIELD_OFFSET + 3]    = (byte)  messageID;
    
                int offset = LIST_OF_MISSING_DATA_PDU_OFFSET;
                for ( int i=0; i<missing.length; i++ ) {
                    binary[offset++] = (byte) (missing[i] >>> 8);
                    binary[offset++] = (byte) (missing[i]);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.writeLine(Log.LOG_LEVEL_NORMAL, "Packet data could not fit into binary in constructor in class " +
                		"AckInfoEntry");
            }
        }

        /**
         * Creates an AckInfoEntry instance from a slice of binary from a given offset
         * @param binary
         *          Byte array representing the binary
         * @param offset 
         *          Offset for first byte to count
         * @throws ArrayIndexOutOfBoundsException
         *          Length field of AckInfoEntry does not exist in binary
         */
        private AckInfoEntry(byte[] binary, int offset) throws ArrayIndexOutOfBoundsException {
            int length;
            length = (int) (binary[offset] & 0xff << 8) | (binary[offset + 1] & 0xff);
            this.binary = new byte[ length ];
            for ( int i = 0; i < length; ++i) {
                this.binary[ i ] = binary[ i + offset ];
            }
        }

        /**
         * Returns the binary of this ack info entry
         * @return binary
         */
        private byte[] getBinary() {
            return binary;
        }

        /**
         * Returns the size of the binary of this ack info entry
         * @return size of binary
         */
        public int size() {
            return binary.length;
        }

        /**
         * This field holds the identifier of the transmitting note. Its value 
         * is equivalent to the value "sourceId" of the corresponding AddressPDU.
         * @return sourceID
         */
        public int getSourceID() {
            return concatenateBytes( binary[SOURCE_ID_FIELD_OFFSET],
                                      binary[SOURCE_ID_FIELD_OFFSET+1], 
                                      binary[SOURCE_ID_FIELD_OFFSET+2], 
                                      binary[SOURCE_ID_FIELD_OFFSET+3] );
        }

        /**
         * Returns the unique identifier created within the scope of Source_ID by 
         * the transmitter
         * @return message ID
         */
        public int getMessageID() {
            return concatenateBytes( binary[MESSAGE_ID_FIELD_OFFSET],
                                      binary[MESSAGE_ID_FIELD_OFFSET+1], 
                                      binary[MESSAGE_ID_FIELD_OFFSET+2],
                                      binary[MESSAGE_ID_FIELD_OFFSET+3] );
        }

        /**
         * Returns an array containing all sequence numbers we are missing for 
         * this message contained in this ack info entry.
         * @return missing sequence numbers
         */
        public int[] getMissingSequenceNumbers() {
            int length;
            length = unsigned ( concatenateBytes( binary[LENGTH_OF_ACK_ENTRY_OFFSET],
                                                  binary[LENGTH_OF_ACK_ENTRY_OFFSET+1] )
                                                   ) - ACK_INFO_ENTRY_BASE_SIZE;
            length /= 2; //short is 2 bytes
            int[] sequenceNumbers = new int[ length ];
            int offset = LIST_OF_MISSING_DATA_PDU_OFFSET; 
            for (int i = 0; i < length; i++) {
                sequenceNumbers[i] = unsigned( concatenateBytes(binary[offset++],
                                                                binary[offset++]));
            }
            return sequenceNumbers;
        }
    }
}