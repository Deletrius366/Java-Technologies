package ru.ifmo.rain.levashov.bank;

import java.io.Serializable;
import java.rmi.RemoteException;

public class LocalPerson extends AbstractPerson implements Serializable {

    public LocalPerson(String firstName, String lastName, int passportId) {
        super(firstName, lastName, passportId);
    }

    LocalPerson(Person person) throws RemoteException {
        this.firstName = person.getFirstName();
        this.lastName = person.getLastName();
        this.passportId = person.getPassportId();
        this.accounts = person.getAccounts();
    }

    public Account createAccount(String id) {return null; }

    @Override
    public Account getAccount(String id) throws RemoteException {
        return null;
    }
}
