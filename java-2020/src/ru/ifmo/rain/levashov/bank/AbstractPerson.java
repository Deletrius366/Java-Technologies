package ru.ifmo.rain.levashov.bank;

import java.rmi.Remote;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractPerson implements Person {
    protected String firstName;
    protected String lastName;
    protected int passportId;
    protected ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();

    AbstractPerson() {}

    AbstractPerson(String firstName, String lastName, int passportId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.passportId = passportId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public int getPassportId() {
        return passportId;
    }

    public ConcurrentMap<String, Account> getAccounts() {
        return accounts;
    }
}
