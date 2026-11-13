package klinik;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ClinicFrame extends JFrame {
    private final ClinicManager manager = new ClinicManager();

    // GUI Components (Week 13)
    private final JTextField txtId = new JTextField();
    private final JTextField txtName = new JTextField();
    private final JTextField txtAge = new JTextField();
    private final JComboBox<String> cmbPoli = new JComboBox<>(new String[]{
            "Poli Umum", "Poli Gigi", "Poli Anak", "Poli Mata"
    });
    private final JTextField txtComplaint = new JTextField();
    private final JCheckBox chkBpjs = new JCheckBox("Gunakan BPJS Kesehatan (Subsidi 80%)");
    private final JTextField txtBpjsNo = new JTextField();
    private final JTextField txtSearch = new JTextField(10);

    // Label peringatan merah di bawah tiap kolom
    private final JLabel lblIdWarn = new JLabel(" ");
    private final JLabel lblNameWarn = new JLabel(" ");
    private final JLabel lblAgeWarn = new JLabel(" ");
    private final JLabel lblBpjsWarn = new JLabel(" ");

    // Border normal vs border merah terang (kompatibel dengan macOS)
    private final Border normalBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 185, 190), 1),
            BorderFactory.createEmptyBorder(3, 6, 3, 6)
    );
    private final Border errorBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.RED, 2),
            BorderFactory.createEmptyBorder(2, 5, 2, 5)
    );

    // WEEK 13: Custom ImageIcon untuk pop-up Berhasil (Centang Hijau) & Gagal (Tanda Seru)
    private final ImageIcon successIcon = createSuccessIcon();
    private final ImageIcon warningIcon = createWarningIcon();

    private final JTextArea areaQueue = new JTextArea();
    private final JTextArea areaHistory = new JTextArea();
    private final JTextArea areaLog = new JTextArea(5, 40);
    private final JLabel lblClockAndThread = new JLabel("Memuat jam sistem...");

    private volatile boolean runningThread = true;

    public ClinicFrame() {
        setTitle("Sistem Antrean & Administrasi Klinik SehatKita");
        setSize(1140, 760);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        // WEEK 13: BorderLayout as main container layout
        setLayout(new BorderLayout(10, 10));

        initComponents();
        setupFocusAndLiveValidation();
        loadInitialData();
        startBackgroundThread(); // WEEK 14: Launch Multithreading
    }

    // Membuat ImageIcon Centang Hijau untuk notifikasi berhasil
    private ImageIcon createSuccessIcon() {
        int size = 44;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Lingkaran hijau
        g2.setColor(new Color(22, 163, 74));
        g2.fillOval(2, 2, size - 4, size - 4);

        // Tanda centang putih
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(12, 23, 19, 30);
        g2.drawLine(19, 30, 32, 15);
        g2.dispose();
        return new ImageIcon(img);
    }

    // Membuat ImageIcon Tanda Seru untuk notifikasi gagal / peringatan
    private ImageIcon createWarningIcon() {
        int size = 44;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Lingkaran merah peringatan
        g2.setColor(new Color(220, 38, 38));
        g2.fillOval(2, 2, size - 4, size - 4);

        // Tanda seru putih (!)
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(4.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(22, 12, 22, 25);
        g2.fillOval(19, 30, 6, 6);
        g2.dispose();
        return new ImageIcon(img);
    }

    private void initComponents() {
        Color clinicGreen = new Color(16, 124, 65);

        // 1. NORTH PANEL: Header & Live Thread Status
        JPanel headerPanel = new JPanel(new BorderLayout(20, 0));
        headerPanel.setBackground(clinicGreen);
        headerPanel.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel lblTitle = new JLabel("Antrean & Rekam Medis");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblTitle.setForeground(Color.WHITE);

        lblClockAndThread.setFont(new Font("Monospaced", Font.BOLD, 12));
        lblClockAndThread.setForeground(new Color(220, 248, 232));

        headerPanel.add(lblTitle, BorderLayout.WEST);
        headerPanel.add(lblClockAndThread, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // 2. WEST PANEL: Patient Registration Form using GridLayout
        JPanel formPanel = new JPanel(new GridLayout(0, 1, 2, 2));
        formPanel.setBorder(new TitledBorder("Form Pendaftaran Pasien Baru"));
        formPanel.setPreferredSize(new Dimension(340, 0));

        txtId.setBorder(normalBorder);
        txtName.setBorder(normalBorder);
        txtAge.setBorder(normalBorder);
        txtComplaint.setBorder(normalBorder);
        txtBpjsNo.setBorder(normalBorder);

        Font warnFont = new Font("SansSerif", Font.BOLD, 11);
        lblIdWarn.setForeground(Color.RED);
        lblIdWarn.setFont(warnFont);
        lblNameWarn.setForeground(Color.RED);
        lblNameWarn.setFont(warnFont);
        lblAgeWarn.setForeground(Color.RED);
        lblAgeWarn.setFont(warnFont);
        lblBpjsWarn.setForeground(Color.RED);
        lblBpjsWarn.setFont(warnFont);

        formPanel.add(new JLabel("ID Pasien (angka saja, contoh: 001):"));
        formPanel.add(txtId);
        formPanel.add(lblIdWarn);

        formPanel.add(new JLabel("Nama Lengkap Pasien (HURUF KAPITAL):"));
        formPanel.add(txtName);
        formPanel.add(lblNameWarn);

        formPanel.add(new JLabel("Umur (Tahun):"));
        formPanel.add(txtAge);
        formPanel.add(lblAgeWarn);

        formPanel.add(new JLabel("Pilih Poliklinik Tujuan:"));
        formPanel.add(cmbPoli);
        formPanel.add(new JLabel("Keluhan Medis:"));
        formPanel.add(txtComplaint);
        formPanel.add(chkBpjs);
        formPanel.add(new JLabel("Nomor Kartu BPJS (13 digit angka):"));
        txtBpjsNo.setEnabled(false);
        formPanel.add(txtBpjsNo);
        formPanel.add(lblBpjsWarn);

        JButton btnAdd = new JButton("1. Tambah ke Antrean");
        btnAdd.setBackground(clinicGreen);
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setOpaque(true);
        btnAdd.setBorderPainted(false);
        btnAdd.setFont(new Font("SansSerif", Font.BOLD, 13));
        formPanel.add(btnAdd);

        add(formPanel, BorderLayout.WEST);

        // Toggle BPJS field
        chkBpjs.addActionListener(e -> {
            boolean selected = chkBpjs.isSelected();
            txtBpjsNo.setEnabled(selected);
            if (selected) {
                txtBpjsNo.requestFocus();
            } else {
                txtBpjsNo.setText("");
                txtBpjsNo.setBorder(normalBorder);
                lblBpjsWarn.setText(" ");
            }
        });

        // 3. CENTER PANEL: Split view for Live Queue (FIFO) and Master Patient History
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 10));

        areaQueue.setEditable(false);
        areaQueue.setFont(new Font("Monospaced", Font.PLAIN, 12));
        areaQueue.setBackground(new Color(244, 251, 247));
        JScrollPane scrollQueue = new JScrollPane(areaQueue);
        scrollQueue.setBorder(new TitledBorder("Antrean Tunggu Saat Ini (Queue - FIFO)"));

        areaHistory.setEditable(false);
        areaHistory.setFont(new Font("Monospaced", Font.PLAIN, 12));
        areaHistory.setBackground(new Color(244, 251, 247));
        JScrollPane scrollHistory = new JScrollPane(areaHistory);
        scrollHistory.setBorder(new TitledBorder("Riwayat Seluruh Pasien (ArrayList)"));

        centerPanel.add(scrollQueue);
        centerPanel.add(scrollHistory);
        add(centerPanel, BorderLayout.CENTER);

        // 4. SOUTH PANEL: Action Buttons (FlowLayout) + Thread-Safe Activity Log
        JPanel southContainer = new JPanel(new BorderLayout(5, 5));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        JButton btnCallNext = new JButton("2. Panggil Pasien & Cetak Struk (.txt)");
        JButton btnStats = new JButton("3. Statistik & Urutkan Tagihan");
        JButton btnSave = new JButton("4. Simpan DB (.ser)");
        JLabel lblSearch = new JLabel("Cari ID/Nama:");
        JButton btnSearch = new JButton("Cari");

        buttonPanel.add(btnCallNext);
        buttonPanel.add(btnStats);
        buttonPanel.add(btnSave);
        JButton btnReset = new JButton("Reset Data");
        buttonPanel.add(btnReset);
        btnReset.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Hapus seluruh riwayat dan antrean pasien?", "Konfirmasi Reset",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, warningIcon);
            if (confirm == JOptionPane.YES_OPTION) {
                manager.resetDatabase();
                refreshDisplay();
                JOptionPane.showMessageDialog(this, "Data berhasil direset menjadi 0!",
                        "Reset Berhasil", JOptionPane.INFORMATION_MESSAGE, successIcon);
            }
        });
        buttonPanel.add(lblSearch);
        buttonPanel.add(txtSearch);
        buttonPanel.add(btnSearch);

        areaLog.setEditable(false);
        areaLog.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane scrollLog = new JScrollPane(areaLog);
        scrollLog.setBorder(new TitledBorder("Log Aktivitas Thread-Safe (StringBuffer)"));

        southContainer.add(buttonPanel, BorderLayout.NORTH);
        southContainer.add(scrollLog, BorderLayout.CENTER);
        add(southContainer, BorderLayout.SOUTH);

        // WEEK 13: Event Handling with ActionListener
        btnAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleRegisterPatient();
            }
        });

        btnCallNext.addActionListener(e -> handleCallNextPatient());
        btnStats.addActionListener(e -> JOptionPane.showMessageDialog(this,
                manager.getClinicStatistics(), "Statistik Klinik (Collections.sort/max/min)",
                JOptionPane.INFORMATION_MESSAGE, successIcon));

        btnSave.addActionListener(e -> {
            try {
                manager.saveDatabaseToFile();
                refreshDisplay();
                JOptionPane.showMessageDialog(this, "Database berhasil disimpan ke clinic_data.ser!",
                        "Penyimpanan Berhasil", JOptionPane.INFORMATION_MESSAGE, successIcon);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Gagal menyimpan file: " + ex.getMessage(),
                        "I/O Error", JOptionPane.ERROR_MESSAGE, warningIcon);
            }
        });

        btnSearch.addActionListener(e -> handleSearch());
    }

    // Validasi otomatis saat kursor keluar dari kolom (focusLost) & saat isi berubah
    private void setupFocusAndLiveValidation() {
        // 1. ID Pasien: wajib angka saja
        Runnable validateId = () -> {
            String val = txtId.getText().trim();
            if (!val.isEmpty() && !val.matches("\\d+")) {
                txtId.setBorder(errorBorder);
                lblIdWarn.setText("Peringatan: ID Pasien harus berupa angka!");
            } else {
                txtId.setBorder(normalBorder);
                lblIdWarn.setText(" ");
            }
        };
        txtId.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) { validateId.run(); }
        });
        txtId.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { validateId.run(); }
            public void removeUpdate(DocumentEvent e) { validateId.run(); }
            public void changedUpdate(DocumentEvent e) { validateId.run(); }
        });

        // 2. Nama Lengkap: wajib huruf KAPITAL semua
        Runnable validateName = () -> {
            String val = txtName.getText();
            if (!val.isEmpty() && !val.equals(val.toUpperCase())) {
                txtName.setBorder(errorBorder);
                lblNameWarn.setText("Peringatan: Nama ditulis dengan huruf KAPITAL!");
            } else {
                txtName.setBorder(normalBorder);
                lblNameWarn.setText(" ");
            }
        };
        txtName.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) { validateName.run(); }
        });
        txtName.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { validateName.run(); }
            public void removeUpdate(DocumentEvent e) { validateName.run(); }
            public void changedUpdate(DocumentEvent e) { validateName.run(); }
        });

        // 3. Umur: wajib bentuk angka saja
        Runnable validateAge = () -> {
            String val = txtAge.getText().trim();
            if (!val.isEmpty() && !val.matches("\\d+")) {
                txtAge.setBorder(errorBorder);
                lblAgeWarn.setText("Peringatan: Umur harus dalam bentuk angka!");
            } else {
                txtAge.setBorder(normalBorder);
                lblAgeWarn.setText(" ");
            }
        };
        txtAge.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) { validateAge.run(); }
        });
        txtAge.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { validateAge.run(); }
            public void removeUpdate(DocumentEvent e) { validateAge.run(); }
            public void changedUpdate(DocumentEvent e) { validateAge.run(); }
        });

        // 4. Nomor BPJS: wajib 13 digit angka jika BPJS dicentang
        Runnable validateBpjs = () -> {
            if (!chkBpjs.isSelected()) {
                txtBpjsNo.setBorder(normalBorder);
                lblBpjsWarn.setText(" ");
                return;
            }
            String val = txtBpjsNo.getText().trim();
            if (!val.matches("\\d{13}")) {
                txtBpjsNo.setBorder(errorBorder);
                lblBpjsWarn.setText("Peringatan: Nomor BPJS harus 13 digit angka!");
            } else {
                txtBpjsNo.setBorder(normalBorder);
                lblBpjsWarn.setText(" ");
            }
        };
        txtBpjsNo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) { validateBpjs.run(); }
        });
        txtBpjsNo.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { validateBpjs.run(); }
            public void removeUpdate(DocumentEvent e) { validateBpjs.run(); }
            public void changedUpdate(DocumentEvent e) { validateBpjs.run(); }
        });
    }

    private void handleRegisterPatient() {
        try {
            manager.registerPatient(
                    txtId.getText(),
                    txtName.getText(),
                    txtAge.getText(),
                    (String) cmbPoli.getSelectedItem(),
                    txtComplaint.getText(),
                    chkBpjs.isSelected(),
                    txtBpjsNo.getText()
            );
            txtId.setText("");
            txtName.setText("");
            txtAge.setText("");
            txtComplaint.setText("");
            txtBpjsNo.setText("");
            chkBpjs.setSelected(false);
            txtBpjsNo.setEnabled(false);

            txtId.setBorder(normalBorder);
            txtName.setBorder(normalBorder);
            txtAge.setBorder(normalBorder);
            txtBpjsNo.setBorder(normalBorder);
            lblIdWarn.setText(" ");
            lblNameWarn.setText(" ");
            lblAgeWarn.setText(" ");
            lblBpjsWarn.setText(" ");

            refreshDisplay();
            JOptionPane.showMessageDialog(this, "Pasien berhasil masuk ke dalam antrean!",
                    "Pendaftaran Berhasil", JOptionPane.INFORMATION_MESSAGE, successIcon);
        } catch (InvalidPatientDataException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Validasi Input Gagal", JOptionPane.WARNING_MESSAGE, warningIcon);
        } finally {
            System.out.println("Proses percobaan registrasi pasien selesai.");
        }
    }

    private void handleCallNextPatient() {
        Patient called = manager.callNextPatient();
        if (called == null) {
            JOptionPane.showMessageDialog(this, "Antrean saat ini kosong!",
                    "Info Antrean", JOptionPane.WARNING_MESSAGE, warningIcon);
            return;
        }
        String fileName = "Struk_" + called.getId() + ".txt";
        try {
            manager.exportReceiptToTxt(called, fileName);
            refreshDisplay();
            JOptionPane.showMessageDialog(this,
                    called.generateReceipt() + "\nStruk telah diekspor ke file: " + fileName,
                    "Pasien Dipanggil & Struk Dicetak", JOptionPane.INFORMATION_MESSAGE, successIcon);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Gagal mencetak struk: " + ex.getMessage(),
                    "File I/O Error", JOptionPane.ERROR_MESSAGE, warningIcon);
        }
    }

    private void handleSearch() {
        String query = txtSearch.getText().trim();
        if (query.isEmpty()) return;

        Patient byId = manager.searchPatient(query.startsWith("PSN-") ? query : "PSN-" + query);
        if (byId != null) {
            JOptionPane.showMessageDialog(this, "Ditemukan via ID:\n" + byId.generateReceipt(),
                    "Hasil Pencarian", JOptionPane.INFORMATION_MESSAGE, successIcon);
            return;
        }
        List<Patient> byName = manager.searchPatient(query, 0);
        if (byName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pasien '" + query + "' tidak ditemukan.",
                    "Hasil Pencarian", JOptionPane.WARNING_MESSAGE, warningIcon);
        } else {
            StringBuilder sb = new StringBuilder("Ditemukan " + byName.size() + " pasien:\n\n");
            for (Patient p : byName) sb.append(p.toString()).append("\n");
            JOptionPane.showMessageDialog(this, sb.toString(),
                    "Hasil Pencarian", JOptionPane.INFORMATION_MESSAGE, successIcon);
        }
    }

    private void loadInitialData() {
        try {
            manager.loadDatabaseFromFile();
        } catch (Exception ex) {
            manager.logActivity("Memulai dengan database baru.");
        }
        refreshDisplay();
    }

    private void refreshDisplay() {
        StringBuilder qText = new StringBuilder();
        int pos = 1;
        for (Patient p : manager.getWaitingQueue()) {
            qText.append(pos++).append(". ").append(p.toString()).append("\n");
        }
        areaQueue.setText(qText.length() == 0 ? "(Tidak ada pasien dalam antrean)" : qText.toString());

        StringBuilder hText = new StringBuilder();
        for (Patient p : manager.getPatientHistory()) {
            hText.append("• ").append(p.toString()).append("\n");
        }
        areaHistory.setText(hText.length() == 0 ? "(Belum ada riwayat pasien)" : hText.toString());
        areaLog.setText(manager.getActivityLog());
    }

    // WEEK 14: Multithreading via Runnable, Thread.sleep(), and synchronized Auto-Save
    private void startBackgroundThread() {
        Runnable backgroundTask = new Runnable() {
            private int secondsCounter = 0;
            private final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

            @Override
            public void run() {
                while (runningThread) {
                    try {
                        Thread.sleep(1000);
                        secondsCounter++;
                        String timeStr = sdf.format(new Date());

                        if (secondsCounter % 30 == 0 && !manager.getPatientHistory().isEmpty()) {
                            manager.saveDatabaseToFile();
                            SwingUtilities.invokeLater(() -> refreshDisplay());
                        }

                        final int nextSave = 30 - (secondsCounter % 30);
                        SwingUtilities.invokeLater(() -> lblClockAndThread.setText(
                                "Jam: " + timeStr + " | Auto-Save Thread: Aktif (" + nextSave + "d)"
                        ));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (IOException ioEx) {
                        System.err.println("Auto-save gagal: " + ioEx.getMessage());
                    }
                }
            }
        };
        Thread daemonThread = new Thread(backgroundTask, "Clinic-AutoSave-Thread");
        daemonThread.setDaemon(true);
        daemonThread.start();
    }
}