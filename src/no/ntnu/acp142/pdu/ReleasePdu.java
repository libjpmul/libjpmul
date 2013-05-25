package no.ntnu.acp142.pdu;

/*
 * Copyright (c) 2013, Erik Lothe
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
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
 * Class to handle the Release PDU
 * @author Erik Lothe
 *
 */
public class ReleasePdu extends RequestRejectReleasePdu {

    /**
     * Initializes a ReleasePdu from binary
     * @param binary
     *          Byte array representing the PDU
     */
    protected ReleasePdu(byte[] binary) {
        super(binary);
    }
    
    /**
     * Initializes a ReleasePdu instance.
     * @param sourceId
     *          Source ID
     * @param messageId
     *          Message ID
     * @param multicastGroupAddress
     *          Address of multicast group
     */
    private ReleasePdu(int sourceId, int messageId, int multicastGroupAddress) {
        super(Release_PDU, sourceId, messageId, multicastGroupAddress);
    }
    
    /**
     * The Release_PDU is used in the following two situations: <br>
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
    public static ReleasePdu create(int sourceId, int messageId, int multicastGroupAddress) {
        return new ReleasePdu(sourceId, messageId, multicastGroupAddress);
    }
}
