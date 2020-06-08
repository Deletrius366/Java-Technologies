package ru.ifmo.rain.levashov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// :NOTE(solved): module-info
public class HelloUDPServer implements HelloServer {


    private DatagramSocket socket;
    private int bufferSize;
    private ExecutorService workers;

    // :NOTE(solved): Пробелы
    static void serverMain(HelloServer server, final String[] args) {
        if (args == null || args.length != 2 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.out.println("Expected exactly 2 not-null arguments");
            return;
        }
        try {
            try (server) {
                server.start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
                final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                br.readLine();
            } catch (final IOException e) {
                System.out.println("Failed to read line from input: " + e.getMessage());
            }
        } catch (final NumberFormatException e) {
            System.out.println("First and second arguments should be integer");
        }
    }

    public static void main(final String[] args) {
        serverMain(new HelloUDPServer(), args);
    }

    @Override
    public void start(final int port, final int threads) {
        try {
            socket = new DatagramSocket(port);
            bufferSize = socket.getReceiveBufferSize();
            workers = Executors.newFixedThreadPool(threads);
            doTask(threads);
        } catch (final SocketException e) {
            System.err.println("Failed to establish connection via socket: " + e.getMessage());
        }
    }

    private void doTask(final int threads) {
        for (int i = 0; i < threads; i++) {
            workers.submit(() -> {
                byte[] buffer = new byte[bufferSize];
                final DatagramPacket packet = new DatagramPacket(buffer, bufferSize);
                try {
                    while (!socket.isClosed() && !Thread.interrupted()) {
                        // :NOTE(solved): Каждый раз новый пакет
                        try {
                            packet.setData(buffer);
                            socket.receive(packet);
                            final String responce = "Hello, " + new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                            packet.setData(responce.getBytes(StandardCharsets.UTF_8));
                            socket.send(packet);
                        } catch (final IOException e) {
                            if (!socket.isClosed()) {
                                System.err.println("Failed to send or receive message via socket: " + e.getMessage());
                            }
                        }
                    }
                } finally {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }


    @Override
    public void close() {
        socket.close();
        workers.shutdown();
        try {
            workers.awaitTermination(1, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            System.err.println("Failed to terminate threads: " + e.getMessage());
        }
    }
}
