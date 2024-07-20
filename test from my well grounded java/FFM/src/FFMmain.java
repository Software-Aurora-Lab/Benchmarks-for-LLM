package ch15;

public class FFMmain {
    public static void main(String[] args) {
        AtomicAccount acc1 = new AtomicAccount(100);
        AtomicAccount acc2 = new AtomicAccount(200);

        // now acc1 = 100, acc2 = 200
        System.out.println("Operate 1: Account 1 has " + acc1.getBalance());
        System.out.println("Operate 1: Account 2 has " + acc2.getBalance());
        System.out.println();

        acc1.deposit(1000);
        // now acc1 = 1100, acc2 = 200
        System.out.println("Operate 2: Account 1 has " + acc1.getBalance());
        System.out.println("Operate 2: Account 2 has " + acc2.getBalance());
        System.out.println();

        acc2.withdraw(100);
        // now acc1 = 1100, acc2 = 100
        System.out.println("Operate 3: Account 1 has " + acc1.getBalance());
        System.out.println("Operate 3: Account 2 has " + acc2.getBalance());
        System.out.println();

        acc1.transferTo(acc2, 500);
        // now acc1 = 600, acc2 = 600
        System.out.println("Operate 4: Account 1 has " + acc1.getBalance());
        System.out.println("Operate 4: Account 2 has " + acc2.getBalance());
        System.out.println();
    }
}
