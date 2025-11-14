import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Simple console-based Expense Tracker in Java.
 *
 * Features:
 *  - Add expenses with category, date and amount
 *  - List all expenses (sorted by date)
 *  - Show summary by category and by month
 *  - Persist data in a CSV file between runs
 *
 * This program demonstrates:
 *  - Object-Oriented Design (Expense class + Category enum)
 *  - Collections (List, Map)
 *  - File I/O with java.nio
 *  - Basic input validation and exception handling
 */
public class ExpenseTracker {

    private static final Scanner SCANNER = new Scanner(System.in);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Path DATA_FILE = Paths.get("expenses.csv");

    private final List<Expense> expenses = new ArrayList<>();
    private int nextId = 1;

    public static void main(String[] args) {
        ExpenseTracker tracker = new ExpenseTracker();
        tracker.loadFromFile();
        tracker.runMenu();
        tracker.saveToFile();
        System.out.println("Goodbye!");
    }

    private void runMenu() {
        while (true) {
            System.out.println("\n==== Expense Tracker ====");
            System.out.println("1. Add expense");
            System.out.println("2. List all expenses");
            System.out.println("3. Show summary by category");
            System.out.println("4. Show summary by month");
            System.out.println("5. Delete an expense");
            System.out.println("0. Exit");
            System.out.print("Choose an option: ");

            String choice = SCANNER.nextLine().trim();
            switch (choice) {
                case "1":
                    addExpense();
                    break;
                case "2":
                    listExpenses();
                    break;
                case "3":
                    showSummaryByCategory();
                    break;
                case "4":
                    showSummaryByMonth();
                    break;
                case "5":
                    deleteExpense();
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
    }

    private void addExpense() {
        System.out.println("\n-- Add Expense --");

        System.out.print("Description: ");
        String description = SCANNER.nextLine().trim();

        BigDecimal amount = readAmount();
        LocalDate date = readDate();
        Category category = readCategory();

        Expense expense = new Expense(nextId++, description, amount, date, category);
        expenses.add(expense);
        System.out.println("Expense added: " + expense);
    }

    private void listExpenses() {
        System.out.println("\n-- All Expenses --");
        if (expenses.isEmpty()) {
            System.out.println("No expenses recorded.");
            return;
        }

        expenses.sort(Comparator.comparing(Expense::date));
        for (Expense e : expenses) {
            System.out.println(e);
        }
    }

    private void showSummaryByCategory() {
        System.out.println("\n-- Summary by Category --");
        if (expenses.isEmpty()) {
            System.out.println("No expenses recorded.");
            return;
        }

        Map<Category, BigDecimal> totals = new HashMap<>();
        for (Expense e : expenses) {
            totals.put(e.category(),
                    totals.getOrDefault(e.category(), BigDecimal.ZERO).add(e.amount()));
        }

        for (Category c : Category.values()) {
            BigDecimal total = totals.getOrDefault(c, BigDecimal.ZERO);
            System.out.println(c + ": " + total);
        }
    }

    private void showSummaryByMonth() {
        System.out.println("\n-- Summary by Month (YYYY-MM) --");
        if (expenses.isEmpty()) {
            System.out.println("No expenses recorded.");
            return;
        }

        Map<String, BigDecimal> totals = new HashMap<>();
        for (Expense e : expenses) {
            String monthKey = e.date().getYear() + "-" +
                    String.format("%02d", e.date().getMonthValue());
            totals.put(monthKey,
                    totals.getOrDefault(monthKey, BigDecimal.ZERO).add(e.amount()));
        }

        totals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry ->
                        System.out.println(entry.getKey() + ": " + entry.getValue()));
    }

    private void deleteExpense() {
        System.out.println("\n-- Delete Expense --");
        if (expenses.isEmpty()) {
            System.out.println("No expenses to delete.");
            return;
        }

        listExpenses();
        System.out.print("Enter ID of expense to delete: ");
        String input = SCANNER.nextLine().trim();
        try {
            int id = Integer.parseInt(input);
            boolean removed = expenses.removeIf(e -> e.id() == id);
            if (removed) {
                System.out.println("Expense with ID " + id + " deleted.");
            } else {
                System.out.println("No expense found with ID " + id + ".");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID.");
        }
    }

    private BigDecimal readAmount() {
        while (true) {
            System.out.print("Amount (e.g. 250.75): ");
            String value = SCANNER.nextLine().trim();
            try {
                return new BigDecimal(value);
            } catch (NumberFormatException e) {
                System.out.println("Invalid amount. Please enter a numeric value.");
            }
        }
    }

    private LocalDate readDate() {
        while (true) {
            System.out.print("Date (yyyy-MM-dd), leave blank for today: ");
            String value = SCANNER.nextLine().trim();
            if (value.isEmpty()) {
                return LocalDate.now();
            }
            try {
                return LocalDate.parse(value, DATE_FORMAT);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Try again.");
            }
        }
    }

    private Category readCategory() {
        while (true) {
            System.out.println("Choose category:");
            for (Category c : Category.values()) {
                System.out.println((c.ordinal() + 1) + ". " + c);
            }
            System.out.print("Enter number: ");
            String value = SCANNER.nextLine().trim();
            try {
                int idx = Integer.parseInt(value) - 1;
                if (idx >= 0 && idx < Category.values().length) {
                    return Category.values()[idx];
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Invalid choice. Try again.");
        }
    }

    private void loadFromFile() {
        if (!Files.exists(DATA_FILE)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(DATA_FILE);
            for (String line : lines) {
                if (line.isBlank()) continue;
                Expense e = Expense.fromCsv(line);
                expenses.add(e);
                nextId = Math.max(nextId, e.id() + 1);
            }
            System.out.println("Loaded " + expenses.size() + " expenses from file.");
        } catch (IOException e) {
            System.out.println("Could not read data file: " + e.getMessage());
        }
    }

    private void saveToFile() {
        List<String> lines = new ArrayList<>();
        for (Expense e : expenses) {
            lines.add(e.toCsv());
        }
        try {
            Files.write(DATA_FILE, lines);
            System.out.println("Saved " + expenses.size() + " expenses to file.");
        } catch (IOException e) {
            System.out.println("Could not save data file: " + e.getMessage());
        }
    }

    // ----- Inner types -----

    /**
     * Expense record (Java 16+). If using older Java, replace with a normal class.
     */
    private record Expense(int id,
                           String description,
                           BigDecimal amount,
                           LocalDate date,
                           Category category) {

        @Override
        public String toString() {
            return String.format("[%d] %s | %s | %s | %s",
                    id, date, category, amount, description);
        }

        public String toCsv() {
            // id;description;amount;date;category
            return id + ";" +
                    description.replace(";", ",") + ";" +
                    amount + ";" +
                    date + ";" +
                    category;
        }

        public static Expense fromCsv(String line) {
            String[] parts = line.split(";", -1);
            int id = Integer.parseInt(parts[0]);
            String description = parts[1];
            BigDecimal amount = new BigDecimal(parts[2]);
            LocalDate date = LocalDate.parse(parts[3]);
            Category category = Category.valueOf(parts[4]);
            return new Expense(id, description, amount, date, category);
        }
    }

    /**
     * Categories for expenses.
     */
    private enum Category {
        FOOD,
        TRANSPORT,
        RENT,
        ENTERTAINMENT,
        SHOPPING,
        OTHER
    }
}
