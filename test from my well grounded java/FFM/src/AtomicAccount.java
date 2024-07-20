package ch15;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class AtomicAccount implements Account {

    private volatile int balance = 0;
    private static final ValueLayout.OfInt LAYOUT = ValueLayout.JAVA_INT;
    private final Arena arena;
    private final MemorySegment nativeBalance;

    public AtomicAccount(int openingBalance) {
        this.arena = Arena.ofConfined();
        this.nativeBalance = arena.allocate(LAYOUT);
        this.nativeBalance.set(LAYOUT, 0, openingBalance);
        this.balance = openingBalance;
    }

    @Override
    public int getBalance() {
        return nativeBalance.get(LAYOUT, 0);
    }

    @Override
    public void deposit(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        synchronized (this) {
            int currentBalance = nativeBalance.get(LAYOUT, 0);
            int newBalance = currentBalance + amount;
            nativeBalance.set(LAYOUT, 0, newBalance);
            balance = newBalance;
        }
    }

    @Override
    public boolean withdraw(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        synchronized (this) {
            int currentBalance = nativeBalance.get(LAYOUT, 0);
            int newBalance = currentBalance - amount;
            if (newBalance >= 0) {
                nativeBalance.set(LAYOUT, 0, newBalance);
                balance = newBalance;
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean transferTo(Account other, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        synchronized (this) {
            if (withdraw(amount)) {
                other.deposit(amount);
                return true;
            }
            return false;
        }
    }

    // Clean up resources when the account is no longer needed
    public void close() {
        arena.close();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
