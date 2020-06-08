package ru.ifmo.rain.levashov.bank.tests;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

public class BankTests extends org.junit.Assert {
    public static void main(String [] args) {
        final Result result = new JUnitCore().run(Tests.class);
        if (result.wasSuccessful()) {
            System.out.println("Tests passed!");
        } else {
            System.out.println("Tests failed!");
        }
    }
}
