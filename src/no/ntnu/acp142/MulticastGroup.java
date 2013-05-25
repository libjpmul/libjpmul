package no.ntnu.acp142;

import java.net.InetAddress;
import java.util.ArrayList;

/*
 * Copyright (c) 2013, Erik Lothe, Karl Mardoff Kittilsen, Bjørn Tungesvik
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
 * 
 * @author Erik Lothe, Karl Mardoff Kittilsen, Bjørn Tungesvik
 *
 */

public class MulticastGroup {

    private InetAddress        multicastAddress;
    private ArrayList<Integer> clientIds;

    /**
     * 
     * Public constructor where you only set the multicast address of this group. You need
     * to set the members later on.
     * 
     * @param multicastAddress of this group.
     */
    public MulticastGroup(InetAddress multicastAddress) {
        this.multicastAddress = multicastAddress;
    }

    /**
     * 
     * Public constructor for a multicast group where you set both the multicast address
     * and the destination id's at once.
     * 
     * @param multicastAddress of this group.
     * @param sourceIds that should be contained in this group.
     */
    public MulticastGroup(InetAddress multicastAddress, ArrayList<Integer> sourceIds) {
        this.multicastAddress = multicastAddress;
        this.clientIds = sourceIds;
    }

    /**
     * 
     * Get the multicast address of this group.
     * 
     * @return multicastAddress of this group.
     */
    public InetAddress getMulticastAddress( ) {
        return multicastAddress;
    }

    /**
     * 
     * Set the multicast address of this group.
     * 
     * @param multicastAddress to set.
     */
    public void setMulticastAddress( InetAddress multicastAddress ) {
        this.multicastAddress = multicastAddress;
    }

    /**
     * 
     * Get the source id's of this multicast group.
     * 
     * @return sourceIds of this multicast group.
     */
    public ArrayList<Integer> getSourceIds( ) {
        return clientIds;
    }

    /**
     * 
     * Set the source id's this multicast group contains.
     * 
     * @param sourceIds of this multicast group.
     */
    public void setSourceIds( ArrayList<Integer> sourceIds ) {
        this.clientIds = sourceIds;
    }

    /**
     * Check if the given array is a subset of the clientId's in this multicast
     * group.
     * 
     * @param destinations
     *            The set that you want to see if is a subset of this
     *            MulticastGroup
     * @return true if destinations is a subset of this MulticastGroup
     */
    public boolean isSubset( ArrayList<Integer> destinations ) {
        for (Integer destination : destinations) {
            boolean found = false;
            for (Integer clientId : clientIds) {
                Log.writeLine(Log.LOG_LEVEL_DEBUG,
                        "Checking if " + destination.toString() + " equals " + clientId.toString());
                if ( destination.equals(clientId) ) {
                    // This destination is contained in this multicast group,
                    // continue to check the others.
                    Log.writeLine(Log.LOG_LEVEL_DEBUG, destination.toString() + " equals " + clientId.toString());
                    found = true;
                }else{
                	Log.writeLine(Log.LOG_LEVEL_DEBUG, "Different");	
                }
                
            }
            if (found == false) {
               Log.writeLine(Log.LOG_LEVEL_VERBOSE, "We did not find the " + destination + " in this multicast group.");
               return false;
            }
        }
        // All destinations had been found in this MulticastGroup.
        return true;
    }

    public String toString( ) {
        String returnString = this.multicastAddress.getHostAddress() + ": ";
        if ( clientIds != null ) {
            for (Integer client : this.clientIds) {
                if ( client != null ) {
                returnString += client.toString() + " ";
                }
            }
        }
        return returnString;
    }
}
