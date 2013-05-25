package no.ntnu.acp142;

/*
 * Copyright (c) 2013, Thomas Martin Schmid
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
 * This is the interface that needs to be implemented to write custom a
 * EMCON handler; to use a different higher management function to determine
 * which nodes are in EMCON mode, to notify them and so on.
 * 
 * The handler passed is responsible for keeping track of the Emcon states
 * of all ACP142 instances on all nodes of the network. It is called
 * whenever our state is changed or when other node's states are queried.
 * If no EmconHandler is created, a DefaultEmconHandler is created and used.
 * 
 * @author Thomas Martin Schmid
 * 
 */
public interface IEmconHandler {

    /**
     * When EMCON is entered, this is called before the internal state is
     * changed.
     * 
     * @param nodeId
     *            source ID of the node which entered EMCON.
     */
    public void enterEmcon( int nodeId );

    /**
     * When EMCON is left, this is called before the internal state is changed.
     * 
     * @param nodeId
     *            source ID of the node which left EMCON.
     */
    public void leaveEmcon( int nodeId );

    /**
     * This function must return the EMCON state of the wanted node.
     * 
     * @param nodeId
     *            of node we want the state of.
     * @return True if it is in EMCON
     */
    public boolean isInEmcon( int nodeId );

}
