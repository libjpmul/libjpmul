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

package no.ntnu.acp142;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The default emcon handler uses <b>unreliable</b> UDP multicast to listen on a
 * configurable port for announces whenever we enter or leave emcon. The use of
 * unreliable announce is forced by the need to not block when emcon is wanted.
 * We announce local status changes, and when remote ones are received we store
 * these in a list to keep track of them.
 * 
 * This class must be stopped manually by calling shutdown().
 * 
 * @author Thomas Martin Schmid, Karl Mardoff Kittilsen
 * 
 */
public class DefaultEmconHandler implements IEmconHandler {

	/**
	 * List containing all the nodes in emcon
	 */
	private ConcurrentLinkedQueue<Integer> nodeList = null;

	/**
	 * The socket we use to listen on
	 */
	private MulticastSocket socket = null;

	/**
	 * The port to listen on and announce to
	 */
	private int port = 2755;

	/**
	 * The multicast group used to announce changes.
	 */
	private InetAddress multicastGroup;

	/**
	 * Thread we use to listen for incoming datagrams.
	 */
	private final ListeningThread listenThread;
	
	private final InetAddress bindAddress;

	/**
	 * Create the class using default variables.
	 * @param bindAddress address of the interface to bind our listener to.
	 */
	public DefaultEmconHandler(InetAddress bindAddress) {
		nodeList = new ConcurrentLinkedQueue<Integer>();
		this.bindAddress = bindAddress;

		try {
            if ( Configuration.getBindInterfaceAddress().getAddress().length == 4 ) {
			    multicastGroup = InetAddress.getByName("239.1.1.2");
            } else {
                multicastGroup = InetAddress.getByName("ff0e:ffff:1:1:1:1:1:2");
            }
		} catch (UnknownHostException e) {
            Log.writeLine(Log.LOG_LEVEL_NORMAL, "Could not resolve multicast group used by DefaultEmconHandler (239.1.1.2).");
		}

		listenThread = new ListeningThread();
		new Thread(listenThread).start();
	}

	/**
	 * Create an EMCON handler that listens on the given port and multicast
	 * address.
	 * 
	 * @param port
	 *            port to listen and send to.
	 * @param multicastAddress
	 *            Multicast address to listen and send to.
	 * @param bindAddress address of the interface to bind our listener to.
	 */
	public DefaultEmconHandler(int port, InetAddress multicastAddress, InetAddress bindAddress) {
		nodeList = new ConcurrentLinkedQueue<Integer>();
		this.bindAddress = bindAddress;
		
		this.port = port;
		this.multicastGroup = multicastAddress;

		listenThread = new ListeningThread();
		new Thread(listenThread).start();
	}

	/**
	 * Stops the emcon handler's listening thread.
	 */
	public void shutdown() {
		listenThread.stop();
	}

	/**
	 * Specifies the multicast group to announce over.
	 * 
	 * @param group
	 *            New multicast group
	 */
	public void setMulticastGroup(InetAddress group) {
		this.multicastGroup = group;
	}

	/**
	 * Specifies the port to listen for announces on.
	 * 
	 * @param port
	 *            New port
	 */
	public void setPort(short port) {
		this.port = port;
	}

	/**
	 * Listening thread class. Listens for incoming notifications that nodes'
	 * emcon states have changed.	 * 
	 */
	private class ListeningThread implements Runnable {

		/**
		 * While this is true, the thread keeps listening.
		 */
		private boolean isRunning = true;

		@Override
		public void run() {

			try {
				Log.writeLine(Log.LOG_LEVEL_DEBUG,
						"Binding our listening thread to port "
								+ port);
				socket = new MulticastSocket(port);
				socket.setInterface(bindAddress);
			} catch (IOException e) {
				// Was not able to listen on port.
				Log.writeLine(Log.LOG_LEVEL_QUIET,
						"We are not able to bind to port " + port);
				Log.writeLine(Log.LOG_LEVEL_QUIET, e.toString());
			}

			try {
				Log.writeLine(Log.LOG_LEVEL_DEBUG,
						"Joining multicast group "
								+ multicastGroup.toString());
				socket.joinGroup(multicastGroup);
			} catch (IOException e) {
				// Was not able to join the multicast group.
				Log.writeLine(Log.LOG_LEVEL_QUIET,
						"We where not able to join multicast group "
								+ multicastGroup.toString());
				Log.writeLine(Log.LOG_LEVEL_QUIET, e.toString());
			}
			Log.writeLine(Log.LOG_LEVEL_DEBUG,
					"Now listening on: "
							+ multicastGroup.toString() + ":" + port);

			while (isRunning) {
				byte[] buffer = new byte[5];
				DatagramPacket packet = new DatagramPacket(buffer, 5);
				try {
					socket.receive(packet);
				} catch (IOException e) {
					Log.writeLine(
							Log.LOG_LEVEL_NORMAL,
							"socket.receive() method had an exception, we are probably able to recover from it.");
					Log.writeLine(Log.LOG_LEVEL_NORMAL, e.toString());
				}
				if (packet.getData().length == 5) {
					// Get the id
					byte[] byteArray = new byte[4];
					for (int i = 0; i < packet.getData().length-1; i++) {
                        byteArray[i] = packet.getData()[i];
                    }
					
					BigInteger bint = new BigInteger(byteArray);
					int id = bint.intValue();
					
					// Check that the id is not the local id; changes of local
					// node's emcon state are not allowed via the network.
					if (id == Configuration.getNodeId()) {
						Log.writeLine(Log.LOG_LEVEL_DEBUG,
								"Received an EMCON state change for the local node via network and ignored it.");
						continue;
					}
					// Remove the node from the list. Do this either way to
					// avoid
					// duplicates
					nodeList.remove(id);
					// If it has entered EMCON, add it
					if (packet.getData()[4] == 1) {
						// Node has entered EMCON
					    Log.writeLine(Log.LOG_LEVEL_NORMAL, "Added " + id + " to our list of EMCON nodes.");
						nodeList.add(id);
					}
				}
			}

			try {
				Log.writeLine(Log.LOG_LEVEL_DEBUG,
						"Leaving multicast group "
								+ multicastGroup.toString());
				socket.leaveGroup(multicastGroup);
			} catch (IOException e) {
				// There where some kind of error leaving the multicast group,
				// or a security manager interfered.
				Log.writeLine(Log.LOG_LEVEL_NORMAL,
						"Failed to leave multicast group "
								+ multicastGroup.toString());
				Log.writeLine(Log.LOG_LEVEL_NORMAL, e.toString());
			}
			socket.close();
		}

		/**
		 * Stops the thread by setting isRunning to false.
		 */
		public void stop() {
			this.isRunning = false;
		}

	}

	@Override
	public void enterEmcon(int nodeId) {
		// Announce state change
		DatagramPacket packet = createEnterEmconPacket(nodeId);
		Log.writeLine(Log.LOG_LEVEL_DEBUG, "Announcing that " + nodeId
				+ " entered EMCON.");
		try {
			socket.send(packet);
		} catch (IOException e) {
			Log.writeLine(Log.LOG_LEVEL_NORMAL, "Could not announce"
					+ " change in EMCON state in DefaultEmconHandler");
			/*
			 * We intentionally fall through here, since even though we could
			 * not inform the network, we are making no guarantees that we can
			 * (we transmit unreliable anyway), and we know about the changes
			 * locally.
			 */
		}

		// Change local state ( first remove old entries to avoid duplicates )
		nodeList.remove(nodeId);
		nodeList.add(nodeId);
	}

	@Override
	public void leaveEmcon(int nodeId) {
		// Announce state change
		DatagramPacket packet = createLeaveEmconPacket(nodeId);

		try {
			socket.send(packet);
		} catch (IOException e) {
			Log.writeLine(Log.LOG_LEVEL_NORMAL, "Could not announce"
					+ " change in EMCON state in DefaultEmconHandler");
			/*
			 * We intentionally fall through here, since even though we could
			 * not inform the network, we are making no guarantees that we can
			 * (we transmit unreliable anyway), and we know about the changes
			 * locally.
			 */
		}

		// Change local state
		nodeList.remove(nodeId);
	}

	@Override
	public boolean isInEmcon(int nodeId) {
		for (int id : nodeList) {
			if (id == nodeId) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Creates a datagram with the information needed to notify of entry
	 * to EMCON mode to other nodes. This packet is defined as following: [ byte
	 * | content ] [ 0 | node ID ] [ 1 | node ID ] [ 2 | node ID ] [ 3 | node ID
	 * ] [ 4 | 1 ]
	 * 
	 * @param nodeId
	 *            ID of the node that changes state
	 * @return The packet
	 */
	private DatagramPacket createEnterEmconPacket(int nodeId) {
		byte buffer[] = new byte[5];
		buffer[0] = (byte) (nodeId >> 24);
		buffer[1] = (byte) (nodeId >> 16);
		buffer[2] = (byte) (nodeId >> 8);
		buffer[3] = (byte) (nodeId);
		buffer[4] = (byte) (1);

		DatagramPacket packet = new DatagramPacket(buffer, 5);
		packet.setPort(this.port);
		packet.setAddress(this.multicastGroup);

		return packet;
	}

	/**
	 * Creates a datagram with the information needed to notify of entry
	 * to EMCON mode to other nodes. This packet is defined as following: [ byte
	 * | content ] [ 0 | node ID ] [ 1 | node ID ] [ 2 | node ID ] [ 3 | node ID
	 * ] [ 4 | 0 ]
	 * 
	 * @param nodeId
	 *            ID of the node that changes state
	 * @return The packet
	 */
	private DatagramPacket createLeaveEmconPacket(int nodeId) {
		byte buffer[] = new byte[5];
		buffer[0] = (byte) (nodeId >> 24);
		buffer[1] = (byte) (nodeId >> 16);
		buffer[2] = (byte) (nodeId >> 8);
		buffer[3] = (byte) (nodeId);
		buffer[4] = (byte) (0);

		DatagramPacket packet = new DatagramPacket(buffer, 5);
		packet.setPort(this.port);
		packet.setAddress(this.multicastGroup);
		
		return packet;
	}
}
