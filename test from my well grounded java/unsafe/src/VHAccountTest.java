package ch15;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VHAccountTest {

    private VHAccount account;
    private VHAccount anotherAccount;

    @BeforeEach
    void setUp() {
        account = new VHAccount(100);
        anotherAccount = new VHAccount(50);
    }

    @Test
    void getBalance() {
        assertEquals(100, account.getBalance());
        assertEquals(50, anotherAccount.getBalance());
    }

    @Test
    void deposit() {
        account.deposit(50);
        assertEquals(150, account.getBalance());
    }

    @Test
    void withdraw() {
        assertTrue(account.withdraw(50));
        assertEquals(50, account.getBalance());

        assertFalse(account.withdraw(100));
        assertEquals(50, account.getBalance());
    }

    @Test
    void transferTo() {
        assertTrue(account.transferTo(anotherAccount, 50));
        assertEquals(50, account.getBalance());
        assertEquals(100, anotherAccount.getBalance());

        assertFalse(account.transferTo(anotherAccount, 100));
        assertEquals(50, account.getBalance());
        assertEquals(100, anotherAccount.getBalance());
    }

    @Test
    void concurrentDeposits() throws InterruptedException {
        int initialBalance = account.getBalance();
        int depositAmount = 10;
        int threadCount = 1000;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> account.deposit(depositAmount));
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(initialBalance + depositAmount * threadCount, account.getBalance());
    }

    @Test
    void concurrentWithdrawals() throws InterruptedException {
        int initialBalance = account.getBalance();
        int withdrawAmount = 1;
        int threadCount = 100;

        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> account.withdraw(withdrawAmount));
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(initialBalance - withdrawAmount * threadCount, account.getBalance());
    }

    @Test
    void concurrentTransfers() throws InterruptedException {
        int initialBalance = account.getBalance();
        int transferAmount = 1;
        int threadCount = 50;

        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> account.transferTo(anotherAccount, transferAmount));
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(initialBalance - transferAmount * threadCount, account.getBalance());
        assertEquals(50 + transferAmount * threadCount, anotherAccount.getBalance());
    }
}
