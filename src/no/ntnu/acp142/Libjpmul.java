package no.ntnu.acp142;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import no.ntnu.acp142.rdt.MessageEntry;
import no.ntnu.acp142.rdt.ReliableDataTransfer;

/*
 * Copyright (c) 2013, Karl Mardoff Kittilsen, Bjørn Tungesvik
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
 * @author Karl Mardoff Kittilsen, Bjørn Tungesvik
 */
public class Libjpmul implements IAcp142 {

    /**
     * Emcon callback object
     */
     private IEmconHandler                     emconHandler;

    /**
     * Our reference to the RDT layer.
     */
    private ReliableDataTransfer              rdt;

    /**
     * Here the RDT layer will put completely received messages.
     */
    public static BlockingQueue<MessageEntry> completedMessages = new LinkedBlockingDeque<MessageEntry>();

    /**
     * Atomic Integer to keep track of our message ID.
     */
    private static final AtomicInteger        currentMessageId  = new AtomicInteger((int) (Calendar.getInstance()
                                                                        .getTimeInMillis() / 1000));

    // --------------------
    // Public interface
    // --------------------

    /**
     * Public constructor
     * 
     * Creates a new ACP142 instance using a DefaultEmconHandler.
     */
    public Libjpmul() {
        
        InetAddress bindAddress = null;

        try {
            bindAddress = Configuration.getBindInterfaceAddress();
        } catch (UnknownHostException e) {
            Log.writeLine(Log.LOG_LEVEL_VERBOSE, "Could not bind to interface specified in configuration.");
        }

        if ( bindAddress == null ) {
            try {
                bindAddress = InetAddress.getLocalHost();
                Configuration.setBindInterfaceAddress(bindAddress);
            } catch (UnknownHostException e1) {
                Log.writeLine(Log.LOG_LEVEL_QUIET, "Could not bind to localhost interface (fallback).");
            }
        } else {
            // If no nodeID is manually set, set it automatically
            if ( Configuration.getNodeId() == 0 ) {
                Configuration.setBindInterfaceAddress(bindAddress);
            }
        }
        
        this.rdt = new ReliableDataTransfer(this,bindAddress);
            
        Log.writeLine(Log.LOG_LEVEL_DEBUG, "RDT layer initialized.");
        this.emconHandler = new DefaultEmconHandler(bindAddress);
        Log.writeLine(Log.LOG_LEVEL_DEBUG, "DefaultEmconHandler initialized.");
        Log.writeLine(Log.LOG_LEVEL_DEBUG, "MessageId initialized to: " + Libjpmul.currentMessageId.get());
        
        // If we find any static multicast groups in our config file,
        // check if we are set as one of the clientId's in this group, 
        // if so, join this group.
        final ArrayList<MulticastGroup> multicastGroups = Configuration.getMulticastGroups();
        
        final ArrayList<Integer> myClientId = new ArrayList<Integer>();
        myClientId.add(Configuration.getNodeId());
        Log.writeLine(Log.LOG_LEVEL_DEBUG, "Our clientId is: " + Configuration.getNodeId() + " "
                + Integer.toHexString(Configuration.getNodeId()));
        
        for (MulticastGroup multicastGroup : multicastGroups) {
            if (multicastGroup.isSubset(myClientId)) {
                // We are contained in this multicast group, so we
                // shall join it.
                Log.writeLine(Log.LOG_LEVEL_DEBUG, "Joining multicast group: " + multicastGroup.getMulticastAddress());
                rdt.joinMulticastGroup(multicastGroup.getMulticastAddress());
            }
        }
    }

    /**
     * Public constructor, with custom {@code EmconHandler}
     * 
     * Creates a new ACP142 instance using the given EmconHandler.
     * 
     * @param handler
     *            EmconHandler to use.
     */
    public Libjpmul(IEmconHandler handler) {
        this();
        emconHandler = handler;
    }

    /**
     *
     * Public constructor with the InetAddress of the interface
     * we want to bind to and a custom EmconHandler.
     *
     * @param bindAddress address of interface to bind to.
     * @param handler EmconHandler to use.

     */
    public Libjpmul(InetAddress bindAddress, IEmconHandler handler) {
        this(bindAddress);
        this.emconHandler = handler;
    }

    /**
     * 
     * Public constructor with the InetAddress of the interface
     * we want to bind to.
     * 
     * @param bindAddress address of interface to bind to.
     */
    public Libjpmul(InetAddress bindAddress) {
        Configuration.setBindInterfaceAddress(bindAddress);
        
        this.rdt = new ReliableDataTransfer(this, bindAddress);
        Log.writeLine(Log.LOG_LEVEL_DEBUG, "RDT layer initialized.");
        this.emconHandler = new DefaultEmconHandler(bindAddress);
        Log.writeLine(Log.LOG_LEVEL_DEBUG, "DefaultEmconHandler initialized.");
        Log.writeLine(Log.LOG_LEVEL_DEBUG, "MessageId initialized to: " + Libjpmul.currentMessageId.get());
        
        // If we find any static multicast groups in our config file,
        // check if we are set as one of the clientId's in this group, 
        // if so, join this group.
        ArrayList<MulticastGroup> multicastGroups = Configuration.getMulticastGroups();
        
        ArrayList<Integer> myClientId = new ArrayList<Integer>();
        myClientId.add(Configuration.getNodeId());
        Log.writeLine(Log.LOG_LEVEL_DEBUG, "Our clientId is: " + Configuration.getNodeId() + " "
                + Integer.toHexString(Configuration.getNodeId()));
        
        for (MulticastGroup multicastGroup : multicastGroups) {
            if (multicastGroup.isSubset(myClientId)) {
                // We are contained in this multicast group, so we
                // shall join it.
                Log.writeLine(Log.LOG_LEVEL_DEBUG, "Joining multicast group: " + multicastGroup.getMulticastAddress());
                rdt.joinMulticastGroup(multicastGroup.getMulticastAddress());
            }
        }
        
    }

    /**
     * Send raw data over the ACP142 protocol
     * 
     * Sends the data given to a static multicast group defined by the given
     * destination. Blocks until all non-EMCON nodes have acknowledged complete
     * reception. Makes no guarantees that nodes in EMCON mode have received the
     * data.
     * 
     * @param data
     *            Data to send
     * @param destinations
     *            List of destination ID's.
     * @param dynamic
     *            Specifies whether to create multicast group dynamically or
     *            not.
     * @param persistent
     * 			  Specifies whether to use persistent groups or not.
     * @param priority
     *            Specifies the priority of the message.
     */
    @Override
    public void send( byte[] data, ArrayList<Integer> destinations, long expiryTime, boolean dynamic, boolean persistent, int priority ) {

        while( emconHandler.isInEmcon(Configuration.getNodeId()) && (System.currentTimeMillis() / 1000L) < expiryTime ) {
            try {
                Thread.sleep( 1000L );
            } catch ( Exception e ) {
                Log.writeLine(Log.LOG_LEVEL_VERBOSE, "Could not sleep waiting for node to leave EMCON.");
            }
        }

        Acp142Message acp142Message = new Acp142Message();
        acp142Message.setData(data);
        acp142Message.setDestinations(destinations);
        acp142Message.setExpiryTime(expiryTime);
        acp142Message.setDynamic(dynamic);
        acp142Message.setPriority(priority);
        acp142Message.setPersistent(persistent);

        MessageEntry message = acp142Message.toMessageEntry();

        if ( acp142Message.useDynamic() ) {
            // Set destination to null to indicate that RDT has to create a
            // dynamic multicast address.
            message.setMulticastAddress(null);
        } else {
            // We are using static multicast groups and need to check in what
            // group we find these destinations as a subset.
            for (MulticastGroup mGroup : Configuration.getMulticastGroups()) {
                if ( mGroup.isSubset(acp142Message.getDestinations()) ) {
                    // We found a MulticastGroup where all our destinations is a
                    // subset. Use this group to send out the message.
                    message.setMulticastAddress(mGroup.getMulticastAddress());

                    try {
                        rdt.getOutMessageQueue().put(message);
                    } catch (InterruptedException e) {
                        Log.writeLine(Log.LOG_LEVEL_NORMAL, "Interrupted while putting message in out queue; message not sent.");
                    }

                    return;
                }
            }

            // If the destination is still null we did not find any multicast
            // groups to send do.
            // Error out.
            Log.writeLine(Log.LOG_LEVEL_QUIET, "We are suppose to use static multicast groups, "
                    + "but we did not find any that had all our destinations in it!");

            String out = "We are looking for: ";
            for (Integer destination : acp142Message.getDestinations()) {
                out += destination + ", ";
            }
            Log.writeLine(Log.LOG_LEVEL_QUIET, out);

            Log.writeLine(Log.LOG_LEVEL_QUIET, "But our multicast groups are:");
            for (MulticastGroup mGroup : Configuration.getMulticastGroups()) {
                Log.writeLine(Log.LOG_LEVEL_QUIET, "\t" + mGroup.toString());
            }
        }

    }

    /**
     * Send a {@code Acp142Message} message over the ACP142 protocol
     * 
     * Sends the data given to a static multicast group defined by the given
     * destination. Blocks until all non-EMCON nodes have acknowledged complete
     * reception. Makes no guarantees that nodes in EMCON mode have received the
     * data.
     * 
     * @param acp142Message
     *            Message to send.
     */
    @Override
    public void send( Acp142Message acp142Message ) {

        while( emconHandler.isInEmcon(Configuration.getNodeId())
                && (System.currentTimeMillis() / 1000L) < acp142Message.getExpiryTime() ) {
            try {
                Thread.sleep( 1000L );
            } catch ( Exception e ) {
                Log.writeLine(Log.LOG_LEVEL_VERBOSE, "Could not sleep waiting for node to leave EMCON.");
            }
        }

        MessageEntry message = acp142Message.toMessageEntry();

        if ( acp142Message.useDynamic() ) {
            // Set destination to null to indicate that RDT has to create a
            // dynamic multicast address.
            message.setMulticastAddress(null);
            try {
                rdt.getOutMessageQueue().put(message);
            } catch (InterruptedException e) {
                Log.writeLine(Log.LOG_LEVEL_NORMAL, "Interrupted while putting message in out queue; message not sent.");
            }
        } else {
            // We are using static multicast groups and need to check in what
            // group we find these destinations as a subset.
            for (MulticastGroup mGroup : Configuration.getMulticastGroups()) {
                if ( mGroup.isSubset(acp142Message.getDestinations()) ) {
                    // We found a MulticastGroup where all our destinations is a
                    // subset. Use this group to send out the message.
                    message.setMulticastAddress(mGroup.getMulticastAddress());

                    try {
                        rdt.getOutMessageQueue().put(message);
                    } catch (InterruptedException e) {
                        Log.writeLine(Log.LOG_LEVEL_NORMAL, "Interrupted while putting message in out queue; message not sent.");
                    }

                    return;
                }
            }

            // If the destination is still null we did not find any multicast
            // groups to send do.
            // Error out.
            Log.writeLine(Log.LOG_LEVEL_QUIET, "We are suppose to use static multicast groups, "
                    + "but we did not find any that had all our destinations in it!");

            String out = "We are looking for: ";
            for (Integer destination : acp142Message.getDestinations()) {
                out += destination + ", ";
            }
            Log.writeLine(Log.LOG_LEVEL_QUIET, out);

            Log.writeLine(Log.LOG_LEVEL_QUIET, "But our multicast groups are:");
            for (MulticastGroup mGroup : Configuration.getMulticastGroups()) {
                Log.writeLine(Log.LOG_LEVEL_QUIET, "\t" + mGroup.toString());
            }
        }
    }

    /**
     * Fetch next available incoming message
     * 
     * Receives the next available message. Blocks until one arrives. If we get
     * interrupted while listening, an exception will be logged.
     * 
     * @return the Acp142Message we received.
     */
    @Override
    public Acp142Message receive( ) {
        try {
            return Acp142Message.convertMessageEntry(completedMessages.take());
        } catch (InterruptedException e) {
            Log.writeLine(Log.LOG_LEVEL_VERBOSE, "Interrupted while retrieving message.");
        }
        return null; // Will never get here.
    }

    /**
     * Enter EMCON.
     * 
     * Enters emission control mode for this node.
     */
    @Override
    public void enterEmcon( ) {
        if ( emconHandler != null ) {
            emconHandler.enterEmcon(Configuration.getNodeId());
            //Notify rdt that the node has entered EMCON
            rdt.enterEmcon();
        }
    }

    /**
     * Leave EMCON.
     * 
     * Leaves emission control mode for this node.
     */
    @Override
    public void leaveEmcon( ) {
        if ( emconHandler != null ) {
            emconHandler.leaveEmcon(Configuration.getNodeId());
            //Notify rdt that the node has left EMCON
            rdt.leaveEmcon();
        }
    }
    
    /**
     * Leave EMCON.
     * 
     * Unset emission control mode for the given nodeId. This is useful
     * if we think that someone is in emcon, but get an ack back from them
     * then they clearly is not in emcon anymore, and we just missed that
     * information.
     * @param nodeId of client to remove from emcon. Can be our own.
     */
    public void leaveEmcon( int nodeId ) {
        if ( emconHandler != null && nodeId == Configuration.getNodeId() ) {
            emconHandler.leaveEmcon(Configuration.getNodeId());
            //Notify rdt that we have left EMCON.
            rdt.leaveEmcon();
        } else if ( emconHandler != null ){
            emconHandler.leaveEmcon(nodeId);
        }
    }
    
    /**
     * Check if the node given by the sourceID is currently in EMCON
     * @param sourceID of the node to check.
     * @return true if in emcon, else false.
     */
    public boolean isEmcon(int sourceID){
    	if(emconHandler != null){
    		return emconHandler.isInEmcon(sourceID);
    	}
    	return false;
    }
    
    /**
     * When using persistent multicast groups, if we explicitly want to leave a
     * group, give the list of destinations and we will discard the corresponding group.
     * @param destinations in the group we want to discard.
     */
    public void leavePersistentMulticastGroup(ArrayList<Integer> destinations) {
        rdt.leavePersistentMulticastGroup(destinations);
    }
    
    // ----------------------------
    // Package private & private
    // ----------------------------

    /**
     * Get current message ID
     * 
     * Returns the current message id to use when sending messages. It also
     * makes sure to increment it atomically so that two threads never will get
     * the same message id returned. Thread Safe.
     * 
     * @return messageId The current message id to use for outgoing messages.
     */
    public static int getMessageId( ) {
        return currentMessageId.getAndIncrement();
    }

    /**
     * Get a random multicast address from the pool of addresses specified in
     * the config file with multicastRangeStart and multicastRangeEnd.
     * 
     * @return A random multicast address form our pool.
     * 
     */
    public static InetAddress getRandomMulticastGroup( ) {
        InetAddress startRange = null;
        InetAddress endRange = null;
        try {
            startRange = Configuration.getMulticastStartRange();
            endRange = Configuration.getMulticastEndRang();
        } catch (UnknownHostException e) {
            Log.writeLine(Log.LOG_LEVEL_NORMAL, "Could not resolve multicast start or end range address.");
        }
        BigInteger start = new BigInteger(startRange.getAddress());
        BigInteger end = new BigInteger(endRange.getAddress());
        
        Log.writeLine(Log.LOG_LEVEL_DEBUG, "Subtracting " + start + " from "
        + end);
        
        BigInteger numberOfAddressesInPool = end.subtract(start);
        // Add one, because even if the two addresses are the same, we still got that one.
        numberOfAddressesInPool = numberOfAddressesInPool.add(BigInteger.ONE);

        Log.writeLine(Log.LOG_LEVEL_DEBUG, "The number of addresses in the multicast pool is: "
                + numberOfAddressesInPool);

        // Generate a random number between zero and numberOfAddressesInPool
        Random random = new Random();
        int randomInt = random.nextInt(numberOfAddressesInPool.intValue());
        BigInteger randomBigInteger = new BigInteger(String.valueOf(randomInt));        
        
        BigInteger returnAddress = start.add(randomBigInteger);

        byte[] bytes = returnAddress.toByteArray();
        try {
            return InetAddress.getByAddress(bytes);
        } catch (UnknownHostException e) {
            Log.writeLine(Log.LOG_LEVEL_NORMAL, "Could not resolve the selected multicast group's address");
        }
        return null; // Should never get here.
    }
}
