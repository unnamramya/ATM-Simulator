import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

// --- CO5 (Weeks 9 & 10): Record for immutable transaction, File I/O persistence ---
record Transaction(String cardNumber, String type, double amount, LocalDateTime timestamp, String result) {
    @Override
    public String toString() {
        return timestamp + "," + cardNumber + "," + type + "," + amount + "," + result;
    }
}

// --- CO4 (Weeks 7 & 8): Custom exceptions for domain-specific error handling ---
class InvalidPinException extends Exception { public InvalidPinException(String msg) { super(msg); } }
class CardBlockedException extends Exception { public CardBlockedException(String msg) { super(msg); } }
class InsufficientCashException extends Exception { public InsufficientCashException(String msg) { super(msg); } }
class CannotDispenseException extends Exception { public CannotDispenseException(String msg) { super(msg); } }

// --- CO4 (Weeks 7 & 8): Dispenser strategy (interface) for polymorphic behavior ---
interface DispenserStrategy {
    Map<Integer, Integer> calculate(int amount, Map<Integer, Integer> inventory) throws CannotDispenseException;
}

// --- CO4 (Weeks 7 & 8): Polymorphic transaction handling implementing the strategy ---
class GreedyFallbackDispenser implements DispenserStrategy {
    @Override
    public Map<Integer, Integer> calculate(int amount, Map<Integer, Integer> inventory) throws CannotDispenseException {
        // --- CO3 (Weeks 5 & 6): Arrays for denominations and note counts ---
        int[] denoms = inventory.keySet().stream().sorted(Collections.reverseOrder()).mapToInt(Integer::intValue).toArray();
        int[] counts = new int[denoms.length];
        for(int i=0; i<denoms.length; i++) counts[i] = inventory.get(denoms[i]);
        
        int[] plan = new int[denoms.length];
        
        // Triggering CO3 Recursion
        if (calculateDispenseRecursive(amount, 0, plan, denoms, counts)) {
            Map<Integer, Integer> result = new LinkedHashMap<>();
            for (int i = 0; i < denoms.length; i++) {
                if (plan[i] > 0) result.put(denoms[i], plan[i]);
            }
            return result;
        }
        throw new CannotDispenseException("Cannot dispense exact amount");
    }

    // --- CO3 (Weeks 5 & 6): Methods per operation; recursion inside the change-making fallback ---
    private boolean calculateDispenseRecursive(int remaining, int denomIndex, int[] plan, int[] denoms, int[] counts) {
        if (remaining == 0) return true; // Base case: success
        if (denomIndex >= denoms.length) return false; // Base case: failed

        int denom = denoms[denomIndex];
        int maxNotes = Math.min(remaining / denom, counts[denomIndex]);

        // --- CO2 (Weeks 3 & 4): Loops to build the note mix ---
        for (int i = maxNotes; i >= 0; i--) {
            plan[denomIndex] = i;
            if (calculateDispenseRecursive(remaining - (i * denom), denomIndex + 1, plan, denoms, counts)) {
                return true; 
            }
        }
        plan[denomIndex] = 0; // Backtrack
        return false; 
    }
}

// --- CO4 (Weeks 7 & 8): Encapsulated Account class ---
class Account {
    // --- CO1 (Weeks 1 & 2): double amounts ---
    private double balance;
    private double dailyWithdrawn;
    
    // --- CO6 (Weeks 11 & 12): Bounded list of recent transactions ---
    private LinkedList<Transaction> recentTransactions; 

    public Account(double balance, double dailyWithdrawn) {
        this.balance = balance;
        this.dailyWithdrawn = dailyWithdrawn;
        this.recentTransactions = new LinkedList<>();
    }

    public double getBalance() { return balance; }
    public double getDailyWithdrawn() { return dailyWithdrawn; }
    public List<Transaction> getRecentTransactions() { return recentTransactions; }

    public void withdraw(double amount) {
        balance -= amount;
        dailyWithdrawn += amount;
    }
    public void deposit(double amount) { balance += amount; }
    
    public void addTransaction(Transaction t) {
        recentTransactions.addFirst(t);
        if (recentTransactions.size() > 5) recentTransactions.removeLast(); // Bounded to 5
    }
}

// --- CO4 (Weeks 7 & 8): Encapsulated Card class ---
class Card {
    private String number;
    // --- CO1 (Weeks 1 & 2): int PINs, boolean blocked flags ---
    private int pinHash; 
    private boolean blocked;
    private int failedAttempts;
    private Account account;

    public Card(String number, int pinHash, boolean blocked, int failedAttempts, Account account) {
        this.number = number;
        this.pinHash = pinHash;
        this.blocked = blocked;
        this.failedAttempts = failedAttempts;
        this.account = account;
    }

    public String getNumber() { return number; }
    public int getPinHash() { return pinHash; }
    public boolean isBlocked() { return blocked; }
    public int getFailedAttempts() { return failedAttempts; }
    public Account getAccount() { return account; }

    public void recordFailedAttempt() {
        failedAttempts++;
        if (failedAttempts >= 3) blocked = true;
    }
    public void resetFailedAttempts() { failedAttempts = 0; }
}

// --- CO4 (Weeks 7 & 8): ATM composed of cassettes and current session state ---
class ATM {
    // --- CO6 (Weeks 11 & 12): Map card to account ---
    private Map<String, Card> cardDatabase = new HashMap<>();
    private Map<Integer, Integer> cassettes = new TreeMap<>(Collections.reverseOrder());
    private DispenserStrategy dispenser = new GreedyFallbackDispenser();
    private double dailyLimit;

    public ATM(double dailyLimit) { this.dailyLimit = dailyLimit; }

    public void addCard(Card card) { cardDatabase.put(card.getNumber(), card); }
    public void setCassette(int denom, int count) { cassettes.put(denom, count); }
    public double getDailyLimit() { return dailyLimit; }
    public Collection<Card> getAllCards() { return cardDatabase.values(); }
    public Map<Integer, Integer> getCassettes() { return cassettes; }

    // --- CO3 (Weeks 5 & 6): Methods per operation (Authenticate) ---
    public Card authenticate(String cardNumber, int pin) throws InvalidPinException, CardBlockedException {
        Card card = cardDatabase.get(cardNumber);
        
        // --- CO2 (Weeks 3 & 4): Branch on auth result ---
        if (card == null) throw new InvalidPinException("Card not recognized.");
        if (card.isBlocked()) throw new CardBlockedException("Card is BLOCKED.");
        
        if (card.getPinHash() != pin) {
            card.recordFailedAttempt();
            if (card.isBlocked()) throw new CardBlockedException("Maximum attempts reached. Card BLOCKED.");
            throw new InvalidPinException("Incorrect PIN.");
        }
        card.resetFailedAttempts();
        return card;
    }

    public void deposit(Card card, double amount) {
        card.getAccount().deposit(amount);
        Transaction t = new Transaction(card.getNumber(), "Deposit", amount, LocalDateTime.now(), "SUCCESS");
        card.getAccount().addTransaction(t);
        logTransaction(t);
    }

    public String withdraw(Card card, double amount) throws CannotDispenseException, InsufficientCashException {
        Account acc = card.getAccount();
        
        // --- CO2 (Weeks 3 & 4): Limits and availability guards ---
        if (amount <= 0 || amount % 100 != 0) throw new InsufficientCashException("Must be positive multiple of 100.");
        if (amount > acc.getBalance()) throw new InsufficientCashException("Insufficient account balance.");
        if (acc.getDailyWithdrawn() + amount > dailyLimit) throw new InsufficientCashException("Exceeds daily limit.");

        // CO4 Strategy invocation
        Map<Integer, Integer> plan = dispenser.calculate((int) amount, cassettes);
        
        acc.withdraw(amount);
        plan.forEach((denom, count) -> cassettes.put(denom, cassettes.get(denom) - count));
        
        Transaction t = new Transaction(card.getNumber(), "Withdrawal", amount, LocalDateTime.now(), "SUCCESS");
        acc.addTransaction(t);
        logTransaction(t);

        return plan.entrySet().stream()
                .map(e -> e.getValue() + "x" + e.getKey())
                .collect(Collectors.joining(", "));
    }

    // --- CO5 (Weeks 9 & 10): Persist the session log ---
    public void logTransaction(Transaction t) {
        try (PrintWriter out = new PrintWriter(new FileWriter("session_log.txt", true))) {
            out.println(t.toString());
        } catch (IOException e) {
            System.out.println("Could not write to session log.");
        }
    }
}

public class ATMSimulator {

    private static final String ACCOUNTS_FILE = "accounts.txt";
    private static final String CASH_FILE = "atm_cash.txt";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ATM atm = new ATM(10000.0);
        initializeOrLoadSystem(atm);

        // --- CO1 (Weeks 1 & 2): Menu loop for the ATM session ---
        while (true) {
            System.out.println("\n=== ATM ===");
            System.out.print("Card: ");
            String cardNum = scanner.next();
            
            System.out.print("PIN: ");
            int pin = scanner.nextInt();

            try {
                Card card = atm.authenticate(cardNum, pin);
                System.out.println("-> authenticated\n");
                sessionMenu(scanner, card, atm);
            } catch (InvalidPinException | CardBlockedException e) {
                System.out.println("Error: " + e.getMessage());
                atm.logTransaction(new Transaction(cardNum, "Auth_Failed", 0.0, LocalDateTime.now(), "FAILED: " + e.getMessage()));
                saveSystemState(atm); 
            }
        }
    }

    // --- CO1 (Weeks 1 & 2): Console ATM menu ---
    static void sessionMenu(Scanner scanner, Card card, ATM atm) {
        boolean active = true;
        List<Transaction> currentSessionLog = new ArrayList<>(); 
        long lastActionTime = System.currentTimeMillis();

        while (active) {
            // Idle timeout guard (Simulated: checks time since last loop iteration)
            if (System.currentTimeMillis() - lastActionTime > 60000) {
                System.out.println("\nSession timed out due to inactivity. Ejecting card...");
                break;
            }

            System.out.println("1) Balance  2) Withdraw  3) Deposit  4) Mini-statement  5) Exit");
            System.out.print("Choice: ");
            int choice = scanner.nextInt();
            lastActionTime = System.currentTimeMillis();

            try {
                // --- CO2 (Weeks 3 & 4): Branch on operations ---
                switch (choice) {
                    case 1:
                        System.out.printf("Balance: %.2f. Daily remaining: %.2f.%n", 
                                          card.getAccount().getBalance(), 
                                          (atm.getDailyLimit() - card.getAccount().getDailyWithdrawn()));
                        break;
                    case 2:
                        handleWithdrawal(scanner, card, atm, currentSessionLog);
                        break;
                    case 3:
                        System.out.print("Amount: ");
                        double dAmount = scanner.nextDouble();
                        if (dAmount <= 0) {
                            System.out.println("Error: Deposit amount must be positive.");
                        } else {
                            atm.deposit(card, dAmount);
                            System.out.printf("Successfully deposited. New Balance: %.2f%n", card.getAccount().getBalance());
                            currentSessionLog.add(new Transaction(card.getNumber(), "Deposit", dAmount, LocalDateTime.now(), "SUCCESS"));
                        }
                        break;
                    case 4:
                        printMiniStatement(card);
                        break;
                    case 5:
                        System.out.println("Session ended. Ejecting card...\n");
                        runW12StreamAnalytics(currentSessionLog, card.getNumber()); 
                        active = false;
                        break;
                    default:
                        System.out.println("Invalid choice.");
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            // --- CO5 (Weeks 9 & 10): Persist accounts and machine cash ---
            saveSystemState(atm); 
        }
    }

    private static void handleWithdrawal(Scanner scanner, Card card, ATM atm, List<Transaction> sessionLog) {
        System.out.print("Amount: ");
        double amount = scanner.nextDouble();
        
        while (true) {
            try {
                String dispenseMsg = atm.withdraw(card, amount);
                System.out.println("-> dispense " + dispenseMsg);
                System.out.printf("Balance %.2f. Daily remaining %.2f.%n", 
                                  card.getAccount().getBalance(), 
                                  (atm.getDailyLimit() - card.getAccount().getDailyWithdrawn()));
                
                sessionLog.add(new Transaction(card.getNumber(), "Withdrawal", amount, LocalDateTime.now(), "SUCCESS"));
                
                // Track low cash warnings 
                atm.getCassettes().forEach((denom, count) -> {
                    if (count < 10) System.out.println("[Warning] Low cash for denomination: " + denom);
                });
                break; 
            } catch (CannotDispenseException e) {
                System.out.print("  Cannot dispense " + (int)amount + " with available notes -> try a multiple of 100? (y/n): ");
                if (scanner.next().equalsIgnoreCase("y")) {
                    System.out.print("  Amount: ");
                    amount = scanner.nextDouble();
                } else {
                    System.out.println("Transaction cancelled.");
                    break;
                }
            } catch (InsufficientCashException e) {
                System.out.println("Error: " + e.getMessage());
                sessionLog.add(new Transaction(card.getNumber(), "Withdrawal", amount, LocalDateTime.now(), "FAILED: NSF/Limit"));
                break;
            }
        }
    }

    // --- CO2 (Weeks 3 & 4): Loop to build the mini-statement ---
    static void printMiniStatement(Card card) {
        System.out.println("--- Mini-Statement ---");
        List<Transaction> history = card.getAccount().getRecentTransactions();
        if (history.isEmpty()) {
            System.out.println("No recent transactions.");
        } else {
            // --- CO6 (Weeks 11 & 12): Comparator by time ---
            Comparator<Transaction> byTime = Comparator.comparing(Transaction::timestamp).reversed();
            history.stream().sorted(byTime).forEach(t -> {
                // --- CO5 (Weeks 9 & 10): Parse card input, mask and format receipts ---
                String maskedCard = t.cardNumber().substring(0, 4) + "********" + t.cardNumber().substring(12);
                System.out.printf("[%s] %s %s: %.2f (%s)%n", 
                                  t.timestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")), 
                                  maskedCard, t.type(), t.amount(), t.result());
            });
        }
        System.out.println("----------------------");
    }

    // --- CO6 (Weeks 11 & 12): Streams to total dispensing, filter, and group ---
    static void runW12StreamAnalytics(List<Transaction> sessionLog, String currentCard) {
        System.out.println("=== End of Session Stream Analytics ===");
        
        double totalDispensed = sessionLog.stream()
            .filter(t -> t.type().equals("Withdrawal") && t.result().equals("SUCCESS"))
            .mapToDouble(Transaction::amount)
            .sum();
        System.out.println("Total Dispensed this session: " + totalDispensed);

        List<Transaction> depositsOnly = sessionLog.stream()
            .filter(t -> t.cardNumber().equals(currentCard) && t.type().equals("Deposit"))
            .collect(Collectors.toList());
        System.out.println("Deposits made this session: " + depositsOnly.size());

        Map<String, List<Transaction>> groupedByType = sessionLog.stream()
            .collect(Collectors.groupingBy(Transaction::type));
        
        System.out.print("Transaction breakdown by type: ");
        groupedByType.forEach((type, list) -> System.out.print(type + "=" + list.size() + " "));
        System.out.println("\n=======================================\n");
    }

    // --- CO5 (Weeks 9 & 10): Persist accounts, machine cash; reload machine state ---
    static void initializeOrLoadSystem(ATM atm) {
        try {
            if (!Files.exists(Paths.get(ACCOUNTS_FILE)) || !Files.exists(Paths.get(CASH_FILE))) {
                atm.addCard(new Card("4021000011112222", 1234, false, 0, new Account(15000.0, 0.0)));
                atm.addCard(new Card("4021333344445555", 5678, false, 0, new Account(5000.0, 0.0)));
                atm.addCard(new Card("4021666677778888", 9012, false, 0, new Account(25000.0, 0.0)));
                
                atm.setCassette(1000, 10);
                atm.setCassette(500, 20);
                atm.setCassette(100, 50);
                saveSystemState(atm);
            } else {
                List<String> accLines = Files.readAllLines(Paths.get(ACCOUNTS_FILE));
                for (String line : accLines) {
                    String[] parts = line.split(",");
                    Account acc = new Account(Double.parseDouble(parts[4]), Double.parseDouble(parts[5]));
                    Card c = new Card(parts[0], Integer.parseInt(parts[1]), Boolean.parseBoolean(parts[2]), Integer.parseInt(parts[3]), acc);
                    atm.addCard(c);
                }
                List<String> cashLines = Files.readAllLines(Paths.get(CASH_FILE));
                for (String line : cashLines) {
                    String[] parts = line.split(",");
                    atm.setCassette(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                }
            }
        } catch (IOException e) {
            System.out.println("System boot error: " + e.getMessage());
        }
    }

    static void saveSystemState(ATM atm) {
        try (PrintWriter accWriter = new PrintWriter(new FileWriter(ACCOUNTS_FILE));
             PrintWriter cashWriter = new PrintWriter(new FileWriter(CASH_FILE))) {
            
            for (Card c : atm.getAllCards()) {
                accWriter.printf("%s,%d,%b,%d,%.2f,%.2f%n", 
                    c.getNumber(), c.getPinHash(), c.isBlocked(), c.getFailedAttempts(), 
                    c.getAccount().getBalance(), c.getAccount().getDailyWithdrawn());
            }
            
            for (Map.Entry<Integer, Integer> entry : atm.getCassettes().entrySet()) {
                cashWriter.printf("%d,%d%n", entry.getKey(), entry.getValue());
            }
        } catch (IOException e) {
            System.out.println("State save error: " + e.getMessage());
        }
    }
}
