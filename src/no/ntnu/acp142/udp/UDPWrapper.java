package no.ntnu.acp142.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import no.ntnu.acp142.Configuration;
import no.ntnu.acp142.Log;
import no.ntnu.acp142.PriorityMapping;

/*
 * Copyright (c) 2013, Bjørn Tungesvik, Karl Mardoff Kittilsen, Erik Lothe
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
 * This class is responsible for collecting all the UDP packets that we are interested
 * in, and putting them in a blocking queue where the RDT layer will get them.
 * 
 * It is also responsible for keeping the send sockets that we use to send packets.
 * 
 * @author Bjørn Tungesvik, Karl Mardoff Kittilsen, Erik Lothe
 *
 */
public class UDPWrapper {
    /**
     * A blocking queue where we put tuples of source address and datagram packets.
     */
    protected BlockingQueue<Tuple<InetAddress,byte[]>> queue;
    
    /**
     * Sockets
     */
    private MulticastSocket announce;
    private MulticastSocket requestRejectRelease;
    private MulticastSocket data;
    private MulticastSocket ack;
    
    private MulticastSocket multicastSendSocket;
    private DatagramSocket unicastSendSocket;
    
    /**
     * Threads
     */
    private ReceiveThread receiveRPort;
    private ReceiveThread receiveTPort;
    private ReceiveThread receiveAPort;
    private ReceiveThread receiveDPort;
    
    /**
     * List of multicast groups
     */
     private CopyOnWriteArrayList<InetAddress> multicastGroups;
     
     /**
      * Default traffic class value
      */
     private int defaultTc;
     private int unicastDefaultTc;
    

    /**
     * 
     * Create a UDPWrapper that listens on the interface where the given address resides.
     * 
     * @param bindAddress address of the interface we want to bind our sockets to.
     */
    public UDPWrapper(InetAddress bindAddress) {
        //Initialize sockets
            try {
                multicastSendSocket = new MulticastSocket();
                multicastSendSocket.setInterface(bindAddress);
                defaultTc = multicastSendSocket.getTrafficClass();
                InetSocketAddress inetBindAddress = new InetSocketAddress(bindAddress, 0);
                unicastSendSocket = new MulticastSocket(inetBindAddress);
                unicastDefaultTc = unicastSendSocket.getTrafficClass();
                
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

        
        try {
          //RPORT
            announce = new MulticastSocket(Configuration.getRPort());
            announce.setInterface(bindAddress);
            //TPORT
            requestRejectRelease = new MulticastSocket(Configuration.getTPort());
            requestRejectRelease.setInterface(bindAddress);
            //DPORT
            data = new MulticastSocket(Configuration.getDPort());
            data.setInterface(bindAddress);
            //APORT
            ack = new MulticastSocket(Configuration.getAPort());
            ack.setInterface(bindAddress);
            
            Log.writeLine(Log.LOG_LEVEL_DEBUG, "interface bound to for announce: " + announce.getNetworkInterface().getDisplayName());
            Log.writeLine(Log.LOG_LEVEL_DEBUG, "interface bound to for release: " + requestRejectRelease.getNetworkInterface().getDisplayName());
            Log.writeLine(Log.LOG_LEVEL_DEBUG, "interface bound to for data: " + data.getNetworkInterface().getDisplayName());
            Log.writeLine(Log.LOG_LEVEL_DEBUG, "interface bound to for ack: " + ack.getNetworkInterface().getDisplayName());
            Log.writeLine(Log.LOG_LEVEL_DEBUG, "interface bound to for multicast send: " + multicastSendSocket.getNetworkInterface().getDisplayName());
            Log.writeLine(Log.LOG_LEVEL_DEBUG, "interface bound to for unicast send: " + unicastSendSocket.getLocalSocketAddress());
            
            // Initialize the queues.
            this.queue = new LinkedBlockingQueue<Tuple<InetAddress,byte[]>>();
            multicastGroups = new CopyOnWriteArrayList<InetAddress>();
        
            //Initialize threads, join GG and set sockets
            receiveRPort = new ReceiveThread(this);
            receiveRPort.setSocket(announce);
            receiveRPort.joinMulticastGroup(Configuration.getGg());
            
            receiveTPort = new ReceiveThread(this);
            receiveTPort.setSocket(requestRejectRelease);
            receiveTPort.joinMulticastGroup(Configuration.getGg());
            
            receiveAPort = new ReceiveThread(this);
            receiveAPort.setSocket(ack);
            
            receiveDPort = new ReceiveThread(this);
            receiveDPort.setSocket(data);
            
            //Start threads
            receiveAPort.start();
            receiveDPort.start();
            receiveRPort.start(); 
            receiveTPort.start();
        
        
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        

    }

    /**
     * This function is called by the RDT layer to get a packet
     * from the receiving queue. The Interrupted exception shall
     * be handled by the higher level. This method will block.
     * 
     * @return packet in bytes
     */
    public Tuple<InetAddress, byte[]> receivePacket( ) {
        Tuple <InetAddress, byte[]> tmp = null;
        try {
            tmp = queue.take();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return tmp;
    }

    

    /**
     * Join the given multicast group so that messages sent to us on this
     * group gets picked up.
     * 
     * @param multicastGroup
     *            the multicast group to join.
     */
    public void joinMulticastGroup( InetAddress multicastGroup ) {
        receiveAPort.joinMulticastGroup(multicastGroup);
        receiveDPort.joinMulticastGroup(multicastGroup);
        if (!multicastGroups.contains(multicastGroup)) {
            multicastGroups.add(multicastGroup);
        }
    }

    /**
     * Leave the given multicast group.
     * 
     * @param multicastGroup
     *            the multicast group to leave.
     */
    public void leaveMulticastGroup( InetAddress multicastGroup ) {
        receiveAPort.leaveMulticastGroup(multicastGroup);
        receiveDPort.leaveMulticastGroup(multicastGroup);
        multicastGroups.remove(multicastGroup);
        Log.writeLine(Log.LOG_LEVEL_DEBUG, "Leaving multicast group: " + multicastGroup.getHostAddress());
        Log.writeLine(Log.LOG_LEVEL_DEBUG, "multicast group size:" + multicastGroups.size());
        }
    
    /**
     * Get all multicast groups that we currently listen to/are members of.
     * 
     * @return All multicast groups we are members of.
     */
    public CopyOnWriteArrayList<InetAddress> getCurrentMulticastGroups() {
        return multicastGroups;
    }

    /**
     * This method sends out the Request and Release PDUs
     * to the correct multicast group and port.
     * 
     * @param data
     *            The data representing the PDU.
     * @param group 
     *            The multicast group to send this PDU to.
     */
    public void sendRequestReleasePdu( byte[] data , InetAddress group) {
        try {
            DatagramPacket packet = new DatagramPacket(data, data.length, group, Configuration.getTPort());
            multicastSendSocket.send(packet);
            Log.writeLine(Log.LOG_LEVEL_DEBUG, "Sending");
        } catch (IOException e) {
            Log.writeLine(Log.LOG_LEVEL_NORMAL, "IOException in method sendRequestReleasePdu in class UDPWrapper");
        }
        
    }
    
    /**
     * This method sends out the Reject PDU to the correct port and the given
     * destination as this packet should be sent as unicast.
     * 
     * @param data
     *            The data representing the PDU.
     * @param destination
     *            The destination of the packet.
     */
    public void sendRejectPdu( byte[] data, InetAddress destination) {
        try {
            DatagramPacket packet = new DatagramPacket(data, data.length, destination, Configuration.getTPort());
            multicastSendSocket.send(packet);
            Log.writeLine(Log.LOG_LEVEL_DEBUG, "Sending");
        } catch (IOException e) {
            Log.writeLine(Log.LOG_LEVEL_NORMAL, "IOException in sendRejectPdu in class UDPWrapper");
        }
        
    }

    /**
     * This method sends out the Announce PDUs to the correct
     * multicast group and port.
     * 
     * @param data
     *            The data representing the Announce Pdu.
     */
    public void sendAnnouncePdu( byte[] data) {
        try {
            DatagramPacket packet = new DatagramPacket(data, data.length, Configuration.getGg(), Configuration.getRPort());
            multicastSendSocket.send(packet);
            
            Log.writeLine(Log.LOG_LEVEL_DEBUG, "Sending");
        } catch (IOException e) {
            Log.writeLine(Log.LOG_LEVEL_NORMAL, "IOException in sendAnnouncePdu in class UDPWrapper");
        }
    }

    /**
     * This method sends out the Address PDU to the given Multicast Group and
     * the correct port number.
     * 
     * @param data
     *            The data representing the Addresses PDU.
     * @param destination
     *            The multicast address to send to.
     * @param priority
     *            The P_Mul priority
     * @throws IOException if we fail to send the PDU.
     */
    public void sendAddressDataPdu( byte[] data, InetAddress destination, int priority ) throws IOException {
        
        try {
            DatagramPacket packet = new DatagramPacket(data, data.length, destination, Configuration.getDPort());
            setTrafficClass(priority);
            multicastSendSocket.send(packet);
            multicastSendSocket.setTrafficClass(defaultTc);
            Log.writeLine(Log.LOG_LEVEL_DEBUG, "Sending");
        } catch (IOException e) {
            Log.writeLine(Log.LOG_LEVEL_NORMAL, "IOException in method sendAddressDataPdu in class UDPWrapper");
            e.printStackTrace();
            throw e;
        }
        
    }

    /**
     * This method sends out the Ack PDU to the given Multicast Group and
     * the correct port number.
     * 
     * @param data
     *            The data representing the Ack PDU.
     * @param destination
     *            The multicast address to send to.
     * @param priority
     *            The P_Mul priority
     * @throws IOException if we fail to send the PDU.
     */
    public void sendAckPdu( byte[] data, InetAddress destination, int priority ) throws IOException {
        try {
            DatagramPacket packet = new DatagramPacket(data, data.length, destination, Configuration.getAPort());
            setUnicastTrafficClass(priority);
            unicastSendSocket.send(packet);
            unicastSendSocket.setTrafficClass(unicastDefaultTc);
            Log.writeLine(Log.LOG_LEVEL_DEBUG, "Sending ackPdu to: " + destination.getHostAddress());
            
        } catch (SocketException e) {
            Log.writeLine(Log.LOG_LEVEL_NORMAL, "SocketException in sendAckPdu in class UDPWrapper");
            throw e;
        } catch (IOException e) {
            Log.writeLine(Log.LOG_LEVEL_NORMAL, "IOException in sendAckPdu in class UDPWrapper");
            throw e;
        }
        
    }
    
    /**
     * Sends a DiscardPdu
     * @param binary The data representing the DiscardPDU.
     * @param destinationAddress The destination of this discard message.
     * @param priority
     *            The ACP142 priority.
     * @throws IOException if we fail to send.
     */
    public void sendDiscardPdu( byte[] binary, InetAddress destinationAddress, int priority ) throws IOException {
        DatagramPacket packet = new DatagramPacket(binary, binary.length, destinationAddress, Configuration.getAPort());
        try {
            Log.writeLine(Log.LOG_LEVEL_DEBUG, "Sending discardPdu to: " + destinationAddress.getHostAddress());
            setTrafficClass(priority);
            multicastSendSocket.send(packet);
            multicastSendSocket.setTrafficClass(defaultTc);
        } catch (IOException e) {
            Log.writeLine(Log.LOG_LEVEL_NORMAL, "IOException in sendDiscardPdu in class UDPWrapper");
            throw e;
        }
    }
    
    /**
     * Sets the traffic class, or TOS/DiffServ field, of the IP header.
     * The ACP142 priority is mapped to a traffic class value, if:
     * 1) Priority mappings are enabled, and
     * 2) A mapping from the specific priority exists in configuration.
     * 
     * @param acp142Priority the priority of the libjpmul message.
     * @throws SocketException if we fail to set the TOS/DiffServ field of the IP packet.
     */
    private void setTrafficClass(int acp142Priority) throws SocketException {
        if (!Configuration.isEnablePriorityMapping()) {
            return;
        }
        ArrayList<PriorityMapping> mappings = Configuration.getPriorityMappings();
        for (PriorityMapping mapping: mappings) {
            if (mapping.getFrom() == acp142Priority) {
                multicastSendSocket.setTrafficClass(mapping.getTo());
                return;
            }
        }
    }

    /**
     * Sets the traffic class, or TOS/DiffServ field, of the IP header.
     * The ACP142 priority is mapped to a traffic class value, if:
     * 1) Priority mappings are enabled, and
     * 2) A mapping from the specific priority exists in configuration.
     * 
     * @param acp142Priority the priority of the libjpmul message.
     * @throws SocketException if we fail to set the TOS/DiffServ field of the IP packet.
     */
    private void setUnicastTrafficClass(int acp142Priority) throws SocketException {
        if (!Configuration.isEnablePriorityMapping()) {
            return;
        }
        ArrayList<PriorityMapping> mappings = Configuration.getPriorityMappings();
        for (PriorityMapping mapping: mappings) {
            if (mapping.getFrom() == acp142Priority) {
                unicastSendSocket.setTrafficClass(mapping.getTo());
                return;
            }
        }
    }
}
