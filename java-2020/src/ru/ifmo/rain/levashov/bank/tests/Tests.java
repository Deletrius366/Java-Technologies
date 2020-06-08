package ru.ifmo.rain.levashov.bank.tests;

import org.junit.*;
import ru.ifmo.rain.levashov.bank.*;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class Tests extends Assert {

    private final static int BANK_PORT = 8888;
    private final static int REG_PORT = 6243;
    private static final String ACCOUNT_NAME = "gosha";
    private static final int ACCOUNT_MONEY = ACCOUNT_NAME.hashCode();
    private static final int ACCOUNT_DIFF = 100;

    private static Bank bank;

    @Before
    public void init() {
        try {
            bank = new RemoteBank(BANK_PORT);
            UnicastRemoteObject.exportObject(bank, BANK_PORT);
            Naming.rebind("//localhost:" + REG_PORT + "/bank", bank);
        } catch (final RemoteException e) {
            System.err.println("Remote service error occurred: " + e.getMessage());
        } catch (final MalformedURLException e) {
            System.err.println("Malformed URL: " + e.getMessage());
        }
    }

    @BeforeClass
    public static void initClass() throws RemoteException {
        LocateRegistry.createRegistry(REG_PORT);
    }

    @After
    public void close() throws NoSuchObjectException {
        UnicastRemoteObject.unexportObject(bank, true);
    }

    private void checkAccounts(final String id, final int amount, final Account account) throws RemoteException {
        assertEquals(id, account.getId());
        assertEquals(amount, account.getAmount());
    }

    private void checkAmount(final Account account) throws RemoteException {
        account.setAmount(ACCOUNT_DIFF);
        checkAccounts(ACCOUNT_NAME, ACCOUNT_DIFF, account);
        account.addAmount(ACCOUNT_MONEY);
        checkAccounts(ACCOUNT_NAME, ACCOUNT_DIFF + ACCOUNT_MONEY, account);
    }

    @Test
    public void test_0() throws RemoteException {
        checkAccounts(ACCOUNT_NAME, 0, new RemoteAccount(ACCOUNT_NAME));
        checkAccounts(ACCOUNT_NAME, 0, new LocalAccount(ACCOUNT_NAME));
    }

    @Test
    public void test_1() throws RemoteException {
        Account account = new RemoteAccount(ACCOUNT_NAME);
        checkAmount(account);

        account = new LocalAccount(ACCOUNT_NAME);
        checkAmount(account);
    }

    @Test
    public void test_2() throws RemoteException {
        final Account account = bank.createAccount(ACCOUNT_NAME);
        checkAccounts(ACCOUNT_NAME, 0, account);
        assertEquals(account, bank.createAccount(ACCOUNT_NAME));
        assertEquals(account, bank.getAccount(ACCOUNT_NAME));
    }

    @Test
    public void test_3() throws RemoteException {
        final Account account = bank.createAccount(ACCOUNT_NAME);
        account.addAmount(ACCOUNT_DIFF);
        assertEquals(account.getAmount(), bank.getAccount(ACCOUNT_NAME).getAmount());
    }

    @Test
    public void test_4() throws RemoteException {
        assertNull(bank.getAccount(ACCOUNT_NAME));
    }
}
