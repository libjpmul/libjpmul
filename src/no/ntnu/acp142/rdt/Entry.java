package no.ntnu.acp142.rdt;

import java.util.ArrayList;

import no.ntnu.acp142.pdu.Pdu;

/*
 * Copyright (c) 2013, Erik Lothe
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
 * Abstract class representing something to send. It can be either a PacketEntry
 * or MessageEntry.
 * 
 * @author Erik Lothe
 * 
 */
public abstract class Entry {

	/**
	 * Subclass of Entry
	 */
	public enum EntryType {
		/**
		 * The MessageEntry class
		 */
		MESSAGE_ENTRY, 
		/**
		 * The PacketEntry class 
		 */
		PACKET_ENTRY
	}

	/**
	 * The type of (subclass of) Entry
	 */
	public EntryType type;

	/**
	 * Returns the type of this Entry.
	 * 
	 * @return type
	 */
	public abstract EntryType getType();

	/**
	 * Returns all PDU packets in this Entry.
	 * 
	 * @return pdus
	 */
	public abstract ArrayList<Pdu> getPdus();

}
