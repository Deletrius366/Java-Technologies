package ru.ifmo.rain.levashov.hello;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

class Utils {

    static void safeClose(Closeable obj) {
        try {
            obj.close();
        } catch (IOException e) {
            System.err.println("Failed to close " + obj.getClass().getName() + ": " + e.getMessage());
        }
    }

    static void putInt(ByteBuffer buffer, int num) {
        if (num == 0) {
            buffer.put((byte) '0');
        } else {
            putNotZeroInt(buffer, num);
        }
    }

    private static void putNotZeroInt(ByteBuffer buffer, int num) {
        if (num == 0) {
            return;
        }
        putNotZeroInt(buffer, num / 10);
        buffer.put((byte) ('0' + num % 10));
    }

    private static void skipNotDigits(ByteBuffer buffer) {
        while (buffer.position() < buffer.limit()) {
            if (Character.isDigit(buffer.get())) {
                buffer.position(buffer.position() - 1);
                return;
            }
        }
    }

    private static boolean findInt(ByteBuffer buffer, ByteBuffer pattern) {
        skipNotDigits(buffer);
        byte b;
        while (pattern.position() < pattern.limit() && buffer.position() < buffer.limit() && Character.isDigit(b = buffer.get())) {
            if (b != pattern.get()) {
                return false;
            }
        }
        return pattern.position() == pattern.limit();
    }

    static boolean checkReceive(ByteBuffer buffer, ByteBuffer channelId, ByteBuffer requestId) {
        if (!findInt(buffer, channelId)) {
            return false;
        }
        if (!findInt(buffer, requestId)) {
            return false;
        }
        skipNotDigits(buffer);
        return buffer.position() == buffer.limit();
    }
}
