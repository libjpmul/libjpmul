package no.ntnu.acp142.pdu;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
 * Contains "a list of nodes, which are to receive a specific message to the 
 * nodes of R_Nodes".
 * 
 * @author Erik Lothe
 *
 */
public class AnnouncePdu extends Pdu {

    // --- OCTET PLACEMENTS ------------------------------------------------ //
    private static final int COUNT_OF_DESTINATION_IDS_OFFSET        =  4;
    private static final int MSID_OFFSET                            = 12;
    private static final int EXPIRY_TIME_OFFSET                     = 16;
    private static final int MULTICAST_GROUP_ADDRESS_OFFSET         = 20;
    private static final int LIST_OF_DESTINATION_IDS_OFFSET         = 24;
    // --------------------------------------------------------------------- //

    // --- CONSTANTS ------------------------------------------------------- //
    /**
     * The size of an AnnouncePdu excluding all destination IDs
     */
    public static final short ANNOUNCE_PDU_BASE_SIZE              = 4*6;
    // --------------------------------------------------------------------- //

    /**
     * Initializes one or more AnnouncePdus, depending on how many 
     * destination IDs there are.
     * @param sourceId
     *          Source ID
     * @param messageId
     *          Message ID
     * @param expiryTime
     *          Expiry time in Unix time
     * @param multicastGroupAddress
     *          Address of multicast group
     * @param destinationIds
     *          List of IDs of recipients
     * @return list of AnnouncePdus
     */
    public static ArrayList<AnnouncePdu> create(int sourceId, int messageId, int expiryTime, 
            int multicastGroupAddress, int[] destinationIds) {

        ArrayList<AnnouncePdu> announcePdus = new ArrayList<AnnouncePdu>();
        byte map;
        int maxSize = Configuration.getPduMaxSize();

        short lengthOfPdu;
        ArrayList<Integer> packageDestinationIds;


        // For all destination IDs
        //for ( int i = 0; i < destinationIds.length; ) {
        int i = 0;
        do {
            // Create an AnnouncePdu and fit as many destination IDs as possible
            packageDestinationIds = new ArrayList<Integer>();
            lengthOfPdu = ANNOUNCE_PDU_BASE_SIZE;
            while (i < destinationIds.length &&       // There are more destination IDs
                    lengthOfPdu + 4 < maxSize &&       // There are room for more destination IDs
                    !overflows16bit( lengthOfPdu + 4 ) // New PDU size does not overflow
                    ) {
                lengthOfPdu += 4;
                packageDestinationIds.add(destinationIds[i]);
                i++;
            }

            // Set MAP variable
            if (i == destinationIds.length) {
                map = 0b00;     // Last one
            } else {
                map = 0b01;     // Not last one     
            }
            if (announcePdus.size() > 0) {
                map |= 0b10;    // Not first
            }

            // Add packet to list
            announcePdus.add(new AnnouncePdu(map, sourceId, messageId, 
                    expiryTime, multicastGroupAddress, packageDestinationIds));

        } while ( i < destinationIds.length );
        return announcePdus;
    }

    /**
     * Initializes an AnnouncePdu
     * @param map
     *          The MAP field of this PDU. See JavaDoc for getMap() in class Pdu
     * @param sourceId
     *          Source ID
     * @param messageId
     *          Message ID
     * @param expiryTime
     *          Expiry time in Unix time
     * @param multicastGroupAddress
     *          Address of multicast group
     * @param destinationIds
     *          List of IDs of recipients
     */
    private AnnouncePdu(byte map, int sourceId, int messageId, int expiryTime, int multicastGroupAddress, 
            ArrayList<Integer> destinationIds) {

        int countOfDestinationIds = destinationIds.size();
        int lengthOfPdu = ANNOUNCE_PDU_BASE_SIZE + countOfDestinationIds * 4;

        try {
            initCommonBinary( lengthOfPdu, (byte)0, map, Announce_PDU, sourceId );
        } catch (IllegalArgumentException e) {
            Log.writeLine(Log.LOG_LEVEL_NORMAL, "Length of PDU field overflows in constructor in class AnnouncePdu");
        }
        
        // Checking for overflow
        if ( overflows16bit(countOfDestinationIds) ) {
            Log.writeLine(Log.LOG_LEVEL_NORMAL, "Field countOfDestinationIds overflows in constructor in class " +
            		"AnnouncePdu");
        }
        
        try {
            addToBinary( COUNT_OF_DESTINATION_IDS_OFFSET, (short) countOfDestinationIds );
            addToBinary( MSID_OFFSET,                     (int)   messageId );
            addToBinary( EXPIRY_TIME_OFFSET,              (int)   expiryTime );
            addToBinary( MULTICAST_GROUP_ADDRESS_OFFSET,  (int)   multicastGroupAddress );
    
            for ( int i = 0; i < countOfDestinationIds; i++ ) {
                addToBinary( LIST_OF_DESTINATION_IDS_OFFSET + i*4, (int) destinationIds.get(i) );
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.writeLine(Log.LOG_LEVEL_NORMAL, "Packet data will not fit into binary array in in constructor in class " +
                    "AnnouncePdu");
        }
        
        setChecksum();
    }

    /**
     * Initializes an AnnouncePdu from binary
     * @param binary
     *          Byte array representing the binary form of the PDU
     */
    protected AnnouncePdu(byte[] binary) {
        this.binary = binary;
    }

    /**
     * Returns the total number of destination IDs within this AnnouncePdu.
     * @return count of destination IDs
     */
    public int getCountOfDestinationIds() {
        return unsigned( concatenateBytes(binary[COUNT_OF_DESTINATION_IDS_OFFSET],
                                           binary[COUNT_OF_DESTINATION_IDS_OFFSET+1]));
    }

    /**
     * Returns the unique identifier created within the scope of Source_ID by 
     * the transmitter.
     * @return message ID
     */
    public int getMessageId() {
        return concatenateBytes(binary[MSID_OFFSET],
                                 binary[MSID_OFFSET + 1],
                                 binary[MSID_OFFSET + 2], 
                                 binary[MSID_OFFSET + 3] );
    }

    /**
     * Returns the expiry time of the message.
     * @return expiry time
     */
    public long getExpiryTime() {
        return unsigned( concatenateBytes(binary[EXPIRY_TIME_OFFSET],
                                           binary[EXPIRY_TIME_OFFSET + 1],
                                           binary[EXPIRY_TIME_OFFSET + 2], 
                                           binary[EXPIRY_TIME_OFFSET + 3] ) );
    }

    /**
     * Returns "the address of the multicast group announced for a message 
     * transfer denoted by Source_ID and MSID".
     * @return multicast group address
     */
    public int getMulticastGroupAddress() {
        return concatenateBytes(binary[MULTICAST_GROUP_ADDRESS_OFFSET],
                                 binary[MULTICAST_GROUP_ADDRESS_OFFSET+1],
                                 binary[MULTICAST_GROUP_ADDRESS_OFFSET+2],
                                 binary[MULTICAST_GROUP_ADDRESS_OFFSET+3]);
    }
    
    /**
     * Returns "the address of the multicast group announced for a message 
     * transfer denoted by Source_ID and MSID".
     * @return multicast group address
     */
    public InetAddress getInetMulticastGroupAddress(){
        byte[] bytes = BigInteger.valueOf(getMulticastGroupAddress()).toByteArray();
        try {
            return InetAddress.getByAddress(bytes);
        } catch (UnknownHostException e) {
            Log.writeLine(Log.LOG_LEVEL_NORMAL, "Multicast group address is invalid");
        }
        return null;
    }

    /**
     * Returns a "list of Destination_IDs of the intended receiving nodes for 
     * the message denoted by source_ID and MSID. A Destination_ID may hold the 
     * Internet address of the receiving node".
     * @return destination IDs
     */
    public int[] getDestinationIds() {
        int length = getCountOfDestinationIds();
        int destinationIds[] = new int[length];

        for ( int i = 0; i < length; i++ ) {
            destinationIds[i] = concatenateBytes(binary[LIST_OF_DESTINATION_IDS_OFFSET + (i * 4)],
                                                 binary[LIST_OF_DESTINATION_IDS_OFFSET + (i * 4) + 1],
                                                 binary[LIST_OF_DESTINATION_IDS_OFFSET + (i * 4) + 2],
                                                 binary[LIST_OF_DESTINATION_IDS_OFFSET + (i * 4) + 3]);
        }

        return destinationIds;
    }
}
