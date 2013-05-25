package no.ntnu.acp142.rdt;


/*
 * Copyright (c) 2013, Bjørn Tungesvik
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
 * The class is used to create an unique hash value to represent the messageId
 * and sourceId. The hash value is used to uniquely identify a message.
 * 
 * @author Bjørn Tungesvik
 * 
 */

public class HashValue {
	private int msid;
	private int sourceId;

	/**
	 * Create a new HashValue with the given message ID and Source ID.
	 * @param msid of the hash value.
	 * @param sourceId of the hash value.
	 */
	public HashValue(int msid, int sourceId) {
		this.msid = msid;
		this.sourceId = sourceId;
	}

	/**
	 * Debug method. Prints the source id and message id in a readable format.
	 */
	public String toString() {
		String string = "";
		string += "MessageId = " + msid;
		string += "SourceId = " + sourceId;
		return string;
	}

	/**
	 * Creates and return the hashValue of <SourceId, MessageId>
	 * 
	 *@return unique hash value
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + msid;
		result = prime * result + sourceId;
		return result;
	}

	/**
	 * Check whether two hash values are equal
	 * 
	 *@return True if equal, false otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HashValue other = (HashValue) obj;
		if (msid != other.msid)
			return false;
		if (sourceId != other.sourceId)
			return false;
		return true;
	}

}
