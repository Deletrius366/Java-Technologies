package ru.ifmo.rain.levashov.bank;

public class RemoteAccount implements Account {
    private final String id;
    private int amount;

    public RemoteAccount(final String id) {
        this.id = id;
        amount = 0;
    }

    public String getId() {
        return id;
    }

    public synchronized int getAmount() {
        System.out.println("Getting amount of money for account " + id);
        return amount;
    }

    public synchronized void setAmount(final int amount) {
        System.out.println("Setting amount " + amount + " of money for account " + id);
        this.amount = amount;
    }

    @Override
    public synchronized void addAmount(int amount) {
        System.out.println("Adding amount " + amount + " of money for account " + id);
        this.amount += amount;
    }
}
