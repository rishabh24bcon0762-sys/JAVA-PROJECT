import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/*
 * Online Ticket Booking System
 * GUI: Java Swing + AWT
 * Concept: Thread Synchronization
 *
 * Login:
 * Username: admin
 * Password: 12345
 *
 * Compile: javac TicketBookingSystem.java
 * Run:     java TicketBookingSystem
 */

class BookingSystem {
    private int availableTickets;
    private int bookingCounter = 1001;

    private final Map<Integer, Booking> bookings = new LinkedHashMap<>();
    private final Random random = new Random();

    static class Booking {
        final int bookingId;
        final String customerName;
        final int ticketCount;

        Booking(int bookingId, String customerName, int ticketCount) {
            this.bookingId = bookingId;
            this.customerName = customerName;
            this.ticketCount = ticketCount;
        }
    }

    BookingSystem(int totalTickets) {
        availableTickets = totalTickets;
    }

    public synchronized int getAvailableTickets() {
        return availableTickets;
    }

    /*
     * This synchronized method is the main demonstration of thread
     * synchronization. Only one thread can update ticket availability
     * and booking records at a time for this BookingSystem object.
     */
    public synchronized String bookTickets(String customerName, int count) {
        if (count <= 0) {
            return "Please enter a ticket quantity greater than zero.";
        }

        if (count > availableTickets) {
            return "Booking failed for " + customerName
                    + ". Only " + availableTickets + " ticket(s) remain.";
        }

        // Small delay makes simultaneous booking attempts easier to demonstrate.
        try {
            Thread.sleep(700);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return "Booking was interrupted.";
        }

        if (count > availableTickets) {
            return "Booking failed for " + customerName
                    + ". Another customer booked the tickets first.";
        }

        availableTickets -= count;
        int id = bookingCounter++;
        bookings.put(id, new Booking(id, customerName, count));

        return "Booking successful!\nBooking ID: " + id
                + "\nCustomer: " + customerName
                + "\nTickets: " + count
                + "\nTickets remaining: " + availableTickets;
    }

    public synchronized String cancelBooking(int bookingId) {
        Booking booking = bookings.remove(bookingId);

        if (booking == null) {
            return "No booking found with ID " + bookingId + ".";
        }

        availableTickets += booking.ticketCount;
        return "Booking " + bookingId + " cancelled successfully.\n"
                + booking.ticketCount + " ticket(s) added back.";
    }

    public synchronized java.util.List<Booking> getBookings() {
        return new ArrayList<>(bookings.values());
    }

    /*
     * Simulates a competing customer. The competitor calls bookTickets()
     * directly; it does not recursively create more competitor threads.
     */
    public boolean shouldSimulateCompetitor() {
        return random.nextInt(100) < 40;
    }
}

public class TicketBookingSystem extends JFrame {
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "12345";

    private BookingSystem bookingSystem;
    private JLabel availableLabel;
    private JLabel statusLabel;
    private DefaultTableModel tableModel;
    private JTable bookingTable;

    public TicketBookingSystem() {
        showLoginScreen();
    }

    private void showLoginScreen() {
        setTitle("Online Ticket Booking System - Login");
        setSize(470, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel background = new JPanel(new GridBagLayout());
        background.setBackground(new Color(239, 244, 250));

        JPanel card = new JPanel();
        card.setPreferredSize(new Dimension(350, 260));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 220, 232)),
                BorderFactory.createEmptyBorder(22, 28, 22, 28)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("TICKET BOOKING");
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setForeground(new Color(31, 78, 121));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Please sign in to continue");
        subtitle.setFont(new Font("Arial", Font.PLAIN, 13));
        subtitle.setForeground(Color.GRAY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        styleInput(usernameField);
        styleInput(passwordField);

        JButton loginButton = new JButton("LOGIN");
        styleButton(loginButton, new Color(31, 112, 183));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hint = new JLabel("Demo: admin / 12345");
        hint.setFont(new Font("Arial", Font.PLAIN, 11));
        hint.setForeground(Color.GRAY);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(title);
        card.add(Box.createVerticalStrut(6));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(22));
        card.add(new JLabel("Username"));
        card.add(Box.createVerticalStrut(5));
        card.add(usernameField);
        card.add(Box.createVerticalStrut(12));
        card.add(new JLabel("Password"));
        card.add(Box.createVerticalStrut(5));
        card.add(passwordField);
        card.add(Box.createVerticalStrut(18));
        card.add(loginButton);
        card.add(Box.createVerticalStrut(10));
        card.add(hint);

        background.add(card);
        setContentPane(background);

        final int[] attempts = {0};
        Runnable loginAction = () -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (USERNAME.equals(username) && PASSWORD.equals(password)) {
                JOptionPane.showMessageDialog(this, "Login successful. Welcome!");
                int total = askForTotalTickets();
                if (total > 0) {
                    bookingSystem = new BookingSystem(total);
                    showDashboard();
                } else {
                    dispose();
                }
            } else {
                attempts[0]++;
                int remaining = 3 - attempts[0];
                if (remaining > 0) {
                    JOptionPane.showMessageDialog(this,
                            "Invalid username or password.\nAttempts remaining: " + remaining,
                            "Login Failed", JOptionPane.ERROR_MESSAGE);
                    passwordField.setText("");
                } else {
                    JOptionPane.showMessageDialog(this,
                            "You have used all 3 attempts. Access denied.",
                            "Access Denied", JOptionPane.ERROR_MESSAGE);
                    dispose();
                }
            }
        };

        loginButton.addActionListener(e -> loginAction.run());
        passwordField.addActionListener(e -> loginAction.run());
        setVisible(true);
    }

    private int askForTotalTickets() {
        while (true) {
            String input = JOptionPane.showInputDialog(this,
                    "Enter the total number of tickets available:", "Ticket Setup",
                    JOptionPane.QUESTION_MESSAGE);
            if (input == null) return -1;
            try {
                int total = Integer.parseInt(input.trim());
                if (total > 0) return total;
            } catch (NumberFormatException ignored) {
                // Show validation message below.
            }
            JOptionPane.showMessageDialog(this,
                    "Please enter a valid positive whole number.",
                    "Invalid Input", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void showDashboard() {
        setTitle("Online Ticket Booking System");
        setSize(900, 620);
        setMinimumSize(new Dimension(760, 540));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(15, 15));
        root.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        root.setBackground(new Color(242, 246, 251));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel heading = new JLabel("Online Ticket Booking System");
        heading.setFont(new Font("Arial", Font.BOLD, 25));
        heading.setForeground(new Color(31, 78, 121));

        availableLabel = new JLabel();
        availableLabel.setFont(new Font("Arial", Font.BOLD, 16));
        availableLabel.setForeground(new Color(28, 130, 75));
        updateAvailableLabel();

        header.add(heading, BorderLayout.WEST);
        header.add(availableLabel, BorderLayout.EAST);

        JPanel actions = new JPanel(new GridLayout(2, 2, 12, 12));
        actions.setOpaque(false);

        JButton bookButton = new JButton("Book Tickets");
        JButton cancelButton = new JButton("Cancel Booking");
        JButton refreshButton = new JButton("Refresh Bookings");
        JButton multiButton = new JButton("Simulate Multiple Customers");

        styleButton(bookButton, new Color(31, 112, 183));
        styleButton(cancelButton, new Color(190, 75, 68));
        styleButton(refreshButton, new Color(92, 105, 120));
        styleButton(multiButton, new Color(42, 139, 105));

        actions.add(bookButton);
        actions.add(cancelButton);
        actions.add(refreshButton);
        actions.add(multiButton);

        String[] columns = {"Booking ID", "Customer Name", "Tickets"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        bookingTable = new JTable(tableModel);
        bookingTable.setRowHeight(27);
        bookingTable.setFont(new Font("Arial", Font.PLAIN, 13));
        bookingTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        JScrollPane scrollPane = new JScrollPane(bookingTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("All Bookings"));

        statusLabel = new JLabel("Ready. Select an operation.");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 3, 0, 3));
        statusLabel.setForeground(new Color(75, 85, 95));

        JPanel center = new JPanel(new BorderLayout(12, 15));
        center.setOpaque(false);
        center.add(actions, BorderLayout.NORTH);
        center.add(scrollPane, BorderLayout.CENTER);

        root.add(header, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);

        setContentPane(root);

        bookButton.addActionListener(e -> bookTicketDialog());
        cancelButton.addActionListener(e -> cancelTicketDialog());
        refreshButton.addActionListener(e -> refreshBookings());
        multiButton.addActionListener(e -> multipleCustomerDialog());

        refreshBookings();
        revalidate();
        repaint();
    }

    private void bookTicketDialog() {
        JTextField nameField = new JTextField();
        JTextField countField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.add(new JLabel("Customer name:"));
        panel.add(nameField);
        panel.add(new JLabel("Number of tickets:"));
        panel.add(countField);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Book Tickets", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        int count;
        try {
            count = Integer.parseInt(countField.getText().trim());
        } catch (NumberFormatException ex) {
            showError("Enter a valid number of tickets.");
            return;
        }

        if (name.isEmpty()) {
            showError("Customer name cannot be empty.");
            return;
        }
        if (count <= 0) {
            showError("Ticket quantity must be greater than zero.");
            return;
        }

        setBusy(true, "Processing booking...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                // 40% chance of a competing customer attempting the same quantity first.
                if (bookingSystem.shouldSimulateCompetitor()) {
                    String competitorResult =
                            bookingSystem.bookTickets("Another Customer", count);
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("A competing customer attempted a booking.");
                    });
                }
                return bookingSystem.bookTickets(name, count);
            }

            @Override
            protected void done() {
                try {
                    String message = get();
                    JOptionPane.showMessageDialog(TicketBookingSystem.this, message,
                            "Booking Result", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    showError("Could not complete booking: " + ex.getMessage());
                }
                refreshBookings();
                setBusy(false, "Booking operation completed.");
            }
        }.execute();
    }

    private void cancelTicketDialog() {
        String input = JOptionPane.showInputDialog(this,
                "Enter the Booking ID to cancel:", "Cancel Booking",
                JOptionPane.QUESTION_MESSAGE);
        if (input == null) return;

        try {
            int id = Integer.parseInt(input.trim());
            String message = bookingSystem.cancelBooking(id);
            JOptionPane.showMessageDialog(this, message, "Cancellation Result",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshBookings();
        } catch (NumberFormatException ex) {
            showError("Please enter a valid numeric Booking ID.");
        }
    }

    private void multipleCustomerDialog() {
        JTextField numberField = new JTextField("3");
        JTextField ticketsField = new JTextField("1");

        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.add(new JLabel("Number of customers (2 to 10):"));
        panel.add(numberField);
        panel.add(new JLabel("Tickets requested by each customer:"));
        panel.add(ticketsField);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Multiple Customer Simulation", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        int customerCount;
        int ticketCount;
        try {
            customerCount = Integer.parseInt(numberField.getText().trim());
            ticketCount = Integer.parseInt(ticketsField.getText().trim());
        } catch (NumberFormatException ex) {
            showError("Please enter valid whole numbers.");
            return;
        }

        if (customerCount < 2 || customerCount > 10 || ticketCount <= 0) {
            showError("Enter 2–10 customers and a positive ticket quantity.");
            return;
        }

        setBusy(true, "Multiple customers are trying to book at the same time...");
        new SwingWorker<java.util.List<String>, Void>() {
            @Override
            protected java.util.List<String> doInBackground() {
                java.util.List<Thread> threads = new ArrayList<>();
                java.util.List<String> results =
                        Collections.synchronizedList(new ArrayList<>());

                for (int i = 1; i <= customerCount; i++) {
                    final String customerName = "Customer " + i;
                    Thread thread = new Thread(() -> {
                        String bookingResult =
                                bookingSystem.bookTickets(customerName, ticketCount);
                        results.add(customerName + ": " + bookingResult);
                    }, "Booking-" + i);
                    threads.add(thread);
                    thread.start();
                }

                for (Thread thread : threads) {
                    try {
                        thread.join();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        results.add("Simulation interrupted.");
                        break;
                    }
                }
                return new ArrayList<>(results);
            }

            @Override
            protected void done() {
                try {
                    StringBuilder summary = new StringBuilder();
                    for (String item : get()) {
                        summary.append(item).append("\n\n");
                    }
                    JTextArea output = new JTextArea(summary.toString(), 15, 55);
                    output.setEditable(false);
                    output.setLineWrap(true);
                    output.setWrapStyleWord(true);
                    JOptionPane.showMessageDialog(TicketBookingSystem.this,
                            new JScrollPane(output), "Concurrent Booking Results",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    showError("Simulation failed: " + ex.getMessage());
                }
                refreshBookings();
                setBusy(false, "Multiple-customer simulation completed.");
            }
        }.execute();
    }

    private void refreshBookings() {
        tableModel.setRowCount(0);
        for (BookingSystem.Booking booking : bookingSystem.getBookings()) {
            tableModel.addRow(new Object[]{
                    booking.bookingId, booking.customerName, booking.ticketCount
            });
        }
        updateAvailableLabel();
    }

    private void updateAvailableLabel() {
        if (availableLabel != null && bookingSystem != null) {
            availableLabel.setText("Available Tickets: " + bookingSystem.getAvailableTickets());
        }
    }

    private void setBusy(boolean busy, String message) {
        statusLabel.setText(message);
        setCursor(busy
                ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                : Cursor.getDefaultCursor());
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Input Error",
                JOptionPane.ERROR_MESSAGE);
    }

    private void styleInput(JTextField field) {
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        field.setPreferredSize(new Dimension(280, 34));
        field.setFont(new Font("Arial", Font.PLAIN, 14));
    }

    private void styleButton(JButton button, Color color) {
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TicketBookingSystem::new);
    }
}
