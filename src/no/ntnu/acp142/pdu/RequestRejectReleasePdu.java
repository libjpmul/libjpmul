package no.ntnu.acp142.pdu;

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
 * This class handles the following PDUs: <br>
 * * RequestPdu <br>
 * * RejectPdu <br>
 * * ReleasePdu <br>
 * 
 * @author Erik Lothe
 *
 */
public class RequestRejectReleasePdu extends Pdu {

    // --- OCTET PLACEMENTS ------------------------------------------------ //
    private static final int MSID_OFFSET                            = 12;
    private static final int MULTICAST_GROUP_ADDRESS_OFFSET         = 16;
    // --------------------------------------------------------------------- //

    // --- CONSTANTS ------------------------------------------------------- //
    /**
     * The size of RequestPdu, RejectPdu, and ReleasePdu.
     */
    public static final short REQUEST_REJECT_RELEASE_PDU_SIZE     = 4*5;
    // --------------------------------------------------------------------- //

    /**
     * <b>This method will be removed shortly. Its new equivalent is RequestPdu.create()</b>.<br>
     * Creates a Request PDU. <br>
     * "The Request_PDU is distributed by a node wishing to initiate a new 
     * multicast group within T_Nodes, using group GG and port TPORT."
     * @param sourceId
     *          Source ID
     * @param messageId
     *          Message ID
     * @param multicastGroupAddress
     *          Address of multicast group
     * @return RequestPdu
     */
    @Deprecated
    public static RequestRejectReleasePdu createRequestPdu(int sourceId, int messageId, int multicastGroupAddress) {
        return new RequestRejectReleasePdu(Request_PDU, sourceId, messageId, multicastGroupAddress);
    }

    /**
     * <b>This method will be removed shortly. Its new equivalent is RejectPdu.create().</b><br>
     * Creates a Reject PDU. <br>
     * "The Reject_PDU is used by a member of T_Nodes which already "owns" the 
     * multicast group. The Reject_PDU is sent to the requesting node in 
     * unicast mode.
     * @param sourceId
     *          Source ID
     * @param messageId
     *          Message ID
     * @param multicastGroupAddress
     *          Address of multicast group
     * @return RejectPdu
     */
    @Deprecated
    public static RequestRejectReleasePdu createRejectPdu(int sourceId, int messageId, int multicastGroupAddress) {
        return new RequestRejectReleasePdu(Reject_PDU, sourceId, messageId, multicastGroupAddress);
    }

    /**
     * <b>This method will be removed shortly. Its new equivalent is ReleasePdu.create().</b><br>
     * Creates a Release PDU. <br>
     * "The Release_PDU is used in the following two situations: <br>
     * <br>
     * (1) after the sender has received a Reject_PDU, <br>
     * (2) after a transmission has finished. <br>
     * <br>
     * This PDU type is used to inform those members of T_Nodes, which senders 
     * have relinquished particular multicast addresses."
     * @param sourceId
     *          Source ID
     * @param messageId
     *          Message ID
     * @param multicastGroupAddress
     *          Address of multicast group
     * @return ReleasePdu
     */
    @Deprecated
    public static RequestRejectReleasePdu createReleasePdu(int sourceId, int messageId, int multicastGroupAddress) {
        return new RequestRejectReleasePdu(Release_PDU, sourceId, messageId, multicastGroupAddress);
    }


    /**
     * Initializes a RequestPdu, RejectPdu or ReleasePdu from binary.
     * @param binary
     *          Byte array representing the binary of Pdu
     */
    protected RequestRejectReleasePdu(byte[] binary) {
        this.binary = binary;
    }

    /**
     * Initializes a RequestRejectReleasePdu instance.
     * @param pduType
     *          Type of PDU (Request, Reject or Release PDU)
     * @param sourceId
     *          Source ID
     * @param messageId
     *          Message ID
     * @param multicastGroupAddress
     *          Address of multicast group
     */
    protected RequestRejectReleasePdu(byte pduType, int sourceId, int messageId, int multicastGroupAddress) {
        try {
            initCommonBinary(REQUEST_REJECT_RELEASE_PDU_SIZE, (byte)0, (byte)0, pduType, sourceId);
        } catch (IllegalArgumentException e) {
            Log.writeLine(Log.LOG_LEVEL_NORMAL, "Length of PDU field overflows in constructor in class " +
            		"RequestRejectReleasePdu");
        }
        
        try {
            addToBinary(MSID_OFFSET,                    messageId);
            addToBinary(MULTICAST_GROUP_ADDRESS_OFFSET, multicastGroupAddress);
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.writeLine(Log.LOG_LEVEL_NORMAL, "Length of PDU field too small to contain all packet " +
                    "data in constructor in class RequestRejectReleasePdu");
        }
        setChecksum();
    }

    /**
     * Returns the unique identifier created within the scope of Source_ID by 
     * the transmitter
     * @return message ID
     */
    public int getMessageId() {
        return concatenateBytes(binary[MSID_OFFSET],
                                 binary[MSID_OFFSET+1],
                                 binary[MSID_OFFSET+2],
                                 binary[MSID_OFFSET+3]);
    }

    /**
     * Returns the address of a multicast group to be requested, rejected or 
     * released.
     * @return multicast group address
     */
    public int getMulticastGroupAddress() {
        return concatenateBytes(binary[MULTICAST_GROUP_ADDRESS_OFFSET],
                                 binary[MULTICAST_GROUP_ADDRESS_OFFSET+1],
                                 binary[MULTICAST_GROUP_ADDRESS_OFFSET+2],
                                 binary[MULTICAST_GROUP_ADDRESS_OFFSET+3]);
    }
}
