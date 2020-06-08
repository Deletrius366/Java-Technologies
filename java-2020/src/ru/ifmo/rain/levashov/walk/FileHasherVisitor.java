package ru.ifmo.rain.levashov.walk;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

class FileHasherVisitor extends SimpleFileVisitor<Path> {
    private final static int PRIME = 0x01000193;
    private final static int FIRST = 0x811c9dc5;

    private BufferedWriter hashWriter;

    FileHasherVisitor(BufferedWriter w) {
        hashWriter = w;
    }

    FileVisitResult writeHash(int hash, String file) throws IOException {
        hashWriter.write(String.format("%08x %s%n", hash, file));
        return FileVisitResult.CONTINUE;
    }

    private FileVisitResult hash(Path file) throws IOException {
        int hash = FIRST;

        try (InputStream is = new BufferedInputStream(Files.newInputStream(file))) {
            byte[] buf = new byte[8096];
            int len;

            while ((len = is.read(buf)) >= 0) {
                for (int i = 0; i < len; i++) {
                    hash *= PRIME;
                    hash ^= Byte.toUnsignedInt(buf[i]);
                }
            }
        } catch (IOException e) {
            hash = 0;
            System.err.println("Failed to read file: " + file + ": " + e.getMessage());
        }

        return writeHash(hash, file.toString());
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes bfa) throws IOException {
        return hash(file);
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
        return writeHash(0, file.toString());
    }
}
