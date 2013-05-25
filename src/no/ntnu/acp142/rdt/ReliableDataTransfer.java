package no.ntnu.acp142.rdt;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import no.ntnu.acp142.Libjpmul;
import no.ntnu.acp142.Configuration;
import no.ntnu.acp142.Log;
import no.ntnu.acp142.MulticastGroup;
import no.ntnu.acp142.pdu.Pdu;
import no.ntnu.acp142.udp.Tuple;
import no.ntnu.acp142.udp.UDPWrapper;

/*
 * Copyright (c) 2013, Bjørn Tungesvik, Karl Mardoff Kittilsen
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
 * This class represents the communication "glue" between the threads for sending,
 * receiving and handling timers. This class will also represent the interface
 * exposed to the ACP layer. 
 * @author Bjørn Tungesvik, Karl Mardoff Kittilsen
 *
 */
 
public class ReliableDataTransfer {
    /**
     * Reference to the UDP layer
     */
	protected UDPWrapper                                 udpWrapper;
    /**
     * Reference to the ACP layer
     */
	protected Libjpmul                                      libjpmul;
    /**
     * Reference to the thread used for sending
     */
    private SendThread                                 sendThread;
    /**
     * Reference to the thread used for receiving
     */
    private ReceiveThread                              receiveThread;
    /**
     * Reference to the thread used for handling timers
     */
    private TimerThread                                timerThread;
    /**
     * Thread safe queue used for incoming messages
     */
    protected ConcurrentHashMap<HashValue, MessageEntry> inMessages;
    /**
     * Thread safe queue used for outgoing messages
     */
    protected BlockingQueue<Entry>                       outMessages;
    /**
     * Mapping users to a persistent group
     */
    protected ConcurrentHashMap<Integer, InetAddress> persistentGroups;
    /**
     * Mapping ip addresses to multicast groups
     */
    protected ConcurrentHashMap<InetAddress, MulticastGroup> dynamicMulticast;
    /**
     * EMCON flag, stores EMCON status
     */
    protected AtomicBoolean EMCON;
    
    /**
     * Structure to store reference to every message not acknowledgment when in EMCON.
     */
    protected ConcurrentHashMap<HashValue, MessageEntry> readyToAckEmcon;

    /**
     * Given a multicast group (persistent ) it stores references to messages currently using this group. 
     * The last value of the Tuple indicates when it is safe to stop listening to this group.
     * False = can leave if not used. 
     * True = Should not be released
     */
    protected ConcurrentHashMap<InetAddress, Tuple<CopyOnWriteArraySet<MessageEntry>, AtomicBoolean>> persistentGroupSessions;
    
    /**
     * Default Constructor
     * Will start the threads and initialize data structures
     * Necessary.
     * @param libjpmul a reference to the ACP142 layer.
     * @param bindAddress of the local interface to bind to.
     */

    public ReliableDataTransfer(Libjpmul libjpmul, InetAddress bindAddress) {
    	this.libjpmul = libjpmul;
    	EMCON = new AtomicBoolean(false);
    	//Queues
    	inMessages = new ConcurrentHashMap<HashValue, MessageEntry>();
    	outMessages = new LinkedBlockingQueue<Entry>();
    	persistentGroups = new ConcurrentHashMap<Integer, InetAddress>();
    	dynamicMulticast = new ConcurrentHashMap<InetAddress, MulticastGroup>();
        readyToAckEmcon = new ConcurrentHashMap<HashValue, MessageEntry>();
        persistentGroupSessions = new ConcurrentHashMap<InetAddress, Tuple<CopyOnWriteArraySet<MessageEntry>, AtomicBoolean>>();
	
		//Threads
		udpWrapper = new UDPWrapper(bindAddress);
    	sendThread = new SendThread(this);
        receiveThread = new ReceiveThread(this);
        timerThread = new TimerThread(this);
        
        
        //Start threads
        sendThread.start();
        receiveThread.start();
        timerThread.start();
    }

    /**
     * Join a dynamic multicast group
     * 
     * @param group to join.
     */
    public void joinMulticastGroup(InetAddress group) {
        udpWrapper.joinMulticastGroup(group);
    }

    /**
     * Leave multicast group.
     * @param group to leave.
     */
    public void leaveMulticastGroup(InetAddress group) {
        udpWrapper.leaveMulticastGroup(group);
    }

    /**
     * Get the blocking queue used for outgoing messages
     * 
     * @return outMessages containing messages scheduled for sending.
     */
    public BlockingQueue<Entry> getOutMessageQueue() {
        return outMessages;
    }

   
    /**
     * Get packet from the UDP layer. This method is blocking until it has
     * something to return.
     * 
     * @return packet represented as bytes.
     */
    protected Tuple<InetAddress, byte[]> receive() {
        Log.writeLine(Log.LOG_LEVEL_DEBUG, "Receive in RDT called");
        return udpWrapper.receivePacket();
    }

    /**
     * Create a new timer
     * The newly created timer is transferred to the timer thread
     * 
     * @param endTime of timer
     * 
     * @param type of timer
     * 
     * @param messageEntry to be associated with this timer
     * 
     * @param entries to be associated with this timer
     */

    protected void initializeTimer(long endTime, TimerType type, MessageEntry messageEntry, ArrayList <MessageEntry> entries) {
        long startTime = System.currentTimeMillis();
        

        if ( messageEntry.getAddressPdu() != null && inMessages.get(new HashValue(messageEntry.getAddressPdu().getMessageId(), messageEntry
                .getAddressPdu().getSourceID())) == null ) {
            // If someone want to start a timer that references a messageEntry that we
            // have removed, don't start the timer. Don't use the previous timer as
            // the source for the message entry of the new timer.
            Log.writeLine(Log.LOG_LEVEL_VERBOSE, "We tried to start a timer with a reference to a messageEntry" +
            		"we no longer have. Don't start the timer.");
            return;
        }
        
        switch ( type ) {
        case EXPIRY_TIMER_TRANSMIT:
            timerThread.addTimer(new Timer(endTime, startTime, TimerType.EXPIRY_TIMER_TRANSMIT, messageEntry));
            break;
        case EXPIRY_TIMER_RECEIVE:
            timerThread.addTimer(new Timer(endTime, startTime, TimerType.EXPIRY_TIMER_RECEIVE, messageEntry));
            break;
        case RETRANSMISSION_TIMER:
            timerThread.addTimer(new Timer(endTime, startTime, TimerType.RETRANSMISSION_TIMER, messageEntry));
            break;
        case EMCON_RETRANSMISSION_TIMER:
            timerThread.addTimer(new Timer(endTime, startTime, TimerType.EMCON_RETRANSMISSION_TIMER, messageEntry));
            break;
        case ACK_TIMER:
            timerThread.addTimer(new Timer(endTime, startTime, TimerType.ACK_TIMER, messageEntry));
            break;
        case UNIDENTIFIED_DATA_DPU_VALIDITY_TIMER:
            timerThread.addTimer(new Timer(endTime, startTime, TimerType.UNIDENTIFIED_DATA_DPU_VALIDITY_TIMER,
                    messageEntry));
            break;
        case WAIT_FOR_REJECT_TIME:
            timerThread.addTimer(new Timer(endTime,startTime, TimerType.WAIT_FOR_REJECT_TIME, messageEntry));
            break;
        case ANNOUNCE_DELAY:
            timerThread.addTimer(new Timer(endTime, startTime, TimerType.ANNOUNCE_DELAY, messageEntry));
            break;
        
        case ACK_DELAY:
        	Timer timer = new Timer(endTime, startTime, TimerType.ACK_DELAY, messageEntry);
        	timer.setMessageEntryList(entries);
        	timerThread.addTimer(timer);
        	break;
        	
        case ANNOUNCE_RE_TRANSMISSION_TIMER:
        	timerThread.addTimer(new Timer(endTime, startTime, TimerType.ANNOUNCE_RE_TRANSMISSION_TIMER, messageEntry));
        	break;
        	
        case COMPLETE_ACK_TIMER:
        	timerThread.addTimer(new Timer(endTime, startTime, TimerType.COMPLETE_ACK_TIMER, messageEntry));
        	break;
        
        default:
            Log.writeLine(Log.LOG_LEVEL_DEBUG, "Unknown timer: "+ type);

        }
    }

    /**
     * Stops all the timers of the given type, associated with the given messageEntry.
     * @param type of the timer.
     * @param messageEntry the timer belongs to.
     */
    protected void stopTimer(TimerType type, MessageEntry messageEntry) {
        timerThread.stopTimer(messageEntry, type);
    }

    /**
     * Send Address or data Pdu to the their destination.
     * 
     * @param pdu to send
     * @param destination where to send it
     * @throws IOException if we fail to send the pdu.
     */
    protected void sendAddressDataPdu(Pdu pdu, InetAddress destination) throws IOException {
        
    	try {
			Thread.sleep(Configuration.getDataAndAddressPduSendDelay());
		} catch (InterruptedException e) {
			Log.writeLine(Log.LOG_LEVEL_DEBUG, "Congestion delay sleep interrupted");
		}
    	
    	udpWrapper.sendAddressDataPdu(pdu.getBinary(), destination, pdu.getPriority());
    }

    /**
     * Sends an AckPdu to its destination
     * 
     * @param pdu to send
     * @param destination where to send it
     * @throws IOException if we fail to send the pdu.
     */
    protected void sendAckPdu(Pdu pdu, InetAddress destination) throws IOException {
        udpWrapper.sendAckPdu(pdu.getBinary(), destination, pdu.getPriority());
    }

   
   
    /**
     * Send Request or Release Pdu
     * 
     * @param pdu packet to send
     * @param group where to send it
     */
    protected void sendRequestRelease(Pdu pdu, InetAddress group){
       Log.writeLine(Log.LOG_LEVEL_DEBUG, "Sending Release/Request");
       udpWrapper.sendRequestReleasePdu(pdu.getBinary(), group);   
    }
    
    /**
     * Send RejectPdu
     * 
     * @param pdu packet to send
     * @param destination where to send it
     */
    protected void sendReject(Pdu pdu, InetAddress destination){
        udpWrapper.sendRejectPdu(pdu.getBinary(), destination);
    }

    /**
     * Send announce pdu. Sent on GG
     * 
     * @param pdu announce packet to send
     */
    protected void sendAnnouncePdu(Pdu pdu) {
        udpWrapper.sendAnnouncePdu(pdu.getBinary());
    }
    
    /**
     * Sends a discard PDU to given destination
     * 
     * @param pdu discard pdu to send.
     * @param destinationAddress where to send it
     * @throws IOException if we fail to send the pdu.
     */
    public void sendDiscard( Pdu pdu, InetAddress destinationAddress ) throws IOException {
        udpWrapper.sendDiscardPdu(pdu.getBinary(), destinationAddress, pdu.getPriority());
    }
    
    
    /**
     * Map a list of recipients to a multicast group. Used when creating persistent groups.
     * 
     * @param recipients array list of integers representing destination ID's.
     * @param multicastGroup that these clients is on.
     */
    protected void setPersistentMulticastGroup(ArrayList <Integer> recipients, InetAddress multicastGroup) {
    	persistentGroups.put(recipients.hashCode(), multicastGroup);
    }
    
    /**
     * Will use an already existing persistent group mapping if it exists. The
     * existing group address will be set as the destination address in the
     * message. On the other hand, if it does not exist this method will return
     * false.
     * 
     * @param currentEntry
     *            of the message we want to check if can be sent on a existing
     *            persistent multicast group.
     * @return <tt>true</tt> if we found a group with these destinations, and
     *         are going to use it. <tt>false</tt> othervice.
     */
    protected boolean usePersistentGroupIfAvailable(MessageEntry currentEntry){
    	int key = currentEntry.getRecipients().hashCode();
    	
    	if(persistentGroups.containsKey(key)){
    		//Success use the existing group
    		InetAddress groupAddress = persistentGroups.get(key);
    		CopyOnWriteArraySet<MessageEntry> entries;
    		if((entries = persistentGroupSessions.get(groupAddress).t1) != null){
    			entries.add(currentEntry);
    			
    			if(currentEntry.isPersistentMulticastGroups() == false){
    				persistentGroupSessions.get(groupAddress).t2.set(false);
    			}
    		}
    		currentEntry.setMulticastAddress(groupAddress);
    		return true;
    	}
    	//The persistent group does not exist
    	return false;
    }
    
    /**
     * Given a list of recipients, we will tear down the corresponding persistent multicast
     * group as soon as we are done sending any messages over it.
     * 
     * @param recipients list of destinations.
     */
    public void leavePersistentMulticastGroup(ArrayList <Integer> recipients){
    	int key = recipients.hashCode();
    	InetAddress multicastGroup;
    	if ((multicastGroup = persistentGroups.get(key)) != null) {
    	    Tuple<CopyOnWriteArraySet<MessageEntry>, AtomicBoolean> tuple;
    	    if ((tuple = persistentGroupSessions.get(multicastGroup)) != null && !tuple.t1.isEmpty()) {
    	        tuple.t2.set(false);
    	    } else if ((tuple == null) || (tuple != null && tuple.t1.isEmpty())) {
    	        persistentGroupSessions.remove(multicastGroup);
    	        persistentGroups.remove(key);
    	        // Magic number:
    	        // When we release a persistent multicast group, the release
    	        // is referring to multiple message id's. There is also no way
    	        // for us to get the message id of the last message that was sent
    	        // on this multicast group (yes there is, but don't argue!).
    	        
    	        // For us, 0 (zero) is a special case in this specific
    	        // implementation.
    	        receiveThread.prepareReleasePdu(multicastGroup, 0);
    	    }
    	    
    	}
    }
    
    /**
     * Enter EMCON
     */
    public void enterEmcon() {
    	EMCON.set(true);
    }

    /**
     * Leave EMCON
     */
    public void leaveEmcon() {
    	EMCON.set(false);
    	new LeavingEmcon(this).start();
    }
    /**
     * Determine if the given node id is in EMCON
     * @param sourceID id of the node to check.
     * @return <tt>true</tt> if the node is in emcon, <tt>false</tt> othervice.
     */
    public boolean isEmcon(int sourceID){
    	return libjpmul.isEmcon(sourceID);
    }
}
