package ru.ifmo.rain.levashov.bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Objects;

public class Client {

    private final static int REG_PORT = 6243;

    public static void main (String [] args) {
        if (args == null || args.length < 4 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Expected at least 4 not-null arguments");
            return;
        }
        try {
            String firstName = args[0];
            String lastName = args[1];
            int passportId = Integer.parseInt(args[2]);
            String subId = args[3];
            int moneyDiff = args.length > 4 ? Integer.parseInt(args[4]) : 0;
            String id = passportId + ":" + subId;
            Bank bank;

            try {
                bank = (Bank) Naming.lookup("//localhost:" + REG_PORT + "/bank");
            } catch (NotBoundException e) {
                System.err.println("Failed to find a bank: " + e.getMessage());
                return;
            } catch (MalformedURLException e) {
                System.err.println("Malformed URL: " + e.getMessage());
                return;
            }

            Person person = bank.createRemotePerson(firstName, lastName, passportId);
            Account account = person.getAccount(id);
            int money = (account != null ? account.getAmount() : 0);
            System.out.println("Money on account " + subId + ": " + money);
            account = person.createAccount(id);
            account.addAmount(moneyDiff);
            System.out.println("Money on account " + subId + " after adding: " + account.getAmount());

        } catch (RemoteException e) {
            System.err.println("Remote service error occured: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Third and fifth arguments should be numbers");
        }
    }
}
