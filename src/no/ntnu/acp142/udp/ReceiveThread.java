package no.ntnu.acp142.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;

import no.ntnu.acp142.Configuration;
import no.ntnu.acp142.Log;

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
 * 
 * This thread is spawned once for every port that we wish to listen to. It
 * needs a udp wrapper with a queue to put the messages it receives into, and it
 * needs a multicast socket to listen to.
 * 
 * @author Bjørn Tungesvik, Karl Mardoff Kittilsen
 * 
 */
public class ReceiveThread extends Thread {

    /**
     * Socket
     */
    private MulticastSocket                           socket = null;
    /**
     * Reference to UDPWrapper
     */
    private UDPWrapper                                udpWrapper;
 
    /**
     * 
     * @param udpWrapper 
     *          Reference to UDPWrapper.
     */
    public ReceiveThread(UDPWrapper udpWrapper) {
        this.udpWrapper = udpWrapper;
    }
    
    /**
     * Main thread method
     */
    public void run() {
        if(socket == null){
            Log.writeLine(Log.LOG_LEVEL_QUIET, "Socket == null");
            return;
        }
       
        while (true) {
            Tuple<InetAddress, byte[]> data = null;
            try {
                
                int length = Configuration.getPduMaxSize();
                byte buffer[] = new byte[length];
                DatagramPacket packet = new DatagramPacket(buffer, length);
                socket.receive(packet);

                Log.writeLine(Log.LOG_LEVEL_DEBUG, "Received data");
                data = new Tuple<InetAddress, byte[]>(packet.getAddress(), packet.getData());
                udpWrapper.queue.put(data);
                Log.writeLine(Log.LOG_LEVEL_DEBUG, "Received and put data in queue");

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                Log.writeLine(Log.LOG_LEVEL_DEBUG, "Interrupted receive thread");
               
                e.printStackTrace();
            } 

        }
    }
    
    /**
     * Set listening socket.
     * @param socket to listen on.
     */
    public void setSocket(MulticastSocket socket) {
        this.socket = socket;
    }
    
    /**
     *  Join MulticastGroup.
     * @param multicastAddress to join.
     */
    public void joinMulticastGroup(InetAddress multicastAddress){
        
        try {
            Log.writeLine(Log.LOG_LEVEL_DEBUG, "Trying to join: " + multicastAddress.getHostAddress());
            socket.joinGroup(multicastAddress);
       
        } catch(SocketException se){
        	//Already in use
        	if(se.getMessage().equals("Address already in use"))
        	{
        		Log.writeLine(Log.LOG_LEVEL_DEBUG, "Got request to join a multicast group, but we where already a member.");
        	} else {
        		se.printStackTrace();
        	}
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /**
     * Leave multicast group.
     * @param multicastAddress to leave.
     */
    public void leaveMulticastGroup(InetAddress multicastAddress){
       
        try {
            Log.writeLine(Log.LOG_LEVEL_DEBUG, "Leaving multicast group: " + multicastAddress.getHostAddress());
            socket.leaveGroup(multicastAddress);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
    }
}
