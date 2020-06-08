package ru.ifmo.rain.levashov.bank;

import java.rmi.RemoteException;

public class RemotePerson extends AbstractPerson {

    private final Bank bank;

    RemotePerson(String firstName, String lastName, int passportId, Bank bank) {
        super(firstName, lastName, passportId);
        this.bank = bank;
    }

    @Override
    public Account createAccount(String id) throws RemoteException {
        Account account = getAccount(id);
        if (account == null) {
            account = bank.createAccount(id);
            accounts.put(id, account);
        }
        return account;
    }

    @Override
    public Account getAccount(String id) throws RemoteException {
        return getAccounts().get(id);
    }
}
