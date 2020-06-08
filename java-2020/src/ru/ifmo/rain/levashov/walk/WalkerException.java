package ru.ifmo.rain.levashov.walk;

import java.io.IOException;

class WalkerException extends Exception {
    WalkerException(String message, Throwable e) {
        super(message, e);
    }
}
