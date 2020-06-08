package ru.ifmo.rain.levashov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentMap;

public interface Person extends Remote {
    Account createAccount(String id) throws RemoteException;

    Account getAccount(String id) throws RemoteException;

    String getFirstName() throws RemoteException;

    String getLastName() throws RemoteException;

    int getPassportId() throws RemoteException;

    ConcurrentMap<String, Account> getAccounts() throws RemoteException;
}
