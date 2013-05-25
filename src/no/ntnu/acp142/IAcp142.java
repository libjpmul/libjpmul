package no.ntnu.acp142;

import java.util.ArrayList;

/*
 * Copyright (c) 2013, Thomas Martin Schmid, Karl Mardoff Kittilsen
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
 * This is the public interface that our ACP142 implementation exposes.
 * It details all methods available to the end user of the protocol,
 * and is the interface that needs to be followed to make alternative
 * implementations compatible with ours.
 * 
 * @author Thomas Martin Schmid, Karl Mardoff Kittilsen
 *
 */
public interface IAcp142 {

    /**
     * Sends the data given to a static multicast group defined by the given
     * destination. Blocks until the message is transmitted or expires; it is
     * only transmitted if not in EMCON mode. Makes no guarantees that nodes
     * in EMCON mode have received the data.
     * 
     * @param data
     *            Data to send
     * @param destinations
     *            Identifiers of destination nodes.
     * @param expiryTime
     *            Unix timestamp of when this message is considered invalid.
     * @param dynamic
     *            Specifies whether to create multicast group dynamically or
     *            not.
     * @param persistent
     * 			  Specifies whether to use persistent groups or not.
     * @param priority
     *            Specifies the priority of the message.
     */
    public void send( byte[] data, ArrayList<Integer> destinations, long expiryTime, boolean dynamic, boolean persistent, int priority );

    /**
     * Sends the data given to a static multicast group defined by the given
     * destination. Blocks until the message is transmitted or expires; it is
     * only transmitted if not in EMCON mode. Makes no guarantees that nodes
     * in EMCON mode have received the data.
     * 
     * @param message
     *            Message to send
     */
    public void send( Acp142Message message );

    /**
     * Receives the next message past to the joined multicast groups, blocking
     * until one arrives. Returns this as an ACP142Message object, where only
     * the applicable fields are set.
     * 
     * @return New ACP142Message object containing message and metadata.
     */
    public Acp142Message receive( );

    /**
     * Puts this instance of ACP142 into EMCON mode. First calls EmconHandler's
     * enterEmcon method, before updating internal state.
     */
    public void enterEmcon( );

    /**
     * Releases this instance of ACP142 from EMCON mode. First calls
     * EmconHandler's leaveEmcon method, before updating internal state.
     */
    public void leaveEmcon( );

    /**
     * When using persistent multicast groups, if we explicitly want to leave a
     * group, give the list of destinations and we will discard the corresponding group.
     * @param destinations List of the destinations
     */
    public void leavePersistentMulticastGroup(ArrayList<Integer> destinations);
}
