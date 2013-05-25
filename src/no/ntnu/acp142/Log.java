package no.ntnu.acp142;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

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

/**
 * Class that handles writing to the log file
 * @author Thomas Martin Schmid, Karl Mardoff Kittilsen
 *
 */
public class Log {


	// --------------------------------------
	// Log levels
	/**
	 * At this log level, only critical errors are reported. Any exception we
	 * can not recover from, or which hinders normal operation of the protocol
	 * is logged.
	 */
	public static final byte LOG_LEVEL_QUIET = 0;
	/**
	 * All caught exceptions are logged. If multiple warnings of the same type
	 * are observed indicating abnormal functionality of the protocol itself of
	 * lower level components, this should also be mentioned.
	 */
	public static final byte LOG_LEVEL_NORMAL = 1;
	/**
	 * Any warning and abnormal behavior is logged. In addition, major events
	 * should be logged, including start and completion of a message
	 * transmission.
	 */
	public static final byte LOG_LEVEL_VERBOSE = 2;
	/**
	 * Any message that might be useful for debugging is logged. This could be
	 * method entry and exit markers, information on messages, PDUs or datagrams
	 * sent or received, statistics on individual nodes etc.
	 */
	public static final byte LOG_LEVEL_DEBUG = 3;
	// --------------------------------------

	/**
	 * The location of the log file
	 */
	File logFile = null;
	/**
	 * The level of operation
	 */
	byte logLevel;
	/**
	 * If set to true, everything that is written to file is also printed to
	 * system out.
	 */
	boolean printToStdOut;
	/**
	 * File writer we write to the file with.
	 */
	private LogThread writerThread = null;
	/**
	 * Queue where messages to write are queued for the writer thread.
	 */
	private ConcurrentLinkedQueue<String> messageQueue = null;
	/**
	 * The writing thread requires an instance, therefore the singleton. This is
	 * the Log instance.
	 */
	private static Log instance = null; 
	/**
	 * The thread we create to handle the shutdown hook for writerThread
	 */
	private Thread shutdownHookThread = null;
	
	/**
	 * Creates the singleton instance. Sets standard values.
	 */
	private Log() {
		logFile = new File(Configuration.getLogFileLocation());
		logLevel = LOG_LEVEL_NORMAL;
		printToStdOut = false;
		messageQueue = new ConcurrentLinkedQueue<String>();
		
		shutdownHookThread = new Thread() {
			public void run() {
				writerThread.stop();
			}
		};
		startWriterThread();
	}
	
	/**
	 * Gets the singleton instance, initializing it first if it does not already exist.
	 * @return instance of Log with running LogThread.
	 */
	private synchronized static Log getSingleton() {
		if ( instance == null ) {
			instance = new Log();
		}
		if ( instance.writerThread == null ) {
			instance.startWriterThread();
		}
		return instance;
	}
	
	/**
	 * Starts the writer thread (LogThread) and adds a shutdown hook for it.
	 */
	private void startWriterThread() {
		// Start listening thread
		writerThread = new LogThread();
		new Thread(writerThread).start();
		
		// Add the shutdown hook to stop the thread in case of
		// crashes/interrupts.
		Runtime.getRuntime().addShutdownHook(shutdownHookThread);
	}
	
	/**
	 * Closes the writer and stops the writerThread (removing the shutdown hook)
	 */
	public static void close() {
		Log log = getSingleton();
		log.writerThread.stop();
		log.writerThread = null;
		Runtime.getRuntime().removeShutdownHook(log.shutdownHookThread);
	}
	
	/**
	 * Set the log level to write at. Anything with a lower or equal level than
	 * this will be written. If the level is DEBUG, it also sets printToStdOut
	 * to true.
	 * 
	 * @param level the log level.
	 */
	public static void setLogLevel(byte level) {
		getSingleton().logLevel = level;
		if ( level == LOG_LEVEL_DEBUG ) {
			setPrintToStdOut(true);
		}
	}

	/**
	 * Sets the log file's location
	 *
	 * @param fileLocation
	 *            Path to log file
	 */
	public static void setFileLocation(String fileLocation) {
		Log log = getSingleton();
		log.logFile = new File(fileLocation);
		log.writerThread.setFileDirty();
	}

	/**
	 * Writes a string to the logfile.
	 * 
	 * @param level
	 *            Minimum level the log should be set to for this to be written.
	 *            See the finals in this class.
	 * @param str
	 *            String to log.
	 */
	public static void write(byte level, String str) {
		Log log = getSingleton();
		if (level > log.logLevel) {
			return;
		}
		// Check if the writerThread is not running, if so, start it
		if ( log.writerThread == null ) {
			log.startWriterThread();
		}

		log.messageQueue.add(str);
	}

	/**
	 * Writes a string to the log file followed by a newline. Starts by printing
	 * the calling function.
	 * 
	 * @param level
	 *            Minimum level the log should be set to for this to be written.
	 *            See the finals in this class.
	 * @param str
	 *            String to log.
	 */
	public static void writeLine(byte level, String str) {
		// Get caller
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		write(level,
				elements[2].getClassName() + "." + elements[2].getMethodName()
						+ ": " + str + "\n");
	}

	/**
	 * Sets whether all output is printed to system out as well as written to
	 * log file.
	 * 
	 * @param print
	 *            to standard out?
	 */
	public static void setPrintToStdOut(boolean print) {
		getSingleton().printToStdOut = print;
	}
	
	/**
	 * Class that takes queued messages and writes them to the file.
	 */
	private class LogThread implements Runnable {

		/**
		 * Boolean that controls the lifespan of the thread.
		 */
		private boolean isRunning = true;
		
		/**
		 * Boolean that tells the thread loop if the file is dirty and the
		 * writer must be closed and reopened.
		 */
		private boolean fileDirty = false;
		
		/**
		 * Stops the thread
		 */
		public void stop() {
			isRunning = false;
		}
		
		/**
		 * Sets that logFile is dirty. This prompts the loop to close and reopen
		 * the writer.
		 */
		public void setFileDirty() {
			fileDirty = true;
		}
		
		@Override
		public void run() {
			// Create the writer
			FileWriter writer;
			try {
				writer = new FileWriter(logFile);
				// Write that the log was started
				if ( logLevel > LOG_LEVEL_DEBUG ) {
					writer.append("LogThread started\n");
				}
			} catch (IOException e) {
				System.out.println("Error in Log (LogThread.run): Could not open log file for writing!");
				System.out.println(e.toString());
				return;
			}			
			
			// Listen to messageQueue & append any strings that come in
			while( isRunning ) {
				if ( messageQueue.peek() == null ) {
					try {
						writer.close();
						Thread.sleep(500L);
					} catch (InterruptedException e) {
						System.out.println("Sleep interrupted in Log( LogThread.run). Exception:");
						e.printStackTrace();
					} catch( IOException e ) {
						System.out.println("Could not close Log file. Exception:");
						e.printStackTrace();
					}
					continue;
				}
				// Test if the file is dirty, and if it is, reopen the writer to the potentially new file location
				if ( fileDirty ) {
					try {
						writer.close();
						writer = new FileWriter(logFile);
					} catch (IOException e) {
						System.out.println("Error in Log (LogThread.run): Could not close and reopen log file");
					}
					fileDirty = false;
				}
				// We have a message, write it.
				String out = messageQueue.poll();
				try {
					writer = new FileWriter(logFile);
					writer.append(out);
				} catch (IOException e) {
					System.out.println("Error in Log (LogThread.run): Could not append to log file");
				}

                // Print to std out
                if ( printToStdOut ) {
                    System.out.print(out);
                }
				
			}
			
			// Close the writer
			try {
				if ( logLevel > LOG_LEVEL_DEBUG ) {
					writer.append("LogThread stopped\n");
				}
				writer.close();
			} catch (IOException e) {
				System.out.println("Error in Log (LogThread.run): Could not close log file for writing!");
				System.out.println(e.toString());
			}
		}
		
	}
}
