package ru.ifmo.rain.levashov.bank;

import java.io.Serializable;
import java.rmi.RemoteException;

public class LocalAccount extends AbstractAccount implements Serializable {

    public LocalAccount(String id) {
        super(id);
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public void addAmount(int amount) throws RemoteException {
        this.amount += amount;
    }
}
