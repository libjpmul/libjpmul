package no.ntnu.acp142;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import no.ntnu.acp142.pdu.AddressPdu;
import no.ntnu.acp142.pdu.AddressPdu.DestinationEntry;
import no.ntnu.acp142.pdu.DataPdu;
import no.ntnu.acp142.rdt.MessageEntry;
import no.ntnu.acp142.rdt.States;

/*
 * Copyright (c) 2013, Thomas Martin Schmid, Karl Mardoff Kittilsen, Bjørn Tungesvik
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
 * Class that contains all information of a ACP142 message.
 * 
 * @author Thomas Martin Schmid, Karl Mardoff Kittilsen, Bjørn Tungesvik
 * 
 */
public class Acp142Message {

	/**
	 * Data to send
	 */
	private byte[] data;
	/**
	 * List of SourceIDs of the destination nodes.
	 */
	private ArrayList<Integer> destinations;
	/**
	 * Time to stop retransmitting; expiration time of the message.
	 */
	private long expiryTime = 0;
	/**
	 * Defines whether to use dynamic multicast groups.
	 */
	private boolean useDynamic = false;
	/**
	 * Source ID for incoming packages
	 */
	private int sourceID = 0;

	/**
	 * Defines whether to use persistent dynamic groups or not
	 * Default value is false
	 */
	private boolean usePersistentGroup = false;
	
	/**
	 * Priority of the message
	 */
	private int priority;

	/**
	 * Default constructor for a Acp142Message.
	 */
	public Acp142Message() {
		destinations = new ArrayList<Integer>();
	}

	/**
	 * Get the data of the message
	 * 
	 * @return Data of the message
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * Set the data of the message
	 * 
	 * @param data
	 *            of the message
	 */
	public void setData(byte[] data) {
		this.data = data;
	}

	/**
	 * Get list of destination IDs to send the message to
	 * 
	 * @return ArrayList of destination IDs.
	 */
	public ArrayList<Integer> getDestinations() {
		return destinations;
	}

	/**
	 * Set the list of destination IDs to send to.
	 * @param destinations
	 *            ArrayList of destination IDs
	 */
    @SuppressWarnings("unchecked")
    public void setDestinations(ArrayList<?> destinations) {
        if (destinations != null && !destinations.isEmpty()) {
            if ( destinations.get(0) instanceof Integer ) {
                    this.destinations = (ArrayList<Integer>) destinations;
            } else if ( destinations.get(0) instanceof InetAddress ) {
                // Convert the addresses to Integers
                ArrayList<Integer> integerDestinations = new ArrayList<Integer>();
                
                for ( Object dest : destinations ) {
                    InetAddress address = (InetAddress) dest;
                    integerDestinations.add(ByteBuffer.wrap(address.getAddress()).getInt());
                    this.destinations = integerDestinations;
                }
            } else {
                throw new IllegalArgumentException("Method only supports Integer and InetAddress.");
            }
        }
    }

	/**
	 * Get the time at which the message is considered expired and
	 * retransmission is stopped.
	 * 
	 * @return Time of expiration
	 */
	public long getExpiryTime() {
		return expiryTime;
	}

	/**
	 * Set the time at which the message is set to expire.
	 * 
	 * @param expiryTime
	 *            of message, in unix time (seconds since 1.1.1970)
	 */
	public void setExpiryTime(long expiryTime) {
		this.expiryTime = expiryTime;
	}

	/**
	 * If this returns true, a dynamic multicast group is to be created for the
	 * transmission of this message.
	 * 
	 * @return True if dynamic multicast is to be used.
	 */
	public boolean useDynamic() {
		return useDynamic;
	}

	/**
	 * If this is set to true, a dynamic multicast group is to be created for
	 * the transmission of this message.
	 * 
	 * @param useDynamic
	 *            True to use dynamic multicast groups.
	 */
	public void setDynamic(boolean useDynamic) {
		this.useDynamic = useDynamic;
	}

    /**
     * If this is set to true, the dynamic multicast group created will be able
     * to be used persistent over several messages
     * 
     * 
     * @param usePersistentGroup
     *            True to use persistent dynamic multicast groups
     */
    public void setPersistent( boolean usePersistentGroup ) {
        this.usePersistentGroup = usePersistentGroup;
    }
	
	
	/**
	 * Check if this message is suppose to use persistent multicast
	 * groups.
	 * 
	 * @return true if we are to use persistent groups.
	 */
	public boolean usePersistentGroup(){
		return usePersistentGroup;
	}

	/**
	 * Sets the sourceI if this is an incoming message. For outgoing messages,
	 * this should not be set and will be ignored.
	 * 
	 * @param sourceID
	 *            Source ID of message transmitter.
	 */
	public void setSourceID(int sourceID) {
		this.sourceID = sourceID;
	}

	/**
	 * Gets the source ID of the message transmitter.
	 * 
	 * @return Source ID of message transmitter.
	 */
	public int getSourceID() {
		return this.sourceID;
	}
	
    /**
     * Sets the priority of the message, if mapped in the configuration file,
     * this will propagate down to the DiffServ field in the IP header.
     * 
     * @param priority
     *            ACP142 priority of this message.
     */
    public void setPriority( int priority ) {
        if ( priority < 0 || priority > 255 ) {
            throw new IllegalArgumentException("ACP142 priority field must be between 0 and 255.");
        } else {
            this.priority = priority;
        }
    }
	
	/**
	 * Returns the priority of the message.
	 * @return priority of the message.
	 */
	public int getPriority() {
	    return priority;
	}

	/**
	 * Creates a MessageEntry from this Acp142Message, and returns it.
	 * 
	 * @return messageEntry The message entry representing this Acp142Message.
	 */
	public MessageEntry toMessageEntry() {
		MessageEntry messageEntry = new MessageEntry();
		// Set state
		messageEntry.setState(States.START_STATE);
		messageEntry.setDynamicGroupTag(useDynamic);
		messageEntry.setPersistentMulticastGroup(usePersistentGroup);
		int messageId = Libjpmul.getMessageId();
		messageEntry.setPriority(this.priority);

		// First we create all the data pdu(s) that we want to send, and add
		// them to the message entry.
		ArrayList<DataPdu> dataPdus = DataPdu.create(priority,
				Configuration.getNodeId(), messageId, data);

		for (DataPdu dataPdu : dataPdus) {
			messageEntry.addDataPdu(dataPdu);
		}

		// We need to make an array of DestinationEntries to pass to our
		// address pdu create function.
		int seqNumber = 1; // Sequence number for destination entries.
		byte[] b = new byte[0];
		ArrayList<DestinationEntry> destinationEntries = new ArrayList<DestinationEntry>();
		for (Integer destination : destinations) {
			destinationEntries.add(new DestinationEntry(destination,
					seqNumber++, b));
		}

		// Then we create the corresponding address pdu(s).
		DestinationEntry[] destinationsArray = new DestinationEntry[destinationEntries
				.size()];
		for (int i = 0; i < destinationEntries.size(); i++) {
			destinationsArray[i] = destinationEntries.get(i);
		}

		ArrayList<AddressPdu> addressPdus = AddressPdu.create(priority, (short) dataPdus.size(),
				Configuration.getNodeId(), messageId, (int) this.expiryTime,
				destinationsArray, (byte) 0);
		for (AddressPdu addressPdu : addressPdus) {
			messageEntry.addAddressPdu(addressPdu);
		}
		return messageEntry;
	}

	/**
	 * Convert the given message entry to an Acp142Message
	 * 
	 * @param messageEntry
	 *            the given message entry.
	 * @return an Acp142Message with the data from the given messageEntry.
	 */
	public static Acp142Message convertMessageEntry(MessageEntry messageEntry) {

		Acp142Message message = new Acp142Message();

		message.setDestinations(messageEntry.getRecipients());
		Log.writeLine(Log.LOG_LEVEL_DEBUG, "Adding "
				+ messageEntry.getRecipients().size() + " recipients.");
		message.setData(messageEntry.getData());
		Log.writeLine(Log.LOG_LEVEL_DEBUG, "Adding: " + messageEntry.getData()
				+ " as data.");
		message.setSourceID(messageEntry.getAddressPdu().getSourceID());
		Log.writeLine(Log.LOG_LEVEL_DEBUG, "Adding sourceId: "
				+ messageEntry.getAddressPdu().getSourceID());
		message.setExpiryTime(messageEntry.getAddressPdu().getExpiryTime());
		Log.writeLine(Log.LOG_LEVEL_DEBUG, "Expiry time set to: "
				+ messageEntry.getAddressPdu().getExpiryTime());

		return message;
	}

	public String toString() {
		String returnString = "";
		returnString += "Source: " + this.sourceID + ", ";
		returnString += "Expiry date/time: " + this.expiryTime + ", ";
		returnString += "Number of recipients: " + this.destinations.size()
				+ ", ";
		returnString += "Data Length: " + this.data.length + "\n";
		String dataString = new String(data, Charset.forName("UTF-8"));
		returnString += "String representation of data: " + dataString;

		return returnString;
	}

}
