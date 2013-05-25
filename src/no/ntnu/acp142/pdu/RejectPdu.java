package no.ntnu.acp142.pdu;

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
 * Class to handle the Reject PDU
 * @author Erik Lothe
 *
 */
public class RejectPdu extends RequestRejectReleasePdu {

    /**
     * Initializes a RejectPdu from binary
     * @param binary
     *          Byte array representing PDU
     */
    protected RejectPdu(byte[] binary) {
        super(binary);
    }
    
    /**
     * Initializes a RejectPdu instance.
     * @param sourceId
     *          Source ID
     * @param messageId
     *          Message ID
     * @param multicastGroupAddress
     *          Address of multicast group
     */
    private RejectPdu(int sourceId, int messageId, int multicastGroupAddress) {
        super(Reject_PDU, sourceId, messageId, multicastGroupAddress);
    }
    
    /**
     * Creates a Reject PDU. <br>
     * The Reject_PDU is used by a member of T_Nodes which already "owns" the 
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
    public static RejectPdu create(int sourceId, int messageId, int multicastGroupAddress) {
        return new RejectPdu(sourceId, messageId, multicastGroupAddress);
    }
}
