# ATM Transaction Simulator

A robust, console-based Java application that simulates the core logic of a real-world Automated Teller Machine (ATM). This project focuses on strict security protocols, dynamic cash inventory management, and mathematically accurate note dispensing.

## 📌 Project Overview
This simulator goes beyond a simple digital ledger. It enforces the physical constraints of a real ATM, meaning transactions are only approved if the machine has the exact physical note denominations required to fulfill the request. The project was built adhering to specific foundational programming constraints (procedural Java using arrays, loops, and recursive backtracking) before introducing Object-Oriented paradigms.

## ✨ Key Features
*   **Secure Authentication:** Validates users via Card Number and PIN.
*   **Fraud Prevention (Lockout):** Automatically blocks a card after 3 consecutive failed PIN attempts.
*   **Dynamic Note Dispensing (Greedy + Fallback):** Calculates the exact mix of notes (e.g., 1000, 500, 100) to dispense based on current machine inventory. Uses a recursive fallback algorithm if the greedy approach fails due to depleted denominations.
*   **Physical & Account Limits:** Validates withdrawals against user account balances, maximum daily withdrawal limits, and the physical cash currently available inside the ATM cassettes.
*   **Mini-Statement Generation:** Maintains and displays a rolling window of the 5 most recent transactions using array manipulation.
*   **Interactive Retry:** Prompts the user to adjust their requested amount if the machine cannot dispense the exact change.

## 🛠️ Technical Implementation (Week 1–6 Constraints)
This milestone of the project is intentionally built using foundational Java concepts without relying on custom Objects/Classes or File I/O:
*   **State Management:** Parallel arrays track card numbers, PINs, balances, daily limits, and block flags.
*   **Control Flow:** Switch statements and while-loops drive the interactive session menu.
*   **Algorithmic Logic:** A recursive backtracking algorithm handles the complex change-making constraints.

## 🚀 Getting Started

### Prerequisites
*   Java Development Kit (JDK) 8 or higher installed on your machine.

### Compilation & Execution
1. Clone the repository or download the `ATMSimulator.java` file.
2. Open your terminal or command prompt.
3. Navigate to the directory containing the file.
4. Compile the code:
   ```bash
   javac ATMSimulator.java
