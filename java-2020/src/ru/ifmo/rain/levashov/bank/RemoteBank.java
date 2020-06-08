package ru.ifmo.rain.levashov.bank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBank implements Bank {
    private final int port;

    private ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
    private ConcurrentMap<Integer, Person> clients = new ConcurrentHashMap<>();

    public RemoteBank(int port) {
        this.port = port;
    }

    public Person createRemotePerson(final String firstName, final String lastName, final int passportId) throws RemoteException {
        Person person = getRemotePerson(passportId);
        if (person == null) {
            person = new RemotePerson(firstName, lastName, passportId, this);
            UnicastRemoteObject.exportObject(person, port);
            clients.put(passportId, person);
        }
        return person;
    }

    public Person getRemotePerson(final int passportId) {
        return clients.get(passportId);
    }

    public Person getLocalPerson(final int passportId) throws RemoteException {
        Person person = getRemotePerson(passportId);
        if (person != null) {
            person = new LocalPerson(person);
        }
        return person;
    }

    @Override
    public Account createAccount(final String id) throws RemoteException {
        System.out.println("Checking existing accounts...");
        Account account = getAccount(id);
        if (account != null) {
            System.out.println("Account with given ID exists");
        } else {
            account = new RemoteAccount(id);
            UnicastRemoteObject.exportObject(account, port);
            accounts.put(id, account);
            System.out.println("Account created successfully");
        }
        return account;
    }

    @Override
    public Account getAccount(final String id) throws RemoteException {
        return accounts.get(id);
    }
}
