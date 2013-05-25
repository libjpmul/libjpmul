package no.ntnu.acp142.pdu;

import java.util.ArrayList;

import no.ntnu.acp142.Configuration;
import no.ntnu.acp142.Log;

/*
 * Copyright (c) 2013, Erik Lothe
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
 * AddressPdu <br>
 * Used to announce intended recipients of a subsequent message.
 * 
 * @author Erik Lothe
 * 
 */
public class AddressPdu extends Pdu {

    // --- OCTET PLACEMENTS ------------------------------------------------ //
    private static final int TOTAL_NUMBER_OF_PDUS_OFFSET            =  4;
    private static final int MSID_OFFSET                            = 12;
    private static final int EXPIRY_TIME_OFFSET                     = 16;
    private static final int COUNT_OF_DESTINATION_ENTRIES_OFFSET    = 20;
    private static final int LENGTH_OF_RESERVED_FIELD_OFFSET        = 22;
    private static final int DESTINATION_ENTRIES_OFFSET             = 24;
    // --------------------------------------------------------------------- //

    // --- CONSTANTS ------------------------------------------------------- //
    /**
     * The size of an AddressPdu excluding all destination entries
     */
    public static final int ADDRESS_PDU_BASE_SIZE               = 4 * 6;
    // --------------------------------------------------------------------- //

    /**
     * Creates an AddressPdu from binary
     * 
     * @param binary
     *          The byte array representing the PDU
     */
    protected AddressPdu(byte[] binary) {
        this.binary = binary;
    }

    /**
     * Creates an AddressPdu
     * 
     * @param priority
     *          P_Mul priority of the packet
     * @param totalNumberOfPdus
     *          Total number of subsequent Data_PDUs
     * @param sourceId
     *          ID of sender
     * @param messageId
     *          Message ID
     * @param expiryTime
     *          Expiry time in Unix time
     * @param destinationEntries
     *          List of destination entry instances
     * @param lengthOfReservedField
     *          The size of the reserved field
     * @return A list of AddressPdus
     */
    public static ArrayList<AddressPdu> create( int priority, short totalNumberOfPdus, int sourceId, int messageId,
            long expiryTime, DestinationEntry[] destinationEntries, int lengthOfReservedField ) {

        int pduMaxSize = Configuration.getPduMaxSize();
        ArrayList<AddressPdu> pduList = new ArrayList<AddressPdu>();

        int lengthOfPDU;
        byte map;
        int destinationEntrySize = DestinationEntry.DESTINATION_ENTRY_BASE_SIZE + lengthOfReservedField;

        ArrayList<DestinationEntry> packageDestinationEntries;

        // For all destination entries
        int i = 0;
        do {
            // Create an AddressPdu and fit as many destination entries as 
            // possible
            packageDestinationEntries = new ArrayList<DestinationEntry>();
            lengthOfPDU = ADDRESS_PDU_BASE_SIZE;
            while (i < destinationEntries.length &&                         // There are more destination entries
                    lengthOfPDU + destinationEntrySize < pduMaxSize &&      // There is room for another entry
                    !overflows16bit(lengthOfPDU + destinationEntrySize) &&  // New PDU length will not overflow
                    !overflows16bit(packageDestinationEntries.size() + 1)   // New count of destination entries will not overflow
                    ) {

                lengthOfPDU += destinationEntrySize;
                if ( destinationEntries[i].size() != destinationEntrySize ) {
                    throw new IllegalArgumentException("At least one destination entry's length of reserved field "
                            + "differs from given length of reserved field");
                }
                packageDestinationEntries.add(destinationEntries[i]);
                i++;
            }

            // Set map variable
            if ( i == destinationEntries.length ) {
                map = 0b00;
            } else {
                map = 0b01;
            }
            if ( pduList.size() > 0 ) {
                map |= 0b10;
            }

            // Add packet to list
            try {
                pduList.add(new AddressPdu(lengthOfPDU, priority, map, totalNumberOfPdus, sourceId, messageId, expiryTime,
                    lengthOfReservedField, packageDestinationEntries));
            } catch (IllegalArgumentException e) {
                Log.writeLine(Log.LOG_LEVEL_NORMAL, e.getMessage());
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.writeLine(Log.LOG_LEVEL_NORMAL, "Packet data could not fit into binary in method create() in class " +
                		"AddressPdu");
            }
            
        } while (i < destinationEntries.length);

        return pduList;
    }

    /**
     * Initializes a single AddressPdu.
     * 
     * @param lengthOfPDU
     *          Total length of this PDU
     * @param priority
     *          P_Mul priority
     * @param map
     *          The MAP field of this PDU. See JavaDoc for getMap() in class Pdu
     * @param totalNumberOfPdus
     *          The total number of Data_PDUs of the message 
     * @param sourceId
     *          ID of sender
     * @param messageId
     *          Message ID
     * @param expiryTime
     *          Expiry time in Unix time
     * @param lengthOfReservedField
     *          Length of the reserved field
     * @param destinationEntries
     *          List of DestinationEntry instances
     * @throws IllegalArgumentException
     *              If length of PDU field overflows
     * @throws ArrayIndexOutOfBoundsException
     *              If packet data could not fit into binary
     */
    private AddressPdu( int lengthOfPDU, int priority, byte map, int totalNumberOfPdus, int sourceId, int messageId,
            long expiryTime, int lengthOfReservedField, ArrayList<DestinationEntry> destinationEntries ) 
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException { 

        byte pduType = Address_PDU;
        int countOfDestinationEntries = destinationEntries.size();

        initCommonBinary(lengthOfPDU, priority, map, pduType, sourceId);

        addToBinary(TOTAL_NUMBER_OF_PDUS_OFFSET,         (short) totalNumberOfPdus);
        addToBinary(MSID_OFFSET,                         (int)   messageId);
        addToBinary(EXPIRY_TIME_OFFSET,                  (int)   expiryTime);
        addToBinary(COUNT_OF_DESTINATION_ENTRIES_OFFSET, (short) countOfDestinationEntries);
        addToBinary(LENGTH_OF_RESERVED_FIELD_OFFSET,     (short) lengthOfReservedField);

        // Adds destination entries
        int i = DESTINATION_ENTRIES_OFFSET;
        int destinationEntrySize = DestinationEntry.DESTINATION_ENTRY_BASE_SIZE + lengthOfReservedField;

        for (DestinationEntry destinationEntry : destinationEntries) {
            byte[] destinationEntryBinary = destinationEntry.getBinary();
            for (int j = 0; j < destinationEntrySize; j++, i++) {
                binary[i] = destinationEntryBinary[j];
            }
        }

        setChecksum();
    }

    /**
     * Returns total number of Data_PDUs of the message.
     * 
     * @return total number of PDUs
     */
    public short getTotalNumberOfPDUs( ) {
        return concatenateBytes( binary[TOTAL_NUMBER_OF_PDUS_OFFSET],
                                  binary[TOTAL_NUMBER_OF_PDUS_OFFSET + 1]);
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
     * Returns the expiry time of the message (Unix Time).
     * 
     * @return expiry time
     */
    public long getExpiryTime( ) {
        return unsigned(concatenateBytes(binary[EXPIRY_TIME_OFFSET],
                                          binary[EXPIRY_TIME_OFFSET + 1],
                                          binary[EXPIRY_TIME_OFFSET + 2], 
                                          binary[EXPIRY_TIME_OFFSET + 3]));
    }

    /**
     * Returns the count of destination entries in this AddressPdu.
     * 
     * @return count of destination entries
     */
    public int getCountOfDestinationEntries( ) {
        return unsigned(concatenateBytes(binary[COUNT_OF_DESTINATION_ENTRIES_OFFSET],
                                          binary[COUNT_OF_DESTINATION_ENTRIES_OFFSET + 1]));
    }

    /**
     * Returns the length of the reserved field.
     * 
     * @return length of the reserved field
     */
    public int getLengthOfReservedField( ) {
        return unsigned(concatenateBytes(binary[LENGTH_OF_RESERVED_FIELD_OFFSET],
                                          binary[LENGTH_OF_RESERVED_FIELD_OFFSET + 1]));
    }

    /**
     * Returns all DestinationEntries associated with this AddressPdu.
     * 
     * @return list of destination entries
     */
    public ArrayList<DestinationEntry> getDestinationEntries( ) {
        ArrayList<DestinationEntry> destinationEntries = new ArrayList<DestinationEntry>();
        int length = getLengthOfPDU();
        int lengthOfReservedField = getLengthOfReservedField();
        int lengthOfDestinationEntry = DestinationEntry.DESTINATION_ENTRY_BASE_SIZE + lengthOfReservedField;

        for (int offset = DESTINATION_ENTRIES_OFFSET; offset < length;) {
            DestinationEntry e = new DestinationEntry(this.binary, offset, lengthOfReservedField);
            destinationEntries.add(e);
            offset += lengthOfDestinationEntry;
        }
        return destinationEntries;
    }

    /**
     * Static nested class representing a Destination Entry. <br>
     * 
     * @author Erik Lothe
     * 
     */
    public static class DestinationEntry {

        // --- OCTET PLACEMENTS -------------------------------------------- //
        private static final int DESTINATION_ID_OFFSET               = 0;
        private static final int MESSAGE_SEQUENCE_NUMBER_OFFSET      = 4;
        private static final int RESERVED_FIELD_OFFSET               = 8;
        // ----------------------------------------------------------------- //

        // --- CONSTANTS --------------------------------------------------- //
        /**
         * The size of a DestinationEntry excluding the reserved field
         */
        public static final int  DESTINATION_ENTRY_BASE_SIZE     = 4 * 2;
        // ----------------------------------------------------------------- //

        private byte[] binary;

        /**
         * Creates a DestinationEntry object.
         * 
         * @param destinationID
         *          ID of recipient
         * @param messageSequenceNumber
         *          Message sequence number
         * @param reservedField
         *          Reserved field. This can be null if no reserved field is needed.
         */
        public DestinationEntry(int destinationID, int messageSequenceNumber, byte[] reservedField) {
            int lengthOfReservedField;
            if (reservedField == null) {
                lengthOfReservedField = 0;
            } else {
                lengthOfReservedField = reservedField.length;
            }
            int reservedFieldMaxSize = Configuration.getPduMaxSize() - ADDRESS_PDU_BASE_SIZE
                    - DestinationEntry.DESTINATION_ENTRY_BASE_SIZE;

            if ( lengthOfReservedField > reservedFieldMaxSize ) {
                throw new IllegalArgumentException("Reserved field too large to fit into a DestinationEntry");
            }

            this.binary = new byte[DESTINATION_ENTRY_BASE_SIZE + lengthOfReservedField];

            binary[DESTINATION_ID_OFFSET] =     (byte) (destinationID >>> 24);
            binary[DESTINATION_ID_OFFSET + 1] = (byte) (destinationID >>> 16);
            binary[DESTINATION_ID_OFFSET + 2] = (byte) (destinationID >>> 8);
            binary[DESTINATION_ID_OFFSET + 3] = (byte)  destinationID;

            binary[MESSAGE_SEQUENCE_NUMBER_OFFSET] =     (byte) (messageSequenceNumber >>> 24);
            binary[MESSAGE_SEQUENCE_NUMBER_OFFSET + 1] = (byte) (messageSequenceNumber >>> 16);
            binary[MESSAGE_SEQUENCE_NUMBER_OFFSET + 2] = (byte) (messageSequenceNumber >>> 8);
            binary[MESSAGE_SEQUENCE_NUMBER_OFFSET + 3] = (byte)  messageSequenceNumber;

            for (int i = 0; i < lengthOfReservedField; i++) {
                binary[RESERVED_FIELD_OFFSET + i] = reservedField[i];
            }
        }

        /**
         * Creates a DestinationEntry object from binary
         * 
         * @param binary
         *            binary array
         * @param offset
         *            offset into binary
         * @param lengthOfReservedField
         *            length of the reserved field
         */
        private DestinationEntry(byte[] binary, int offset, int lengthOfReservedField) {
            int length = DESTINATION_ENTRY_BASE_SIZE + lengthOfReservedField;
            this.binary = new byte[length];
            for (int i = 0; i < length; ++i) {
                this.binary[i] = binary[offset + i];
            }
        }

        /**
         * Returns the binary of this destination entry
         * 
         * @return binary
         */
        private byte[] getBinary( ) {
            return binary;
        }

        /**
         * Returns the size of the binary of this destination entry
         * 
         * @return size of binary
         */
        private int size( ) {
            return binary.length;
        }

        /**
         * Returns the destination ID.
         * 
         * @return destination ID
         */
        public int getDestinationID( ) {
            return concatenateBytes(binary[DESTINATION_ID_OFFSET],
                                     binary[DESTINATION_ID_OFFSET + 1],
                                     binary[DESTINATION_ID_OFFSET + 2], 
                                     binary[DESTINATION_ID_OFFSET + 3]);
        }

        /**
         * Returns the message sequence number.
         * 
         * @return message sequence number
         */
        public int getMessageSequenceNumber( ) {
            return concatenateBytes(binary[MESSAGE_SEQUENCE_NUMBER_OFFSET],
                                     binary[MESSAGE_SEQUENCE_NUMBER_OFFSET + 1],
                                     binary[MESSAGE_SEQUENCE_NUMBER_OFFSET + 2], 
                                     binary[MESSAGE_SEQUENCE_NUMBER_OFFSET + 3]);
        }

        /**
         * "The reserved field is provided for future expansion."
         * 
         * @return reserved field
         */
        public byte[] getReservedField( ) {
            byte[] reservedField = new byte[this.binary.length - RESERVED_FIELD_OFFSET];
            for (int i = 0; i < reservedField.length; i++) {
                reservedField[i] = this.binary[RESERVED_FIELD_OFFSET + i];
            }
            return reservedField;
        }
    }
}
