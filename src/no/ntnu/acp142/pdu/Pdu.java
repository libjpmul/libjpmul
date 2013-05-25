package no.ntnu.acp142.pdu;

import java.util.Arrays;

import no.ntnu.acp142.Log;

/*
 * Copyright (c) 2013, Erik Lothe, Bjørn Tungesvik, Karl Mardoff Kittilsen
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
 * The Protocol Data Unit (PDU) <br>
 * This abstract class handles the common functionality of a PDU. <br>
 * When creating a specific PDU type, use the static create method of the
 * specific PDU type's class. When parsing a PDU from an array of bytes
 * PDU.parsePDU() will generate a PDU subclass of the proper type.
 * 
 * @author Erik Lothe, Bjørn Tungesvik, Karl Mardoff Kittilsen
 * 
 */
public abstract class Pdu {

    // --- PDU TYPES ------------------------------------------------------- //
    /**
     * pduType value for DataPdu
     */
    public static final byte Data_PDU                             = 0x00;
    /**
     * pduType value for AckPdu
     */
    public static final byte Ack_PDU                              = 0x01;
    /**
     * pduType value for AddressPdu
     */
    public static final byte Address_PDU                          = 0x02;
    /**
     * pduType value for DiscardMessagePdu
     */
    public static final byte Discard_Message_PDU                  = 0x03;
    /**
     * pduType value for AnnouncePdu
     */
    public static final byte Announce_PDU                         = 0x04;
    /**
     * pduType value for RequestPdu
     */
    public static final byte Request_PDU                          = 0x05;
    /**
     * pduType value for RejectPdu
     */
    public static final byte Reject_PDU                           = 0x06;
    /**
     * pduType value for ReleasePdu
     */
    public static final byte Release_PDU                          = 0x07;
    // --------------------------------------------------------------------- //

    // --- COMMON OCTET PLACEMENTS ----------------------------------------- //
    private static final int LENGTH_OF_PDU_OFFSET                    = 0;
    private static final int PRIORITY_POS                            = 2;
    private static final int MAP_PDU_TYPE_OFFSET                     = 3;
    private static final int CHECKSUM_OFFSET                         = 6;
    private static final int SOURCE_ID_OFFSET                        = 8;
    // --------------------------------------------------------------------- //

    // --- PDU CONTENT ----------------------------------------------------- //
    /**
     * The binary representation of the PDU
     */
    protected byte[]         binary;
    // --------------------------------------------------------------------- //

    /**
     * Returns the length field of this PDU.
     * 
     * @return length
     */
    public int getLengthOfPDU( ) {
        return unsigned( (concatenateBytes( binary[LENGTH_OF_PDU_OFFSET],
                                             binary[LENGTH_OF_PDU_OFFSET + 1] ) ) );
    }

    /**
     * Returns the priority field of this PDU.
     * 
     * @return priority
     */
    public short getPriority( ) {
        return unsigned( binary[PRIORITY_POS] );
    }

    /**
     * Returns content of the MAP field of this PDU <br>
     * <br>
     * This 2-bit field specifies whether this PDU is first, middle or last. <br>
     * <br>
     * The high order bit is set to<br>
     * '0': This is the last one of a set of PDUs.<br>
     * '1': This is NOT the last one of a set of PDUs.<br>
     * <br>
     * The low order bit is set to<br>
     * '0': This is the last one of a set of PDUs. <br>
     * '1': This is NOT the last one of a set of PDUs.<br>
     * <br>
     * When both bits are set to '0', it means there is only one PDU. <br>
     * <br>
     * Warning: some PDU types does not use this field.
     * 
     * @return MAP
     */
    public byte getMap( ) {
        return (byte) ( ( binary[MAP_PDU_TYPE_OFFSET] & 0b11000000 ) >>> 6 );
    }

    /**
     * Returns the PDU type field of this PDU in number form.
     * 
     * @return PDU type
     */
    public byte getPduType( ) {
        return (byte) ( binary[MAP_PDU_TYPE_OFFSET] & 0b00111111 );
    }

    /**
     * Returns the checksum field of this PDU
     * 
     * @return checksum
     */
    public short getChecksum( ) {
        return concatenateBytes( binary[CHECKSUM_OFFSET],
                                  binary[CHECKSUM_OFFSET + 1] );
    }

    /**
     * Returns the ID of the sender of this Pdu
     * 
     * @return ID of the sender of this Pdu
     */
    public int getSourceID( ) {
        return concatenateBytes( binary[SOURCE_ID_OFFSET],
                                  binary[SOURCE_ID_OFFSET + 1], 
                                  binary[SOURCE_ID_OFFSET + 2],
                                  binary[SOURCE_ID_OFFSET + 3] );
    }

    /**
     * Parses any PDU type from an array of bytes.
     * 
     * @param binary
     *          The binary representation of the the PDU 
     * @return PDU subclass of the proper PDU type
     */
    public static Pdu parsePDU( byte[] binary ) {
        byte pduType = (byte) (binary[MAP_PDU_TYPE_OFFSET] & 0b00111111);

        switch ( pduType ) {
        case Data_PDU:              return new DataPdu(binary);
        case Ack_PDU:               return new AckPdu(binary);
        case Address_PDU:           return new AddressPdu(binary);
        case Discard_Message_PDU:   return new DiscardMessagePdu(binary);
        case Announce_PDU:          return new AnnouncePdu(binary);
        case Request_PDU:           return new RequestPdu(binary);
        case Reject_PDU:            return new RejectPdu(binary);
        case Release_PDU:           return new ReleasePdu(binary);

        default:                    return null;
        }
    }

    /**
     * Returns an array of bytes making up this PDU. This array can be
     * transferred over UDP.
     * 
     * @return binary
     */
    public byte[] getBinary( ) {
        return binary;
    }

    /**
     * Initializes the byte array with the 6 non type-specific first bytes.
     * @param lengthOfPDU
     *          Total length of PDU
     * @param priority
     *          P_Mul priority
     * @param map
     *          MAP field. See JavaDoc for getMap()
     * @param pduType
     *          Type of PDU
     * @param sourceId
     *          Source ID
     * @throws IllegalArgumentException
     *          If length of PDU field overflows 16 bit or priority field overflows 8 bit
     */
    protected void initCommonBinary( int lengthOfPDU, int priority, byte map, byte pduType, int sourceId ) 
            throws IllegalArgumentException {
        if ( overflows16bit(lengthOfPDU) ) {
            throw new IllegalArgumentException("Length of PDU field overflows 16 bits");
        }
        if ( overflows8bit(priority)) {
            throw new IllegalArgumentException("Priority field overflows 8 bits");
        }
        
        this.binary = new byte[lengthOfPDU];

        try {
            binary[PRIORITY_POS]        = (byte) priority;
            binary[MAP_PDU_TYPE_OFFSET] = (byte) ((map << 6) | (pduType & 0b00111111));

            addToBinary(LENGTH_OF_PDU_OFFSET, (short) lengthOfPDU);
            addToBinary(SOURCE_ID_OFFSET,     (int)   sourceId);
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.writeLine(Log.LOG_LEVEL_NORMAL, "Data could not fit into binary array in method initCommonBinary in " 
                    + getClass());
        }
    }

    /**
     * Generates the checksum field of a PDU and adds it to the binary
     */
    protected void setChecksum( ) {
        int checksum = checksum();

        if ( binary.length < CHECKSUM_OFFSET + 2 ) {
            Log.writeLine(Log.LOG_LEVEL_NORMAL, "Checksum field could not fit into binary.");
            return;
        }
        
    	binary[CHECKSUM_OFFSET] = (byte)((checksum >> 8) & 0xff);
    	binary[CHECKSUM_OFFSET + 1] = (byte)(checksum & 0xff);
    }

    /**
     * Generates the checksum of a PDU. <br>
     * 
     * This method is a direct adaption from Annex B of the ACP142 specification.
     * 
     * @return checksum
     */
    private int checksum() {
        
    	int c0 = 0;
    	int c1 = 0;
    	
    	binary[CHECKSUM_OFFSET] = 0;
    	binary[CHECKSUM_OFFSET+1] = 0;
    	int ctmp = binary.length - CHECKSUM_OFFSET - 1;
    	
    	for ( int i = 0; i < binary.length; ++i ) {
    		if ((c0 += ((int)binary[i] & 0xff)) > 254 ) {
    			c0 -= 255;
    		}
    		if ((c1 += c0) > 254 ) {
    			c1 -= 255;
    		}
    	}
    	
    	int cs = ( ( ctmp * c0) - c1 ) % 255;
    	if ( cs < 0 ) {
    		cs += 255;
    	}
    	int ret = cs << 8;
    	cs = ( c1 - ( ( ctmp + 1 ) * c0 ) ) % 255;
    	if ( cs < 0 ) {
    		cs += 255; 
    	}
    	ret |= cs;
    	
    	return ret;
    }
    
    /**
     * Adds 16 bits to the binary representation starting from given offset.
     * 
     * @param offset
     *          Offset into binary array
     * @param value
     *          The 16 bit value
     * @throws ArrayIndexOutOfBoundsException
     *           If data will not fit into the binary array
     */
    protected void addToBinary( int offset, short value ) throws ArrayIndexOutOfBoundsException {
        binary[offset] = (byte) (value >>> 8);
        binary[offset + 1] = (byte) value;
    }
    
    /**
     * Adds 32 bits to the binary representation starting from given offset.
     * 
     * @param offset
     *          Offset into binary array
     * @param value
     *          The 32 bit value
     * @throws ArrayIndexOutOfBoundsException
     *           If value will not fit into the binary array
     */
    protected void addToBinary( int offset, int value ) throws ArrayIndexOutOfBoundsException {
        binary[offset] =     (byte) (value >>> 24);
        binary[offset + 1] = (byte) (value >>> 16);
        binary[offset + 2] = (byte) (value >>> 8);
        binary[offset + 3] = (byte)  value;
    }

    /**
     * Adds given number of bytes from given data array starting from given
     * offset
     * into the binary array starting from given offset into binary.
     * 
     * @param binaryOffset
     *           The offset into the binary array
     * @param data
     *           The data array
     * @param dataOffset
     *           The offset into data array
     * @param count
     *           The number of bytes to be copied from data array
     * @return number of bytes actually added
     * @throws ArrayIndexOutOfBoundsException
     *           If data will not fit into the binary array
     */
    protected int addToBinary( int binaryOffset, byte[] data, int dataOffset, int count ) 
            throws ArrayIndexOutOfBoundsException {
        int lengthOfPDU = getLengthOfPDU();
        if ( !(binaryOffset + count <= lengthOfPDU) ) {
            throw new ArrayIndexOutOfBoundsException("Data can not fit into binary array");
        }
        int i, j;
        for (i = binaryOffset, j = dataOffset; j < dataOffset + count && j < data.length; ++i, ++j) {
            this.binary[i] = data[j];
        }
        return dataOffset + count - j;
    }

    /**
     * Returns four bytes as a 32 bit integer.
     * 
     * @param a 
     *          most significant byte
     * @param b
     *          center byte
     * @param c
     *          center byte
     * @param d
     *          least significant byte
     * @return 32 bit integer
     */
    protected static int concatenateBytes( byte a, byte b, byte c, byte d ) {
        return (a & 0xff) << 24 | (b & 0xff) << 16 | (c & 0xff) << 8 | (d & 0xff);
    }

    /**
     * Returns two bytes as a 16 bit short.
     * 
     * @param a
     *           most significant
     * @param b
     *           least significant
     * @return 16 bit short
     */
    protected static short concatenateBytes( byte a, byte b ) {
        return (short) ((a & 0xff) << 8 | (b & 0xff));
    }

    /**
     * Returns the unsigned value of a 32 bit integer.
     * 
     * @param i
     *            signed integer
     * @return unsigned integer
     */
    protected static long unsigned( int i ) {
        long l = 0xffffffffL;
        return i & l;
    }

    /**
     * Returns the unsigned value of a 16 bit short.
     * 
     * @param s
     *            signed short
     * @return unsigned short
     */
    protected static int unsigned( short s ) {
        return s & 0xffff;
    }

    /**
     * Returns the unsigned value of a short.
     * 
     * @param b
     *            signed byte
     * @return unsigned byte
     */
    protected static short unsigned( byte b ) {
        return (short) (b & 0xff);
    }

    /**
     * Determines whether given value will overflow an 8 bit byte.
     * 
     * @param value
     *          The value in question
     * @return will overflow
     */
    protected static boolean overflows8bit( int value ) {
        return (value & 0xffffff00) != 0;
    }
    
    /**
     * Determines whether given value will overflow a 16 bit short.
     * 
     * @param value
     *          The value in question
     * @return will overflow
     */
    protected static boolean overflows16bit( int value ) {
        return (value & 0xffff0000) != 0;
    }

    /**
     * Determines whether given value will overflow a 32 bit int.
     * 
     * @param value
     *          The value in question
     * @return will overflow
     */
    protected static boolean overflows32bit( long value ) {
        return (value & 0xffffffff00000000L) != 0;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(binary);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pdu other = (Pdu) obj;
		if (!Arrays.equals(binary, other.binary))
			return false;
		return true;
	}
	
	/**
	 * Returns an expire time of proper format at the given amount of 
	 * seconds into the future.
	 * @param seconds
	 *         Amount of seconds into the future
	 * @return expiry time in Unix time
	 */
	public static long computeExpiryTime(int seconds) {
	    return (System.currentTimeMillis()/1000) + seconds;
	}
}
