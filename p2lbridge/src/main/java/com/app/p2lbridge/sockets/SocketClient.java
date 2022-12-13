package com.app.p2lbridge.sockets;

import com.app.p2lbridge.Util.Util;
import com.app.p2lbridge.bridge.DataStream;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;


public class SocketClient implements Runnable, DataStream  {

	ByteBuffer bufferIn;
	ByteBuffer bufferOut;

	SocketChannel socket;
	String ipAddress;
	int port;
	SocketChannel client;

	private final List<ChangeRequest> pendingChanges = new LinkedList<ChangeRequest>();
	private final Map<SocketChannel, List<ByteBuffer>> pendingData = new HashMap<SocketChannel, List<ByteBuffer>>();

	private Selector selector;

	private BlockingQueue<ByteBuffer> socketData = new LinkedBlockingDeque<ByteBuffer>();

	public SocketClient(String ipAddress, int tcp_port) {
		this.ipAddress = ipAddress;
		this.port = tcp_port;
		bufferIn = ByteBuffer.allocate(1024);
		bufferOut = ByteBuffer.allocate(1024);
	}

	public void write(ByteBuffer message) {
		// And queue the data we want written
		synchronized (pendingData) {
			List<ByteBuffer> queue = pendingData.get(socket);
			if (queue == null) {
				queue = new ArrayList<ByteBuffer>();

			}
			queue.add(message);
			pendingData.put(socket, queue);
			
		}

		// Finally, wake up our selecting thread so it can make the required changes
		selector.wakeup();
	}

	public ByteBuffer read() {
		try {
			return socketData.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	protected void put(ByteBuffer input) {
		try {
			socketData.put(input);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	public void handleRead(SelectionKey key) throws IOException {

		SocketChannel socketChannel = (SocketChannel) key.channel();

        // Clear out our read buffer so it's ready for new data
        bufferIn.clear();

        // Attempt to read off the channel
        int numRead;
        try {
            numRead = socketChannel.read(bufferIn);
			put(bufferIn);
        } catch (IOException e) {
            // The remote forcibly closed the connection, cancel
            // the selection key and close the channel.
            key.cancel();
            socketChannel.close();
            return;
        }

        if (numRead == -1) {
            // Remote entity shut the socket down cleanly. Do the
            // same from our end and cancel the channel.
            key.channel().close();
            key.cancel();
            return;
        }

        // Handle the response
        handleResponse(socketChannel, bufferIn.array(), numRead);
	}
    private void handleResponse(SocketChannel socketChannel, byte[] data, int numRead) throws IOException {
        // Make a correctly sized copy of the data before handing it
        // to the client
        byte[] rspData = new byte[numRead];
        System.arraycopy(data, 0, rspData, 0, numRead);

        // handle responses
        System.out.println("Client: "  + Util.bytesToHex(rspData));
		
    }
	public void handleWrite(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		socketChannel.register(selector, SelectionKey.OP_WRITE); 

        synchronized (pendingData) {
            List<ByteBuffer> queue = pendingData.get(socketChannel);

            // Write until there's not more data ...
            while (queue != null && !queue.isEmpty()) {
                ByteBuffer buf = queue.get(0);
				buf.flip();
				buf.mark();
                socketChannel.write(buf);
                if (buf.remaining() > 0) {
                    // ... or the socket's buffer fills up
                    break;
                }
                queue.remove(0);
            }

            if (queue == null || queue.isEmpty()) {
                // We wrote away all data, so we're no longer interested in
                // writing on this socket. Switch back to waiting for data.
                key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            }
        }
	}
	public void disconnect(SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();

        // Finish the connection. If the connection operation failed
        // this will raise an IOException.
        try {
            socketChannel.finishConnect();
        } catch (IOException e) {
            // Cancel the channel's registration with our selector
            System.out.println(e);
            key.cancel();
            return;
        }

        // Register an interest in writing on this channel
        key.interestOps(SelectionKey.OP_WRITE);
	}
	public void run() { // this will run in parallel to the main thread

			while (true) {
				try {
					if(client == null || !client.isConnected()){

						if(client == null){
							client = SocketChannel.open();
							client.configureBlocking(false);
							selector = SelectorProvider.provider().openSelector();
						}
			
						SocketAddress remote = new InetSocketAddress(ipAddress, port);
						client.connect(remote);
						this.socket = client;
			
			
						synchronized(this.pendingChanges) {
							this.pendingChanges.add(new ChangeRequest(client, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
						}
					}

					synchronized (pendingChanges) {
						for (ChangeRequest pendingChange : pendingChanges) {   
							switch (pendingChange.type) {
								case ChangeRequest.CHANGEOPS:
									SelectionKey key = pendingChange.socket.keyFor(selector);
									key.interestOps(pendingChange.ops);
									break;
								case ChangeRequest.REGISTER:
									pendingChange.socket.register(selector,pendingChange.ops);
									break;
							}
						}
						pendingChanges.clear();
					}
	
					// Wait for an event one of the registered channels
					selector.select();
	
					// Iterate over the set of keys for which events are available
					Iterator<SelectionKey>  selectedKeys = this.selector.selectedKeys().iterator();
	
					while (selectedKeys.hasNext()) {
						SelectionKey key = (SelectionKey) selectedKeys.next();
						selectedKeys.remove();
	
						if (!key.isValid()) {
							continue;
						}
	
						// Check what event is available and deal with it
						if (key.isConnectable()) {
							this.disconnect(key);
						} else if (key.isReadable()) {
							this.handleRead(key);
						} else if (key.isWritable()) {
							this.handleWrite(key);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					client = null;
				}
			}

	}

}


