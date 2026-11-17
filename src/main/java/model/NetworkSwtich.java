package helpdesk.model;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Represents a managed or unmanaged Ethernet network switch in the IT inventory.
 */
public class NetworkSwitch extends Equipment {

    private static final long serialVersionUID = 2L;

    private static final int MAX_PORTS = 512;
    private static final int VLAN_MIN = 1;
    private static final int VLAN_MAX = 4094;
    private static final String DEFAULT_PORT_SPEED = "1 Gbps";

    /**
     * Strict IPv4 dotted-quad regex (rejects ambiguous leading zeros like 010).
     */
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)$");

    private int ethernetPortCount;
    private int usedPortCount;
    private String brand;
    private String model;
    private String portSpeed;
    private boolean managed;
    private boolean poeSupported;
    private String managementIp;
    private int managementVlan;

    /**
     * Minimal constructor for quick switch registration.
     */
    public NetworkSwitch(int serialNumber, int ethernetPortCount) {
        this(serialNumber, ethernetPortCount, DEFAULT_UNSET, DEFAULT_UNSET,
                DEFAULT_PORT_SPEED, true, false, DEFAULT_UNASSIGNED, VLAN_MIN, 0);
    }

    /**
     * Full constructor for a detailed network switch profile.
     */
    public NetworkSwitch(int serialNumber, int ethernetPortCount, String brand, String model,
                         String portSpeed, boolean managed, boolean poeSupported,
                         String managementIp, int managementVlan, int usedPortCount) {
        super(validateSerialNumber(serialNumber), "Switch");
        setEthernetPortCount(ethernetPortCount);
        setBrand(brand);
        setModel(model);
        setPortSpeed(portSpeed);
        this.managed = managed;
        this.poeSupported = poeSupported;
        setManagementIp(managementIp);
        setManagementVlan(managementVlan);
        setUsedPortCount(usedPortCount);
    }

    public static Builder builder(int serialNumber, int ethernetPortCount) {
        return new Builder(serialNumber, ethernetPortCount);
    }

    // ==================== GETTERS & VALIDATED SETTERS ====================

    public int getEthernetPortCount() {
        return ethernetPortCount;
    }

    public void setEthernetPortCount(int ethernetPortCount) {
        if (ethernetPortCount < 1 || ethernetPortCount > MAX_PORTS) {
            throw new IllegalArgumentException(
                    "Ethernet port count must be between 1 and " + MAX_PORTS + " (received: " + ethernetPortCount + ").");
        }
        if (ethernetPortCount < this.usedPortCount) {
            throw new IllegalArgumentException(
                    "Total port count (" + ethernetPortCount + ") cannot be lower than currently used ports ("
                            + this.usedPortCount + ").");
        }
        this.ethernetPortCount = ethernetPortCount;
    }

    public int getUsedPortCount() {
        return usedPortCount;
    }

    public void setUsedPortCount(int usedPortCount) {
        if (usedPortCount < 0 || usedPortCount > this.ethernetPortCount) {
            throw new IllegalArgumentException(
                    "Used port count must be between 0 and " + this.ethernetPortCount + " (received: " + usedPortCount + ").");
        }
        this.usedPortCount = usedPortCount;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        String cleaned = cleanText(brand, "", MAX_TEXT_LENGTH);
        this.brand = isUnsetValue(cleaned) ? DEFAULT_UNSET : cleaned;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        String cleaned = cleanText(model, "", MAX_TEXT_LENGTH);
        this.model = isUnsetValue(cleaned) ? DEFAULT_UNSET : cleaned;
    }

    public String getPortSpeed() {
        return portSpeed;
    }

    public void setPortSpeed(String portSpeed) {
        this.portSpeed = cleanText(portSpeed, DEFAULT_PORT_SPEED, 32);
    }

    public boolean isManaged() {
        return managed;
    }

    public void setManaged(boolean managed) {
        this.managed = managed;
    }

    public boolean isPoeSupported() {
        return poeSupported;
    }

    public void setPoeSupported(boolean poeSupported) {
        this.poeSupported = poeSupported;
    }

    public String getManagementIp() {
        return managementIp;
    }

    public void setManagementIp(String managementIp) {
        String cleaned = cleanText(managementIp, "", 45);
        if (isUnsetValue(cleaned) || "Non assignée".equalsIgnoreCase(cleaned)
                || DEFAULT_UNASSIGNED.equalsIgnoreCase(cleaned)) {
            this.managementIp = DEFAULT_UNASSIGNED;
            return;
        }
        if ("DHCP".equalsIgnoreCase(cleaned)) {
            this.managementIp = "DHCP";
            return;
        }
        if (!IPV4_PATTERN.matcher(cleaned).matches()) {
            throw new IllegalArgumentException(
                    "Invalid IPv4 management address (valid examples: '192.168.10.2', 'DHCP', or empty).");
        }
        this.managementIp = cleaned;
    }

    public int getManagementVlan() {
        return managementVlan;
    }

    public void setManagementVlan(int managementVlan) {
        if (managementVlan < VLAN_MIN || managementVlan > VLAN_MAX) {
            throw new IllegalArgumentException(
                    "Management VLAN must be between " + VLAN_MIN + " and " + VLAN_MAX + " (received: " + managementVlan + ").");
        }
        this.managementVlan = managementVlan;
    }

    // ==================== NETWORK BUSINESS LOGIC ====================

    public int getAvailablePortCount() {
        return ethernetPortCount - usedPortCount;
    }

    public double getPortUtilizationPercent() {
        return (usedPortCount * 100.0) / ethernetPortCount;
    }

    public boolean isFull() {
        return usedPortCount >= ethernetPortCount;
    }

    public void allocatePort() {
        if (isFull()) {
            throw new IllegalStateException("No available ports left on switch SN=" + getSerialNumber() + ".");
        }
        usedPortCount++;
    }

    public void releasePort() {
        if (usedPortCount <= 0) {
            throw new IllegalStateException("No used ports to release on switch SN=" + getSerialNumber() + ".");
        }
        usedPortCount--;
    }

    // ==================== DISPLAY & EXPORT ====================

    @Override
    public String toFormattedString() {
        String utilization = String.format(Locale.ROOT, "%.0f%%", getPortUtilizationPercent());
        return new StringBuilder(256)
                .append("  Type         : Network Switch (").append(getType()).append(") | SN : ").append(getSerialNumber()).append('\n')
                .append("  Hardware     : ").append(brand).append(' ').append(model).append(" (").append(portSpeed).append(")\n")
                .append("  Ports        : ").append(usedPortCount).append(" / ").append(ethernetPortCount)
                .append(" used (").append(getAvailablePortCount()).append(" free, ").append(utilization).append(')')
                .append(" | PoE : ").append(poeSupported ? "Yes" : "No").append('\n')
                .append("  Network      : ").append(managed ? "Managed (L2/L3)" : "Unmanaged")
                .append(" | IP : ").append(managementIp).append(" | VLAN : ").append(managementVlan)
                .toString();
    }

    @Override
    public void display() {
        System.out.println(toFormattedString());
    }

    /**
     * Backward-compatible semicolon-delimited export format:
     * SN;Type;NbPorts;Brand;Model;PortSpeed;Managed;PoE;ManagementIP;ManagementVLAN;UsedPorts
     */
    @Override
    public String export() {
        return String.join(";",
                String.valueOf(getSerialNumber()),
                getType(),
                String.valueOf(ethernetPortCount),
                brand,
                model,
                portSpeed,
                String.valueOf(managed),
                String.valueOf(poeSupported),
                managementIp,
                String.valueOf(managementVlan),
                String.valueOf(usedPortCount)
        );
    }

    @Override
    public String toString() {
        return "NetworkSwitch{sn=" + getSerialNumber()
                + ", model='" + brand + ' ' + model + '\''
                + ", ports=" + usedPortCount + '/' + ethernetPortCount
                + ", ip='" + managementIp + '\''
                + ", vlan=" + managementVlan + '}';
    }

    // ==================== FLUENT BUILDER ====================

    public static final class Builder {
        private final int serialNumber;
        private final int ethernetPortCount;

        private String brand = DEFAULT_UNSET;
        private String model = DEFAULT_UNSET;
        private String portSpeed = DEFAULT_PORT_SPEED;
        private boolean managed = true;
        private boolean poeSupported = false;
        private String managementIp = DEFAULT_UNASSIGNED;
        private int managementVlan = VLAN_MIN;
        private int usedPortCount = 0;

        private Builder(int serialNumber, int ethernetPortCount) {
            this.serialNumber = serialNumber;
            this.ethernetPortCount = ethernetPortCount;
        }

        public Builder brand(String brand) {
            this.brand = brand;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder portSpeed(String portSpeed) {
            this.portSpeed = portSpeed;
            return this;
        }

        public Builder managed(boolean managed) {
            this.managed = managed;
            return this;
        }

        public Builder poeSupported(boolean poeSupported) {
            this.poeSupported = poeSupported;
            return this;
        }

        public Builder managementIp(String managementIp) {
            this.managementIp = managementIp;
            return this;
        }

        public Builder managementVlan(int managementVlan) {
            this.managementVlan = managementVlan;
            return this;
        }

        public Builder usedPortCount(int usedPortCount) {
            this.usedPortCount = usedPortCount;
            return this;
        }

        public NetworkSwitch build() {
            return new NetworkSwitch(serialNumber, ethernetPortCount, brand, model, portSpeed,
                    managed, poeSupported, managementIp, managementVlan, usedPortCount);
        }
    }
}
