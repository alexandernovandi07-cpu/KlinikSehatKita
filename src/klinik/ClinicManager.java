package klinik;

import java.io.*;
import java.util.*;

public class ClinicManager {
    // WEEK 10: All 4 Core Java Collection Interfaces with Diamond Operator <>
    private final List<Patient> patientHistory = new ArrayList<>();         // Ordered history list
    private final Set<String> registeredIds = new HashSet<>();              // Unique ID set (no duplicates)
    private final Queue<Patient> waitingQueue = new LinkedList<>();         // FIFO live waiting queue
    private final Map<String, Double> polyTariffMap = new HashMap<>();      // Key-Value tariff lookup

    // WEEK 9: StringBuffer for thread-safe activity logging across GUI and background threads
    private final StringBuffer activityLog = new StringBuffer();

    private static final String DB_FILE = "clinic_data.ser";

    public ClinicManager() {
        // WEEK 3 & 10: Autoboxing primitive double into Double wrapper inside HashMap
        polyTariffMap.put("Poli Umum", 75000.0);
        polyTariffMap.put("Poli Gigi", 150000.0);
        polyTariffMap.put("Poli Anak", 120000.0);
        polyTariffMap.put("Poli Mata", 135000.0);
        logActivity("Sistem Klinik SehatKita diinisialisasi.");
    }

    // WEEK 9: Thread-safe StringBuffer append
    public void logActivity(String entry) {
        activityLog.append("• ").append(entry).append("\n");
    }

    public String getActivityLog() {
        return activityLog.toString();
    }

    public Map<String, Double> getPolyTariffMap() {
        return polyTariffMap;
    }

    // WEEK 11 & WEEK 14: Throws Custom Checked Exception + Synchronized for Thread Safety
    public synchronized void registerPatient(String rawId, String rawName, String rawAge,
                                             String poliName, String complaint,
                                             boolean isBpjs, String bpjsNo) throws InvalidPatientDataException {
        String idInput = rawId.trim();
        String name = rawName.trim();
        String ageInput = rawAge.trim();
        String cleanComplaint = complaint.trim();

        if (idInput.isEmpty() || name.isEmpty() || ageInput.isEmpty() || cleanComplaint.isEmpty()) {
            throw new InvalidPatientDataException("ID, Nama, Umur, dan Keluhan tidak boleh kosong!");
        }

        // Validasi 1: ID Pasien wajib angka saja (tanpa huruf/simbol)
        if (!idInput.matches("\\d+")) {
            throw new InvalidPatientDataException("ID Pasien harus berupa angka saja (tanpa huruf atau simbol)!");
        }
        String id = "PSN-" + idInput;

        // WEEK 10: Set prevents duplicate IDs
        if (registeredIds.contains(id)) {
            throw new InvalidPatientDataException("ID Pasien '" + id + "' sudah terdaftar!");
        }

        // Validasi 2: Nama Lengkap wajib huruf KAPITAL semua (WEEK 9: toUpperCase & equals)
        if (!name.equals(name.toUpperCase())) {
            throw new InvalidPatientDataException("Nama lengkap harus ditulis dengan huruf KAPITAL semua!");
        }

        // Validasi 3: Umur wajib bentuk angka saja (tanpa huruf/simbol)
        if (!ageInput.matches("\\d+")) {
            throw new InvalidPatientDataException("Umur harus dalam bentuk angka (tidak boleh ada huruf atau simbol)!");
        }

        int age;
        try {
            // WEEK 3 & 11: Parsing String to Wrapper Integer & Unboxing to primitive int
            Integer boxedAge = Integer.valueOf(ageInput);
            age = boxedAge;
        } catch (NumberFormatException e) {
            throw new InvalidPatientDataException("Umur harus dalam bentuk angka yang valid!");
        }

        if (age <= 0 || age > 130) {
            throw new InvalidPatientDataException("Umur harus berada di antara 1 hingga 130 tahun!");
        }

        double baseFee = polyTariffMap.getOrDefault(poliName, 75000.0);
        String fullComplaint = poliName + " - " + cleanComplaint;

        // WEEK 6: Upcasting (Subclass object referenced by Superclass type Patient)
        Patient newPatient;
        if (isBpjs) {
            if (bpjsNo == null || bpjsNo.trim().length() < 5) {
                throw new InvalidPatientDataException("Nomor BPJS minimal harus 5 karakter!");
            }
            newPatient = new BPJSPatient(id, name, age, fullComplaint, baseFee, bpjsNo.trim());
        } else {
            newPatient = new RegulerPatient(id, name, age, fullComplaint, baseFee);
        }

        registeredIds.add(id);
        patientHistory.add(newPatient);
        waitingQueue.add(newPatient);

        // WEEK 11: Assertion checking internal invariant (enable with -ea VM flag)
        assert !waitingQueue.isEmpty() : "Internal Error: Antrean tidak boleh kosong setelah tambah pasien";

        logActivity("Pasien terdaftar ke antrean: " + newPatient.getName() + " (" + id + ")");
    }

    // WEEK 10: Polling from FIFO Queue
    public synchronized Patient callNextPatient() {
        Patient next = waitingQueue.poll();
        assert waitingQueue.size() >= 0 : "Ukuran queue tidak mungkin negatif";
        if (next != null) {
            logActivity("Memanggil pasien dari antrean: " + next.getName());
        }
        return next;
    }

    // WEEK 6: Compile-time Polymorphism (Method Overloading #1 - Search by exact ID)
    public Patient searchPatient(String id) {
        for (Patient p : patientHistory) {
            if (p.getId().equalsIgnoreCase(id.trim())) {
                return p;
            }
        }
        return null;
    }

    // WEEK 6: Compile-time Polymorphism (Method Overloading #2 - Search by Name keyword & min age)
    public List<Patient> searchPatient(String nameKeyword, int minAge) {
        List<Patient> results = new ArrayList<>();
        String key = nameKeyword.trim().toLowerCase();
        for (Patient p : patientHistory) {
            if (p.getName().toLowerCase().contains(key) && p.getAge() >= minAge) {
                results.add(p);
            }
        }
        return results;
    }

    // WEEK 10: Collections Utility Methods (sort, max, min)
    public synchronized String getClinicStatistics() {
        if (patientHistory.isEmpty()) {
            return "Belum ada data pasien untuk dianalisis.";
        }
        List<Patient> sortedCopy = new ArrayList<>(patientHistory);
        Collections.sort(sortedCopy);
        Patient minBillPatient = Collections.min(sortedCopy);
        Patient maxBillPatient = Collections.max(sortedCopy);

        double totalRevenue = 0;
        for (Patient p : patientHistory) {
            totalRevenue += p.calculateTotalBill();
        }

        return String.format(
            "Total Pasien Terdaftar : %d orang\n" +
            "Pasien Dalam Antrean   : %d orang\n" +
            "Total Pendapatan Klinik: Rp %,.0f\n" +
            "Tagihan Terendah       : %s (Rp %,.0f)\n" +
            "Tagihan Tertinggi      : %s (Rp %,.0f)",
            patientHistory.size(), waitingQueue.size(), totalRevenue,
            minBillPatient.getName(), minBillPatient.calculateTotalBill(),
            maxBillPatient.getName(), maxBillPatient.calculateTotalBill()
        );
    }

    // WEEK 12 & 14: Serialization (ObjectOutputStream) with synchronized lock
    public synchronized void saveDatabaseToFile() throws IOException {
        File file = new File(DB_FILE);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(new ArrayList<>(patientHistory));
        }
        logActivity("Auto-Save / Penyimpanan database berhasil (" + file.length() + " bytes).");
    }

    // WEEK 12: Deserialization (ObjectInputStream) & File class check
    @SuppressWarnings("unchecked")
    public synchronized void loadDatabaseFromFile() throws IOException, ClassNotFoundException {
        File file = new File(DB_FILE);
        if (!file.exists()) {
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            List<Patient> loaded = (List<Patient>) ois.readObject();
            patientHistory.clear();
            registeredIds.clear();
            for (Patient p : loaded) {
                patientHistory.add(p);
                registeredIds.add(p.getId());
            }
            logActivity("Database berhasil dimuat: " + loaded.size() + " riwayat pasien.");
        }
    }

    // WEEK 11 & 12: Character Stream (BufferedWriter) with try-with-resources
    public void exportReceiptToTxt(Patient patient, String fileName) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            bw.write(patient.generateReceipt());
            bw.newLine();
            bw.write("Dicetak otomatis oleh Sistem Klinik SehatKita.");
        }
        logActivity("Struk pasien " + patient.getId() + " diekspor ke " + fileName);
    }

    public synchronized List<Patient> getPatientHistory() { return new ArrayList<>(patientHistory); }
    public synchronized Queue<Patient> getWaitingQueue() { return new LinkedList<>(waitingQueue); }
    
    // WEEK 12: Menghapus file database menggunakan File.delete()
    public synchronized void resetDatabase() {
    patientHistory.clear();
    registeredIds.clear();
    waitingQueue.clear();
    File file = new File(DB_FILE);
    if (file.exists()) {
        file.delete();
    }
    logActivity("Seluruh data antrean dan riwayat berhasil direset ke 0.");
    }
}