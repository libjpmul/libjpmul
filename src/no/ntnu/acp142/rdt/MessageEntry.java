package no.ntnu.acp142.rdt;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import no.ntnu.acp142.Configuration;
import no.ntnu.acp142.Log;
import no.ntnu.acp142.pdu.AckPdu.AckInfoEntry;
import no.ntnu.acp142.pdu.AddressPdu;
import no.ntnu.acp142.pdu.AddressPdu.DestinationEntry;
import no.ntnu.acp142.pdu.AnnouncePdu;
import no.ntnu.acp142.pdu.DataPdu;
import no.ntnu.acp142.pdu.Pdu;

/*
 * Copyright (c) 2013, Erik Lothe, Bjørn Tungesvik,  Karl Mardoff Kittilsen
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
 * MessageEntry <br>
 * This class contains all the information regarding storage of AddressPDUs and
 * DataPdus. This involves the PDUs themselves, as well as state information to
 * aid the reliable data transfer.
 * 
 * @author Erik Lothe, Bjørn Tungesvik, Karl Mardoff Kittilsen
 * 
 */
public class MessageEntry extends Entry {

    /**
     * Announce PDU (used in dynamic and persistent multicast mode)
     */
    private AnnouncePdu announcePdu = null;
    
    /**
     * Destination address IPv4 and IPv6
     */
    private InetAddress           address;

    /**
     * List containing the dataPdus in correct order
     */
    private DataPduList           dataPdus;
    /**
     * AddressPdu for this entry
     */
    private ArrayList<AddressPdu> addressPdus = null;

    /**
     * Matrix of acked pdus
     */
    private boolean[][]           acked = null;
     
     /**
      * Each message carry a state which is currently in
      */
     private States state;
     
     /**
      * True if the entry is sent using dynamic groups
      */
     private boolean useDynamic = false;
     
     /**
      * Acknowledgment address (unicast address)
      */
     private InetAddress ackAddress = null;
     
     /**
      * The last data pdu received
      */
     private DataPdu lastReceived = null;
     
     /**
      * The highest missing sequence number
      */
     private int highestMissingSequenceNumber;
     
     /**
      * Set to true if the entry uses persistent multicast groups
      */
     private boolean persistent = false;
     
    /**
     * Numbers of retransmissions to nodes in EMCON
     */
     private int emconRtc;
     
     /**
      * Number of transmission of announce pdus
      */
     private int announceCt;
     
     /**
      * The time until next re-transmission. Used when creating re-transmission timers
      */
     private long reTransmissionTime = 0;
     
     /**
      * ACP 142 message priority.
      */
     private int priority;
    
    /**
     * Locks
     */
    private final ReentrantReadWriteLock readWriteLockData = new ReentrantReadWriteLock();
    private final Lock read = readWriteLockData.readLock(); 
    private final Lock write = readWriteLockData.writeLock();
    
    
    private final ReentrantReadWriteLock readWriteLockAcked = new ReentrantReadWriteLock();
    private final Lock readAcked = readWriteLockAcked.readLock();
    private final Lock writeAcked = readWriteLockAcked.writeLock();

    private boolean               ackedInitialized;
    private ArrayList<Integer>    recipients;

    /**
     * Creates a new MessageEntry
     */
    public MessageEntry() {
        dataPdus = new DataPduList();
        addressPdus = new ArrayList<AddressPdu>();
        ackedInitialized = false;
        emconRtc = Configuration.getEmconRtc();
    }

    /**
     * Determines whether all data has been received or not.
     * 
     * @return True if data is ready, False otherwise
     */
    public boolean dataReady( ) {
        read.lock();
        try {
            if (addressPdus == null || addressPdus.isEmpty()) {
                return false;
            }
            int numberOfDataPdus = addressPdus.get(0).getTotalNumberOfPDUs();
            Log.writeLine(Log.LOG_LEVEL_DEBUG, "Expected packet count" + numberOfDataPdus);
            Log.writeLine(Log.LOG_LEVEL_DEBUG, "Actual packet count" + dataPdus.getNumberOfDataPdus());
            return dataPdus.getNumberOfDataPdus() == numberOfDataPdus;
        } finally {
            read.unlock();
        }
    }

    /**
     * Returns the data of this message entry in a byte array.
     * 
     * @return data, null if DataPdus are missing
     */
    public byte[] getData( ) {
        read.lock();
        try {
            byte[] data;
            int size = 0;

            for (DataPdu dataPdu : dataPdus.getPdus()) {
                size += dataPdu.getLengthOfDataFragment();
            }

            data = new byte[size];
            int i = 0;
            for (DataPdu dataPdu : dataPdus.getPdus()) {
                i += dataPdu.getDataFragment(data, i);
            }
            return data;
        } finally {
            read.unlock();
        }
        
    }

    /**
     * Adds an AddressPdu to this message entry.
     * 
     * @param addressPdu to add.
     */
    public void addAddressPdu( AddressPdu addressPdu ) {
        write.lock();
        try {
            addressPdus.add(addressPdu);  
        } finally {
            write.unlock();
        }
       
    }

    /**
     * Adds a DataPdu to this message entry.
     * 
     * @param dataPdu to add.
     */
    public void addDataPdu( DataPdu dataPdu ) {
        write.lock();
        try {
            Log.writeLine(Log.LOG_LEVEL_DEBUG, "Added data pdu");
            dataPdus.add(dataPdu.getSequenceNumber(), dataPdu); 

        } finally {
            write.unlock();
        }
        
    }

    /**
     * Returns the matrix specifying who has acked which data packets.<br>
     * The first dimension represents the recipients, and the second represents
     * the data package. <br>
     * Example: acked[3][8] specifies whether client 3 has acked package with
     * sequence number 8. <br>
     * The client's sourceId is located in the parallel array getSourceIds().
     * 
     * @return acked
     */
    public boolean[][] getAckedMatrix( ) {
        readAcked.lock();
        try {
            return acked; 
        } finally {
            readAcked.unlock();
        }
        
    }

    /**
     * Returns a list of recipients' sourceIds. <br>
     * This list is parallel to the recipient dimension in getAckedMatrix() and
     * can be used to determine which sourceId belongs to which line in the ack
     * matrix.
     * 
     * @return sourceIds
     */
    public ArrayList<Integer> getSourceIds( ) {
        readAcked.lock();
        try {
            return recipients;
        } finally {
            readAcked.unlock();
        }
        
    }

    /**
     * Acknowledge the reception of the packets given by the received ackInfoEntry
     * 
     * @param ackInfoEntry received, to store acknowledge for.
     */
    public void setAcked( AckInfoEntry ackInfoEntry) {
        writeAcked.lock();
        try {
            // Initialize ack matrix if needed
            if ( !ackedInitialized ) {
                initAckedList();
            }

            // Find source position in ack matrix
            int sourceId = ackInfoEntry.getSourceID();
            int sourcePosition = -1;
            
            for (int i = 0; i < recipients.size(); i++) {
                
                if ( sourceId == recipients.get(i) ) {
                    
                    sourcePosition = i;
                    
                    break;
                }
            }

            // Throw exception if no ack was expected from source
            if ( sourcePosition < 0 ) {
                Log.writeLine(Log.LOG_LEVEL_VERBOSE, "Did not expect ack from this source! SourceID = " + sourceId);
                return;
            }

            // Assume all data packets are received by this source
            for (int i = 1; i < acked[sourcePosition].length; i++) {
                
            	acked[sourcePosition][i] = true;
            }

            // Determine which packets are missing
            
            int [] missingSequenceNumbers = ackInfoEntry.getMissingSequenceNumbers();
            
            for (int i = 0; i < missingSequenceNumbers.length; i++) {
                
            	int missing = missingSequenceNumbers[i];
            	System.err.println("Missing:  " + missing);
            	
                acked[sourcePosition][missing] = false;
            }
        } finally {
            writeAcked.unlock();
        }
       

    }

	/**
	 * Initialize the acknowledgment matrix. Only use when sending a message.
	 * The method will throw RuntimeException when used without an AddressPdu.
	 * 
	 * @throws RuntimeException
	 *             Initialize Acknowledgment matrix without an AddressPdu
	 */
    public void initAckedList( ) {
        read.lock();
        try {
        	
        	if(ackedInitialized){
        		return;
        	}
        	
        	//Illegal operation
        	if(addressPdus.get(0) == null){
            	throw new RuntimeException("Initialize Acknowledgment matrix without an AddressPdu makes no sense");
            }
        	
            recipients = new ArrayList<Integer>();
            if ( addressPdus.size() == 0 ) {
                return;
            }
            
            
            
            int numberOfDataPdus = addressPdus.get(0).getTotalNumberOfPDUs();
            int numberOfRecipients = 0;
           
            ArrayList<DestinationEntry> entries = new ArrayList<DestinationEntry>();
            for (AddressPdu addressPdu: addressPdus) {
                entries.addAll(addressPdu.getDestinationEntries());
            }
            
            Log.writeLine(Log.LOG_LEVEL_DEBUG, "Number of destination entries: " + entries.size());
            for (DestinationEntry destinationEntry : entries) {
                Log.writeLine(Log.LOG_LEVEL_DEBUG, "DestinationID: " + destinationEntry.getDestinationID());
                
                recipients.add(destinationEntry.getDestinationID());
                numberOfRecipients++;
            }
            
            acked = new boolean[numberOfRecipients][numberOfDataPdus + 1];
            
            ackedInitialized = true;
        } finally {
            read.unlock();
        }
        
    }
    
    /**
     * Return the recipients of this message
     * 
     * @return ArrayList <Integer> consisting of recipients Ids
     */
    public ArrayList <Integer> getRecipients(){
        if (recipients == null) {
            ArrayList<DestinationEntry> destinationEntries;
            ArrayList<Integer> destinations = new ArrayList<Integer>();

            for (AddressPdu addressPdu : addressPdus) {
                destinationEntries = addressPdu.getDestinationEntries();
                for (DestinationEntry destinationEntry : destinationEntries) {
                    destinations.add(destinationEntry.getDestinationID());
                }
            }
            return destinations;
        } else {
            return recipients;
        }
    }

    /**
     * Returns a list of sequence numbers of the missing data.
     * 
     * @return ArrayList <Integer> of missing sequence numbers
     */
    public ArrayList<Integer> getMissingDataSequenceNumbers( ) {
        read.lock();
        try {
            if ( addressPdus.size() == 0) {
                return null;
            }
            int highestSequenceNumber = addressPdus.get(0).getTotalNumberOfPDUs();
            return dataPdus.getMissing(highestSequenceNumber);
        } finally {
            read.unlock();
        }
       
    }

    @Override
    public EntryType getType( ) {
        return EntryType.MESSAGE_ENTRY;
    }

    @Override
    public ArrayList<Pdu> getPdus( ) {
       read.lock();
       try {
           ArrayList<Pdu> pdus = new ArrayList<Pdu>();
           
           for (Pdu pdu : addressPdus) {
               pdus.add(pdu);
           }
           
           for (Pdu pdu : dataPdus) {
               pdus.add(pdu);
           }
           return pdus;
       } finally {
           read.unlock();
       }
       
    }
    /**
     * Get DataPDUs contained in this entry
     * 
     * @return ArrayList <DataPdu> of all PDUs in this MessageEntry
     */
    
    public ArrayList<DataPdu> getDataPdus(){
        read.lock();
        try{
            ArrayList <DataPdu> dataPdus = new ArrayList <DataPdu>();
            for(Pdu pdu : this.dataPdus){
                if (pdu != null) {
                    dataPdus.add((DataPdu)pdu);
                }
            }
            return dataPdus;
        } finally {
            read.unlock();
        }
    }
    
   
    
    /**
     * Returns the first AddressPdu if it exits, otherwise it returns null
     * 
     * @return AddressPdu 
     */
    public AddressPdu getAddressPdu( ) {
        read.lock();
        try {
            
        	for (AddressPdu addr : addressPdus) {
				if(addr == null){
					Log.writeLine(Log.LOG_LEVEL_DEBUG, "AddressPdu is NULL");
				}else {
					//Found one addressPdu
					return addr;
				}
			}
        
           return null;
        } finally {
            read.unlock();
        }
    }

    /**
     * Returns all AddressPdus in an ArrayList, or null if no exists.
     * 
     * @return ArrayList<AddressPdu> of all addressPdus contained in this MessageEntry
     */
    public ArrayList<AddressPdu> getAddressPdus( ) {
        read.lock();
        try {
            return addressPdus;
        } finally {
            read.unlock();
        }
    }

    /**
     * Set the multicast address for this MessageEntry
     * 
     * @param address multicast address
     */
    public void setMulticastAddress( InetAddress address ) {
        this.address = address;
    }

    /**
     * Get multicast address from this MessageEntry
     *
     *@return InetAddress destination multicast address            
     */
    public InetAddress getMulticastAddress( ) {
        return address;
    }
    
    /**
     * Add AnnouncePdu in messageEntry
     * @param announcePdu to add.
     */
    public void addAnnouncePdu(AnnouncePdu announcePdu){
        write.lock();
        try  {
            this.announcePdu = announcePdu;
        } finally {
            write.unlock();
        }
        
    }
    /**
     * Get AnnouncePdu from MessageEntry
     * 
     * @return AnnouncePdu if exists, and null otherwise
     */
    
    public AnnouncePdu getAnnouncePdu(){
        
        return announcePdu;
    }
    
    /**
     * Set state for this message.
     *
     * @param state to set.
     */
    public void setState(States state){
    	this.state = state;
    }
    
    /**
     * Return the state of this entry
     * 
     * @return state
     */
    public States getState(){
    	return state;
    }
    
    
    /**
     * Determines whether the entry uses dynamic groups
     *
     * @return True if it uses dynamic groups False otherwise
     */
    public boolean usesDynamicGroup(){
    	return useDynamic;
    }
    
    /**
     * Set to true if you use dynamic groups
     * 
     * @param useDynamic whether to use dynamic groups or not
     */
    public void setDynamicGroupTag(boolean useDynamic){
    	this.useDynamic = useDynamic;
    }
    
   /**
    * Set acknowledgment address
    * 
    * @param ackAddress where to send acknowledgment packets
    */
    public void setAckAddress(InetAddress ackAddress){
    	this.ackAddress = ackAddress;
    }
    /**
     * Get acknowledgment address
     * @return InetAddress where to send acknowledgment packets
     */
    public InetAddress getAckAddress(){
    	return ackAddress;
    }
    
    /**
     * Set the last received PDU for this MessageEntry
     * 
     * @param lastReceived the last received dataPdu
     */
    public void setLastReceivedDataPdu(DataPdu lastReceived){
    	this.lastReceived = lastReceived; 
    }
    
    /**
     * Get the last received data PDU
     * @return DataPdu the last received pdu in this MessageEntry
     */
    public DataPdu getLastReceivedDataPdu(){
    	return lastReceived;
    }
    
    /**
     * Set the highest missing sequence number for this MessageEntry
     * 
     * @param highestMissingSequenceNumber number of the highest missing sequence number
     */
    public void setHighestMissingSequenceNumber(int highestMissingSequenceNumber){
    	this.highestMissingSequenceNumber = highestMissingSequenceNumber;
    }
    
    /**
     * Get the highest missing sequence number from this MessageEntry
     * 
     * @return the highestMissing sequence number in this MessageEntry
     */
    public int getHighestMissingSequenceNumber(){
    	return highestMissingSequenceNumber;
    }
    
    /**
     * Set the value to true if the message use a persistent multicast group
     * 
     * @param persistent true if using persistent groups, false otherwise
     */
    public void setPersistentMulticastGroup(boolean persistent){
    	this.persistent = persistent;
    }
    
    /**
     * Get the boolean value representing the use of persistent groups
     * 
     * @return true if persistent and false otherwise
     */
    
    public boolean isPersistentMulticastGroups() {
    	return persistent;
    }
    
	/**
	 * Set number of retransmissions that should occur for this message when
	 * sending it to EMCON nodes
	 * 
	 * @param count of re-transmissions to EMCON nodes
	 *            
	 */
    public void setEmconRTC(int count){
    	this.emconRtc = count;
    }
    
    /**
     * Decrement the Emcon re-transmission counter then return it. 
     * If the value returned is >-1, then it is possible to perform a re-transmission.
     *  
     * @return number of remaining EMCON re-transmissions
     */
    public int getAndDecrementEmconRtc(){
    	return --emconRtc;
    }
    
    /**
     * Set the number of times the announce PDU should be re-transmitted
     * 
     * @param announceCt number of announce re-transmissions
     */
    public void setAnnounceCt(int announceCt){
    	this.announceCt = announceCt;
    }
    
	/**
	 * Get and decrement the value representing the number of times the
	 * announcePdu should be re-transmitted
	 * 
	 * @return the remaining number of announce re-transmissions
	 */
    public int getAndDecrementAnnounceCt(){
    	return --announceCt;
    }
    /**
     * Get the remaining number of re-transmissions for the announcePdu
     * 
     * @return remaining number of announcePdu re-transmission.  
     */
    public int getAnnounceCt(){
    	return announceCt;
    }

    /**
     * Get the highest sequence number in this MessageEntry
     * 
     * @return the highest sequence number
     */
	public int getHighestSequenceNumber() {
		return dataPdus.getHighestSequenceNumber();
	}
	
	/**
	 * Set Re-Transmission time in milliseconds until expiry.
	 * 
	 * @param time in milliseconds.
	 */
	public void setReTransmissionTime(long time){
		this.reTransmissionTime = time;
	}
	
	/**
	 * Get Re-transmission time in milliseconds until expiry.
	 * 
	 * @return milliseconds until expiry.
	 */
	public long getReTransmissionTime(){
		return reTransmissionTime;
	}

    /**
     * Set the priority of this message. Valid values are from 0 to 255.
     * 
     * @param priority of this message.
     */
    public void setPriority( int priority ) {
        if (priority < 0 || priority > 255) {
            throw new IllegalArgumentException("ACP142 priority field must be between 0 and 255.");
        } else {
            this.priority = priority;
        }
    }
    
    /**
     * Returns the ACP142 priority of this message.
     * 
     * @return priority of this message.
     */
    public int getPriority( ) {
        return this.priority;
    }
    
}
