package helpdesk.model;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Represents a workstation or laptop in the IT asset inventory.
 */
public class Computer extends Equipment {

    private static final int MAX_RAM_GB = 4096;
    private static final int MAX_STORAGE_GB = 102_400; // 100 TB
    private static final int MAX_TEXT_LENGTH = 120;

    private static final String DEFAULT_UNSET = "Unset";
    private static final String DEFAULT_UNKNOWN = "Unknown";
    private static final String DEFAULT_GPU = "Integrated / Unknown";

    private static final Pattern HOSTNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{1,63}$");
    private static final Pattern MAC_PATTERN = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$");

    private String os;
    private String cpu;
    private int ramGb;
    private String gpu;
    private String brand;
    private String model;
    private String formFactor;
    private int storageGb;
    private String hostname;
    private String macAddress;
    private boolean diskEncrypted;
    private boolean antivirusActive;

    /**
     * Minimal constructor for quick registration.
     */
    public Computer(int serialNumber, String os, String cpu, int ramGb, String gpu) {
        this(serialNumber, os, cpu, ramGb, gpu,
                DEFAULT_UNSET, DEFAULT_UNSET, DEFAULT_UNSET,
                0, DEFAULT_UNSET, DEFAULT_UNSET, false, false);
    }

    /**
     * Full constructor for a detailed workstation profile.
     */
    public Computer(int serialNumber, String os, String cpu, int ramGb, String gpu,
                    String brand, String model, String formFactor, int storageGb,
                    String hostname, String macAddress, boolean diskEncrypted, boolean antivirusActive) {
        super(validateSerialNumber(serialNumber), "Computer");
        setOs(os);
        setCpu(cpu);
        setRamGb(ramGb);
        setGpu(gpu);
        setBrand(brand);
        setModel(model);
        setFormFactor(formFactor);
        setStorageGb(storageGb);
        setHostname(hostname);
        setMacAddress(macAddress);
        this.diskEncrypted = diskEncrypted;
        this.antivirusActive = antivirusActive;
    }

    public static Builder builder(int serialNumber, String os, String cpu, int ramGb, String gpu) {
        return new Builder(serialNumber, os, cpu, ramGb, gpu);
    }

    // ==================== GETTERS & VALIDATED SETTERS ====================

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = cleanText(os, DEFAULT_UNKNOWN, MAX_TEXT_LENGTH);
    }

    public String getCpu() {
        return cpu;
    }

    public void setCpu(String cpu) {
        this.cpu = cleanText(cpu, DEFAULT_UNKNOWN, MAX_TEXT_LENGTH);
    }

    public int getRamGb() {
        return ramGb;
    }

    public void setRamGb(int ramGb) {
        if (ramGb < 1 || ramGb > MAX_RAM_GB) {
            throw new IllegalArgumentException(
                    "RAM must be between 1 and " + MAX_RAM_GB + " GB (received: " + ramGb + ").");
        }
        this.ramGb = ramGb;
    }

    public String getGpu() {
        return gpu;
    }

    public void setGpu(String gpu) {
        this.gpu = cleanText(gpu, DEFAULT_GPU, MAX_TEXT_LENGTH);
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = cleanText(brand, DEFAULT_UNSET, MAX_TEXT_LENGTH);
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = cleanText(model, DEFAULT_UNSET, MAX_TEXT_LENGTH);
    }

    public String getFormFactor() {
        return formFactor;
    }

    public void setFormFactor(String formFactor) {
        this.formFactor = cleanText(formFactor, DEFAULT_UNSET, MAX_TEXT_LENGTH);
    }

    public int getStorageGb() {
        return storageGb;
    }

    public void setStorageGb(int storageGb) {
        if (storageGb < 0 || storageGb > MAX_STORAGE_GB) {
            throw new IllegalArgumentException(
                    "Storage capacity must be between 0 and " + MAX_STORAGE_GB + " GB (received: " + storageGb + ").");
        }
        this.storageGb = storageGb;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        String cleaned = cleanText(hostname, "", 63);
        if (isUnsetValue(cleaned)) {
            this.hostname = DEFAULT_UNSET;
            return;
        }
        if (!HOSTNAME_PATTERN.matcher(cleaned).matches()) {
            throw new IllegalArgumentException(
                    "Invalid hostname (allowed: letters, digits, '.', '-', '_', max 63 chars).");
        }
        this.hostname = cleaned;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        String cleaned = cleanText(macAddress, "", 17);
        if (isUnsetValue(cleaned)) {
            this.macAddress = DEFAULT_UNSET;
            return;
        }
        if (!MAC_PATTERN.matcher(cleaned).matches()) {
            throw new IllegalArgumentException(
                    "Invalid MAC address (expected format: AA:BB:CC:DD:EE:FF or AA-BB-CC-DD-EE-FF).");
        }
        this.macAddress = cleaned.replace('-', ':').toUpperCase(Locale.ROOT);
    }

    public boolean isDiskEncrypted() {
        return diskEncrypted;
    }

    public void setDiskEncrypted(boolean diskEncrypted) {
        this.diskEncrypted = diskEncrypted;
    }

    public boolean isAntivirusActive() {
        return antivirusActive;
    }

    public void setAntivirusActive(boolean antivirusActive) {
        this.antivirusActive = antivirusActive;
    }

    // ==================== SECURITY & BUSINESS LOGIC ====================

    public boolean isSecurityCompliant() {
        return diskEncrypted && antivirusActive;
    }

    // ==================== DISPLAY & EXPORT ====================

    public String toFormattedString() {
        String storageLabel = storageGb > 0 ? storageGb + " GB" : DEFAULT_UNSET;
        return new StringBuilder(256)
                .append("  Type         : ").append(getType()).append(" (").append(formFactor).append(") | SN : ").append(getSerialNumber()).append('\n')
                .append("  Hardware     : ").append(brand).append(' ').append(model).append(" | Hostname : ").append(hostname).append('\n')
                .append("  System       : ").append(os).append(" | CPU : ").append(cpu).append('\n')
                .append("  Resources    : RAM ").append(ramGb).append(" GB | Storage : ").append(storageLabel).append(" | GPU : ").append(gpu).append('\n')
                .append("  Network/Sec  : MAC ").append(macAddress)
                .append(" | Disk Encryption : ").append(diskEncrypted ? "Enabled" : "Disabled")
                .append(" | EDR/AV : ").append(antivirusActive ? "Active" : "Inactive")
                .toString();
    }

    @Override
    public void display() {
        System.out.println(toFormattedString());
    }

    /**
     * Backward-compatible semicolon-delimited export format:
     * SN;Type;OS;CPU;RAM;GPU;Brand;Model;FormFactor;Storage;Hostname;MAC;DiskEncrypted;AntivirusActive
     */
    @Override
    public String export() {
        return String.join(";",
                String.valueOf(getSerialNumber()),
                getType(),
                os,
                cpu,
                String.valueOf(ramGb),
                gpu,
                brand,
                model,
                formFactor,
                String.valueOf(storageGb),
                hostname,
                macAddress,
                String.valueOf(diskEncrypted),
                String.valueOf(antivirusActive)
        );
    }

    // ==================== UTILITIES & OBJECT CONTRACTS ====================

    private static int validateSerialNumber(int serialNumber) {
        if (serialNumber <= 0) {
            throw new IllegalArgumentException("Computer serial number (SN) must be strictly positive.");
        }
        return serialNumber;
    }

    private static boolean isUnsetValue(String value) {
        return value.isEmpty()
                || DEFAULT_UNSET.equalsIgnoreCase(value)
                || "Non renseigné".equalsIgnoreCase(value)
                || "Non renseignée".equalsIgnoreCase(value);
    }

    private static String cleanText(String text, String defaultValue, int maxLength) {
        if (text == null) {
            return defaultValue;
        }
        StringBuilder sb = new StringBuilder(Math.min(text.length(), maxLength));
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ';') {
                sb.append(',');
            } else if (c == '|') {
                sb.append('/');
            } else if (c == '\r' || c == '\n' || c == '\t') {
                sb.append(' ');
            } else if (!Character.isISOControl(c)) {
                sb.append(c);
            }
        }
        String cleaned = sb.toString().trim();
        if (cleaned.isEmpty()) {
            return defaultValue;
        }
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }

    @Override
    public String toString() {
        return "Computer{sn=" + getSerialNumber()
                + ", hostname='" + hostname + '\''
                + ", model='" + brand + ' ' + model + '\''
                + ", os='" + os + '\''
                + ", ramGb=" + ramGb + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Computer other = (Computer) o;
        return getSerialNumber() == other.getSerialNumber();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(getSerialNumber());
    }

    // ==================== FLUENT BUILDER ====================

    public static final class Builder {
        private final int serialNumber;
        private final String os;
        private final String cpu;
        private final int ramGb;
        private final String gpu;

        private String brand = DEFAULT_UNSET;
        private String model = DEFAULT_UNSET;
        private String formFactor = DEFAULT_UNSET;
        private int storageGb = 0;
        private String hostname = DEFAULT_UNSET;
        private String macAddress = DEFAULT_UNSET;
        private boolean diskEncrypted = false;
        private boolean antivirusActive = false;

        private Builder(int serialNumber, String os, String cpu, int ramGb, String gpu) {
            this.serialNumber = serialNumber;
            this.os = os;
            this.cpu = cpu;
            this.ramGb = ramGb;
            this.gpu = gpu;
        }

        public Builder brand(String brand) {
            this.brand = brand;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder formFactor(String formFactor) {
            this.formFactor = formFactor;
            return this;
        }

        public Builder storageGb(int storageGb) {
            this.storageGb = storageGb;
            return this;
        }

        public Builder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder macAddress(String macAddress) {
            this.macAddress = macAddress;
            return this;
        }

        public Builder diskEncrypted(boolean diskEncrypted) {
            this.diskEncrypted = diskEncrypted;
            return this;
        }

        public Builder antivirusActive(boolean antivirusActive) {
            this.antivirusActive = antivirusActive;
            return this;
        }

        public Computer build() {
            return new Computer(serialNumber, os, cpu, ramGb, gpu,
                    brand, model, formFactor, storageGb,
                    hostname, macAddress, diskEncrypted, antivirusActive);
        }
    }
}
