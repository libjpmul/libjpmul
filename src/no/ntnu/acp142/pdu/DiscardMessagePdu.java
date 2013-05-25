/**
 * 
 */
package no.ntnu.acp142.pdu;

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
 * "The Discard_PDU is used to inform the receiving nodes that the transfer of
 * a specific message has been terminated and no further PDUs of this message
 * will be transmitted."
 * 
 * @author Thomas Martin Schmid, Erik Lothe
 */
public class DiscardMessagePdu extends Pdu {

    // --- OCTET PLACEMENTS ------------------------------------------------ //
    private static final int   MSID_OFFSET                          = 12;
    // --------------------------------------------------------------------- //

    // --- CONSTANTS ------------------------------------------------------- //
    /**
     * The size of a DiscardMessagePdu
     */
    public static final short DISCARD_MESSAGE_PDU_SIZE          = 4 * 4;
    // --------------------------------------------------------------------- //

    /**
     * Initializes a Discard_Message_PDU
     * 
     * @param priority
     *          P_Mul priority
     * @param sourceId
     *          Source ID
     * @param messageId
     *          Message ID
     * @return DiscardMessagePdu
     */
    public static DiscardMessagePdu create( int priority, int sourceId, int messageId ) {
        return new DiscardMessagePdu(priority, sourceId, messageId);
    }

    /**
     * Creates a Discard_Message_PDU from binary
     * 
     * @param binary
     *          Byte array representing the PDU
     */
    protected DiscardMessagePdu(byte[] binary) {
        this.binary = binary;
    }

    /**
     * Initializes a Discard_Message_PDU
     * 
     * @param priority
     *          P_Mul priority
     * @param sourceId
     *          Source ID
     * @param messageId
     *          MessageId
     */
    private DiscardMessagePdu(int priority, int sourceId, int messageId) {
        byte pduType = Discard_Message_PDU;

        try {
            initCommonBinary(DISCARD_MESSAGE_PDU_SIZE, priority, (byte) 0, pduType, sourceId);
        } catch (IllegalArgumentException e) {
            Log.writeLine(Log.LOG_LEVEL_NORMAL, e.getMessage());
        }
        
        try {
            addToBinary(MSID_OFFSET, (int) messageId);
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.writeLine(Log.LOG_LEVEL_NORMAL, "Length of PDU field too small to contain all packet data " +
                    "in constructor in class DiscardMessagePdu");
        }

        setChecksum();
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

}
