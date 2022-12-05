package com.app.p2lbridge.bridge;

import java.nio.ByteBuffer;

public interface DataStream {

	/**
	 * Allows reading packets from the stream
	 * @return the oldest packet that has not been parsed yet 
	 */
	public ByteBuffer read();

	/**
	 * Allows writing packets (as strings) to the stream
	 * @param the packet to be written to the stream, as a string
	 */
	public void write(ByteBuffer s);
}
