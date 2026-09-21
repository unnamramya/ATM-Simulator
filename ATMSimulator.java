import java.util.Scanner;

public class ATMSimulator {

    // --- W6: Arrays for Data Storage (Pre-OOP approach) ---
    // User Database (Parallel Arrays)
    static final int MAX_USERS = 3;
    static String[] cardNumbers = {"4021000011112222", "4021333344445555", "4021666677778888"};
    
    // W2: int PINs, double amounts, boolean blocked flags
    static int[] pins = {1234, 5678, 9012};
    static double[] balances = {15000.0, 5000.0, 25000.0};
    static boolean[] isBlocked = {false, false, false};
    static int[] failedAttempts = {0, 0, 0};
    
    // W6: Per-day withdrawal tracking array
    static double[] dailyWithdrawn = {0.0, 0.0, 0.0};
    static final double DAILY_LIMIT = 10000.0;

    // ATM Machine Cash Cassettes State
    // W6: Denominations array
    static int[] denominations = {1000, 500, 100};
    static int[] noteCounts = {10, 20, 50}; // Initial stock: 10x1000, 20x500, 50x100

    // Mini-statement tracking (Max 5 recent transactions per user)
    static String[][] transactionHistory = new String[MAX_USERS][5];
    static int[] historyCounts = new int[MAX_USERS];

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // W1: Menu loop for the ATM session
        while (true) {
            System.out.println("\n=== ATM ===");
            System.out.print("Card: ");
            String card = scanner.next();
            
            System.out.print("PIN: ");
            int pin = scanner.nextInt();

            // W5: Method per operation
            int userIdx = authenticate(card, pin);

            // W3: Branch on auth result
            if (userIdx != -1) {
                System.out.println("-> authenticated\n");
                sessionMenu(scanner, userIdx);
            }
        }
    }

    // W5: Method for authentication
    static int authenticate(String card, int pin) {
        int userIdx = -1;
        for (int i = 0; i < cardNumbers.length; i++) {
            if (cardNumbers[i].equals(card)) {
                userIdx = i;
                break;
            }
        }

        if (userIdx == -1) {
            System.out.println("Error: Card not recognized.");
            return -1;
        }

        if (isBlocked[userIdx]) {
            System.out.println("Error: Card is blocked due to too many failed attempts.");
            return -1;
        }

        if (pins[userIdx] != pin) {
            failedAttempts[userIdx]++;
            System.out.println("Error: Incorrect PIN.");
            // Edge case: Block after 3 failed attempts
            if (failedAttempts[userIdx] >= 3) {
                isBlocked[userIdx] = true;
                System.out.println("Warning: Card is now BLOCKED.");
            }
            return -1;
        }

        failedAttempts[userIdx] = 0; // Reset on success
        return userIdx;
    }

    // W1 & W3: Console ATM menu and operational branching
    static void sessionMenu(Scanner scanner, int userIdx) {
        boolean active = true;
        while (active) {
            System.out.println("1) Balance  2) Withdraw  3) Deposit  4) Mini-statement  5) Exit");
            System.out.print("Choice: ");
            int choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    checkBalance(userIdx);
                    break;
                case 2:
                    System.out.print("Amount: ");
                    double wAmount = scanner.nextDouble();
                    withdraw(userIdx, wAmount);
                    break;
                case 3:
                    System.out.print("Amount: ");
                    double dAmount = scanner.nextDouble();
                    deposit(userIdx, dAmount);
                    break;
                case 4:
                    printMiniStatement(userIdx);
                    break;
                case 5:
                    System.out.println("Session ended. Ejecting card...\n");
                    active = false;
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
            System.out.println();
        }
    }

    static void checkBalance(int userIdx) {
        System.out.printf("Balance: %.2f. Daily remaining: %.2f.%n", 
                          balances[userIdx], (DAILY_LIMIT - dailyWithdrawn[userIdx]));
    }

    static void deposit(int userIdx, double amount) {
        // Edge case validation
        if (amount <= 0) {
            System.out.println("Error: Deposit amount must be positive.");
            return;
        }
        balances[userIdx] += amount;
        addTransaction(userIdx, String.format("Deposit: +%.2f", amount));
        System.out.printf("Successfully deposited. New Balance: %.2f%n", balances[userIdx]);
    }

    static void withdraw(int userIdx, double amount) {
        // Edge cases & Limits validation (W3)
        if (amount <= 0 || amount % 100 != 0) {
            System.out.println("Error: Amount must be a positive multiple of 100.");
            return;
        }
        if (amount > balances[userIdx]) {
            System.out.println("Error: Insufficient account balance.");
            return;
        }
        if (dailyWithdrawn[userIdx] + amount > DAILY_LIMIT) {
            System.out.println("Error: Amount exceeds daily withdrawal limit of " + DAILY_LIMIT);
            return;
        }

        // Setup dispensing plan
        int[] dispensePlan = new int[denominations.length];

        // W5: Trigger recursive note dispensing logic
        if (calculateDispense((int) amount, 0, dispensePlan)) {
            // Commit transaction: Deduct notes, update balance and limits
            for (int i = 0; i < denominations.length; i++) {
                noteCounts[i] -= dispensePlan[i];
            }
            balances[userIdx] -= amount;
            dailyWithdrawn[userIdx] += amount;

            // W4: Loop to build the dispensed note mix output
            System.out.print("Dispensing: ");
            for (int i = 0; i < denominations.length; i++) {
                if (dispensePlan[i] > 0) {
                    System.out.print(dispensePlan[i] + "x" + denominations[i] + " ");
                }
            }
            System.out.println();
            
            addTransaction(userIdx, String.format("Withdrawal: -%.2f", amount));
            checkBalance(userIdx);
        } else {
            // Edge Case: Amount cannot be made from available notes
            System.out.println("Cannot dispense " + (int)amount + " with available notes -> try a multiple of a different denomination.");
        }
    }

    /**
     * W5: Recursion inside the change-making fallback.
     * Attempts a greedy approach but recursively backtracks if the initial greedy choice 
     * fails to provide an exact match using the available machine inventory.
     */
    static boolean calculateDispense(int remaining, int denomIndex, int[] plan) {
        // Base cases
        if (remaining == 0) return true; // Successfully matched amount
        if (denomIndex >= denominations.length) return false; // Ran out of denominations

        int denom = denominations[denomIndex];
        // Maximum notes we can theoretically take of this denomination
        int maxNotes = Math.min(remaining / denom, noteCounts[denomIndex]);

        // Iterate downwards to allow backtracking fallback
        for (int i = maxNotes; i >= 0; i--) {
            plan[denomIndex] = i;
            int newRemaining = remaining - (i * denom);
            
            // Recurse to next denomination
            if (calculateDispense(newRemaining, denomIndex + 1, plan)) {
                return true; 
            }
        }
        // Backtrack
        plan[denomIndex] = 0; 
        return false; 
    }

    // W4: Loop to construct and print the mini-statement
    static void printMiniStatement(int userIdx) {
        System.out.println("--- Mini-Statement ---");
        int count = historyCounts[userIdx];
        if (count == 0) {
            System.out.println("No recent transactions.");
            return;
        }
        for (int i = 0; i < count; i++) {
            System.out.println((i + 1) + ". " + transactionHistory[userIdx][i]);
        }
        System.out.println("----------------------");
    }

    // Utility to maintain a rolling window of recent transactions
    static void addTransaction(int userIdx, String record) {
        int capacity = transactionHistory[userIdx].length;
        int currentCount = historyCounts[userIdx];

        if (currentCount < capacity) {
            transactionHistory[userIdx][currentCount] = record;
            historyCounts[userIdx]++;
        } else {
            // Shift array to drop the oldest transaction and append the newest
            for (int i = 1; i < capacity; i++) {
                transactionHistory[userIdx][i - 1] = transactionHistory[userIdx][i];
            }
            transactionHistory[userIdx][capacity - 1] = record;
        }
    }
}