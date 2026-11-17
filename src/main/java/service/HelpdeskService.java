package helpdesk.service;

import helpdesk.model.Computer;
import helpdesk.model.Equipment;
import helpdesk.model.EquipmentStatus;
import helpdesk.model.HelpdeskRecord;
import helpdesk.model.NetworkSwitch;
import helpdesk.model.Phone;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Core service managing IT asset inventory, helpdesk records, audits, stats, and safe file I/O.
 */
public class HelpdeskService {

    public static final String DEFAULT_EXPORT_FILE = "parc_helpdesk.txt";
    public static final String DEFAULT_CSV_FILE = "parc_rapport.csv";

    private static final long MAX_IMPORT_BYTES = 10L * 1024 * 1024; // 10 MB
    private static final int WARRANTY_ALERT_DAYS = 30;
    private static final double SWITCH_ALERT_PCT = 85.0;
    private static final String DEFAULT_PARK_NAME = "Main IT Inventory";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]+$");

    private String parkName;
    private final Map<Integer, Equipment> equipmentBySn;
    private final Map<Integer, HelpdeskRecord> recordsBySn;

    public HelpdeskService() {
        this(DEFAULT_PARK_NAME);
    }

    public HelpdeskService(String parkName) {
        this.parkName = normalizeParkName(parkName);
        this.equipmentBySn = new LinkedHashMap<>();
        this.recordsBySn = new LinkedHashMap<>();
    }

    // ==================== INVENTORY MANAGEMENT (O(1)) ====================

    public String getParkName() {
        return parkName;
    }

    public void setParkName(String parkName) {
        String cleaned = cleanText(parkName, 120);
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Inventory name cannot be empty.");
        }
        this.parkName = cleaned;
    }

    public int getEquipmentCount() {
        return equipmentBySn.size();
    }

    public boolean addEquipment(Equipment equipment) {
        Objects.requireNonNull(equipment, "Equipment cannot be null.");
        int sn = equipment.getSerialNumber();
        if (equipmentBySn.containsKey(sn)) {
            return false;
        }
        equipmentBySn.put(sn, equipment);
        recordsBySn.putIfAbsent(sn, new HelpdeskRecord());
        return true;
    }

    public boolean removeEquipment(int serialNumber) {
        Equipment removed = equipmentBySn.remove(serialNumber);
        if (removed == null) {
            return false;
        }
        recordsBySn.remove(serialNumber);
        return true;
    }

    public Equipment findBySerialNumber(int serialNumber) {
        return equipmentBySn.get(serialNumber);
    }

    public HelpdeskRecord getOrCreateRecord(int serialNumber) {
        if (!equipmentBySn.containsKey(serialNumber)) {
            throw new IllegalArgumentException("No equipment found with SN " + serialNumber + ".");
        }
        return recordsBySn.computeIfAbsent(serialNumber, k -> new HelpdeskRecord());
    }

    public List<Equipment> getAllSortedBySerialNumber() {
        List<Equipment> sorted = new ArrayList<>(equipmentBySn.values());
        Collections.sort(sorted);
        return sorted;
    }

    // ==================== SEARCH QUERIES ====================

    public List<Equipment> findByType(String type) {
        String target = normalizeTypeAlias(type);
        if (target.isEmpty()) {
            return Collections.emptyList();
        }
        List<Equipment> results = new ArrayList<>();
        for (Equipment equipment : equipmentBySn.values()) {
            if (equipment.getType().equalsIgnoreCase(target)) {
                results.add(equipment);
            }
        }
        Collections.sort(results);
        return results;
    }

    public List<Equipment> findByStatus(EquipmentStatus status) {
        Objects.requireNonNull(status, "Status cannot be null.");
        List<Equipment> results = new ArrayList<>();
        for (Equipment equipment : equipmentBySn.values()) {
            if (getOrCreateRecord(equipment.getSerialNumber()).getStatus() == status) {
                results.add(equipment);
            }
        }
        Collections.sort(results);
        return results;
    }

    public List<Equipment> findByUser(String userQuery) {
        String target = cleanText(userQuery, 120).toLowerCase(Locale.ROOT);
        if (target.isEmpty()) {
            return Collections.emptyList();
        }
        List<Equipment> results = new ArrayList<>();
        for (Equipment equipment : equipmentBySn.values()) {
            HelpdeskRecord record = getOrCreateRecord(equipment.getSerialNumber());
            if (record.getAssignedUser().toLowerCase(Locale.ROOT).contains(target)) {
                results.add(equipment);
            }
        }
        Collections.sort(results);
        return results;
    }

    public List<Equipment> searchByKeyword(String keyword) {
        String target = cleanText(keyword, 120).toLowerCase(Locale.ROOT);
        if (target.isEmpty()) {
            return Collections.emptyList();
        }
        List<Equipment> results = new ArrayList<>();
        for (Equipment equipment : equipmentBySn.values()) {
            HelpdeskRecord record = getOrCreateRecord(equipment.getSerialNumber());
            String searchable = (equipment.export() + " "
                    + equipment.getInventoryNumber() + " "
                    + equipment.getSupplier() + " "
                    + record.getAssignedUser() + " "
                    + record.getLocation() + " "
                    + record.getStatus().getLabel()).toLowerCase(Locale.ROOT);
            if (searchable.contains(target)) {
                results.add(equipment);
            }
        }
        Collections.sort(results);
        return results;
    }

    // ==================== FORMATTING, AUDIT & DASHBOARD ====================

    public String formatEquipmentDetails(Equipment equipment, boolean includeTickets) {
        Objects.requireNonNull(equipment, "Equipment cannot be null.");
        HelpdeskRecord record = getOrCreateRecord(equipment.getSerialNumber());

        StringBuilder sb = new StringBuilder(512);
        sb.append("---------------------------------------------------------\n");
        sb.append(equipment.toFormattedString()).append('\n');
        sb.append(equipment.toAdminFormattedString()).append('\n');
        sb.append("  [Helpdesk] Status       : ").append(record.getStatus().getLabel()).append('\n');
        sb.append("  [Helpdesk] User         : ").append(record.getAssignedUser()).append('\n');
        sb.append("  [Helpdesk] Location     : ").append(record.getLocation()).append('\n');
        sb.append("  [Helpdesk] Last updated : ").append(record.getLastUpdated()).append('\n');
        sb.append("  [Helpdesk] Tickets      : ").append(record.getTickets().size());

        if (includeTickets && !record.getTickets().isEmpty()) {
            sb.append("\n  Incident / intervention history:");
            for (String ticket : record.getTickets()) {
                sb.append("\n    - ").append(ticket);
            }
        }
        return sb.toString();
    }

    public String buildAuditReport() {
        StringBuilder sb = new StringBuilder(512);
        sb.append("\n=== HELPDESK AUDIT & ALERTS : ").append(parkName).append(" ===\n");
        if (equipmentBySn.isEmpty()) {
            sb.append("No equipment registered.\n");
            return sb.toString();
        }

        LocalDate today = LocalDate.now();
        int alertItems = 0;

        for (Equipment equipment : getAllSortedBySerialNumber()) {
            List<String> alerts = new ArrayList<>();
            HelpdeskRecord record = getOrCreateRecord(equipment.getSerialNumber());
            EquipmentStatus status = record.getStatus();

            if (status == EquipmentStatus.BROKEN || status == EquipmentStatus.IN_REPAIR) {
                alerts.add("Critical status: " + status.getLabel());
            }

            if (equipment.getWarrantyEndDate() != null && status != EquipmentStatus.RETIRED) {
                long days = ChronoUnit.DAYS.between(today, equipment.getWarrantyEndDate());
                if (days < 0) {
                    alerts.add("Warranty expired (" + equipment.getWarrantyEndDate().format(DATE_FORMAT) + ")");
                } else if (days <= WARRANTY_ALERT_DAYS) {
                    alerts.add("Warranty expiring soon (" + days + " day(s))");
                }
            }

            if (equipment instanceof Computer) {
                Computer pc = (Computer) equipment;
                if (!pc.isSecurityCompliant() && status == EquipmentStatus.IN_USE) {
                    alerts.add("Workstation non-compliant (Encryption=" + pc.isDiskEncrypted()
                            + ", EDR/AV=" + pc.isAntivirusActive() + ")");
                }
            } else if (equipment instanceof NetworkSwitch) {
                NetworkSwitch sw = (NetworkSwitch) equipment;
                if (sw.getPortUtilizationPercent() >= SWITCH_ALERT_PCT) {
                    alerts.add(String.format(Locale.ROOT, "Switch nearly full (%.0f%% ports used)",
                            sw.getPortUtilizationPercent()));
                }
            } else if (equipment instanceof Phone) {
                Phone phone = (Phone) equipment;
                if (!phone.isSecurityCompliant() && status == EquipmentStatus.IN_USE) {
                    alerts.add("Production phone not enrolled in MDM");
                }
            }

            if (!alerts.isEmpty()) {
                alertItems++;
                sb.append("  [!] SN ").append(equipment.getSerialNumber())
                        .append(" (").append(equipment.getType())
                        .append(" - ").append(record.getAssignedUser()).append(") :\n");
                for (String alert : alerts) {
                    sb.append("      -> ").append(alert).append('\n');
                }
            }
        }

        if (alertItems == 0) {
            sb.append("[OK] No alerts detected: inventory is healthy and compliant.\n");
        } else {
            sb.append("\nTotal equipment with alerts: ").append(alertItems).append('\n');
        }
        return sb.toString();
    }

    public String buildDashboardReport() {
        StringBuilder sb = new StringBuilder(512);
        sb.append("\n=== HELPDESK DASHBOARD : ").append(parkName).append(" ===\n");
        int total = equipmentBySn.size();
        sb.append("Total equipment count: ").append(total).append('\n');
        if (total == 0) {
            return sb.toString();
        }

        Map<String, Integer> byType = new LinkedHashMap<>();
        Map<EquipmentStatus, Integer> byStatus = new LinkedHashMap<>();
        for (EquipmentStatus s : EquipmentStatus.values()) {
            byStatus.put(s, 0);
        }

        int totalTickets = 0;
        int unassigned = 0;
        BigDecimal totalValueExclTax = BigDecimal.ZERO;

        int totalComputers = 0;
        int compliantComputers = 0;
        int totalPhones = 0;
        int mdmPhones = 0;
        int totalSwitches = 0;
        int totalSwitchPorts = 0;
        int totalUsedPorts = 0;
        int underWarranty = 0;
        int expiredWarranty = 0;

        LocalDate today = LocalDate.now();

        for (Equipment equipment : equipmentBySn.values()) {
            String type = equipment.getType();
            byType.put(type, byType.getOrDefault(type, 0) + 1);
            totalValueExclTax = totalValueExclTax.add(equipment.getPurchasePriceExclTax());

            if (equipment.getWarrantyEndDate() != null) {
                if (equipment.isUnderWarranty(today)) {
                    underWarranty++;
                } else {
                    expiredWarranty++;
                }
            }

            if (equipment instanceof Computer) {
                totalComputers++;
                if (((Computer) equipment).isSecurityCompliant()) {
                    compliantComputers++;
                }
            } else if (equipment instanceof NetworkSwitch) {
                totalSwitches++;
                NetworkSwitch sw = (NetworkSwitch) equipment;
                totalSwitchPorts += sw.getEthernetPortCount();
                totalUsedPorts += sw.getUsedPortCount();
            } else if (equipment instanceof Phone) {
                totalPhones++;
                if (((Phone) equipment).isSecurityCompliant()) {
                    mdmPhones++;
                }
            }

            HelpdeskRecord record = getOrCreateRecord(equipment.getSerialNumber());
            byStatus.put(record.getStatus(), byStatus.get(record.getStatus()) + 1);
            totalTickets += record.getTickets().size();
            if (HelpdeskRecord.DEFAULT_ASSIGNEE.equalsIgnoreCase(record.getAssignedUser())) {
                unassigned++;
            }
        }

        sb.append(String.format(Locale.ROOT, "Cumulative asset value      : %.2f EUR excl. tax%n", totalValueExclTax));

        sb.append("\nBreakdown by type:\n");
        for (Map.Entry<String, Integer> entry : byType.entrySet()) {
            sb.append(String.format(Locale.ROOT, "  - %-15s : %d%n", entry.getKey(), entry.getValue()));
        }

        sb.append("\nBreakdown by status:\n");
        for (Map.Entry<EquipmentStatus, Integer> entry : byStatus.entrySet()) {
            double pct = (entry.getValue() * 100.0) / total;
            sb.append(String.format(Locale.ROOT, "  - %-26s : %d (%.1f%%)%n",
                    entry.getKey().getLabel(), entry.getValue(), pct));
        }

        int inUse = byStatus.get(EquipmentStatus.IN_USE);
        int brokenOrRepair = byStatus.get(EquipmentStatus.BROKEN) + byStatus.get(EquipmentStatus.IN_REPAIR);
        sb.append("\nHelpdesk & Security KPIs:\n");
        sb.append("  - Equipment in production        : ").append(inUse).append('\n');
        sb.append("  - Unassigned equipment           : ").append(unassigned).append('\n');
        sb.append("  - Broken / in repair             : ").append(brokenOrRepair).append('\n');
        sb.append("  - Total logged tickets           : ").append(totalTickets).append('\n');
        sb.append("  - Warranties (active / expired)  : ").append(underWarranty).append(" / ").append(expiredWarranty).append('\n');

        if (totalComputers > 0) {
            sb.append("  - Cyber compliance (Computers)   : ").append(compliantComputers).append(" / ").append(totalComputers)
                    .append(" (Encryption + EDR/AV)\n");
        }
        if (totalSwitches > 0) {
            sb.append("  - Network capacity (Switches)    : ").append(totalUsedPorts).append(" / ").append(totalSwitchPorts)
                    .append(" ports used\n");
        }
        if (totalPhones > 0) {
            sb.append("  - MDM compliance (Phones)        : ").append(mdmPhones).append(" / ").append(totalPhones).append('\n');
        }
        return sb.toString();
    }

    // ==================== ATOMIC EXPORT / IMPORT (UTF-8) ====================

    public Path exportInventory(String fileName) throws IOException {
        Path target = resolveSafePath(fileName);
        Path tempFile = target.resolveSibling(target.getFileName() + ".tmp");
        Path backupFile = target.resolveSibling(target.getFileName() + ".bak");

        try {
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                writer.write("PARC;" + cleanText(this.parkName, 120));
                writer.newLine();

                for (Equipment equipment : equipmentBySn.values()) {
                    writer.write("MAT;" + equipment.export());
                    writer.newLine();

                    String purchaseDateStr = (equipment.getPurchaseDate() != null)
                            ? equipment.getPurchaseDate().toString() : "";
                    String warrantyEndStr = (equipment.getWarrantyEndDate() != null)
                            ? equipment.getWarrantyEndDate().toString() : "";
                    writer.write("ADMIN;" + equipment.getSerialNumber() + ";"
                            + cleanText(equipment.getInventoryNumber(), 120) + ";"
                            + cleanText(equipment.getSupplier(), 120) + ";"
                            + purchaseDateStr + ";"
                            + warrantyEndStr + ";"
                            + equipment.getPurchasePriceExclTax().toPlainString());
                    writer.newLine();

                    HelpdeskRecord record = getOrCreateRecord(equipment.getSerialNumber());
                    String serializedTickets = String.join("|", record.getTickets());
                    writer.write("HD;" + equipment.getSerialNumber() + ";"
                            + record.getStatus().name() + ";"
                            + cleanText(record.getAssignedUser(), 120) + ";"
                            + cleanText(record.getLocation(), 120) + ";"
                            + cleanText(record.getLastUpdated(), 32) + ";"
                            + serializedTickets);
                    writer.newLine();
                }
                writer.flush();
            }

            if (Files.exists(target)) {
                Files.copy(target, backupFile, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public Path exportCsvReport(String fileName) throws IOException {
        Path target = resolveSafePath(fileName);
        Path tempFile = target.resolveSibling(target.getFileName() + ".tmp");

        try {
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                writer.write('\uFEFF'); // UTF-8 BOM for Excel
                writer.write("SN;Type;Inventory;Status;Assigned_User;Location;Supplier;Warranty_End;Price_Excl_Tax;Tickets_Count;Details");
                writer.newLine();

                for (Equipment equipment : getAllSortedBySerialNumber()) {
                    HelpdeskRecord record = getOrCreateRecord(equipment.getSerialNumber());
                    String warrantyEnd = (equipment.getWarrantyEndDate() != null)
                            ? equipment.getWarrantyEndDate().format(DATE_FORMAT)
                            : "Unset";
                    String price = String.format(Locale.FRANCE, "%.2f", equipment.getPurchasePriceExclTax());

                    writer.write(equipment.getSerialNumber() + ";"
                            + protectCsvCell(equipment.getType()) + ";"
                            + protectCsvCell(equipment.getInventoryNumber()) + ";"
                            + protectCsvCell(record.getStatus().getLabel()) + ";"
                            + protectCsvCell(record.getAssignedUser()) + ";"
                            + protectCsvCell(record.getLocation()) + ";"
                            + protectCsvCell(equipment.getSupplier()) + ";"
                            + protectCsvCell(warrantyEnd) + ";"
                            + protectCsvCell(price) + ";"
                            + record.getTickets().size() + ";"
                            + protectCsvCell(formatDetailsForCsv(equipment)));
                    writer.newLine();
                }
                writer.flush();
            }

            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public ImportResult importInventory(String fileName, boolean replaceExisting) throws IOException {
        Path source = resolveSafePath(fileName);
        if (!Files.exists(source) || !Files.isRegularFile(source)) {
            throw new IOException("File \"" + source.getFileName() + "\" was not found.");
        }
        if (Files.size(source) > MAX_IMPORT_BYTES) {
            throw new IOException("Security limit: file exceeds maximum allowed size (10 MB).");
        }

        String importedParkName = this.parkName;
        Map<Integer, Equipment> parsedEquipment = new LinkedHashMap<>();
        Map<Integer, HelpdeskRecord> parsedRecords = new LinkedHashMap<>();
        Map<Integer, String[]> rawAdminBySn = new LinkedHashMap<>();
        int invalidLines = 0;

        try (BufferedReader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                if (line.startsWith("PARC;")) {
                    String[] parts = line.split(";", 2);
                    if (parts.length == 2 && !parts[1].trim().isEmpty()) {
                        importedParkName = cleanText(parts[1], 120);
                    }
                } else if (line.startsWith("MAT;")) {
                    Equipment eq = parseEquipment(line.substring(4));
                    if (eq != null) {
                        parsedEquipment.putIfAbsent(eq.getSerialNumber(), eq);
                    } else {
                        invalidLines++;
                    }
                } else if (line.startsWith("ADMIN;")) {
                    String[] parts = line.split(";", -1);
                    if (parts.length >= 7) {
                        try {
                            int sn = Integer.parseInt(parts[1].trim());
                            rawAdminBySn.put(sn, parts);
                        } catch (NumberFormatException e) {
                            invalidLines++;
                        }
                    } else {
                        invalidLines++;
                    }
                } else if (line.startsWith("HD;")) {
                    String[] parts = line.split(";", -1);
                    if (parts.length >= 6) {
                        try {
                            int sn = Integer.parseInt(parts[1].trim());
                            HelpdeskRecord record = new HelpdeskRecord();
                            record.setStatus(EquipmentStatus.fromString(parts[2]));
                            record.setAssignedUser(parts[3]);
                            record.setLocation(parts[4]);
                            record.setLastUpdated(parts[5]);
                            if (parts.length >= 7 && !parts[6].trim().isEmpty()) {
                                for (String rawTicket : parts[6].split("\\|")) {
                                    record.addImportedTicket(rawTicket);
                                }
                            }
                            parsedRecords.put(sn, record);
                        } catch (NumberFormatException e) {
                            invalidLines++;
                        }
                    } else {
                        invalidLines++;
                    }
                } else {
                    invalidLines++;
                }
            }
        }

        if (replaceExisting) {
            this.equipmentBySn.clear();
            this.recordsBySn.clear();
            this.parkName = normalizeParkName(importedParkName);
        }

        int added = 0;
        int duplicatesSkipped = 0;

        for (Equipment eq : parsedEquipment.values()) {
            int sn = eq.getSerialNumber();
            if (!equipmentBySn.containsKey(sn)) {
                String[] admin = rawAdminBySn.get(sn);
                if (admin != null) {
                    applyAdminMetadata(eq, admin);
                }
                equipmentBySn.put(sn, eq);
                recordsBySn.put(sn, parsedRecords.getOrDefault(sn, new HelpdeskRecord()));
                added++;
            } else {
                duplicatesSkipped++;
            }
        }

        return new ImportResult(added, duplicatesSkipped, invalidLines);
    }

    // ==================== PARSING & SECURITY HELPERS ====================

    private void applyAdminMetadata(Equipment equipment, String[] parts) {
        try {
            equipment.setInventoryNumber(parts[2]);
            equipment.setSupplier(parts[3]);
            LocalDate purchaseDate = parts[4].trim().isEmpty() ? null : LocalDate.parse(parts[4].trim());
            LocalDate warrantyDate = parts[5].trim().isEmpty() ? null : LocalDate.parse(parts[5].trim());
            equipment.setDates(purchaseDate, warrantyDate);
            if (!parts[6].trim().isEmpty()) {
                equipment.setPurchasePriceExclTax(new BigDecimal(parts[6].trim().replace(',', '.')));
            }
        } catch (DateTimeParseException | IllegalArgumentException ignored) {
            // Keep safe defaults if optional admin fields are partially corrupted
        }
    }

    private Equipment parseEquipment(String exportLine) {
        String[] parts = exportLine.split(";", -1);
        if (parts.length < 3) {
            return null;
        }
        try {
            int sn = Integer.parseInt(parts[0].trim());
            if (sn <= 0) {
                return null;
            }
            String type = normalizeTypeAlias(parts[1]);

            if ("Computer".equalsIgnoreCase(type) && parts.length >= 6) {
                int ram = Integer.parseInt(parts[4].trim());
                if (parts.length >= 14) {
                    return new Computer(
                            sn, parts[2], parts[3], ram, parts[5],
                            parts[6], parts[7], parts[8], Integer.parseInt(parts[9].trim()),
                            parts[10], parts[11], Boolean.parseBoolean(parts[12].trim()),
                            Boolean.parseBoolean(parts[13].trim())
                    );
                }
                return new Computer(sn, parts[2], parts[3], ram, parts[5]);
            }

            if ("Switch".equalsIgnoreCase(type) && parts.length >= 3) {
                int ports = Integer.parseInt(parts[2].trim());
                if (parts.length >= 11) {
                    return new NetworkSwitch(
                            sn, ports, parts[3], parts[4], parts[5],
                            Boolean.parseBoolean(parts[6].trim()), Boolean.parseBoolean(parts[7].trim()),
                            parts[8], Integer.parseInt(parts[9].trim()), Integer.parseInt(parts[10].trim())
                    );
                }
                return new NetworkSwitch(sn, ports);
            }

            if ("Phone".equalsIgnoreCase(type) && parts.length >= 3) {
                if (parts.length >= 8) {
                    return new Phone(
                            sn, parts[2], parts[3], parts[4],
                            parts[5], parts[6], Boolean.parseBoolean(parts[7].trim())
                    );
                }
                return new Phone(sn, parts[2]);
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        return null;
    }

    private String formatDetailsForCsv(Equipment equipment) {
        if (equipment instanceof Computer) {
            Computer pc = (Computer) equipment;
            return pc.getBrand() + " " + pc.getModel() + " (" + pc.getFormFactor() + ") | Host: " + pc.getHostname()
                    + " | OS: " + pc.getOs() + " | CPU: " + pc.getCpu() + " | RAM: " + pc.getRamGb() + "GB"
                    + " | MAC: " + pc.getMacAddress()
                    + " | Encrypted: " + (pc.isDiskEncrypted() ? "Yes" : "No")
                    + " | EDR/AV: " + (pc.isAntivirusActive() ? "Yes" : "No");
        }
        if (equipment instanceof NetworkSwitch) {
            NetworkSwitch sw = (NetworkSwitch) equipment;
            return sw.getBrand() + " " + sw.getModel() + " (" + sw.getPortSpeed() + ")"
                    + " | Ports: " + sw.getUsedPortCount() + "/" + sw.getEthernetPortCount()
                    + " | PoE: " + (sw.isPoeSupported() ? "Yes" : "No")
                    + " | IP: " + sw.getManagementIp() + " | VLAN: " + sw.getManagementVlan();
        }
        if (equipment instanceof Phone) {
            Phone phone = (Phone) equipment;
            return phone.getBrand() + " " + phone.getModel() + " | OS: " + phone.getOs()
                    + " | Line: " + phone.getLineNumber() + " | IMEI: " + phone.getImei()
                    + " | MDM: " + (phone.isMdmEnrolled() ? "Yes" : "No");
        }
        return equipment.export().replace(";", " | ");
    }

    private static Path resolveSafePath(String fileName) {
        String cleaned = (fileName == null) ? "" : fileName.trim();
        if (cleaned.isEmpty()
                || ".".equals(cleaned)
                || "..".equals(cleaned)
                || cleaned.contains("..")
                || !SAFE_FILENAME_PATTERN.matcher(cleaned).matches()) {
            throw new IllegalArgumentException(
                    "Invalid file name. Use only letters, digits, '.', '-' and '_'.");
        }
        Path baseDir = Paths.get("").toAbsolutePath().normalize();
        Path resolved = baseDir.resolve(cleaned).normalize();
        if (!baseDir.equals(resolved.getParent())) {
            throw new IllegalArgumentException("Path traversal is not allowed.");
        }
        if (Files.exists(resolved) && Files.isDirectory(resolved)) {
            throw new IllegalArgumentException("Target path cannot be a directory.");
        }
        return resolved;
    }

    private static String normalizeTypeAlias(String rawType) {
        if (rawType == null) {
            return "";
        }
        String trimmed = rawType.trim();
        if ("Ordinateur".equalsIgnoreCase(trimmed)) {
            return "Computer";
        }
        if ("Commutateur".equalsIgnoreCase(trimmed)
                || "Commutateur réseau".equalsIgnoreCase(trimmed)
                || "NetworkSwitch".equalsIgnoreCase(trimmed)) {
            return "Switch";
        }
        if ("Telephone".equalsIgnoreCase(trimmed) || "Téléphone".equalsIgnoreCase(trimmed)) {
            return "Phone";
        }
        return trimmed;
    }

    private static String normalizeParkName(String name) {
        String cleaned = cleanText(name, 120);
        return cleaned.isEmpty() ? DEFAULT_PARK_NAME : cleaned;
    }

    public static String cleanText(String text, int maxLength) {
        if (text == null) {
            return "";
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
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }

    private static String protectCsvCell(String value) {
        String cleaned = cleanText(value, 300);
        if (!cleaned.isEmpty()) {
            char first = cleaned.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@') {
                cleaned = "'" + cleaned;
            }
        }
        return "\"" + cleaned.replace("\"", "\"\"") + "\"";
    }

    public static final class ImportResult {
        private final int added;
        private final int duplicatesSkipped;
        private final int invalidLines;

        public ImportResult(int added, int duplicatesSkipped, int invalidLines) {
            this.added = added;
            this.duplicatesSkipped = duplicatesSkipped;
            this.invalidLines = invalidLines;
        }

        public int getAdded() {
            return added;
        }

        public int getDuplicatesSkipped() {
            return duplicatesSkipped;
        }

        public int getInvalidLines() {
            return invalidLines;
        }
    }
}
