package ru.ifmo.rain.levashov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RecursiveWalk {

    private static boolean badArgs(String[] args) {
        if (args != null && args.length == 2 && args[0] != null && args[1] != null) {
            return false;
        }

        if (args == null || args.length != 2) {
            System.err.println("Expected exactly 2 arguments, input and output files");
        } else if (args[0] == null) {
            System.err.println("Expected not null input file");
        } else {
            System.err.println("Expected not null output file");
        }
        return true;
    }

    private static void createPath(Path file) throws WalkerException {
        Path parent = file.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new WalkerException("Failed to create output file path: " + e.getMessage(), e);
            }
        }
    }

    private static Path safeGetPath(String path, String errorMessage) throws WalkerException {
        try {
            return Paths.get(path);
        } catch (InvalidPathException e) {
            throw new WalkerException(errorMessage + ": " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        if (badArgs(args)) {
            return;
        }

        try {
            Path inputFile = safeGetPath(args[0], "Invalid input file path");
            Path outputFile = safeGetPath(args[1], "Invalid output file path");
            createPath(outputFile);
            walk(inputFile, outputFile);
        } catch (WalkerException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void walk(Path inputFile, Path outputFile) throws WalkerException {
        try (BufferedReader br = Files.newBufferedReader(inputFile)) {
            try (BufferedWriter bw = Files.newBufferedWriter(outputFile)) {
                FileHasherVisitor hasher = new FileHasherVisitor(bw);
                try {
                    String file;
                    while ((file = br.readLine()) != null) {
                        try {
                            try {
                                Files.walkFileTree(Paths.get(file), hasher);
                            } catch (InvalidPathException e) {
                                hasher.writeHash(0, file);
                            }
                        } catch (IOException e) {
                            throw new WalkerException("Failed to write filename and hash to output file: " + e.getMessage(), e);
                        }
                    }
                } catch (IOException e) {
                    throw new WalkerException("Failed to read filename from input file: " + e.getMessage(), e);
                }
            } catch (IOException e) {
                throw new WalkerException("Failed to write to output file: " + e.getMessage(), e);
            }
        } catch (IOException e) {
            throw new WalkerException("Failed to read input file: " + e.getMessage(), e);
        }
    }
}
