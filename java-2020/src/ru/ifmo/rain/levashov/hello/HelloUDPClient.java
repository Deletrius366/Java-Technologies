package ru.ifmo.rain.levashov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static ru.ifmo.rain.levashov.hello.Utils.checkReceive;

public class HelloUDPClient implements HelloClient {

    private static boolean checkReceiveStrings(String s, String threadId, String requestId) {
        return checkReceive(ByteBuffer.wrap(s.getBytes()), ByteBuffer.wrap(threadId.getBytes()), ByteBuffer.wrap(requestId.getBytes()));
    }

    // :NOTE(solved): Пробелы
    static void clientMain(final HelloClient client, final String[] args) {
        if (args == null || args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.out.println("Expected exactly 5 not-null arguments");
            return;
        }
        try {
            final int port = Integer.parseInt(args[1]);
            final int threads = Integer.parseInt(args[3]);
            final int requests = Integer.parseInt(args[4]);
            client.run(args[0], port, args[2], threads, requests);
        } catch (final NumberFormatException e) {
            System.out.println("Second, fourth and fifth arguments should be integer");
        }
    }

    public static void main(final String []args) {
        clientMain(new HelloUDPClient(), args);
    }

    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        try {
            final SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(host), port);
            final ExecutorService workers = Executors.newFixedThreadPool(threads);
            // :NOTE(solved): Stream
            IntStream.range(0, threads).forEach(index -> workers.submit(() -> doTask(socketAddress, prefix, index, requests)));
            workers.shutdown();
            workers.awaitTermination(5 * requests * threads, TimeUnit.SECONDS);
        } catch (final UnknownHostException e) {
            System.out.println("Failed to find host: " + e.getMessage());
        } catch (final InterruptedException e) {
            System.err.println("Failed to terminate threads: " + e.getMessage());
        }
    }

    private void doTask(final SocketAddress address, String prefix, final int threadId, final int requests) {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(300);
            final int bufferSize = socket.getReceiveBufferSize();
            final DatagramPacket packet = new DatagramPacket(new byte[bufferSize], bufferSize, address);
            prefix += threadId;
            byte[] buffer = new byte[bufferSize];
            for (int i = 0; i < requests; i++) {
                final String message = prefix + "_" + i;
                System.out.println("Sending: " + message);

                boolean received = false;
                // :NOTE(solved): каждый раз новая регулярка
                while (!received && !socket.isClosed() && !Thread.interrupted()) {
                    try {
                        packet.setData(message.getBytes(StandardCharsets.UTF_8));
                        socket.send(packet);

                        // :NOTE(solved): Каждый раз новый буфер
                        packet.setData(buffer);
                        socket.receive(packet);

                        final String data = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                        if (checkReceiveStrings(data, Integer.toString(threadId),Integer.toString(i))) {
                            received = true;
                            System.out.println("Received: " + data);
                        }
                    } catch (final IOException e) {
                        System.out.println("Failed to send or receive packet via socket: " + e.getMessage());
                    }
                }
            }
        } catch (final SocketException e) {
            System.err.println("Failed to establish connection via socket: " + e.getMessage());
        }
    }
}
