package no.ntnu.acp142;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import no.ntnu.acp142.pdu.AckPdu;
import no.ntnu.acp142.pdu.AckPdu.AckInfoEntry;
import no.ntnu.acp142.pdu.AddressPdu;
import no.ntnu.acp142.pdu.AddressPdu.DestinationEntry;
import no.ntnu.acp142.pdu.AnnouncePdu;
import no.ntnu.acp142.pdu.DataPdu;
import no.ntnu.acp142.pdu.DiscardMessagePdu;
import no.ntnu.acp142.pdu.RequestRejectReleasePdu;

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
 * Class to handle the configuration file. <br>
 * To load configuration from file, used load(). The set methods can be used to
 * set the different parameters, and save() can be used to store the
 * configuration back into the configuration file. <br>
 * <br>
 * To edit less than all parameters, use first load(), then use the set methods
 * for the parameters in question, then store back to disk with save().
 * 
 * We defer to <a href="http://www.isode.com/support/acp-142-parameters.html">
 * http://www.isode.com/support/acp-142-parameters.html</a> for
 * good parameter values for different network types. Default values are UHF 56
 * kbps.
 * 
 * To add support for a parameter in this class, the following must be done: <br>
 * 1. Add the parameter to the fields. Maintain a logical grouping. <br>
 * 2. Add a getter for the parameter. Add it in the same relative position
 *    from the other parameters as in the fields. <br>
 * 3. Add a setter for the parameter. Same order as previous steps. <br>
 * 4. Add the parameter to the validate() method. Same order as previous steps. <br>
 * 5. Add the parameter to the load() method. Same order as previous steps. <br> 
 * 6. Add the parameter to the configurationFileAsString() method. Same order
 *    as previous steps. <br>
 * 
 * @author Erik Lothe, Karl Mardoff Kittilsen, Bjørn Tungesvik
 * 
 */
public class Configuration {

    // --- [FILE CONSTANTS ] ----------------------------------------------- //
    private static final String DEFAULT_CONF_FILE_LOCATION = "libjpmul.conf";
    private static final String COMMENT                    = "#";
    private static final int    VALUE_COLUMN_OFFSET        = 30;
    private static final String CONF_FILE_TITLE            = COMMENT + " ---[ ACP142 CONFIGURATION FILE ]--- "
                                                                   + COMMENT;
    // --------------------------------------------------------------------- //

    // --- [CONFIGURATION PARAMETERS] -------------------------------------- //

    // B01 Predefined Protocol Parameters
    private static Parameter    waitForRejectTime          = new Parameter("WAIT_FOR_REJECT_TIME", 5000);
    private static Parameter    announceDelay              = new Parameter("ANNOUNCE_DELAY", 5000);
    private static Parameter    announceCt                 = new Parameter("ANNOUNCE_CT", 5);
    private static Parameter    ackRetransmissionTime      = new Parameter("ACK_RE-TRANSMISSION_TIME", 5000);
    private static Parameter    backoffFactor              = new Parameter("BACK-OFF_FACTOR", 1.0);
    private static Parameter    emconRtc                   = new Parameter("EMCON_RTC", 4);
    private static Parameter    emconRti                   = new Parameter("EMCON_RTI", 30000);
    private static Parameter    mm                         = new Parameter("MM", 20);
    private static Parameter    ackPduTime                 = new Parameter("ACK_PDU_TIME", 10000);

    // B02-B03 Multicast group address and port numbers
    private static Parameter    gg                         = new Parameter("GG", "239.1.1.1");
    private static Parameter    tPort                      = new Parameter("TPORT", 2751);
    private static Parameter    rPort                      = new Parameter("RPORT", 2752);
    private static Parameter    dPort                      = new Parameter("DPORT", 2753);
    private static Parameter    aPort                      = new Parameter("APORT", 2754);
    private static Parameter    multicastRangeStart        = new Parameter("MULTICAST_START_RANGE", "224.0.2.0");
    private static Parameter    multicastRangeEnd          = new Parameter("MULTICAST_END_RANGE", "224.0.2.1");

    // Undefined, but useful, parameters
    private static Parameter    pduMaxSize                 = new Parameter("PDU_MAX_SIZE", 0x0f00);
    private static Parameter    undefinedPduExpiryTime     = new Parameter("UNDEFINED_PDU_EXPIRY_TIME", 1800000);
    private static Parameter    nodeId                     = new Parameter("NODE_ID", 0);
    private static Parameter    ackDelayUpperBound         = new Parameter("ACK_DELAY_UPPER_BOUND", 5000L);
    private static Parameter    bindInterfaceAddress       = new Parameter("BIND_INTERFACE_ADDRESS", "");
    private static Parameter    dataAndAddressPduSendDelay = new Parameter("DATA_AND_ADDRESS_PDU_SEND_DELAY", 500);
    // Log file location
    private static Parameter    logFileLocation            = new Parameter("LOG_FILE_LOCATION", "log.txt");

    // Propagation and mapping of P_Mul priority to IP
    private static Parameter    enablePriorityMapping      = new Parameter("ENABLE_PRIORITY_MAPPING", false);
    private static String PRIORITY_MAPPING_IDENTIFIER      = "PRIORITY_MAPPING";
    private static ArrayList<Parameter> priorityMappings    = new ArrayList<Parameter>();
    
    // Static multicast group table
    private static String STATIC_MULTICAST_GROUP_IDENTIFIER   = "STATIC_MULTICAST_GROUP";
    private static ArrayList<Parameter> staticMulticastGroups = new ArrayList<Parameter>();


    // --------------------------------------------------------------------- //

    /**
     * Generates a default sourceID for the node initially. Takes the local IP
     * address and applies a SHA-1 hash to it. Uses the 4 least significant bits
     * of the resulting hash, interpreted as a long integer, as the sourceID.
     * 
     * It is generally a bad idea to use this id instead of a set, unique
     * value in a production environment.
     * 
     * @return A likely unique ID for this node.
     */
    private static int computeIPHash( ) {
        
            try {
                if (Configuration.getBindInterfaceAddress() instanceof Inet4Address) {
                    ByteBuffer bb = ByteBuffer.wrap(Configuration.getBindInterfaceAddress().getAddress());
                    bb.order(ByteOrder.BIG_ENDIAN);
                    return bb.getInt();
                }
            } catch (UnknownHostException e1) {
                Log.writeLine(Log.LOG_LEVEL_NORMAL, "Could not set default nodeID, encountered exception:");
                Log.writeLine(Log.LOG_LEVEL_NORMAL, e1.toString());
            }
        
        byte array[];
        int returnInt;
        try {
            array = Configuration.getBindInterfaceAddress().getAddress();
            array = MessageDigest.getInstance("SHA-1").digest(array);
            returnInt = (array[array.length - 4] << 24) + (array[array.length - 3] << 16)
                    + (array[array.length - 2] << 8) + array[array.length - 1];
        } catch (UnknownHostException | NoSuchAlgorithmException e) {
            Log.writeLine(Log.LOG_LEVEL_NORMAL, "Could not set default nodeID, encountered exception:");
            Log.writeLine(Log.LOG_LEVEL_NORMAL, e.toString());
            return -1;
        }
        return Math.abs(returnInt);
    }

    /**
     * Time between sending a Request_PDU and an affiliated Announce_PDU.
     * 
     * @return wait for reject time in milliseconds.
     */
    public static int getWaitForRejectTime( ) {
        return (int) waitForRejectTime.getValue();
    }

    /**
     * Time between sending an Announce_PDU and the first affiliated
     * Address_PDU.
     * 
     * @return announce delay in milliseconds.
     */
    public static int getAnnounceDelay( ) {
        return (int) announceDelay.getValue();
    }

    /**
     * The number of times the Announce_PDU is transmitted.
     * 
     * @return announceCt number of times to transmit the Announce_PDU.
     */
    public static int getAnnounceCt( ) {
        return (int) announceCt.getValue();
    }

    /**
     * Time a transmitter waits before re-transmitting a message to receivers
     * not in EMCON, if no acknowledgment received.
     * 
     * @return ACK re-transmission time in milliseconds.
     */
    public static int getAckRetransmissionTime( ) {
        return (int) ackRetransmissionTime.getValue();
    }

    /**
     * Multiplying factor applied to ACK_RE-TRANSMISSION_TIME on subsequent
     * re-transmissions, to achieve exponentially increasing delay.
     * 
     * @return back-off factor
     */
    public static double getBackoffFactor( ) {
        return (double) backoffFactor.getValue();
    }

    /**
     * Re-transmission count - Maximum number of message re-transmissions for
     * receivers in EMCON.
     * 
     * @return emconRtc number of retransmissions.
     */
    public static int getEmconRtc( ) {
        return (int) emconRtc.getValue();
    }

    /**
     * Re-transmission interval - Time in milliseconds a transmitter waits before
     * re-transmitting a message for receivers in EMCON.
     * 
     * @return EMCON RTI in milliseconds.
     */
    public static int getEmconRti( ) {
        return (int) emconRti.getValue();
    }

    /**
     * Maximum number of new entries in the list of Missing_Data_PDU_Seq_Numbers
     * field in an Ack_Info_Entry of an Ack_PDU.
     * 
     * @return MM
     */
    public static int getMm( ) {
        return (int) mm.getValue();
    }

    /**
     * Time a receiver waits before re-transmitting Ack_PDU(s) if no response is
     * received from the transmitting node.
     * 
     * @return ACK PDU time in milliseconds.
     */
    public static int getAckPduTime( ) {
        return (int) ackPduTime.getValue();
    }

    /**
     * The multicast group address to which all nodes should join in order to allow
     * dynamic building of multicast groups.
     * 
     * @return GG
     * @throws UnknownHostException if we fail to parse configuration parameter.
     */
    public static InetAddress getGg( ) throws UnknownHostException {
            return InetAddress.getByName((String) gg.getValue());
    }
    
    /**
     * @return Address of the interface we wish to bind to.
     * @throws UnknownHostException if we fail to parse configuration parameter.
     */
    public static InetAddress getBindInterfaceAddress() throws UnknownHostException {
        return InetAddress.getByName((String) bindInterfaceAddress.getValue());
    }
    
    /**
     * Get the delay that we use between sending the address PDU and data PDU, as
     * well as between each data PDU.
     * @return dataAndAddressPduSendDelay in milliseconds.
     */
    public static int getDataAndAddressPduSendDelay() {
        return (int) dataAndAddressPduSendDelay.getValue();
    }
    
    /**
     * Port number used for the transmission of Request_PDUs, Reject_PDUs and
     * Release_PDUs between the transmission programs. All transmitter processes
     * have to listen to this port in conjunction with the multicast group GG.
     * 
     * @return TPORT
     */
    public static int getTPort( ) {
        return (int) tPort.getValue();
    }

    /**
     * Port number used by the transmitters to send the Announce_PDUs, informing
     * those receivers involved in the concerning message transfer to join a
     * specific multicast group. All receiver processes have to listen to this
     * port in conjunction with the multicast group GG.
     * 
     * @return TPORT
     */
    public static int getRPort( ) {
        return (int) rPort.getValue();
    }

    /**
     * Port used for the data traffic from the Message Transmitter of
     * Multicast_OUT to the Message Receiver of Multicast_IN.
     * 
     * @return DPORT
     */
    public static int getDPort( ) {
        return (int) dPort.getValue();
    }

    /**
     * Port used for the traffic from the Message Receiver of Multicast_IN to
     * the message Transmitter of Multicast_OUT.
     * 
     * @return APORT
     */
    public static int getAPort( ) {
        return (int) aPort.getValue();
    }

    /**
     * Returns the maximum size of a PDU. <br>
     * 
     * @return Maximum size of a PDU
     */
    public static int getPduMaxSize( ) {
        return (int) pduMaxSize.getValue();
    }

    /**
     * Returns the default timeout value of a PDU in milliseconds.
     * 
     * @return Default Expiry time of a PDU in milliseconds
     */
    public static int getUndefinedPduExpiryTime( ) {
        return (int) undefinedPduExpiryTime.getValue();
    }

    /**
     * Returns the ID of this node
     * 
     * @return The ID of this node
     */
    public static int getNodeId( ) {
        return (int) nodeId.getValue();
    }

    /**
     * Returns the log file location
     * @return the log file location
     */
    public static String getLogFileLocation( ) {
        return (String) logFileLocation.getValue();
    }

    /**
     * Returns the first address in the pool we use to assign
     * dynamic multicast groups.
     * 
     * @return multicastStartRange start of multicast range.
     * @throws UnknownHostException if we fail to parse configuration parameter.
     */
    public static InetAddress getMulticastStartRange( ) throws UnknownHostException {
        return InetAddress.getByName((String) multicastRangeStart.getValue());
    }

    /**
     * Returns the last address in the pool we use to assign
     * dynamic multicast groups.
     * @return multicastEndRange end of multicast range.
     * @throws UnknownHostException if we fail to parse configuration parameter.
     */
    public static InetAddress getMulticastEndRang( ) throws UnknownHostException {
        return InetAddress.getByName((String) multicastRangeEnd.getValue());
    }
    
    /**
     * Determines whether priority mapping is enabled
     * @return enablePriorityMapping
     */
    public static boolean isEnablePriorityMapping() {
        return (boolean) enablePriorityMapping.getValue();
    }
    
    /**
     * Converts a string to integer. Supports binary and hexadecimal radix
     * when prefixed with 0b and 0x respectively.
     * @param number to convert.
     * @return integer we got out of it.
     * @throws NumberFormatException if we fail to convert the string to an integer.
     */
    private static int stringToInt(String number) throws NumberFormatException {
        if ( number.length() > 2 && number.substring(0, 2).equals("0x") ) {
            // Handle hexadecimal
            return (int) Long.parseLong(number.substring(2), 16);
        } else if (number.length() > 2 && number.substring(0, 2).equals("0b") ) {
            // Handle binary
            return (int) Long.parseLong(number, 2);
        } else {
            return (int)Long.parseLong(number);
        }
    }
    
    /**
     * Returns a list of priority mappings. 
     * @return priorityMappings
     */
    public static ArrayList<PriorityMapping> getPriorityMappings() {
        ArrayList<PriorityMapping> priorityMappingIntegers = new ArrayList<PriorityMapping>();
        ArrayList<Object> values;
        
        for (Parameter parameter : priorityMappings) {
            values = parameter.getValues();
            try {
                // Assume values are stored as Strings
                int from = stringToInt((String) values.get(0));
                int to = stringToInt((String) values.get(1));
                priorityMappingIntegers.add(new PriorityMapping(to, from));
            } catch (IndexOutOfBoundsException e) {
                Log.writeLine(Log.LOG_LEVEL_NORMAL, "Configuration: a priority mapping had missing arguments");
            } catch (NumberFormatException e) {
                Log.writeLine(Log.LOG_LEVEL_NORMAL, "Configuration: a priority mapping had a non-integer argument");
            } catch (IllegalArgumentException e) {
                Log.writeLine(Log.LOG_LEVEL_NORMAL, "Configuration: a priority mapping overflowed 8 bit");
                
            } catch (ClassCastException e) {
                // Assume values are stored as integers
                int from = (int) values.get(0);
                int to = (int) values.get(1);
                priorityMappingIntegers.add(new PriorityMapping(to, from));
            }
        }
        
        return priorityMappingIntegers;
    }
    
    /**
     * Returns a list of the static multicast groups of this configuration.
     * @return multicast groups
     */
    public static ArrayList<MulticastGroup> getMulticastGroups() {
        ArrayList<MulticastGroup> multicastGroups = new ArrayList<MulticastGroup>();
        ArrayList<Object> values;
        ArrayList<Integer> sourceIds;
        
        // For all static multicast groups in the configuration
        for (Parameter parameter : staticMulticastGroups) {
            values = parameter.getValues();
            sourceIds = new ArrayList<Integer>();
            for (int i = 1; i < values.size(); i++) {
                try {
                	if ( values.get(i) instanceof String ) {
                		String val = (String)values.get(i);
                		sourceIds.add(stringToInt(val));
                	} else {
                		sourceIds.add((Integer)values.get(i));
                	}
                } catch (NumberFormatException e) {
                    Log.writeLine(Log.LOG_LEVEL_NORMAL, "Configuration: a multicast group had a " +
                    		"non-integer source ID");
                }
            }

            InetAddress address = null;
            try {
            	address = InetAddress.getByName((String)values.get(0));
            } catch (UnknownHostException e) { 
            	Log.writeLine(Log.LOG_LEVEL_NORMAL, "Configuration: address of a multicast group caused an " +
            			"UnknownHostException");
            } catch (NumberFormatException e) {
                Log.writeLine(Log.LOG_LEVEL_NORMAL, "Configuration: multicast group entry had no arguments");
            }

            multicastGroups.add(new MulticastGroup(address, sourceIds));
        }
        return multicastGroups;
    }
    
    /**
     * Returns the upper bound of how long we wait before sending an ack PDU.
     * @return milliseconds of maximal wait time.
     */
    public static long getAckDelayUpperBound() {
        return (long) ackDelayUpperBound.getValue();
    }

    /**
     * Set the time between sending a Request_PDU and an affiliated Announce_PDU,
     * without writing the parameter to disk.
     * 
     * @param waitForRejectTime in milliseconds.
     */
    public static void setWaitForRejectTime( int waitForRejectTime ) {
        Configuration.waitForRejectTime.setValue(waitForRejectTime);
    }

    /**
     * Set the time between sending an Announce_PDU and the first affiliated
     * Address_PDU, without writing the parameter to disk.
     * 
     * @param announceDelay in milliseconds.
     */
    public static void setAnnounceDelay( int announceDelay ) {
        Configuration.announceDelay.setValue(announceDelay);
    }

    /**
     * Set the number of times the Announce_PDU is transmitted, without
     * writing the parameter to disk.
     * 
     * @param announceCt number of times the Announce_PDU is transmitted.
     */
    public static void setAnnounceCt( int announceCt ) {
        Configuration.announceCt.setValue(announceCt);
    }

    /**
     * Set the time a transmitter waits before re-transmitting a message to receivers
     * not in EMCON, if no acknowledgment has been received.
     * 
     * @param ackRetransmissionTime in milliseconds.
     */
    public static void setAckRetransmissionTime( int ackRetransmissionTime ) {
        Configuration.ackRetransmissionTime.setValue(ackRetransmissionTime);
    }

    /**
     * Set the multiplying factor applied to ACK_RE-TRANSMISSION_TIME on subsequent
     * re-transmissions, to achieve exponentially increasing delay. Do not write the
     * parameter to disk.
     * 
     * @param backoffFactor multiplication factor for consecutive retransmissions.
     */
    public static void setBackoffFactor( double backoffFactor ) {
        Configuration.backoffFactor.setValue(backoffFactor);
    }

    /**
     * Set the re-transmission count - Maximum number of message re-transmissions for
     * receivers in EMCON.
     * 
     * @param emconRtc number of retransmissions to send.
     */
    public static void setEmconRtc( int emconRtc ) {
        Configuration.emconRtc.setValue(emconRtc);
    }

    /**
     * Set the re-transmission interval - Time in milliseconds a transmitter waits before
     * re-transmitting a message for receivers in EMCON, without writing to disk.
     * 
     * @param emconRti in milliseconds.
     */
    public static void setEmconRti( int emconRti ) {
        Configuration.emconRti.setValue(emconRti);
    }

    /**
     * Sets parameter without writing to disk.
     * 
     * @param mm the MM value.
     */
    public static void setMm( int mm ) {
        Configuration.mm.setValue(mm);
    }

    /**
     * Set the time a receiver waits before re-transmitting Ack_PDU(s) if no response is
     * received from the transmitting node.
     * 
     * @param ackPduTime in milliseconds.
     */
    public static void setAckPduTime( int ackPduTime ) {
        Configuration.ackPduTime.setValue(ackPduTime);
    }

    /**
     * Sets the GG parameter without writing to disk.
     * 
     * @param gg the global multicast group.
     */
    public static void setGg( String gg ) {
        Configuration.gg.setValue(gg);
    }
    
    /**
     * Sets the address of the interface we wish to bind to.
     * @param interfaceAddress IP address of the interface.
     */
    public static void setBindInterfaceAddress( InetAddress interfaceAddress ) {
        Configuration.bindInterfaceAddress.setValue(interfaceAddress.getHostAddress());
        Configuration.setNodeId(Configuration.computeIPHash());
    }
    
    /**
     * Set the delay that we use between sending the address PDU and data PDU, as
     * well as between each data PDU.
     * @param dataAndAddressPduSendDelay in milliseconds.
     */
    public static void setDataAndAddressPduSendDelay(int dataAndAddressPduSendDelay) {
        Configuration.dataAndAddressPduSendDelay.setValue(dataAndAddressPduSendDelay);
    }
    
    /**
     * Sets the GG parameter without writing to disk.
     * 
     * @param gg the global multicast group.
     */
    public static void setGg( InetAddress gg ) {
        setGg(gg.getHostAddress());
    }
    
    /**
     * Sets the TPORT parameter without writing to disk.
     * 
     * @param tPort port number.
     */
    public static void setTPort( int tPort ) {
        Configuration.tPort.setValue(tPort);
    }

    /**
     * Sets parameter without writing to disk.
     * 
     * @param rPort port number.
     */
    public static void setRPort( int rPort ) {
        Configuration.rPort.setValue(rPort);
    }

    /**
     * Sets parameter without writing to disk.
     * 
     * @param dPort port number.
     */
    public static void setDPort( int dPort ) {
        Configuration.dPort.setValue(dPort);
    }

    /**
     * Sets parameter without writing to disk.
     * 
     * @param aPort port number.
     */
    public static void setAPort( int aPort ) {
        Configuration.aPort.setValue(aPort);
    }

    /**
     * Sets parameter without writing to disk.
     * 
     * @param pduMaxSize maximum size of a pdu.
     */
    public static void setPduMaxSize( int pduMaxSize ) {
        Configuration.pduMaxSize.setValue(pduMaxSize);
    }

    /**
     * Returns the default timeout value of a PDU in milliseconds, without
     * writing it to disk.
     * 
     * @param undefinedPduExpiryTime Default Expiry time of a PDU.
     */
    public static void setUndefinedPduExpiryTime( int undefinedPduExpiryTime ) {
        Configuration.undefinedPduExpiryTime.setValue(undefinedPduExpiryTime);
    }

    /**
     * Sets parameter without writing to disk.
     * 
     * @param nodeId
     *            Node ID of this node
     */
    public static void setNodeId( int nodeId ) {
        Configuration.nodeId.setValue(nodeId);
    }

    /**
     * Sets the log file location
     * @param location to write the log to
     */
    public static void setLogFileLocation( String location ) {
        Configuration.logFileLocation.setValue( location );
        Log.setFileLocation( location );
    }

    /**
     * Sets parameter without writing to disk.
     * 
     * @param multicastStartRange
     *            start address of the multicast pool.
     */
    public static void setMulticastStartRange( String multicastStartRange ) {
        Configuration.multicastRangeStart.setValue(multicastStartRange);
    }

    /**
     * Sets parameter without writing to disk.
     * 
     * @param multicastStartRange
     *            start address of the multicast pool.
     */
    public static void setMulticastStartRange( InetAddress multicastStartRange ) {
        setMulticastStartRange(multicastStartRange.getHostAddress());
    }
    
    /**
     * Sets parameter without writing to disk.
     * 
     * @param multicastEndRange
     *            start address of the multicast pool.
     */
    public static void setMulticastEndRange( String multicastEndRange ) {
        Configuration.multicastRangeEnd.setValue(multicastEndRange);
    }
    
    /**
     * Sets parameter without writing to disk.
     * 
     * @param multicastEndRange
     *            start address of the multicast pool.
     */
    public static void setMulticastEndRange( InetAddress multicastEndRange ) {
        setMulticastEndRange(multicastEndRange.getHostAddress());
    }
    
    /**
     * Sets the parameter without writing to disk
     * @param enable set to true if you want to enable it.
     */
    public static void setEnablePriorityMapping(boolean enable) {
        enablePriorityMapping.setValue(enable);
    }
    
    /**
     * Adds a priority mapping to the current configuration without 
     * writing to disk.
     * @param priorityMapping the mapping we want
     */
    public static void addPriorityMapping(PriorityMapping priorityMapping) {
        ArrayList<Object> values = new ArrayList<Object>();
        Parameter parameter;
        
        values.add(priorityMapping.getFrom());
        values.add(priorityMapping.getTo());
        
        parameter = new Parameter(PRIORITY_MAPPING_IDENTIFIER);
        parameter.setValues(values);
        priorityMappings.add(parameter);
    }
    
    
    /**
     * Adds a multicast group to the current configuration without writing to 
     * disk.
     * @param multicastGroup the multicast group with members you want to add.
     */
    public static void addMulticastGroup(MulticastGroup multicastGroup) {
        ArrayList<Object> values = new ArrayList<Object>();
        ArrayList<Integer> sourceIds = multicastGroup.getSourceIds();
        Parameter parameter;

        values.add(multicastGroup.getMulticastAddress().getHostAddress());
        for (Integer sourceId : sourceIds) {
            values.add(sourceId);
        }
        parameter = new Parameter(STATIC_MULTICAST_GROUP_IDENTIFIER);
        parameter.setValues(values);
        staticMulticastGroups.add(parameter);
    }
    
    /**
     * Set the upper bound on the sleep delay when sending Ack PDUs
     * @param upperBound the upper bound in milliseconds.
     */
    public static void setAckDelayUpperBound(long upperBound) {
        Configuration.ackDelayUpperBound.setValue(upperBound);
    }

    /**
     * Loads the configuration file into memory from default path.
     * 
     * @throws IndexOutOfBoundsException
     *             File contains missing arguments
     * @throws NumberFormatException
     *             File contains an argument expected to be a number
     * @throws FileNotFoundException
     *             Configuration file does not exist
     * @throws IOException if we fail to load the file.
     */
    public static void load( ) throws IndexOutOfBoundsException, NumberFormatException, IOException {
        load(DEFAULT_CONF_FILE_LOCATION);
    }

    /**
     * Loads the configuration file into memory from given path.
     * 
     * @param path
     *            the path of the file
     * @throws IndexOutOfBoundsException
     *             File contains missing arguments
     * @throws NumberFormatException
     *             File contains an argument expected to be a number
     * @throws FileNotFoundException
     *             Configuration file does not exist
     * @throws IOException if we fail to load the file.
     */
    public static void load( String path ) throws IndexOutOfBoundsException, NumberFormatException,
            IOException {
        BufferedReader bf;
        FileReader fr;
        String line;
        String words[];

        fr = new FileReader(path);
        bf = new BufferedReader(fr);

        while ((line = bf.readLine()) != null) {
            if ( line.length() == 0 ) {
                continue; // Skip empty line
            }
            if ( line.startsWith(COMMENT) ) {
                continue; // Skip commented line
            }

            words = splitArguments(line);

            // Load attributes
            if ( waitForRejectTime.hasKeyword(words[0]) ) {
                waitForRejectTime.setValue(Integer.parseInt(words[1]));

            } else if ( announceDelay.hasKeyword(words[0]) ) {
                announceDelay.setValue(Integer.parseInt(words[1]));

            } else if ( announceCt.hasKeyword(words[0]) ) {
                announceCt.setValue(Integer.parseInt(words[1]));

            } else if ( ackRetransmissionTime.hasKeyword(words[0]) ) {
                ackRetransmissionTime.setValue(Integer.parseInt(words[1]));

            } else if ( backoffFactor.hasKeyword(words[0]) ) {
                backoffFactor.setValue(Double.parseDouble(words[1]));

            } else if ( emconRtc.hasKeyword(words[0]) ) {
                emconRtc.setValue(Integer.parseInt(words[1]));

            } else if ( emconRti.hasKeyword(words[0]) ) {
                emconRti.setValue(Integer.parseInt(words[1]));

            } else if ( mm.hasKeyword(words[0]) ) {
                mm.setValue(Integer.parseInt(words[1]));

            } else if ( ackPduTime.hasKeyword(words[0]) ) {
                ackPduTime.setValue(Integer.parseInt(words[1]));

            } else if ( gg.hasKeyword(words[0]) ) {
                gg.setValue(words[1]);
                
            } else if ( tPort.hasKeyword(words[0]) ) {
                tPort.setValue(Integer.parseInt(words[1]));

            } else if ( rPort.hasKeyword(words[0]) ) {
                rPort.setValue(Integer.parseInt(words[1]));

            } else if ( dPort.hasKeyword(words[0]) ) {
                dPort.setValue(Integer.parseInt(words[1]));

            } else if ( aPort.hasKeyword(words[0]) ) {
                aPort.setValue(Integer.parseInt(words[1]));
          
            } else if ( multicastRangeStart.hasKeyword(words[0]) ) {
                multicastRangeStart.setValue(words[1]);

            } else if ( multicastRangeEnd.hasKeyword(words[0]) ) {
                multicastRangeEnd.setValue(words[1]);

            } else if ( pduMaxSize.hasKeyword(words[0]) ) {
                pduMaxSize.setValue(Integer.parseInt(words[1]));

            } else if ( undefinedPduExpiryTime.hasKeyword(words[0]) ) {
                undefinedPduExpiryTime.setValue(Integer.parseInt(words[1]));

            } else if ( nodeId.hasKeyword(words[0]) ) {
                nodeId.setValue(Integer.parseInt(words[1]));
                
            } else if ( ackDelayUpperBound.hasKeyword(words[0]) ) {
                ackDelayUpperBound.setValue(Long.parseLong(words[1]));
                
            } else if ( bindInterfaceAddress.hasKeyword(words[0]) ) {
                bindInterfaceAddress.setValue(words[1]);
                
            } else if ( dataAndAddressPduSendDelay.hasKeyword(words[0]) ) {
                dataAndAddressPduSendDelay.setValue(words[1]);
                
            } else if ( enablePriorityMapping.hasKeyword(words[0]) ) {
                boolean enable = words[1].equalsIgnoreCase("true");
                enablePriorityMapping.setValue(enable);
                
            } else if ( PRIORITY_MAPPING_IDENTIFIER.equals(words[0]) ) {
                priorityMappings.add(new Parameter(words));
                
            } else if ( STATIC_MULTICAST_GROUP_IDENTIFIER.equals(words[0]) ) {
                staticMulticastGroups.add(new Parameter(words));
                
            } else if ( logFileLocation.hasKeyword(words[0])) {
                logFileLocation.setValue(words[1]);
            }
        }
        bf.close();
    }

    /**
     * Writes a configuration file from the currently loaded configuration to
     * the file of the default path. <br>
     * Warning, any previous configuration file will be overwritten!
     * 
     * @throws IOException if we fail to write the file.
     */
    public static void save( ) throws IOException {
        save(DEFAULT_CONF_FILE_LOCATION);
    }

    /**
     * Writes a configuration file from the currently loaded configuration to
     * the file of given path. <br>
     * Warning, any previous configuration file will be overwritten!
     * @param path a string representing the path we want to write to.
     * 
     * @throws IOException if we fail to write the file.
     */
    public static void save( String path ) throws IOException {
        PrintWriter pw;
        FileWriter fw;

        fw = new FileWriter(path);
        pw = new PrintWriter(fw);

        pw.print(configurationFileAsString());
        pw.flush();
        pw.close();
    }

    /**
     * Generates and returns the configuration file as String.
     * 
     * @return configuration file as a String
     */
    public static String configurationFileAsString( ) {
        String confString = "";
        String identifierColumnHeader = "Parameter ID";
        String valueColumnHeader = "Value";

        // Header
        confString += CONF_FILE_TITLE + "\n\n";
        confString += identifierColumnHeader;
        for (int i = identifierColumnHeader.length(); i < VALUE_COLUMN_OFFSET; i++) {
            confString += " ";
        }
        confString += valueColumnHeader + "\n\n";

        // Parameters and value
        confString += Configuration.waitForRejectTime + "\n";
        confString += Configuration.announceDelay + "\n";
        confString += Configuration.announceCt + "\n";
        confString += Configuration.ackRetransmissionTime + "\n";
        confString += Configuration.backoffFactor + "\n";
        confString += Configuration.emconRtc + "\n";
        confString += Configuration.emconRti + "\n";
        confString += Configuration.mm + "\n";
        confString += Configuration.ackPduTime + "\n";
        confString += Configuration.gg + "\n";
        confString += Configuration.bindInterfaceAddress + "\n";
        confString += Configuration.dataAndAddressPduSendDelay + "\n";
        confString += Configuration.tPort + "\n";
        confString += Configuration.rPort + "\n";
        confString += Configuration.dPort + "\n";
        confString += Configuration.aPort + "\n";
        confString += Configuration.multicastRangeStart + "\n";
        confString += Configuration.multicastRangeEnd + "\n";
        confString += Configuration.pduMaxSize + "\n";
        confString += Configuration.undefinedPduExpiryTime + "\n";
        confString += Configuration.nodeId + "\n";
        confString += Configuration.ackDelayUpperBound + "\n";
        confString += Configuration.enablePriorityMapping + "\n";
        confString += Configuration.logFileLocation + "\n";
        for (Parameter mapping: priorityMappings) {
            confString += mapping + "\n";
        }
        for (Parameter multicastGroup : staticMulticastGroups) {
            confString += multicastGroup + "\n";
        }

        return confString;
    }
    
    /**
     * 
     * Try to validate all the configuration parameters in the config file.
     * 
     * @return invalidParameters a list of invalid parameters.
     */
    public static ArrayList<Parameter> validate() {
        ArrayList<Parameter> faultyParameters = new ArrayList<Parameter>();
        
        if (!validatePduMaxSize()) {
            faultyParameters.add(pduMaxSize);
        }
        
        if (!validateMm()) {
            faultyParameters.add(mm);
        }
        
        if (getWaitForRejectTime() < 0) {
            faultyParameters.add(waitForRejectTime);
        }
        
        if (getAnnounceDelay() < 0) {
            faultyParameters.add(announceDelay);
        }
        if (getAnnounceCt() < 0) {
            faultyParameters.add(announceCt);
        }
        if (getAckRetransmissionTime() < 0) {
            faultyParameters.add(ackRetransmissionTime);
        }
        if (getEmconRtc() < 0) {
            faultyParameters.add(emconRtc);
        }
        if (getEmconRti() < 0) {
            faultyParameters.add(emconRti);
        }
        if (getAckPduTime() < 0) {
            faultyParameters.add(ackPduTime);
        }
        if (getBackoffFactor() < 1.0) {
            faultyParameters.add(backoffFactor);
        }
        
        try {
            getGg();
        } catch (UnknownHostException e) {
            faultyParameters.add(gg);
        }
        try {
            getBindInterfaceAddress();
        } catch (UnknownHostException e) {
            faultyParameters.add(bindInterfaceAddress);
        }
        if (getDataAndAddressPduSendDelay() < 0) {
            faultyParameters.add(dataAndAddressPduSendDelay);
        }
        try {
            getMulticastStartRange();
        } catch (UnknownHostException e) {
            faultyParameters.add(multicastRangeStart);
        }
        try {
            getMulticastEndRang();
        } catch (UnknownHostException e) {
            faultyParameters.add(multicastRangeEnd);
        }
        return faultyParameters;
    }
    
    /**
     * Validates if the current pduMaxSize field can handle an instance of
     * every PDU type with at least one entry.
     * @return valid
     */
    private static boolean validatePduMaxSize() {
        int pduMaxSize = getPduMaxSize();
        
        if (pduMaxSize < AckPdu.ACK_PDU_BASE_SIZE + AckInfoEntry.ACK_INFO_ENTRY_BASE_SIZE) {
            return false;
        }
        if (pduMaxSize < AddressPdu.ADDRESS_PDU_BASE_SIZE + DestinationEntry.DESTINATION_ENTRY_BASE_SIZE) {
            return false;
        }
        if (pduMaxSize < AnnouncePdu.ANNOUNCE_PDU_BASE_SIZE) {
            return false;
        }
        if (pduMaxSize < DataPdu.DATA_PDU_BASE_SIZE + 1) {
            return false;
        }
        if (pduMaxSize < DiscardMessagePdu.DISCARD_MESSAGE_PDU_SIZE) {
            return false;
        }
        if (pduMaxSize < RequestRejectReleasePdu.REQUEST_REJECT_RELEASE_PDU_SIZE) {
            return false;
        }

        return true;
    }

    /**
     * Validates the MM field
     * @return valid
     */
    public static boolean validateMm() {
        int mm = getMm();
        
        if (mm < 1) {
            return false;
        }
        
        // Amount of bytes left for missing data PDU sequence numbers is
        // the number of bytes left when using one ack info entry. 
        // Divide this by 2 to get number of 16 bit missing sequence numbers.
        int availableMmSpace = (getPduMaxSize() - 
                (AckInfoEntry.ACK_INFO_ENTRY_BASE_SIZE + AckPdu.ACK_PDU_BASE_SIZE)) / 2;
        
        if (mm > availableMmSpace) {
            return false;
        }
        return true;
    }
    

    /**
     * Splits a line into arguments separated by one or more tabs and/or spaces
     * 
     * @param line to be split.
     * @return arguments array of arguments.
     */
    private static String[] splitArguments( String line ) {
        ArrayList<String> arguments = new ArrayList<String>();
        for (String word : line.split("[ \t]")) {
            if ( word != null && word.length() > 0 ) {
                arguments.add(word);
            }
        }
        return arguments.toArray(new String[arguments.size()]);
    }

    /**
     * The parameter. <br>
     * This class contains both the parameter's identifier and its value.
     * 
     * @author Erik Lothe
     * 
     */
    private static class Parameter {
        private String identifier;
        private ArrayList<Object> values;

        /**
         * Creates a new parameter and sets the default value
         * 
         * @param identifier string value to identify the parameter.
         * @param defaultValue default value for the parameter.
         */
        public Parameter(String identifier, Object defaultValue) {
            this.identifier = identifier;
            this.values = new ArrayList<Object>();
            setValue(defaultValue);
        }

        /**
         * Creates a new parameter directly from the words of a line of the
         * configuration file
         * 
         * @param words
         *            Words from line of configuration file
         */
        public Parameter(String[] words) {
            this.values = new ArrayList<Object>();
            this.identifier = words[0];
            for (int i = 1; i < words.length; i++) {
                values.add(words[i]);
            }
        }
        
        /**
         * Creates a new parameter without setting any values
         * @param identifier of the parameter to return.
         */
        public Parameter(String identifier) {
            this.identifier = identifier;
            this.values = new ArrayList<Object>();
        }
        
        /**
         * Sets the value of this parameter
         * 
         * @param value you want to set.
         */
        public void setValue( Object value ) {
            if (values.size() > 0) {
                values.set(0, value);
            } else {
                values.add(value);
            }
        }
        
        /**
         * Sets an ArrayList of objects to this parameter's values
         * @param values the array list to set.
         */
        public void setValues( ArrayList<Object> values ) {
            this.values = values;
        }

        /**
         * Returns the first value of this parameter
         * 
         * @return value
         */
        public Object getValue( ) {
            if (values.size() > 0) {
                return values.get(0);
            }
            return null;
        }
        
        /**
         * Returns all values of this parameter
         * @return values
         */
        public ArrayList<Object> getValues( ) {
            return values;
        }

        /**
         * Returns whether this parameter is associated with given identifier
         * 
         * @param identifier identifier to check for.
         * @return this parameter is associated with given identifier
         */
        public boolean hasKeyword( String identifier ) {
            return this.identifier.equalsIgnoreCase(identifier);
        }

        /**
         * Returns a textual representation of this parameter to be placed in a
         * configuration file
         * 
         * @return parameter string
         */
        @Override
        public String toString( ) {
            // At least one space after identifier
            String line = identifier + " ";

            // Add spaces to line up values under a value column
            for (int i = line.length(); i < VALUE_COLUMN_OFFSET; i++) {
                line += " ";
            }
            
            // Add value(s)
            for (int i = 0; i < values.size(); i++) {
                line += " " + values.get(i);
            }
            return line;
        }
    }
}
