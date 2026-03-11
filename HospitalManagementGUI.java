// Save as HospitalManagementGUI.java
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * HospitalManagementGUI - All features integrated into a single file.
 * - NEW: Admin can add beds with types and prices.
 * - NEW: Receptionist and Doctor can cancel appointments.
 * - NEW: Radio buttons for gender, custom calendar for dates, and interactive billing UI.
 * - REMOVED: Lab Technician role.
 * - Three Roles: Admin, Receptionist, Doctor
 * - Real-time search/filter on all data tables
 * - Corrected icon loading for IntelliJ IDEA.
 */
public class HospitalManagementGUI {
    private static final String DB_URL = "jdbc:sqlite:hospital.db";

    // --- UI Design Constants ---
    private static final Color COLOR_NAV_BAR = new Color(44, 62, 80);
    private static final Color COLOR_NAV_BAR_HOVER = new Color(52, 73, 94);
    private static final Color COLOR_CONTENT_BG = new Color(236, 240, 241);
    private static final Color COLOR_WHITE = Color.WHITE;
    private static final Color COLOR_FONT_NAV = new Color(236, 240, 241);
    private static final Color COLOR_PRIMARY_GREEN = new Color(39, 174, 96);
    private static final Color COLOR_HEADER_BG = new Color(52, 73, 94);

    private static final Font FONT_NAV = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_TABLE_HEADER = new Font("Segoe UI", Font.BOLD, 14);

    // CardLayout constants
    private static final String CARD_DASHBOARD = "Dashboard";
    private static final String CARD_REGISTER_PATIENT = "Register Patient";
    private static final String CARD_VIEW_PATIENTS = "View Patients";
    private static final String CARD_APPOINTMENTS = "Appointments";
    private static final String CARD_BEDS = "Beds";
    private static final String CARD_DOCTORS = "Doctors";
    private static final String CARD_REPORTS = "Reports";
    private static final String CARD_USERS = "Users";
    private static final String CARD_TESTS_MGMT = "Manage Tests";
    private static final String CARD_DOCTOR_APPOINTMENTS = "Doctor Appointments";
    private static final String CARD_BEDS_MGMT = "Manage Beds";


    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            initDatabase();
            seedUsers();
            seedBeds();
            SwingUtilities.invokeLater(HospitalManagementGUI::showRoleSelectionGUI);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to start application: " + e.getMessage(), "Startup Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- DASHBOARD METHODS FOR EACH ROLE ---

    private static void showAdminPanel(String username) {
        JFrame frame = new JFrame("Hospital Admin Dashboard");
        setupMainFrame(frame);

        CardLayout cardLayout = new CardLayout();
        JPanel contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(COLOR_CONTENT_BG);

        JPanel navPanel = createAdminNavPanel(contentPanel, cardLayout, frame);

        JTable doctorsTable = createStyledTable();
        doctorsTable.setModel(new DefaultTableModel(new String[]{"ID", "Name", "Specialization", "Phone", "Email"}, 0));
        JPanel doctorsPanel = createDataViewPanel("Manage Doctors", doctorsTable, () -> loadDoctors(doctorsTable));
        doctorsPanel.add(createDoctorActionPanel(doctorsTable), BorderLayout.SOUTH);

        JTable patientsTable = createStyledTable();
        patientsTable.setModel(new DefaultTableModel(new String[]{"ID", "Name", "Age", "Gender", "Phone", "Address", "History"}, 0));
        JPanel patientsPanel = createDataViewPanel("View All Patients", patientsTable, () -> loadPatients(patientsTable));

        JTable usersTable = createStyledTable();
        usersTable.setModel(new DefaultTableModel(new String[]{"ID", "Username", "Role", "Active"}, 0));
        JPanel usersPanel = createDataViewPanel("Manage Users", usersTable, () -> loadUsers(usersTable));
        usersPanel.add(createUserActionPanel(usersTable, frame), BorderLayout.SOUTH);

        JTable testsTable = createStyledTable();
        testsTable.setModel(new DefaultTableModel(new String[]{"ID", "Test Name", "Price", "Description"}, 0));
        JPanel testsPanel = createDataViewPanel("Manage Lab Tests", testsTable, () -> loadTests(testsTable));
        testsPanel.add(createTestManagementActionPanel(testsTable), BorderLayout.SOUTH);

        JTable bedsTable = createStyledTable();
        bedsTable.setModel(new DefaultTableModel(new String[]{"Bed ID", "Number", "Type", "Price/Day", "Status"}, 0));
        JPanel bedsPanel = createDataViewPanel("Manage Beds", bedsTable, () -> loadBedsAdmin(bedsTable)); // Use admin version
        bedsPanel.add(createBedManagementActionPanel(bedsTable), BorderLayout.SOUTH);

        contentPanel.add(createDashboardPanel("Admin"), CARD_DASHBOARD);
        contentPanel.add(doctorsPanel, CARD_DOCTORS);
        contentPanel.add(patientsPanel, CARD_VIEW_PATIENTS);
        contentPanel.add(createReportsPanel(), CARD_REPORTS);
        contentPanel.add(usersPanel, CARD_USERS);
        contentPanel.add(testsPanel, CARD_TESTS_MGMT);
        contentPanel.add(bedsPanel, CARD_BEDS_MGMT);

        frame.add(navPanel, BorderLayout.WEST);
        frame.add(contentPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private static void showReceptionistPanel(String username) {
        JFrame frame = new JFrame("Hospital Receptionist Dashboard");
        setupMainFrame(frame);

        CardLayout cardLayout = new CardLayout();
        JPanel contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(COLOR_CONTENT_BG);

        JPanel navPanel = createReceptionistNavPanel(contentPanel, cardLayout, frame);

        JTable viewPatientsTable = createStyledTable();
        viewPatientsTable.setModel(new DefaultTableModel(new String[]{"ID", "Name", "Age", "Gender", "Phone", "Address", "History"}, 0));
        JPanel viewPatientsPanel = createDataViewPanel("All Patients", viewPatientsTable, () -> loadPatients(viewPatientsTable));

        JTable viewAppointmentsTable = createStyledTable();
        viewAppointmentsTable.setModel(new DefaultTableModel(new String[]{"Appt ID", "Patient", "Patient ID", "Doctor", "Date", "Time", "Status"}, 0));
        JPanel viewAppointmentsPanel = createDataViewPanel("All Appointments", viewAppointmentsTable, () -> loadAppointments(viewAppointmentsTable));
        addAppointmentCancellationMenu(viewAppointmentsTable);

        JTable viewBedsTable = createStyledTable();
        viewBedsTable.setModel(new DefaultTableModel(new String[]{"Bed ID", "Number", "Type", "Price/Day", "Status", "Patient ID", "Patient Name"}, 0));
        JPanel viewBedsPanel = createDataViewPanel("Bed Management", viewBedsTable, () -> loadBedsReceptionist(viewBedsTable)); // Use receptionist version

        contentPanel.add(createDashboardPanel("Receptionist"), CARD_DASHBOARD);
        contentPanel.add(createPatientRegistrationPanel(), CARD_REGISTER_PATIENT);
        contentPanel.add(viewPatientsPanel, CARD_VIEW_PATIENTS);
        contentPanel.add(viewAppointmentsPanel, CARD_APPOINTMENTS);
        contentPanel.add(viewBedsPanel, CARD_BEDS);

        frame.add(navPanel, BorderLayout.WEST);
        frame.add(contentPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private static void showDoctorDashboard(String username) {
        JFrame frame = new JFrame("Doctor's Dashboard");
        setupMainFrame(frame);

        CardLayout cardLayout = new CardLayout();
        JPanel contentPanel = new JPanel(cardLayout);

        JPanel navPanel = createDoctorNavPanel(contentPanel, cardLayout, frame);

        JTable appointmentsTable = createStyledTable();
        appointmentsTable.setModel(new DefaultTableModel(new String[]{"Appt ID", "Patient Name", "Patient ID", "Time Slot", "Status", "Remarks"}, 0));
        int doctorId = getDoctorIdByName(username);

        JPanel appointmentsPanel = createDataViewPanel("Today's Appointments", appointmentsTable, () -> loadAppointmentsForDoctor(appointmentsTable, doctorId));
        addAppointmentCancellationMenu(appointmentsTable);

        appointmentsTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // Double click to view history
                    int selectedRow = appointmentsTable.convertRowIndexToModel(appointmentsTable.getSelectedRow());
                    if (selectedRow >= 0) {
                        int patientId = (int) appointmentsTable.getModel().getValueAt(selectedRow, 2);
                        showPatientHistory(frame, patientId);
                    }
                }
            }
        });

        contentPanel.add(createDashboardPanel("Doctor"), CARD_DASHBOARD);
        contentPanel.add(appointmentsPanel, CARD_DOCTOR_APPOINTMENTS);

        frame.add(navPanel, BorderLayout.WEST);
        frame.add(contentPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    // --- NAVIGATION PANELS (SIDEBARS) ---

    private static JPanel createAdminNavPanel(JPanel contentPanel, CardLayout cardLayout, JFrame mainFrame) {
        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBackground(COLOR_NAV_BAR);
        navPanel.setPreferredSize(new Dimension(250, 0));

        JLabel titleLabel = new JLabel("ADMIN PANEL");
        titleLabel.setFont(FONT_HEADER);
        titleLabel.setForeground(COLOR_WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBorder(new EmptyBorder(20, 10, 20, 10));
        navPanel.add(titleLabel);
        navPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        navPanel.add(createNavItem("Dashboard", "dashboard.png", CARD_DASHBOARD, contentPanel, cardLayout));
        navPanel.add(createNavItem("Doctors", "doctor.png", CARD_DOCTORS, contentPanel, cardLayout));
        navPanel.add(createNavItem("Patients", "view_users.png", CARD_VIEW_PATIENTS, contentPanel, cardLayout));
        navPanel.add(createNavItem("Manage Beds", "bed.png", CARD_BEDS_MGMT, contentPanel, cardLayout));
        navPanel.add(createNavItem("Manage Tests", "lab.png", CARD_TESTS_MGMT, contentPanel, cardLayout));
        navPanel.add(createNavItem("Reports", "report.png", CARD_REPORTS, contentPanel, cardLayout));
        navPanel.add(createNavItem("User Management", "user_management.png", CARD_USERS, contentPanel, cardLayout));

        navPanel.add(Box.createVerticalGlue());
        navPanel.add(createNavItem("Logout", "logout.png", () -> {
            mainFrame.dispose();
            showRoleSelectionGUI();
        }));
        return navPanel;
    }

    private static JPanel createReceptionistNavPanel(JPanel contentPanel, CardLayout cardLayout, JFrame mainFrame) {
        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBackground(COLOR_NAV_BAR);
        navPanel.setPreferredSize(new Dimension(250, 0));

        JLabel titleLabel = new JLabel("CITY HOSPITAL");
        titleLabel.setFont(FONT_HEADER);
        titleLabel.setForeground(COLOR_WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBorder(new EmptyBorder(20, 10, 20, 10));
        navPanel.add(titleLabel);
        navPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        navPanel.add(createNavItem("Dashboard", "dashboard.png", CARD_DASHBOARD, contentPanel, cardLayout));
        navPanel.add(createNavItem("Register Patient", "add_patient.png", CARD_REGISTER_PATIENT, contentPanel, cardLayout));
        navPanel.add(createNavItem("View Patients", "view_users.png", CARD_VIEW_PATIENTS, contentPanel, cardLayout));
        navPanel.add(createNavItem("Book Appointment", "appointment.png", () -> bookAppointment(mainFrame)));
        navPanel.add(createNavItem("View Appointments", "view_appointments.png", CARD_APPOINTMENTS, contentPanel, cardLayout));
        navPanel.add(createNavItem("Bed Management", "bed.png", CARD_BEDS, contentPanel, cardLayout));
        navPanel.add(createNavItem("Generate Bill", "bill.png", () -> generateBill(mainFrame)));
        navPanel.add(createNavItem("Book Test", "lab.png", () -> bookTest()));
        navPanel.add(createNavItem("Patient Dashboard", "patient_folder.png", () -> promptAndShowPatientDashboard()));

        navPanel.add(Box.createVerticalGlue());
        navPanel.add(createNavItem("Logout", "logout.png", () -> {
            mainFrame.dispose();
            showRoleSelectionGUI();
        }));
        return navPanel;
    }

    private static JPanel createDoctorNavPanel(JPanel contentPanel, CardLayout cardLayout, JFrame mainFrame) {
        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBackground(COLOR_NAV_BAR);
        navPanel.setPreferredSize(new Dimension(250, 0));

        JLabel titleLabel = new JLabel("DOCTOR PORTAL");
        titleLabel.setFont(FONT_HEADER);
        titleLabel.setForeground(COLOR_WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBorder(new EmptyBorder(20, 10, 20, 10));
        navPanel.add(titleLabel);

        navPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        navPanel.add(createNavItem("Dashboard", "dashboard.png", CARD_DASHBOARD, contentPanel, cardLayout));
        navPanel.add(createNavItem("Appointments", "appointment.png", CARD_DOCTOR_APPOINTMENTS, contentPanel, cardLayout));

        navPanel.add(Box.createVerticalGlue());
        navPanel.add(createNavItem("Logout", "logout.png", () -> { mainFrame.dispose(); showRoleSelectionGUI(); }));
        return navPanel;
    }

    // --- REUSABLE UI COMPONENTS ---

    private static void setupMainFrame(JFrame frame) {
        frame.setSize(1200, 700);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
    }

    private static JPanel createNavItem(String text, String iconPath, String cardName, JPanel contentPanel, CardLayout cardLayout) {
        return createNavItem(text, iconPath, () -> cardLayout.show(contentPanel, cardName));
    }

    private static JPanel createNavItem(String text, String iconPath, Runnable action) {
        JPanel navItemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        navItemPanel.setBackground(COLOR_NAV_BAR);
        navItemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        navItemPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        try {
            java.net.URL imageUrl = HospitalManagementGUI.class.getResource("/icons/" + iconPath);
            if (imageUrl != null) {
                ImageIcon icon = new ImageIcon(new ImageIcon(imageUrl).getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH));
                navItemPanel.add(new JLabel(icon));
            } else {
                System.err.println("Icon not found: /icons/" + iconPath);
                navItemPanel.add(Box.createHorizontalStrut(24));
            }
        } catch (Exception e) {
            e.printStackTrace();
            navItemPanel.add(Box.createHorizontalStrut(24));
        }
        JLabel textLabel = new JLabel(text);
        textLabel.setFont(FONT_NAV);
        textLabel.setForeground(COLOR_FONT_NAV);
        navItemPanel.add(textLabel);
        navItemPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        navItemPanel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { action.run(); }
            public void mouseEntered(MouseEvent e) { navItemPanel.setBackground(COLOR_NAV_BAR_HOVER); }
            public void mouseExited(MouseEvent e) { navItemPanel.setBackground(COLOR_NAV_BAR); }
        });
        return navItemPanel;
    }

    private static JPanel createDashboardPanel(String role) {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBorder(new EmptyBorder(20, 30, 20, 30));
        panel.setBackground(COLOR_CONTENT_BG);
        JLabel welcomeLabel = new JLabel("Welcome, " + role + "!");
        welcomeLabel.setFont(FONT_HEADER);
        welcomeLabel.setForeground(COLOR_NAV_BAR);
        panel.add(welcomeLabel, BorderLayout.NORTH);
        JLabel infoLabel = new JLabel("Select an option from the left menu to get started.");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(infoLabel, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel createPatientRegistrationPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBorder(new EmptyBorder(20, 30, 20, 30));
        panel.setBackground(COLOR_CONTENT_BG);
        JLabel headerLabel = new JLabel("Register New Patient");
        headerLabel.setFont(FONT_HEADER);
        headerLabel.setForeground(COLOR_NAV_BAR);
        panel.add(headerLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(COLOR_WHITE);
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        JRadioButton maleRadio = new JRadioButton("Male");
        maleRadio.setActionCommand("Male");
        maleRadio.setSelected(true);
        JRadioButton femaleRadio = new JRadioButton("Female");
        femaleRadio.setActionCommand("Female");
        JRadioButton otherRadio = new JRadioButton("Other");
        otherRadio.setActionCommand("Other");
        ButtonGroup genderGroup = new ButtonGroup();
        genderGroup.add(maleRadio);
        genderGroup.add(femaleRadio);
        genderGroup.add(otherRadio);
        JPanel genderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        genderPanel.setBackground(COLOR_WHITE);
        genderPanel.add(maleRadio);
        genderPanel.add(femaleRadio);
        genderPanel.add(otherRadio);

        Component[] formComponents = {
            new JTextField(30),
            new JTextField(30),
            genderPanel,
            new JTextField(30),
            new JTextField(30),
            new JTextField(30)
        };

        String[] labels = {"Name:", "Age:", "Gender:", "Phone:", "Address:", "Medical History:"};
        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
            formPanel.add(new JLabel(labels[i]) {{setFont(FONT_LABEL);}}, gbc);
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
            formPanel.add(formComponents[i], gbc);
        }

        JButton saveButton = new JButton("Save Patient");
        stylePrimaryButton(saveButton);
        gbc.gridx = 1; gbc.gridy = labels.length;
        gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        formPanel.add(saveButton, gbc);

        saveButton.addActionListener(e -> {
            String name = ((JTextField)formComponents[0]).getText().trim();
            String ageStr = ((JTextField)formComponents[1]).getText().trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Patient name is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!ageStr.isEmpty() && !ageStr.matches("\\d+")) {
                JOptionPane.showMessageDialog(panel, "Age must be a valid number.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                PreparedStatement ps = conn.prepareStatement("INSERT INTO patients(name,age,gender,phone,address,medical_history) VALUES(?,?,?,?,?,?)");
                ps.setString(1, name);
                ps.setInt(2, parseIntSafe(ageStr));
                ps.setString(3, genderGroup.getSelection().getActionCommand());
                ps.setString(4, ((JTextField)formComponents[3]).getText().trim());
                ps.setString(5, ((JTextField)formComponents[4]).getText().trim());
                ps.setString(6, ((JTextField)formComponents[5]).getText().trim());
                ps.executeUpdate();
                JOptionPane.showMessageDialog(panel, "Patient registered successfully!");
                ((JTextField)formComponents[0]).setText("");
                ((JTextField)formComponents[1]).setText("");
                ((JTextField)formComponents[3]).setText("");
                ((JTextField)formComponents[4]).setText("");
                ((JTextField)formComponents[5]).setText("");
                maleRadio.setSelected(true);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(panel, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        panel.add(formPanel, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel createDataViewPanel(String title, JTable table, Runnable refreshAction) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 30, 20, 30));
        panel.setBackground(COLOR_CONTENT_BG);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel headerLabel = new JLabel(title);
        headerLabel.setFont(FONT_HEADER);
        headerLabel.setForeground(COLOR_NAV_BAR);
        headerPanel.add(headerLabel, BorderLayout.WEST);

        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlsPanel.setOpaque(false);

        JTextField searchField = new JTextField(20);
        searchField.setFont(FONT_LABEL);
        controlsPanel.add(new JLabel("Search:"));
        controlsPanel.add(searchField);

        JButton refreshButton = new JButton("Refresh");
        stylePrimaryButton(refreshButton);
        refreshButton.addActionListener(e -> refreshAction.run());
        controlsPanel.add(refreshButton);

        headerPanel.add(controlsPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(COLOR_WHITE);
        panel.add(scrollPane, BorderLayout.CENTER);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel) table.getModel());
        table.setRowSorter(sorter);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void insertUpdate(DocumentEvent e) { filter(); }
            private void filter() {
                String text = searchField.getText();
                if (text.trim().length() == 0) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                }
            }
        });

        SwingUtilities.invokeLater(refreshAction);

        return panel;
    }

    private static JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(20,20));
        panel.setBorder(new EmptyBorder(20,30,20,30));
        panel.setBackground(COLOR_CONTENT_BG);

        JLabel headerLabel = new JLabel("Reports");
        headerLabel.setFont(FONT_HEADER);
        headerLabel.setForeground(COLOR_NAV_BAR);
        panel.add(headerLabel, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        JTable revenueTable = createStyledTable();
        revenueTable.setModel(new DefaultTableModel(new String[]{"Month", "Revenue", "Bills Count"}, 0));
        JTable doctorsTable = createStyledTable();
        doctorsTable.setModel(new DefaultTableModel(new String[]{"Doctor ID", "Name", "Specialization", "Appointments"}, 0));

        tabbedPane.addTab("Revenue by Month", new JScrollPane(revenueTable));
        tabbedPane.addTab("Top Doctors by Appointments", new JScrollPane(doctorsTable));

        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 0) loadRevenueByMonth(revenueTable);
            else if (tabbedPane.getSelectedIndex() == 1) loadTopDoctors(doctorsTable);
        });

        panel.add(tabbedPane, BorderLayout.CENTER);
        SwingUtilities.invokeLater(() -> loadRevenueByMonth(revenueTable));
        return panel;
    }

    private static JPanel createDoctorActionPanel(JTable table) {
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setBackground(COLOR_WHITE);
        JButton addDoctorBtn = new JButton("Register New Doctor");
        stylePrimaryButton(addDoctorBtn);
        addDoctorBtn.addActionListener(e -> {
            JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
            JTextField nameField = new JTextField();
            JTextField specField = new JTextField();
            JTextField phoneField = new JTextField();
            JTextField emailField = new JTextField();
            panel.add(new JLabel("Name:")); panel.add(nameField);
            panel.add(new JLabel("Specialization:")); panel.add(specField);
            panel.add(new JLabel("Phone:")); panel.add(phoneField);
            panel.add(new JLabel("Email:")); panel.add(emailField);
            int result = JOptionPane.showConfirmDialog(null, panel, "Register Doctor", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                String name = nameField.getText().trim();
                if (name.isEmpty()) { JOptionPane.showMessageDialog(null, "Name is required."); return; }
                try (Connection conn = DriverManager.getConnection(DB_URL)) {
                    PreparedStatement ps = conn.prepareStatement("INSERT INTO doctors(name,specialization,phone,email) VALUES(?,?,?,?)");
                    ps.setString(1, name);
                    ps.setString(2, specField.getText().trim());
                    ps.setString(3, phoneField.getText().trim());
                    ps.setString(4, emailField.getText().trim());
                    ps.executeUpdate();
                    JOptionPane.showMessageDialog(null, "Doctor registered successfully.");
                    loadDoctors(table);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
                }
            }
        });
        actionPanel.add(addDoctorBtn);
        return actionPanel;
    }

    private static JPanel createUserActionPanel(JTable table, Frame parent) {
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setBackground(COLOR_WHITE);

        JButton addUserBtn = new JButton("Add User");
        JButton resetPassBtn = new JButton("Reset Password");
        JButton toggleActiveBtn = new JButton("Toggle Active");
        stylePrimaryButton(addUserBtn);
        stylePrimaryButton(resetPassBtn);
        stylePrimaryButton(toggleActiveBtn);

        addUserBtn.addActionListener(e -> {
            JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
            JTextField userField = new JTextField();
            JPasswordField passField = new JPasswordField();
            JTextField roleField = new JTextField();
            panel.add(new JLabel("Username:")); panel.add(userField);
            panel.add(new JLabel("Password:")); panel.add(passField);
            panel.add(new JLabel("Role (ADMIN/RECEPTIONIST/DOCTOR):")); panel.add(roleField);
            int result = JOptionPane.showConfirmDialog(parent, panel, "Create User", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                String u = userField.getText().trim();
                String pw = new String(passField.getPassword()).trim();
                String r = roleField.getText().trim().toUpperCase();
                if (u.isEmpty() || pw.isEmpty() || r.isEmpty()) { JOptionPane.showMessageDialog(parent, "All fields required."); return; }
                try (Connection conn = DriverManager.getConnection(DB_URL)) {
                    PreparedStatement ps = conn.prepareStatement("INSERT INTO users(username,password,role,active) VALUES(?,?,?,1)");
                    ps.setString(1, u); ps.setString(2, hashPassword(pw)); ps.setString(3, r);
                    ps.executeUpdate();
                    JOptionPane.showMessageDialog(parent, "User created.");
                    loadUsers(table);
                } catch (SQLException ex) { JOptionPane.showMessageDialog(parent, "Error: " + ex.getMessage()); }
            }
        });

        resetPassBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(parent, "Please select a user from the table."); return; }
            String username = (String)table.getValueAt(table.convertRowIndexToModel(row), 1);
            String newPass = JOptionPane.showInputDialog(parent, "Enter new password for " + username);
            if (newPass == null || newPass.trim().isEmpty()) return;
            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                PreparedStatement ps = conn.prepareStatement("UPDATE users SET password=? WHERE username=?");
                ps.setString(1, hashPassword(newPass.trim()));
                ps.setString(2, username);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(parent, "Password reset successfully.");
            } catch (SQLException ex) { JOptionPane.showMessageDialog(parent, "Error: " + ex.getMessage()); }
        });

        toggleActiveBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(parent, "Please select a user from the table."); return; }

            int confirm = JOptionPane.showConfirmDialog(parent, "Are you sure you want to change this user's status?", "Confirm Action", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            String username = (String)table.getValueAt(table.convertRowIndexToModel(row), 1);
            Object activeValue = table.getValueAt(table.convertRowIndexToModel(row), 3);
            int currentActive = (activeValue instanceof Integer) ? (Integer) activeValue : 0;
            int newActive = (currentActive == 1) ? 0 : 1;
            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                PreparedStatement ps = conn.prepareStatement("UPDATE users SET active=? WHERE username=?");
                ps.setInt(1, newActive);
                ps.setString(2, username);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(parent, "User '" + username + "' status set to " + (newActive == 1 ? "Active" : "Inactive"));
                loadUsers(table);
            } catch (SQLException ex) { JOptionPane.showMessageDialog(parent, "Error: " + ex.getMessage()); }
        });

        actionPanel.add(addUserBtn);
        actionPanel.add(resetPassBtn);
        actionPanel.add(toggleActiveBtn);
        return actionPanel;
    }

    // --- UI Helpers ---
    private static void stylePrimaryButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(COLOR_PRIMARY_GREEN);
        button.setForeground(COLOR_WHITE);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(8, 15, 8, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
    }

    private static JTable createStyledTable() {
        JTable table = new JTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setFont(FONT_LABEL);
        table.setRowHeight(30);
        table.setGridColor(new Color(224, 224, 224));
        table.setSelectionBackground(COLOR_PRIMARY_GREEN.brighter());
        table.setSelectionForeground(COLOR_WHITE);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);

        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(COLOR_HEADER_BG);
                c.setForeground(COLOR_WHITE);
                c.setFont(FONT_TABLE_HEADER);
                if (c instanceof JLabel) {
                    JLabel label = (JLabel) c;
                    label.setHorizontalAlignment(SwingConstants.CENTER);
                    label.setBorder(new EmptyBorder(5, 10, 5, 10));
                }
                return c;
            }
        });

        return table;
    }

    // --- Database and Core Logic ---
    private static void initDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            Statement st = conn.createStatement();
            st.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE NOT NULL, password TEXT NOT NULL, role TEXT NOT NULL, active INTEGER DEFAULT 1)");
            st.execute("CREATE TABLE IF NOT EXISTS patients (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, age INTEGER, gender TEXT, phone TEXT, address TEXT, medical_history TEXT)");
            st.execute("CREATE TABLE IF NOT EXISTS doctors (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE, specialization TEXT, phone TEXT, email TEXT)");
            st.execute("CREATE TABLE IF NOT EXISTS appointments (id INTEGER PRIMARY KEY AUTOINCREMENT, patient_id INTEGER, doctor_id INTEGER, appointment_date TEXT, time_slot TEXT, status TEXT DEFAULT 'SCHEDULED', remarks TEXT, FOREIGN KEY(patient_id) REFERENCES patients(id), FOREIGN KEY(doctor_id) REFERENCES doctors(id))");
            st.execute("CREATE TABLE IF NOT EXISTS bills (id INTEGER PRIMARY KEY AUTOINCREMENT, patient_id INTEGER, amount REAL, details TEXT, created_at TEXT, FOREIGN KEY(patient_id) REFERENCES patients(id))");
            st.execute("CREATE TABLE IF NOT EXISTS tests (id INTEGER PRIMARY KEY AUTOINCREMENT, test_name TEXT NOT NULL, price REAL DEFAULT 0.0, description TEXT)");
            st.execute("CREATE TABLE IF NOT EXISTS booked_tests (id INTEGER PRIMARY KEY AUTOINCREMENT, patient_id INTEGER, test_id INTEGER, booked_date TEXT, status TEXT DEFAULT 'PENDING', result TEXT, FOREIGN KEY(patient_id) REFERENCES patients(id), FOREIGN KEY(test_id) REFERENCES tests(id))");
            st.execute("CREATE TABLE IF NOT EXISTS beds (id INTEGER PRIMARY KEY AUTOINCREMENT, bed_number TEXT UNIQUE NOT NULL, bed_type TEXT NOT NULL, price REAL DEFAULT 0.0, status TEXT DEFAULT 'Available', patient_id INTEGER, FOREIGN KEY(patient_id) REFERENCES patients(id))");
            st.close();
        }
    }

    private static void seedBeds() {
        createBedIfNotExists("GEN-101", "General Ward", 1500.0);
        createBedIfNotExists("PVT-201", "Private Room", 4000.0);
    }

    private static void createBedIfNotExists(String bedNumber, String bedType, double price) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM beds WHERE bed_number = ?");
            ps.setString(1, bedNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                PreparedStatement ins = conn.prepareStatement("INSERT INTO beds(bed_number, bed_type, price) VALUES(?, ?, ?)");
                ins.setString(1, bedNumber);
                ins.setString(2, bedType);
                ins.setDouble(3, price);
                ins.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void seedUsers() throws SQLException {
        createUserIfNotExists("admin", "admin123", "ADMIN", true);
        createUserIfNotExists("reception", "rec123", "RECEPTIONIST", true);
        createUserIfNotExists("doctor", "doc123", "DOCTOR", true);

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
             PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM doctors WHERE name = ?");
             ps.setString(1, "doctor");
             ResultSet rs = ps.executeQuery();
             if (rs.next() && rs.getInt(1) == 0) {
                 PreparedStatement ins = conn.prepareStatement("INSERT INTO doctors(name, specialization) VALUES(?, ?)");
                 ins.setString(1, "doctor");
                 ins.setString(2, "General Physician");
                 ins.executeUpdate();
             }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createUserIfNotExists(String u, String p, String r, boolean a) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM users WHERE username=?");
            ps.setString(1, u);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                PreparedStatement ins = conn.prepareStatement("INSERT INTO users(username,password,role,active) VALUES(?,?,?,?)");
                ins.setString(1, u);
                ins.setString(2, hashPassword(p));
                ins.setString(3, r);
                ins.setInt(4, a ? 1 : 0);
                ins.executeUpdate();
                System.out.println("Created default user: " + u);
            }
            rs.close();
            ps.close();
        }
    }

    private static void showRoleSelectionGUI() {
        JFrame frame = new JFrame("Hospital Management System");
        frame.setSize(450, 400);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(COLOR_CONTENT_BG);
        JLabel titleLabel = new JLabel("Select Your Role", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(COLOR_NAV_BAR);
        titleLabel.setBorder(new EmptyBorder(20, 0, 20, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setBackground(COLOR_CONTENT_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JButton adminButton = new JButton("Admin Login");
        JButton receptionistButton = new JButton("Receptionist Login");
        JButton doctorButton = new JButton("Doctor Login");
        JButton exitButton = new JButton("Exit Application");
        stylePrimaryButton(adminButton);
        stylePrimaryButton(receptionistButton);
        stylePrimaryButton(doctorButton);
        stylePrimaryButton(exitButton);
        gbc.gridy = 0; buttonPanel.add(adminButton, gbc);
        gbc.gridy = 1; buttonPanel.add(receptionistButton, gbc);
        gbc.gridy = 2; buttonPanel.add(doctorButton, gbc);
        gbc.gridy = 3; buttonPanel.add(exitButton, gbc);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        frame.add(mainPanel);
        frame.setVisible(true);

        adminButton.addActionListener(e -> { frame.dispose(); showLoginGUI("ADMIN"); });
        receptionistButton.addActionListener(e -> { frame.dispose(); showLoginGUI("RECEPTIONIST"); });
        doctorButton.addActionListener(e -> { frame.dispose(); showLoginGUI("DOCTOR"); });
        exitButton.addActionListener(e -> System.exit(0));
    }

    private static void showLoginGUI(String role) {
        JDialog dialog = new JDialog((Frame)null, role + " Login", true);
        dialog.setSize(420, 320);
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                Window[] windows = Window.getWindows();
                boolean anyVisible = false;
                for (Window w : windows) {
                    if (w.isVisible()) { anyVisible = true; break; }
                }
                if (!anyVisible) {
                    showRoleSelectionGUI();
                }
            }
        });

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(COLOR_CONTENT_BG);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(FONT_LABEL);
        gbc.gridx = 0; gbc.gridy = 0; panel.add(userLabel, gbc);
        JTextField userField = new JTextField(15);
        userField.setFont(FONT_LABEL);
        gbc.gridx = 1; gbc.gridy = 0; panel.add(userField, gbc);
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(FONT_LABEL);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(passLabel, gbc);
        JPasswordField passField = new JPasswordField(15);
        passField.setFont(FONT_LABEL);
        gbc.gridx = 1; gbc.gridy = 1; panel.add(passField, gbc);
        JButton backBtn = new JButton("Back");
        stylePrimaryButton(backBtn);
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.NONE; panel.add(backBtn, gbc);
        JButton loginBtn = new JButton("Login");
        stylePrimaryButton(loginBtn);
        gbc.gridx = 1; gbc.gridy = 2; panel.add(loginBtn, gbc);
        dialog.add(panel);

        loginBtn.addActionListener(e -> {
            String user = userField.getText().trim();
            String pass = new String(passField.getPassword()).trim();
            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                PreparedStatement ps = conn.prepareStatement("SELECT role,active FROM users WHERE username=? AND password=?");
                ps.setString(1, user);
                ps.setString(2, hashPassword(pass));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String dbRole = rs.getString("role");
                    int active = rs.getInt("active");
                    if (active == 0) {
                        JOptionPane.showMessageDialog(dialog, "User is deactivated. Contact admin.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                    } else if (dbRole.equals(role)) {
                        dialog.removeWindowListener(dialog.getWindowListeners()[0]);
                        dialog.dispose();
                        switch (role) {
                            case "ADMIN": showAdminPanel(user); break;
                            case "RECEPTIONIST": showReceptionistPanel(user); break;
                            case "DOCTOR": showDoctorDashboard(user); break;
                        }
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Access denied. You have the '" + dbRole + "' role, not '" + role + "'.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(dialog, "Invalid credentials.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                }
                rs.close();
                ps.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Database error: " + ex.getMessage());
            }
        });

        backBtn.addActionListener(e -> {
            dialog.dispose();
        });

        dialog.setVisible(true);
    }

    private static void loadTests(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM tests ORDER BY test_name")) {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("test_name"),
                    String.format("Rs. %.2f", rs.getDouble("price")),
                    rs.getString("description")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error loading tests: " + ex.getMessage());
        }
    }

    private static JPanel createTestManagementActionPanel(JTable table) {
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setBackground(COLOR_WHITE);
        JButton addTestBtn = new JButton("Add New Test Type");
        stylePrimaryButton(addTestBtn);
        addTestBtn.addActionListener(e -> {
            JPanel addTestPanel = new JPanel(new GridLayout(0, 2, 5, 5));
            JTextField nameField = new JTextField();
            JTextField priceField = new JTextField();
            JTextField descField = new JTextField();
            addTestPanel.add(new JLabel("Test Name:"));
            addTestPanel.add(nameField);
            addTestPanel.add(new JLabel("Price:"));
            addTestPanel.add(priceField);
            addTestPanel.add(new JLabel("Description:"));
            addTestPanel.add(descField);
            int result = JOptionPane.showConfirmDialog(null, addTestPanel, "Add New Test Type", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                String testName = nameField.getText().trim();
                if (testName.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Test Name cannot be empty.");
                    return;
                }
                try (Connection conn = DriverManager.getConnection(DB_URL)) {
                    PreparedStatement ins = conn.prepareStatement("INSERT INTO tests(test_name,price,description) VALUES(?,?,?)");
                    ins.setString(1, testName);
                    ins.setDouble(2, parseDoubleSafe(priceField.getText().trim()));
                    ins.setString(3, descField.getText().trim());
                    ins.executeUpdate();
                    JOptionPane.showMessageDialog(null, "Test added successfully!");
                    loadTests(table);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(null, "Error adding test: " + ex.getMessage());
                }
            }
        });
        actionPanel.add(addTestBtn);
        return actionPanel;
    }

    private static void loadDoctors(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM doctors")) {
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("id"), rs.getString("name"), rs.getString("specialization"), rs.getString("phone"), rs.getString("email")});
            }
        } catch (SQLException ex) { JOptionPane.showMessageDialog(null, "Error loading doctors: " + ex.getMessage()); }
    }

    private static void loadPatients(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM patients")) {
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("id"), rs.getString("name"), rs.getInt("age"), rs.getString("gender"), rs.getString("phone"), rs.getString("address"), rs.getString("medical_history")});
            }
        } catch (SQLException ex) { JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage()); }
    }

    private static void loadAppointments(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        String sql = "SELECT a.id, p.name as patient_name, a.patient_id, d.name as doctor_name, a.appointment_date, a.time_slot, a.status FROM appointments a LEFT JOIN patients p ON a.patient_id=p.id LEFT JOIN doctors d ON a.doctor_id=d.id ORDER BY a.appointment_date DESC, a.time_slot DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("id"), rs.getString("patient_name"), rs.getInt("patient_id"), rs.getString("doctor_name"), rs.getString("appointment_date"), rs.getString("time_slot"), rs.getString("status")});
            }
        } catch (SQLException e) { JOptionPane.showMessageDialog(null, "Error loading appointments: " + e.getMessage()); }
    }

    private static void loadBedsAdmin(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        String sql = "SELECT id, bed_number, bed_type, price, status FROM beds ORDER BY bed_number";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("id"), rs.getString("bed_number"), rs.getString("bed_type"), String.format("Rs. %.2f", rs.getDouble("price")), rs.getString("status")});
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error loading beds: " + ex.getMessage());
        }
    }

    private static void loadBedsReceptionist(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        String sql = "SELECT b.id, b.bed_number, b.bed_type, b.price, b.status, b.patient_id, p.name as patient_name FROM beds b LEFT JOIN patients p ON b.patient_id = p.id ORDER BY b.bed_number";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("bed_number"),
                    rs.getString("bed_type"),
                    String.format("Rs. %.2f", rs.getDouble("price")),
                    rs.getString("status"),
                    rs.getObject("patient_id") == null ? "N/A" : rs.getInt("patient_id"),
                    rs.getString("patient_name") == null ? "N/A" : rs.getString("patient_name")
                });
            }

            // --- FIX: Remove old listeners before adding a new one to prevent multiple menus ---
            for (MouseListener listener : table.getMouseListeners()) {
                if (listener.getClass().getName().contains("loadBedsReceptionist")) {
                    table.removeMouseListener(listener);
                }
            }

            table.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        int row = table.rowAtPoint(e.getPoint());
                        if (row >= 0) {
                            table.setRowSelectionInterval(row, row);
                            JPopupMenu menu = new JPopupMenu();
                            int modelRow = table.convertRowIndexToModel(row);
                            String status = (String) table.getModel().getValueAt(modelRow, 4);
                            int bedId = (int) table.getModel().getValueAt(modelRow, 0);
                            if ("Available".equalsIgnoreCase(status)) {
                                JMenuItem assignItem = new JMenuItem("Assign Patient");
                                assignItem.addActionListener(ae -> assignPatientToBed(bedId, table));
                                menu.add(assignItem);
                            } else if ("Occupied".equalsIgnoreCase(status)) {
                                JMenuItem vacateItem = new JMenuItem("Vacate Bed");
                                vacateItem.addActionListener(ae -> vacateBed(bedId, table));
                                menu.add(vacateItem);
                            }
                            menu.show(table, e.getX(), e.getY());
                        }
                    }
                }
            });
        } catch (SQLException ex) { JOptionPane.showMessageDialog(null, "Error loading beds: " + ex.getMessage()); }
    }

    private static void loadUsers(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT id, username, role, active FROM users");
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("id"), rs.getString("username"), rs.getString("role"), rs.getInt("active")});
            }
        } catch (SQLException ex) { JOptionPane.showMessageDialog(null, "Error loading users: " + ex.getMessage()); }
    }

    private static void loadRevenueByMonth(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        String sql = "SELECT strftime('%Y-%m', created_at) as month, SUM(amount) as revenue, COUNT(*) as bills_count FROM bills GROUP BY month ORDER BY month DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{rs.getString("month"), String.format("Rs. %.2f", rs.getDouble("revenue")), rs.getInt("bills_count")});
            }
        } catch (SQLException ex) { JOptionPane.showMessageDialog(null, "Error generating report: " + ex.getMessage()); }
    }

    private static void loadTopDoctors(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        String sql = "SELECT d.id, d.name, d.specialization, COUNT(a.id) as appointments FROM doctors d LEFT JOIN appointments a ON d.id = a.doctor_id GROUP BY d.id ORDER BY appointments DESC LIMIT 20";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("id"), rs.getString("name"), rs.getString("specialization"), rs.getInt("appointments")});
            }
        } catch (SQLException ex) { JOptionPane.showMessageDialog(null, "Error generating report: " + ex.getMessage()); }
    }

    private static void loadAppointmentsForDoctor(JTable table, int doctorId) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String sql = "SELECT a.id, p.name as patient_name, a.patient_id, a.time_slot, a.status, a.remarks " +
                     "FROM appointments a JOIN patients p ON a.patient_id = p.id " +
                     "WHERE a.doctor_id = ? AND a.appointment_date = ? ORDER BY a.time_slot";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ps.setString(2, today);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("id"), rs.getString("patient_name"), rs.getInt("patient_id"), rs.getString("time_slot"), rs.getString("status"), rs.getString("remarks")});
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error loading appointments: " + ex.getMessage());
        }
    }

    private static void generateBill(Frame owner) {
        String patientIdStr = JOptionPane.showInputDialog(owner, "Enter Patient ID to generate bill for:", "Generate Bill", JOptionPane.PLAIN_MESSAGE);
        if (patientIdStr == null || patientIdStr.trim().isEmpty()) return;

        int pid = parseIntSafe(patientIdStr);
        if (pid == 0) {
            JOptionPane.showMessageDialog(owner, "Invalid Patient ID");
            return;
        }

        JDialog billDialog = new JDialog(owner, "Generate Bill for Patient ID: " + pid, true);
        billDialog.setSize(500, 400);
        billDialog.setLocationRelativeTo(owner);
        billDialog.setLayout(new BorderLayout(10, 10));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField itemField = new JTextField(20);
        JTextField costField = new JTextField(8);
        JButton addButton = new JButton("Add Item");
        stylePrimaryButton(addButton);
        inputPanel.add(new JLabel("Item/Product Name:"));
        inputPanel.add(itemField);
        inputPanel.add(new JLabel("Cost:"));
        inputPanel.add(costField);
        inputPanel.add(addButton);

        JTable itemsTable = new JTable(new DefaultTableModel(new String[]{"Item", "Cost"}, 0));
        itemsTable.setFont(FONT_LABEL);

        JPanel southPanel = new JPanel(new BorderLayout());
        JLabel totalLabel = new JLabel("Total: Rs. 0.00");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        totalLabel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton generateButton = new JButton("Generate Final Bill");
        stylePrimaryButton(generateButton);
        southPanel.add(totalLabel, BorderLayout.CENTER);
        southPanel.add(generateButton, BorderLayout.EAST);

        billDialog.add(inputPanel, BorderLayout.NORTH);
        billDialog.add(new JScrollPane(itemsTable), BorderLayout.CENTER);
        billDialog.add(southPanel, BorderLayout.SOUTH);

        final double[] total = {0.0};
        DefaultTableModel model = (DefaultTableModel) itemsTable.getModel();

        addButton.addActionListener(e -> {
            String item = itemField.getText().trim();
            double cost = parseDoubleSafe(costField.getText().trim());
            if (!item.isEmpty() && cost > 0) {
                model.addRow(new Object[]{item, String.format("%.2f", cost)});
                total[0] += cost;
                totalLabel.setText(String.format("Total: Rs. %.2f", total[0]));
                itemField.setText("");
                costField.setText("");
                itemField.requestFocus();
            } else {
                JOptionPane.showMessageDialog(billDialog, "Please enter a valid item name and a positive cost.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        generateButton.addActionListener(e -> {
            if (model.getRowCount() == 0) {
                JOptionPane.showMessageDialog(billDialog, "No items added to the bill.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            StringBuilder details = new StringBuilder();
            for (int i = 0; i < model.getRowCount(); i++) {
                details.append(model.getValueAt(i, 0)).append(": ").append(model.getValueAt(i, 1)).append("\n");
            }

            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                PreparedStatement ps = conn.prepareStatement("INSERT INTO bills(patient_id,amount,details,created_at) VALUES(?,?,?,datetime('now'))");
                ps.setInt(1, pid);
                ps.setDouble(2, total[0]);
                ps.setString(3, details.toString());
                ps.executeUpdate();
                JOptionPane.showMessageDialog(owner, "Bill created successfully! Total: " + String.format("Rs. %.2f", total[0]));
                billDialog.dispose();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(billDialog, "Error creating bill: " + ex.getMessage());
            }
        });

        billDialog.setVisible(true);
    }

    private static void assignPatientToBed(int bedId, JTable table) {
        String patientIdStr = JOptionPane.showInputDialog(null, "Enter Patient ID to assign to bed:", "Assign Patient", JOptionPane.PLAIN_MESSAGE);
        if (patientIdStr == null || patientIdStr.trim().isEmpty()) return;
        int patientId = parseIntSafe(patientIdStr.trim());
        if (patientId == 0) {
            JOptionPane.showMessageDialog(null, "Invalid Patient ID.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement checkPs = conn.prepareStatement("SELECT COUNT(*) FROM patients WHERE id = ?");
            checkPs.setInt(1, patientId);
            ResultSet rs = checkPs.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                JOptionPane.showMessageDialog(null, "Patient with ID " + patientId + " does not exist.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            PreparedStatement ps = conn.prepareStatement("UPDATE beds SET status = 'Occupied', patient_id = ? WHERE id = ?");
            ps.setInt(1, patientId);
            ps.setInt(2, bedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Bed assigned successfully.");
            loadBedsReceptionist(table);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error assigning bed: " + ex.getMessage());
        }
    }

    private static void vacateBed(int bedId, JTable table) {
        int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to make this bed available?", "Confirm Vacate", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement("UPDATE beds SET status = 'Available', patient_id = NULL WHERE id = ?");
            ps.setInt(1, bedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Bed is now available.");
            loadBedsReceptionist(table);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error vacating bed: " + ex.getMessage());
        }
    }

    private static void bookAppointment(Frame owner) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Patient ID:"));
        gbc.gridx = 1; gbc.gridy = 0; JTextField patientIdField = new JTextField(15); panel.add(patientIdField);

        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Doctor ID:"));
        gbc.gridx = 1; gbc.gridy = 1; JTextField doctorIdField = new JTextField(15); panel.add(doctorIdField);

        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Date:"));
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel dateLabel = new JLabel("YYYY-MM-DD");
        dateLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        JButton dateButton = new JButton("Select...");
        datePanel.add(dateLabel); datePanel.add(dateButton);
        gbc.gridx = 1; gbc.gridy = 2; panel.add(datePanel);

        dateButton.addActionListener(e -> {
            String selectedDate = selectDate(owner);
            if (selectedDate != null) {
                dateLabel.setText(selectedDate);
                dateLabel.setFont(FONT_LABEL);
            }
        });

        gbc.gridx = 0; gbc.gridy = 3; panel.add(new JLabel("Time Slot (e.g., 10:00-10:30):"));
        gbc.gridx = 1; gbc.gridy = 3; JTextField slotField = new JTextField(15); panel.add(slotField);

        gbc.gridx = 0; gbc.gridy = 4; panel.add(new JLabel("Remarks:"));
        gbc.gridx = 1; gbc.gridy = 4; JTextField remarksField = new JTextField(15); panel.add(remarksField);

        int result = JOptionPane.showConfirmDialog(owner, panel, "Book Appointment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            int pid = parseIntSafe(patientIdField.getText().trim());
            int did = parseIntSafe(doctorIdField.getText().trim());
            String date = dateLabel.getText();
            String slot = slotField.getText().trim();

            if (pid == 0 || did == 0 || date.equals("YYYY-MM-DD") || slot.isEmpty()) {
                JOptionPane.showMessageDialog(owner, "Patient ID, Doctor ID, Date, and Time Slot are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                PreparedStatement check = conn.prepareStatement("SELECT count(*) FROM appointments WHERE doctor_id=? AND appointment_date=? AND time_slot=? AND status='SCHEDULED'");
                check.setInt(1, did); check.setString(2, date); check.setString(3, slot);
                ResultSet rs = check.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    JOptionPane.showMessageDialog(owner, "This time slot is already booked for the selected doctor.", "Booking Failed", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                PreparedStatement ps = conn.prepareStatement("INSERT INTO appointments(patient_id,doctor_id,appointment_date,time_slot,remarks) VALUES(?,?,?,?,?)");
                ps.setInt(1, pid); ps.setInt(2, did); ps.setString(3, date); ps.setString(4, slot); ps.setString(5, remarksField.getText().trim());
                ps.executeUpdate();
                JOptionPane.showMessageDialog(owner, "Appointment scheduled successfully.");

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(owner, "Database Error: " + e.getMessage());
            }
        }
    }

    private static void bookTest() {
        String patientIdStr = JOptionPane.showInputDialog(null, "Enter Patient ID:", "Book Test - Step 1 of 2", JOptionPane.PLAIN_MESSAGE);
        if (patientIdStr == null || patientIdStr.trim().isEmpty()) return;

        int pid = parseIntSafe(patientIdStr.trim());
        if (pid == 0) {
            JOptionPane.showMessageDialog(null, "Invalid Patient ID format.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement psCheck = conn.prepareStatement("SELECT COUNT(*) FROM patients WHERE id = ?");
            psCheck.setInt(1, pid);
            ResultSet rs = psCheck.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                JOptionPane.showMessageDialog(null, "Error: Patient with ID " + pid + " does not exist.", "Patient Not Found", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Database Error: " + ex.getMessage());
            return;
        }

        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        Map<JCheckBox, Integer> checkBoxToTestIdMap = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, test_name, price FROM tests ORDER BY test_name")) {

            if (!rs.isBeforeFirst()) {
                checkboxPanel.add(new JLabel("No tests found in the database."));
            } else {
                while (rs.next()) {
                    int testId = rs.getInt("id");
                    String testName = rs.getString("test_name");
                    double price = rs.getDouble("price");
                    String checkBoxText = String.format("%s (Rs. %.2f)", testName, price);

                    JCheckBox checkBox = new JCheckBox(checkBoxText);
                    checkBox.setFont(FONT_LABEL);
                    checkBoxToTestIdMap.put(checkBox, testId);
                    checkboxPanel.add(checkBox);
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error fetching tests: " + ex.getMessage());
            return;
        }

        JScrollPane scrollPane = new JScrollPane(checkboxPanel);
        scrollPane.setPreferredSize(new Dimension(350, 200));

        int result = JOptionPane.showConfirmDialog(null, scrollPane, "Select Tests for Patient ID: " + pid + " - Step 2 of 2", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            java.util.List<Integer> selectedTestIds = new ArrayList<>();
            for (Map.Entry<JCheckBox, Integer> entry : checkBoxToTestIdMap.entrySet()) {
                if (entry.getKey().isSelected()) {
                    selectedTestIds.add(entry.getValue());
                }
            }

            if (selectedTestIds.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No tests were selected.", "Information", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                conn.setAutoCommit(false);

                String sql = "INSERT INTO booked_tests(patient_id, test_id, booked_date) VALUES(?, ?, datetime('now'))";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (Integer testId : selectedTestIds) {
                        ps.setInt(1, pid);
                        ps.setInt(2, testId);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                    conn.commit();

                    JOptionPane.showMessageDialog(null, selectedTestIds.size() + " test(s) booked successfully for patient ID " + pid);

                } catch (SQLException e) {
                    conn.rollback();
                    JOptionPane.showMessageDialog(null, "Error booking tests: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Database connection error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void promptAndShowPatientDashboard() {
        String patientIdStr = JOptionPane.showInputDialog(null, "Enter Patient ID:", "Open Patient Dashboard", JOptionPane.PLAIN_MESSAGE);
        if (patientIdStr == null || patientIdStr.trim().isEmpty()) return;
        int pid = parseIntSafe(patientIdStr.trim());
        if (pid == 0) { JOptionPane.showMessageDialog(null, "Invalid Patient ID."); return; }

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement("SELECT name FROM patients WHERE id = ?");
            ps.setInt(1, pid);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                JOptionPane.showMessageDialog(null, "Patient with ID " + pid + " not found.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String patientName = rs.getString("name");
            showPatientDashboard(pid, patientName);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Database error: " + ex.getMessage());
        }
    }

    private static void showPatientDashboard(int patientId, String patientName) {
        JDialog dialog = new JDialog((Frame) null, "Dashboard for " + patientName + " (ID: " + patientId + ")", true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(null);
        JTabbedPane tabs = new JTabbedPane();

        JTable apptTable = createStyledTable();
        apptTable.setModel(new DefaultTableModel(new String[]{"Appt ID", "Doctor", "Date", "Time", "Status", "Remarks"}, 0));
        JTable billTable = createStyledTable();
        billTable.setModel(new DefaultTableModel(new String[]{"Bill ID", "Amount", "Details", "Date"}, 0));
        JTable testsTable = createStyledTable();
        testsTable.setModel(new DefaultTableModel(new String[]{"Booked ID", "Test", "Price", "Booked Date", "Status", "Result"}, 0));

        tabs.addTab("Appointments", new JScrollPane(apptTable));
        tabs.addTab("Bills", new JScrollPane(billTable));
        tabs.addTab("Lab Tests", new JScrollPane(testsTable));

        loadPatientAppointments(patientId, apptTable);
        loadPatientBills(patientId, billTable);
        loadPatientTests(patientId, testsTable);

        dialog.add(tabs);
        dialog.setVisible(true);
    }

    private static void showPatientHistory(Frame owner, int patientId) {
        String history = "No medical history found.";
        String patientName = "";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement("SELECT name, medical_history FROM patients WHERE id = ?")) {
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                patientName = rs.getString("name");
                history = rs.getString("medical_history");
                if (history == null || history.trim().isEmpty()) {
                    history = "No medical history on record.";
                }
            }
        } catch (SQLException e) {
            history = "Error fetching medical history.";
            e.printStackTrace();
        }

        JTextArea textArea = new JTextArea(history);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        textArea.setFont(FONT_LABEL);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 250));

        JOptionPane.showMessageDialog(owner, scrollPane, "Medical History for " + patientName, JOptionPane.INFORMATION_MESSAGE);
    }

    private static void updateTestResult(int bookedTestId, String result) {
        String sql = "UPDATE booked_tests SET result = ?, status = 'COMPLETED' WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, result);
            ps.setInt(2, bookedTestId);
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(null, "Test result updated successfully.");
            } else {
                JOptionPane.showMessageDialog(null, "Could not find the booked test to update.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Database error while updating test result: " + ex.getMessage());
        }
    }

    private static void loadPatientAppointments(int pid, JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement("SELECT a.id, d.name as doctor_name, a.appointment_date, a.time_slot, a.status, a.remarks FROM appointments a JOIN doctors d ON a.doctor_id=d.id WHERE a.patient_id=? ORDER BY a.appointment_date DESC");
            ps.setInt(1, pid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("id"), rs.getString("doctor_name"), rs.getString("appointment_date"), rs.getString("time_slot"), rs.getString("status"), rs.getString("remarks")});
            }
        } catch (SQLException ex) { JOptionPane.showMessageDialog(null, "Error loading patient appointments: " + ex.getMessage()); }
    }

    private static void loadPatientBills(int pid, JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement("SELECT id, amount, details, created_at FROM bills WHERE patient_id=? ORDER BY created_at DESC");
            ps.setInt(1, pid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("id"), String.format("Rs. %.2f", rs.getDouble("amount")), rs.getString("details"), rs.getString("created_at")});
            }
        } catch (SQLException ex) { JOptionPane.showMessageDialog(null, "Error loading patient bills: " + ex.getMessage()); }
    }

    private static void loadPatientTests(int pid, JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement("SELECT bt.id, t.test_name, t.price, bt.booked_date, bt.status, bt.result FROM booked_tests bt JOIN tests t ON bt.test_id = t.id WHERE bt.patient_id = ? ORDER BY bt.booked_date DESC");
            ps.setInt(1, pid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("id"), rs.getString("test_name"), String.format("Rs. %.2f", rs.getDouble("price")), rs.getString("booked_date"), rs.getString("status"), rs.getString("result")});
            }
        } catch (SQLException ex) { JOptionPane.showMessageDialog(null, "Error loading patient tests: " + ex.getMessage()); }
    }

    // --- Utility Methods ---
    private static int getDoctorIdByName(String name) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM doctors WHERE name = ?")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Not found
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    private static double parseDoubleSafe(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0.0; }
    }

    private static String hashPassword(String password) {
        if (password == null || password.isEmpty()) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private static String selectDate(Frame owner) {
        JDialog dialog = new JDialog(owner, "Select Date", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(300, 300);
        dialog.setLocationRelativeTo(owner);

        final String[] selectedDate = {null};
        Calendar calendar = Calendar.getInstance();

        JPanel header = new JPanel(new BorderLayout());
        JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JButton prevMonth = new JButton("<");
        JButton nextMonth = new JButton(">");

        header.add(prevMonth, BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(nextMonth, BorderLayout.EAST);

        JPanel daysPanel = new JPanel(new GridLayout(0, 7, 2, 2));

        Runnable updateCalendar = () -> {
            daysPanel.removeAll();
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy");
            monthLabel.setText(sdf.format(calendar.getTime()));

            String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            for (String dayName : dayNames) {
                daysPanel.add(new JLabel(dayName, SwingConstants.CENTER));
            }

            Calendar tempCal = (Calendar) calendar.clone();
            tempCal.set(Calendar.DAY_OF_MONTH, 1);
            int firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK);
            int daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);

            for (int i = 1; i < firstDayOfWeek; i++) {
                daysPanel.add(new JLabel(""));
            }

            for (int i = 1; i <= daysInMonth; i++) {
                JButton dayButton = new JButton(String.valueOf(i));
                int day = i;
                dayButton.addActionListener(e -> {
                    calendar.set(Calendar.DAY_OF_MONTH, day);
                    selectedDate[0] = new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());
                    dialog.dispose();
                });
                daysPanel.add(dayButton);
            }
            daysPanel.revalidate();
            daysPanel.repaint();
        };

        prevMonth.addActionListener(e -> {
            calendar.add(Calendar.MONTH, -1);
            updateCalendar.run();
        });

        nextMonth.addActionListener(e -> {
            calendar.add(Calendar.MONTH, 1);
            updateCalendar.run();
        });

        updateCalendar.run();

        dialog.add(header, BorderLayout.NORTH);
        dialog.add(daysPanel, BorderLayout.CENTER);

        dialog.setVisible(true);
        return selectedDate[0];
    }

    private static JPanel createBedManagementActionPanel(JTable table) {
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setBackground(COLOR_WHITE);
        JButton addBedBtn = new JButton("Add New Bed");
        stylePrimaryButton(addBedBtn);
        addBedBtn.addActionListener(e -> {
            JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
            JTextField numberField = new JTextField();
            JTextField typeField = new JTextField();
            JTextField priceField = new JTextField();
            panel.add(new JLabel("Bed Number (e.g., GEN-102):")); panel.add(numberField);
            panel.add(new JLabel("Bed Type (e.g., General Ward):")); panel.add(typeField);
            panel.add(new JLabel("Price per Day:")); panel.add(priceField);

            int result = JOptionPane.showConfirmDialog(null, panel, "Add New Bed", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                String bedNumber = numberField.getText().trim();
                String bedType = typeField.getText().trim();
                double price = parseDoubleSafe(priceField.getText().trim());

                if (bedNumber.isEmpty() || bedType.isEmpty() || price <= 0) {
                    JOptionPane.showMessageDialog(null, "All fields are required and price must be positive.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try (Connection conn = DriverManager.getConnection(DB_URL)) {
                    PreparedStatement ps = conn.prepareStatement("INSERT INTO beds(bed_number, bed_type, price) VALUES(?, ?, ?)");
                    ps.setString(1, bedNumber);
                    ps.setString(2, bedType);
                    ps.setDouble(3, price);
                    ps.executeUpdate();
                    JOptionPane.showMessageDialog(null, "Bed added successfully.");
                    loadBedsAdmin(table);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(null, "Error adding bed (bed number might already exist): " + ex.getMessage());
                }
            }
        });
        actionPanel.add(addBedBtn);
        return actionPanel;
    }

    private static void addAppointmentCancellationMenu(JTable table) {
        // --- FIX: Remove any old listeners to ensure the new one is always active ---
        for (MouseListener listener : table.getMouseListeners()) {
            if (listener.getClass().getName().contains(HospitalManagementGUI.class.getName())) {
                table.removeMouseListener(listener);
            }
        }

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row);
                        JPopupMenu menu = new JPopupMenu();
                        JMenuItem cancelItem = new JMenuItem("Cancel Appointment");

                        int modelRow = table.convertRowIndexToModel(row);
                        int apptId = (int) table.getModel().getValueAt(modelRow, 0);
                        Object statusObj = table.getModel().getValueAt(modelRow, table.getModel().getColumnCount() - 1);
                        String currentStatus = (statusObj != null) ? statusObj.toString() : "";

                        if ("SCHEDULED".equalsIgnoreCase(currentStatus)) {
                            cancelItem.addActionListener(ae -> {
                                int confirm = JOptionPane.showConfirmDialog(table, "Are you sure you want to cancel this appointment?", "Confirm Cancellation", JOptionPane.YES_NO_OPTION);
                                if (confirm == JOptionPane.YES_OPTION) {
                                    updateAppointmentStatus(apptId, "CANCELLED");
                                    ((DefaultTableModel) table.getModel()).setValueAt("CANCELLED", modelRow, table.getModel().getColumnCount() - 1);
                                }
                            });
                            menu.add(cancelItem);
                            menu.show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                }
            }
        });
    }


    private static void updateAppointmentStatus(int apptId, String newStatus) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement ps = conn.prepareStatement("UPDATE appointments SET status = ? WHERE id = ?");
            ps.setString(1, newStatus);
            ps.setInt(2, apptId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error updating appointment status: " + ex.getMessage());
        }
    }
}

