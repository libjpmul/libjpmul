package no.ntnu.acp142.rdt;

import java.net.InetAddress;
import java.util.ArrayList;

import no.ntnu.acp142.pdu.AckPdu;
import no.ntnu.acp142.pdu.AnnouncePdu;
import no.ntnu.acp142.pdu.DiscardMessagePdu;
import no.ntnu.acp142.pdu.Pdu;
import no.ntnu.acp142.pdu.RequestRejectReleasePdu;


/*
 * Copyright (c) 2013, Erik Lothe, Bjørn Tungesvik
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
 * This class represents an Entry consisting of pure PDU packets.
 * 
 * @author Erik Lothe, Bjørn Tungesvik
 * 
 */
public class PacketEntry extends Entry {

    private ArrayList<Pdu> pdus;
    
    /**
    * Destination address for packet
    */
    private InetAddress destinationAddress;
    
    /**
     * Initializes a PacketEntry
     * 
     * @param pdus PDUs to include in the entry.
     */
    public PacketEntry(ArrayList<Pdu> pdus) {
        this.pdus = pdus;
    }

    /**
     * Default constructor
     */
    public PacketEntry() {
    }

    /**
     * Add AckPdus to a PacketEntry
     * 
     * @param ackPdus
     *            of AckPdu
     */
    public void addAckPdus( ArrayList<AckPdu> ackPdus ) {
        this.pdus = new ArrayList<Pdu>();
        for (AckPdu ackPdu : ackPdus) {
            this.pdus.add(ackPdu);
        }
    }

    /**
     * Add a DiscardPdu to PacketEntry
     * 
     * @param discard pdu to add
     */
    public void addDiscardMessagedPdu( DiscardMessagePdu discard ) {
        pdus = new ArrayList<Pdu>();
        pdus.add(discard);
    }
    
    /**
     * Add an AnnouncePdu to PacketEntry
     * 
     * @param announcePdu to add
     */
    
    public void addAnnouncePdu(AnnouncePdu announcePdu){
        pdus = new ArrayList<Pdu>();
        pdus.add(announcePdu);
    }
    
    /**
     * Add Request, Reject or Release PDU to PacketEntry 
     * 
     * @param pdu to add
     */
    public void addRequestReleaseReject(RequestRejectReleasePdu pdu){
        pdus = new ArrayList<Pdu>();
        pdus.add(pdu);
    }
    
    /**
     * Add destination address to packetEntry
     * 
     * @param destinationAddress of the packet entry.
     */
    public void addDestinationAddress(InetAddress destinationAddress){
        this.destinationAddress = destinationAddress;
    }
    
    /**
     * Get destination address for this packet
     * 
     * @return address
     */
    public InetAddress getDestinationAddress(){
        return destinationAddress;
    }
    

    @Override
    public EntryType getType( ) {
        return EntryType.PACKET_ENTRY;
    }

    @Override
    public ArrayList<Pdu> getPdus( ) {
        return pdus;
    }

}
