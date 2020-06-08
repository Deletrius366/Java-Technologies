package ru.ifmo.rain.levashov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static ru.ifmo.rain.levashov.hello.HelloUDPClient.clientMain;
import static ru.ifmo.rain.levashov.hello.Utils.*;

public class HelloUDPNonblockingClient implements HelloClient {

    private Selector selector;
    private int bufferSize;
    private String globalPrefix;
    private int requests;
    private SocketAddress address;

    private class KeyContext {

        private static final int BUFFER_SIZE_INT = 20;

        private ByteBuffer channelIdBytes;
        private int requestId;
        private ByteBuffer requestIdBytes;
        ByteBuffer buffer;
        private byte[] prefix;

        KeyContext(int channelId) {
            channelIdBytes = ByteBuffer.allocate(BUFFER_SIZE_INT);
            putInt(channelIdBytes, channelId);
            channelIdBytes.flip();
            this.requestId = 0;
            requestIdBytes = ByteBuffer.allocate(bufferSize);
            this.buffer = ByteBuffer.allocate(BUFFER_SIZE_INT);
            this.prefix = (globalPrefix + channelId + "_").getBytes();
        }
    }

    public static void main(final String []args) {
        clientMain(new HelloUDPNonblockingClient(), args);
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        this.globalPrefix = prefix;
        this.requests = requests;
        try {
            address = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (UnknownHostException e) {
            System.err.println("Failed to resolve host: " + e.getMessage());
        }
        try {
            selector = Selector.open();
        } catch (IOException e) {
            System.err.println("Failed to open selector: " + e.getMessage());
        }
        for (int i = 0; i < threads; i++) {
            DatagramChannel channel;
            try {
                channel = DatagramChannel.open();
            } catch (IOException e) {
                System.out.println("Failed to open channel " + i + " :" + e.getMessage());
                continue;
            }
            try {
                channel.configureBlocking(false);
                channel.connect(address);
                bufferSize = channel.socket().getReceiveBufferSize();
                channel.register(selector, SelectionKey.OP_WRITE, new KeyContext(i));
                channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            } catch (IOException e) {
                System.out.println("Error occurred during channel " + i + " :" + e.getMessage());
                safeClose(channel);
            }
        }
        mainWork();
    }

    private void writeWork(SelectionKey key) throws IOException {
        KeyContext context = (KeyContext) key.attachment();
        DatagramChannel channel = (DatagramChannel) key.channel();

        context.buffer.clear().put(context.prefix);
        putInt(context.buffer, context.requestId);
        channel.send(context.buffer.flip(), address);
        System.out.println("Sent: " + StandardCharsets.UTF_8.decode(context.buffer.flip()).toString());

        key.interestOpsOr(SelectionKey.OP_READ);
        key.interestOpsAnd(~SelectionKey.OP_WRITE);
    }

    private void readWork(SelectionKey key) throws IOException {
        KeyContext context = (KeyContext) key.attachment();
        DatagramChannel channel = (DatagramChannel) key.channel();

        ByteBuffer buffer = context.buffer;
        channel.receive(buffer.clear());
        putInt(context.requestIdBytes.clear(), context.requestId);
        context.requestIdBytes.flip();
        context.channelIdBytes.position(0);
        if (checkReceive(context.buffer.flip(), context.channelIdBytes, context.requestIdBytes)) {
            System.out.println("Received: " + StandardCharsets.UTF_8.decode(context.buffer.flip()).toString());
            context.requestId++;
            if (context.requestId == requests) {
                key.channel().close();
                return;
            }
        }
        key.interestOpsOr(SelectionKey.OP_WRITE);
        key.interestOpsAnd(~SelectionKey.OP_READ);
    }

    private void mainWork() {
        while (!Thread.interrupted() && !selector.keys().isEmpty()) {
            try {
                selector.select(200);
                if (selector.selectedKeys().isEmpty()) {
                    for (SelectionKey key : selector.keys()) {
                        if (key.isWritable()) {
                            writeWork(key);
                        }
                    }
                }
                for (final Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                    final SelectionKey key = i.next();
                    if (key.isWritable()) {
                        writeWork(key);
                    }
                    if (key.isReadable()) {
                        readWork(key);
                    }
                    i.remove();
                }
            } catch (IOException e) {
                System.err.println("Failed to send or receive data: " + e.getMessage());
            }
        }
    }
}

