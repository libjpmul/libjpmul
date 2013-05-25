package no.ntnu.acp142.rdt;

import java.util.ArrayList;

/*
 * Copyright (c) 2013, Bjørn Tungesvik, Thomas Martin Schmid, Karl Mardoff Kittilsen
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
 * Class containing timer specific information
 * @author Bjørn Tungesvik, Thomas Martin Schmid, Karl Mardoff Kittilsen
 * 
 */

public class Timer {

    /**
     * Number of milliseconds after startTime, that we want this timer to end.
     */
    private long         endTime;

    /**
     * Type of this timer, as specified in TimerType.
     */
    private TimerType    type;

    /**
     * Message this timer is associated with. This refers to our internal
     * storage structure instance for the message.
     */
    private MessageEntry messageEntry;

    /**
     * Timestamp (milliseconds) at which the timer starts.
     */
    private long         startTime;
    
    /**
     * List of message entries
     */
    private ArrayList <MessageEntry> entries = null;

    /**
     * Create a new timer
     * 
     * @param endTime
     *            Number of milliseconds until expiry.
     * @param startTime 
     *             Should be current unix time in milliseconds.
     * @param type
     *            Type of timer.
     * @param ref
     *            Message it is associated with.
     */
    Timer(long endTime, long startTime, TimerType type, MessageEntry ref) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.type = type;
        this.messageEntry = ref;
    }

    /**
     * Get start time (unix time).
     * 
     * @return start time.
     */
    public long getStartTime( ) {
        return startTime;
    }

    /**
     * Get milliseconds from start time, until the timer should end.
     * 
     * @return end time
     */
    public long getEndTime( ) {
        return endTime;
    }

    /**
     * Get timer type.
     * 
     * @return type of the timer.
     */
    public TimerType getType( ) {
        return type;
    }

    /**
     * Get message entry associated with this timer.
     * 
     * @return message entry associated with this timer.
     */
    public MessageEntry getMsg( ) {
        return this.messageEntry;
    }
    
    /**
     * Set MessageEntryList associated with the timer
     * @param entries all entries associated with this timer
     */
    
    public void setMessageEntryList(ArrayList <MessageEntry> entries){
    	this.entries = entries;
    }
    
    /**
     * Get MessageEntryList associated with the timer
     * 
     * @return list of messageEntries
     */
    
    public ArrayList <MessageEntry> getMessageEntryList(){
    	return entries;
    }
    
    

}
