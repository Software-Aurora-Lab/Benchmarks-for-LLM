package ch15;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AtomicAccountTest {

    private AtomicAccount account;
    private AtomicAccount anotherAccount;

    @BeforeEach
    void setUp() {
        account = new AtomicAccount(100);
        anotherAccount = new AtomicAccount(50);
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
}
