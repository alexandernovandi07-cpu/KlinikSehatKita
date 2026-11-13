package klinik;

import java.io.Serializable;

// WEEK 11: Custom Checked Exception
class InvalidPatientDataException extends Exception {
    public InvalidPatientDataException(String message) {
        super(message);
    }
}

// WEEK 7: Interface (Abstraction & Capability)
interface Billable {
    double ADMIN_FEE = 15000.0; // public static final by default

    double calculateTotalBill(); // abstract method

    // Java 8+ default method (Week 7)
    default String getCurrencyFormat(double amount) {
        return String.format("Rp %,.0f", amount);
    }
}

// WEEK 7 & WEEK 5: Abstract Class + Encapsulation
abstract class Person implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // WEEK 5: Private attributes for data hiding
    private String id;
    private String name;
    private int age;

    // WEEK 2: Parameterized Constructor
    public Person(String id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    // WEEK 7: Abstract method to be overridden by subclasses
    public abstract String getPatientCategory();

    // WEEK 5: Public Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
}

// WEEK 4: Inheritance (Patient extends Person) & Comparable for Week 10 Collections.sort()
abstract class Patient extends Person implements Billable, Comparable<Patient> {
    private static final long serialVersionUID = 1L;

    // WEEK 3: Static variable (class-level counter) vs Instance variables
    private static int totalPatientsCreated = 0;
    protected String complaint;
    protected double baseConsultationFee;

    public Patient(String id, String name, int age, String complaint, double baseConsultationFee) {
        super(id, name, age); // WEEK 4: Calling superclass constructor via super()
        this.complaint = complaint;
        this.baseConsultationFee = baseConsultationFee;
        totalPatientsCreated++;
    }

    public static int getTotalPatientsCreated() {
        return totalPatientsCreated;
    }

    public String getComplaint() { return complaint; }
    public void setComplaint(String complaint) { this.complaint = complaint; }

    public double getBaseConsultationFee() { return baseConsultationFee; }

    // WEEK 9: StringBuilder used to generate formatted receipt text
    public String generateReceipt() {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("       STRUK PEMBAYARAN KLINIK          \n");
        sb.append("========================================\n");
        sb.append("ID Pasien   : ").append(getId()).append("\n");
        sb.append("Nama Pasien : ").append(getName()).append("\n");
        sb.append("Umur        : ").append(getAge()).append(" Tahun\n");
        sb.append("Kategori    : ").append(getPatientCategory()).append("\n");
        sb.append("Keluhan     : ").append(complaint).append("\n");
        sb.append("----------------------------------------\n");
        sb.append("Total Biaya : ").append(getCurrencyFormat(calculateTotalBill())).append("\n");
        sb.append("========================================\n");
        return sb.toString();
    }

    // Used by Collections.sort(), Collections.max(), Collections.min() (Week 10)
    @Override
    public int compareTo(Patient other) {
        return Double.compare(this.calculateTotalBill(), other.calculateTotalBill());
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%d thn) - %s | %s | Tagihan: %s",
                getId(), getName(), getAge(), getPatientCategory(),
                complaint, getCurrencyFormat(calculateTotalBill()));
    }
}

// WEEK 4 & WEEK 6: Multilevel & Hierarchical Inheritance + Method Overriding (Run-time Polymorphism)
class RegulerPatient extends Patient {
    private static final long serialVersionUID = 1L;

    public RegulerPatient(String id, String name, int age, String complaint, double baseFee) {
        super(id, name, age, complaint, baseFee);
    }

    @Override
    public String getPatientCategory() {
        return "Pasien Reguler (Umum)";
    }

    @Override
    public double calculateTotalBill() {
        // Reguler pays base fee + standard admin fee
        return baseConsultationFee + ADMIN_FEE;
    }
}

// WEEK 4 & WEEK 6: Second Subclass for Hierarchical Inheritance & Dynamic Binding
class BPJSPatient extends Patient {
    private static final long serialVersionUID = 1L;
    private String bpjsNumber;

    public BPJSPatient(String id, String name, int age, String complaint, double baseFee, String bpjsNumber) {
        super(id, name, age, complaint, baseFee);
        this.bpjsNumber = bpjsNumber;
    }

    public String getBpjsNumber() { return bpjsNumber; }

    @Override
    public String getPatientCategory() {
        return "Pasien BPJS (No: " + bpjsNumber + ")";
    }

    @Override
    public double calculateTotalBill() {
        // BPJS patients receive an 80% subsidy on consultation and 0 admin fee
        return baseConsultationFee * 0.20;
    }
}