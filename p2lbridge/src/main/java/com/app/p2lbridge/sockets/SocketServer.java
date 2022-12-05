package com.app.p2lbridge.sockets;

import com.app.p2lbridge.Util.Util;
import com.app.p2lbridge.bridge.DataStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;




public class SocketServer implements Runnable, DataStream {

	private ByteBuffer bufferIn;
	private SelectionKey acceptKey;
	ServerSocketChannel acceptor;
	private int port;
	private Selector selector;
	private BlockingQueue<ByteBuffer> socketData = new LinkedBlockingDeque<ByteBuffer>();

	
	private final List<ChangeRequest> pendingChanges = new LinkedList<ChangeRequest>();
	private final Map<SocketChannel, List<ByteBuffer>> pendingData = new HashMap<SocketChannel, List<ByteBuffer>>();

	public SocketServer(int tcp_port) {

		this.port = tcp_port;

		bufferIn = ByteBuffer.allocate(1024);


		try
		{

			// Bind to 0.0.0.0 address which is the local network stack
			InetAddress addr = InetAddress.getByName("0.0.0.0");

			// Open a new ServerSocketChannel so we can listen for connections
			acceptor = ServerSocketChannel.open();

			// Configure the socket to be non-blocking as part of the new-IO library (NIO)
			acceptor.configureBlocking(false);

			// Bind our socket to the local port (5555)
			acceptor.socket().bind(new InetSocketAddress(addr.getHostName(), port));

			// Reuse the address so more than one connection can come in
			acceptor.socket().setReuseAddress(true);

			// Open our selector channel
			selector = SelectorProvider.provider().openSelector();

			// Register an "Accept" event on our selector service which will let us know when sockets connect to our channel
			acceptKey = acceptor.register(selector, SelectionKey.OP_ACCEPT);

			// Set our key's interest OPs to "Accept"
			acceptKey.interestOps(SelectionKey.OP_ACCEPT);

		}
		catch(Exception e){
			e.printStackTrace();
		}


	}

	public void write(ByteBuffer message) {
		try {
			synchronized (this.pendingChanges) {
				synchronized (this.pendingData) {
					selector.select();
					Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
				
					while (iterator.hasNext()) {
						SelectionKey key = (SelectionKey) iterator.next();
						SocketChannel socketChannel = (SocketChannel) key.channel();
						iterator.remove();
			
						List<ByteBuffer> queue = (List<ByteBuffer>) this.pendingData.get(socketChannel);
						if (queue == null) {
							queue = new ArrayList<ByteBuffer>();
							
						}
						queue.add(message);
						this.pendingData.put(socketChannel, queue);
			
					}
	
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

        this.selector.wakeup();
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
        this.bufferIn.clear();

        // Attempt to read off the channel
        int numRead;
        try {
            numRead = socketChannel.read(this.bufferIn);
			put(bufferIn);
					// Handle the response

        } catch (IOException e) {
            // The remote forcibly closed the connection, cancel
            // the selection key and close the channel.
            System.out.println("IOException on socketChannel " + System.identityHashCode(socketChannel) +". closing.");
            key.cancel();
            socketChannel.close();
            return;
        }


        if (numRead == -1) {
            // Remote entity shut the socket down cleanly. Do the
            // same from our end and cancel the channel.
            System.out.println("Client closed socketChannel " + System.identityHashCode(socketChannel));

            key.channel().close();
            key.cancel();
            return;
        }

		handleResponse(socketChannel, bufferIn.array(), numRead);



	}

	private void handleResponse(SocketChannel socketChannel, byte[] data, int numRead) throws IOException {
        // Make a correctly sized copy of the data before handing it
        // to the client
        byte[] rspData = new byte[numRead];
        System.arraycopy(data, 0, rspData, 0, numRead);

        // handle responses
        System.out.println("Server:" + Util.bytesToHex(rspData));
    }

	public void handleWrite(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

        synchronized (this.pendingData) {
            List<ByteBuffer> queue = this.pendingData.get(socketChannel);

            // Write until there's not more data ...
            while (queue != null && !queue.isEmpty()) {
                ByteBuffer buf = (ByteBuffer) queue.get(0);
				buf.flip();
				buf.mark();
                socketChannel.write(buf);
                if (buf.remaining() > 0) {
                    // ... or the socket's buffer fills up
                    break;
                }
                queue.remove(0);
            }

			/*
            if (queue.isEmpty()) {
                // We wrote away all data, so we're no longer interested
                // in writing on this socket. Switch back to waiting for
                // data.
                pendingData.remove(socketChannel);
                socketChannel.close();
                key.cancel();
                System.out.println("Closed socketChannel " + System.identityHashCode(socketChannel));
            }
			*/
			if (queue == null || queue.isEmpty()) {
                // We wrote away all data, so we're no longer interested in
                // writing on this socket. Switch back to waiting for data.
                key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            }
        }
	}

	public void disconnect(SelectionKey key) {
		try {
			SocketChannel client = (SocketChannel) key.channel(); 
			client.close();
			key.cancel();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void run() { // this will run in parallel to the main thread
		
			while(true){
				try {

					// Process any pending changes
					synchronized (this.pendingChanges) {
						Iterator<ChangeRequest> changes = this.pendingChanges.iterator();
						while (changes.hasNext()) {
							ChangeRequest change = (ChangeRequest) changes.next();
							switch (change.type) {
								case ChangeRequest.CHANGEOPS:
									SelectionKey key = change.socket.keyFor(this.selector);
									try {
										key.interestOps(change.ops);
									} catch (Exception e) {
										System.out.println("error changing socketChannel " + System.identityHashCode(change.socket) +
												" to " + change.ops);
									}
							}
						}
						this.pendingChanges.clear();
					}

									

					selector.select();
					Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
	
					while (iterator.hasNext()) {
						SelectionKey key = (SelectionKey) iterator.next();
						iterator.remove();

						if (!key.isValid()) {
							continue;
						}
		
						if (key.isAcceptable()) {
							accept(key);
						}
	
						if (key.isReadable()) {
							handleRead(key);
						}
	
						if (key.isWritable()) {
							handleWrite(key);
						}
	
					}
				} 
				catch(IOException ex)
				{
					ex.printStackTrace();
				}
				catch (Exception e) {
					e.printStackTrace();
				
				}
			}

	}

	private  void accept(SelectionKey key) throws IOException {
        SocketChannel client = acceptor.accept();
        String ipAddress = client.socket().getInetAddress().getHostAddress();

        System.out.println("User connected " + ipAddress);
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		client.configureBlocking(false);
    }
		
}
