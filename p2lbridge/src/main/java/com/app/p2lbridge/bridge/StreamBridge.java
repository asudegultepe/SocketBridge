package com.app.p2lbridge.bridge;

public class StreamBridge implements Runnable {
	DataStream producer;
	DataStream consumer;

	public StreamBridge(DataStream producer, DataStream consumer) {
		this.producer = producer;
		this.consumer = consumer;
	}

	@Override
	public void run() {
		while(true){
			consumer.write(producer.read()); //waits until data is available, fetches them and then writes them to the other stream
		}
	}

}
