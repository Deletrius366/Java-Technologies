package ru.ifmo.rain.levashov.bank;

public abstract class AbstractAccount implements Account {
    protected String id;
    protected int amount = 0;

    AbstractAccount(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
