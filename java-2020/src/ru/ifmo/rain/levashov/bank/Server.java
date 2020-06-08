package ru.ifmo.rain.levashov.bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class Server {

    private final static int BANK_PORT = 8888;
    private final static int REG_PORT = 6243;

    private static Bank bank;

    public static void main(String [] args) {
        System.out.println("Configuring server...");
        try {
            LocateRegistry.createRegistry(REG_PORT);
            bank = new RemoteBank(BANK_PORT);
            UnicastRemoteObject.exportObject(bank, BANK_PORT);
            Naming.rebind("//localhost:" + REG_PORT + "/bank", bank);
        } catch (RemoteException e) {
            System.err.println("Remote service error occured: " + e.getMessage());
            return;
        } catch (MalformedURLException e) {
            System.err.println("Malformed URL: " + e.getMessage());
            return;
        }
        System.out.println("Server successfully started");
    }

    public Bank getBank() {
        return bank;
    }
}
