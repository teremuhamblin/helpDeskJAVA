package helpdesk;

import helpdesk.model.Computer;
import helpdesk.model.Equipment;
import helpdesk.model.EquipmentStatus;
import helpdesk.model.HelpdeskRecord;
import helpdesk.model.NetworkSwitch;
import helpdesk.model.Phone;
import helpdesk.service.HelpdeskService;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Interactive CLI entry point for the Helpdesk & IT Asset Management application.
 */
public class Helpdesk {

    private static final DateTimeFormatter DATE_FORMAT_INPUT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static void main(String[] args) {
        HelpdeskService service = new HelpdeskService();

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Welcome to Helpdesk & IT Asset Management (v1.2 Stable)");

            boolean running = true;
            while (running) {
                displayMenu(service);
                Integer choice = readInt(scanner, "Your choice: ", 0, 12);
                if (choice == null) {
                    break;
                }

                switch (choice) {
                    case 0:
                        if (readConfirmation(scanner, "Are you sure you want to exit?")) {
                            running = false;
                            System.out.println("Closing Helpdesk. Goodbye!");
                        }
                        break;
                    case 1:
                        String newName = readNonEmptyText(scanner, "New inventory name: ");
                        if (newName != null) {
                            service.setParkName(newName);
                            System.out.println("[OK] Inventory renamed to: " + service.getParkName());
                        }
                        break;
                    case 2:
                        handleAddEquipment(scanner, service);
                        break;
                    case 3:
                        handleUpdateRecord(scanner, service, null);
                        break;
                    case 4:
                        handleAddTicket(scanner, service);
                        break;
                    case 5:
                        handleRemoveEquipment(scanner, service);
                        break;
                    case 6:
                        displayAllEquipment(service);
                        break;
                    case 7:
                        handleSearch(scanner, service);
                        break;
                    case 8:
                        System.out.print(service.buildAuditReport());
                        break;
                    case 9:
                        System.out.print(service.buildDashboardReport());
                        break;
                    case 10:
                        handleExport(scanner, service);
                        break;
                    case 11:
                        handleImport(scanner, service);
                        break;
                    case 12:
                        handleCsvExport(scanner, service);
                        break;
                    default:
                        System.out.println("[Error] Unknown menu choice.");
                }
            }
        }
    }

    // ==================== MENU & HANDLERS ====================

    private static void displayMenu(HelpdeskService service) {
        System.out.println("\n================ HELPDESK & IT INVENTORY MENU ================");
        System.out.println(" Inventory: " + service.getParkName() + " | Assets: " + service.getEquipmentCount());
        System.out.println("--------------------------------------------------------------");
        System.out.println(" 1  - Rename inventory");
        System.out.println(" 2  - Add equipment (Computer / Network Switch / Phone)");
        System.out.println(" 3  - Update equipment (Helpdesk / Admin / Technical)");
        System.out.println(" 4  - Log an incident / ticket on equipment");
        System.out.println(" 5  - Remove / retire equipment");
        System.out.println(" 6  - Display all equipment (sorted by SN)");
        System.out.println(" 7  - Search (SN, Type, Status, User, Keyword)");
        System.out.println(" 8  - Audit & Alerts (Warranty, Cybersecurity, Network)");
        System.out.println(" 9  - Dashboard & Statistics");
        System.out.println(" 10 - Export inventory (safe UTF-8 backup)");
        System.out.println(" 11 - Import inventory from file");
        System.out.println(" 12 - Export CSV report (Excel-compatible)");
        System.out.println(" 0  - Exit");
        System.out.println("==============================================================");
    }

    private static void displayAllEquipment(HelpdeskService service) {
        System.out.println("\n=========================================================");
        System.out.println(" IT INVENTORY: " + service.getParkName()
                + " (" + service.getEquipmentCount() + " item(s))");
        System.out.println("=========================================================");

        List<Equipment> all = service.getAllSortedBySerialNumber();
        if (all.isEmpty()) {
            System.out.println("No equipment registered in the inventory.");
            return;
        }
        for (Equipment eq : all) {
            System.out.println(service.formatEquipmentDetails(eq, false));
        }
    }

    private static void handleAddEquipment(Scanner scanner, HelpdeskService service) {
        System.out.println("\nSelect equipment type:");
        System.out.println(" 1 - Computer");
        System.out.println(" 2 - Network Switch");
        System.out.println(" 3 - Phone");
        System.out.println(" 0 - Cancel");

        Integer typeChoice = readInt(scanner, "Your choice: ", 0, 3);
        if (typeChoice == null || typeChoice == 0) {
            return;
        }

        Integer sn = readInt(scanner, "Serial number (SN > 0): ", 1, Integer.MAX_VALUE);
        if (sn == null) {
            return;
        }
        if (service.findBySerialNumber(sn) != null) {
            System.out.println("[Error] Serial number " + sn + " already exists in inventory.");
            return;
        }

        Equipment created = null;
        if (typeChoice == 1) {
            created = promptComputer(scanner, sn);
        } else if (typeChoice == 2) {
            created = promptNetworkSwitch(scanner, sn);
        } else if (typeChoice == 3) {
            created = promptPhone(scanner, sn);
        }

        if (created != null && service.addEquipment(created)) {
            if (readConfirmation(scanner, "Add administrative info (Inventory ID, Warranty, Price)?")) {
                promptAdminMetadata(scanner, created);
            }
            if (readConfirmation(scanner, "Assign Helpdesk metadata now?")) {
                handleUpdateRecord(scanner, service, sn);
            }
            System.out.println("[OK] Equipment (SN " + sn + ") added successfully.");
        }
    }

    private static void handleRemoveEquipment(Scanner scanner, HelpdeskService service) {
        Integer sn = readInt(scanner, "Enter SN to remove: ", 1, Integer.MAX_VALUE);
        if (sn == null) {
            return;
        }
        if (service.findBySerialNumber(sn) == null) {
            System.out.println("[Info] No equipment found with SN " + sn + ".");
            return;
        }
        if (readConfirmation(scanner, "Confirm permanent deletion of SN " + sn + "?")) {
            if (service.removeEquipment(sn)) {
                System.out.println("[OK] Equipment (SN " + sn + ") removed.");
            }
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    private static void handleAddTicket(Scanner scanner, HelpdeskService service) {
        Integer sn = readInt(scanner, "Enter target SN: ", 1, Integer.MAX_VALUE);
        if (sn == null) {
            return;
        }
        if (service.findBySerialNumber(sn) == null) {
            System.out.println("[Info] No equipment found with SN " + sn + ".");
            return;
        }
        String description = readNonEmptyText(scanner, "Incident / intervention description: ");
        if (description == null) {
            return;
        }
        HelpdeskRecord record = service.getOrCreateRecord(sn);
        record.addTicket(description);

        if (readConfirmation(scanner, "Set equipment status to 'Broken / Faulty'?")) {
            record.setStatus(EquipmentStatus.BROKEN);
        }
        System.out.println("[OK] Ticket logged for SN " + sn + ".");
    }

    private static void handleSearch(Scanner scanner, HelpdeskService service) {
        System.out.println("\nSearch criteria:");
        System.out.println(" 1 - By Serial Number (SN + ticket history)");
        System.out.println(" 2 - By Type (Computer / Switch / Phone)");
        System.out.println(" 3 - By Helpdesk Status");
        System.out.println(" 4 - By Assigned User");
        System.out.println(" 5 - Global keyword search (Hostname, IP, Model, Line...)");
        System.out.println(" 0 - Cancel");

        Integer choice = readInt(scanner, "Your choice: ", 0, 5);
        if (choice == null || choice == 0) {
            return;
        }

        if (choice == 1) {
            Integer sn = readInt(scanner, "SN to search: ", 1, Integer.MAX_VALUE);
            if (sn != null) {
                Equipment eq = service.findBySerialNumber(sn);
                if (eq != null) {
                    System.out.println("[OK] Equipment found:");
                    System.out.println(service.formatEquipmentDetails(eq, true));
                } else {
                    System.out.println("[Info] No equipment found with SN " + sn + ".");
                }
            }
        } else if (choice == 2) {
            String type = readNonEmptyText(scanner, "Type (Computer / Switch / Phone): ");
            if (type != null) {
                printSearchResults(service, service.findByType(type), "type \"" + type + "\"");
            }
        } else if (choice == 3) {
            EquipmentStatus[] statuses = EquipmentStatus.values();
            for (int i = 0; i < statuses.length; i++) {
                System.out.println(" " + (i + 1) + " - " + statuses[i].getLabel());
            }
            Integer idx = readInt(scanner, "Filter by status: ", 1, statuses.length);
            if (idx != null) {
                EquipmentStatus selected = statuses[idx - 1];
                printSearchResults(service, service.findByStatus(selected), "status \"" + selected.getLabel() + "\"");
            }
        } else if (choice == 4) {
            String user = readNonEmptyText(scanner, "Assigned user (full or partial name): ");
            if (user != null) {
                printSearchResults(service, service.findByUser(user), "user \"" + user + "\"");
            }
        } else if (choice == 5) {
            String keyword = readNonEmptyText(scanner, "Keyword: ");
            if (keyword != null) {
                printSearchResults(service, service.searchByKeyword(keyword), "keyword \"" + keyword + "\"");
            }
        }
    }

    private static void printSearchResults(HelpdeskService service, List<Equipment> results, String criteriaLabel) {
        if (results.isEmpty()) {
            System.out.println("[Info] No equipment matched " + criteriaLabel + ".");
            return;
        }
        for (Equipment eq : results) {
            System.out.println(service.formatEquipmentDetails(eq, false));
        }
        System.out.println("Total found: " + results.size());
    }

    private static void handleExport(Scanner scanner, HelpdeskService service) {
        System.out.print("Export filename [Enter for '" + HelpdeskService.DEFAULT_EXPORT_FILE + "']: ");
        String input = readLine(scanner);
        if (input == null) {
            return;
        }
        String file = input.trim().isEmpty() ? HelpdeskService.DEFAULT_EXPORT_FILE : input.trim();
        try {
            Path exported = service.exportInventory(file);
            System.out.println("[OK] Inventory exported safely to: " + exported.getFileName());
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("[Error] Export failed: " + e.getMessage());
        }
    }

    private static void handleImport(Scanner scanner, HelpdeskService service) {
        System.out.print("Import filename [Enter for '" + HelpdeskService.DEFAULT_EXPORT_FILE + "']: ");
        String input = readLine(scanner);
        if (input == null) {
            return;
        }
        String file = input.trim().isEmpty() ? HelpdeskService.DEFAULT_EXPORT_FILE : input.trim();
        boolean replace = readConfirmation(scanner, "Replace current inventory (N = merge without overwriting)?");
        try {
            HelpdeskService.ImportResult res = service.importInventory(file, replace);
            System.out.println("[OK] Import complete: " + res.getAdded() + " added, "
                    + res.getDuplicatesSkipped() + " duplicate(s) skipped, "
                    + res.getInvalidLines() + " invalid line(s).");
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("[Error] Import failed: " + e.getMessage());
        }
    }

    private static void handleCsvExport(Scanner scanner, HelpdeskService service) {
        System.out.print("CSV report filename [Enter for '" + HelpdeskService.DEFAULT_CSV_FILE + "']: ");
        String input = readLine(scanner);
        if (input == null) {
            return;
        }
        String file = input.trim().isEmpty() ? HelpdeskService.DEFAULT_CSV_FILE : input.trim();
        try {
            Path csvPath = service.exportCsvReport(file);
            System.out.println("[OK] Excel CSV report generated: " + csvPath.getFileName());
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("[Error] CSV export failed: " + e.getMessage());
        }
    }

    // ==================== EQUIPMENT CREATION & UPDATE WIZARDS ====================

    private static Computer promptComputer(Scanner scanner, int sn) {
        String os = readNonEmptyText(scanner, "OS (e.g., Windows 11 Pro, Ubuntu 24.04): ");
        String cpu = readNonEmptyText(scanner, "CPU: ");
        Integer ram = readInt(scanner, "RAM (GB): ", 1, 4096);
        String gpu = readNonEmptyText(scanner, "GPU: ");
        if (os == null || cpu == null || ram == null || gpu == null) {
            return null;
        }

        Computer.Builder builder = Computer.builder(sn, os, cpu, ram, gpu);
        String brand = readOptionalText(scanner, "Brand (e.g., Dell, Lenovo) [Enter = Unset]: ", "Unset");
        String model = readOptionalText(scanner, "Model (e.g., Latitude 5540) [Enter = Unset]: ", "Unset");
        String form = readOptionalText(scanner, "Form factor (Laptop / Desktop) [Enter = Unset]: ", "Unset");
        Integer storage = readOptionalInt(scanner, "Storage (GB) [Enter = 0]: ", 0, 102_400, 0);
        if (brand == null || model == null || form == null || storage == null) {
            return null;
        }

        Computer computer = builder.brand(brand).model(model).formFactor(form).storageGb(storage).build();

        while (true) {
            System.out.print("Hostname (e.g., PC-FINANCE-01) [Enter = Unset]: ");
            String host = readLine(scanner);
            if (host == null) {
                return null;
            }
            try {
                computer.setHostname(host);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("[Error] " + e.getMessage());
            }
        }

        while (true) {
            System.out.print("MAC address (AA:BB:CC:DD:EE:FF) [Enter = Unset]: ");
            String mac = readLine(scanner);
            if (mac == null) {
                return null;
            }
            try {
                computer.setMacAddress(mac);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("[Error] " + e.getMessage());
            }
        }

        computer.setDiskEncrypted(readConfirmation(scanner, "Disk encryption enabled (BitLocker / FileVault)?"));
        computer.setAntivirusActive(readConfirmation(scanner, "Antivirus / EDR active?"));
        return computer;
    }

    private static NetworkSwitch promptNetworkSwitch(Scanner scanner, int sn) {
        Integer ports = readInt(scanner, "Ethernet port count (1..512): ", 1, 512);
        if (ports == null) {
            return null;
        }

        String brand = readOptionalText(scanner, "Brand (e.g., Cisco, Aruba) [Enter = Unset]: ", "Unset");
        String model = readOptionalText(scanner, "Model (e.g., Catalyst 2960-X) [Enter = Unset]: ", "Unset");
        String speed = readOptionalText(scanner, "Port speed [Enter = 1 Gbps]: ", "1 Gbps");
        if (brand == null || model == null || speed == null) {
            return null;
        }

        NetworkSwitch sw = NetworkSwitch.builder(sn, ports)
                .brand(brand)
                .model(model)
                .portSpeed(speed)
                .managed(readConfirmation(scanner, "Managed switch (L2/L3)?"))
                .poeSupported(readConfirmation(scanner, "Supports PoE?"))
                .build();

        while (true) {
            System.out.print("Management IP (e.g., 192.168.10.2 or DHCP) [Enter = Unassigned]: ");
            String ip = readLine(scanner);
            if (ip == null) {
                return null;
            }
            try {
                sw.setManagementIp(ip);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("[Error] " + e.getMessage());
            }
        }

        Integer vlan = readOptionalInt(scanner, "Management VLAN (1..4094) [Enter = 1]: ", 1, 4094, 1);
        Integer used = readOptionalInt(scanner, "Used ports (0.." + ports + ") [Enter = 0]: ", 0, ports, 0);
        if (vlan == null || used == null) {
            return null;
        }
        sw.setManagementVlan(vlan);
        sw.setUsedPortCount(used);
        return sw;
    }

    private static Phone promptPhone(Scanner scanner, int sn) {
        String os = readNonEmptyText(scanner, "OS (e.g., iOS 18, Android 15): ");
        String brand = readOptionalText(scanner, "Brand (e.g., Apple, Samsung) [Enter = Unset]: ", "Unset");
        String model = readOptionalText(scanner, "Model (e.g., iPhone 15, Galaxy S24) [Enter = Unset]: ", "Unset");
        if (os == null || brand == null || model == null) {
            return null;
        }

        Phone phone = Phone.builder(sn, os).brand(brand).model(model).build();

        while (true) {
            System.out.print("Line number (e.g., 4012, +33 6 12 34 56 78) [Enter = Unassigned]: ");
            String line = readLine(scanner);
            if (line == null) {
                return null;
            }
            try {
                phone.setLineNumber(line);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("[Error] " + e.getMessage());
            }
        }

        while (true) {
            System.out.print("IMEI (15 digits) [Enter = Unset]: ");
            String imei = readLine(scanner);
            if (imei == null) {
                return null;
            }
            try {
                phone.setImei(imei);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("[Error] " + e.getMessage());
            }
        }

        phone.setMdmEnrolled(readConfirmation(scanner, "Enrolled in MDM?"));
        return phone;
    }

    private static void promptAdminMetadata(Scanner scanner, Equipment eq) {
        String inv = readOptionalText(scanner, "Inventory ID [Enter = keep '" + eq.getInventoryNumber() + "']: ",
                eq.getInventoryNumber());
        String supplier = readOptionalText(scanner, "Supplier [Enter = keep '" + eq.getSupplier() + "']: ",
                eq.getSupplier());
        if (inv != null) {
            eq.setInventoryNumber(inv);
        }
        if (supplier != null) {
            eq.setSupplier(supplier);
        }

        while (true) {
            System.out.print("Purchase date (DD/MM/YYYY or YYYY-MM-DD) [Enter = skip]: ");
            String raw = readLine(scanner);
            if (raw == null) {
                return;
            }
            if (raw.trim().isEmpty()) {
                break;
            }
            try {
                eq.setPurchaseDate(parseDateInput(raw));
                break;
            } catch (DateTimeParseException | IllegalArgumentException e) {
                System.out.println("[Error] " + e.getMessage());
            }
        }

        while (true) {
            System.out.print("Warranty end date (DD/MM/YYYY or YYYY-MM-DD) [Enter = skip]: ");
            String raw = readLine(scanner);
            if (raw == null) {
                return;
            }
            if (raw.trim().isEmpty()) {
                break;
            }
            try {
                eq.setWarrantyEndDate(parseDateInput(raw));
                break;
            } catch (DateTimeParseException | IllegalArgumentException e) {
                System.out.println("[Error] " + e.getMessage());
            }
        }

        Double price = readOptionalDouble(scanner,
                "Purchase price excl. tax (EUR) [Enter = " + eq.getPurchasePriceExclTax() + "]: ",
                0.0, eq.getPurchasePriceExclTax().doubleValue());
        if (price != null) {
            eq.setPurchasePriceExclTax(price);
        }
    }

    private static void handleUpdateRecord(Scanner scanner, HelpdeskService service, Integer optionalSn) {
        Integer sn = optionalSn;
        if (sn == null) {
            sn = readInt(scanner, "Enter SN to update: ", 1, Integer.MAX_VALUE);
            if (sn == null) {
                return;
            }
        }
        Equipment eq = service.findBySerialNumber(sn);
        if (eq == null) {
            System.out.println("[Info] No equipment found with SN " + sn + ".");
            return;
        }
        HelpdeskRecord record = service.getOrCreateRecord(sn);

        while (true) {
            System.out.println("\n--- Update SN " + sn + " (" + eq.getType() + ") ---");
            System.out.println(" 1 - Helpdesk status    : " + record.getStatus().getLabel());
            System.out.println(" 2 - Assigned user      : " + record.getAssignedUser());
            System.out.println(" 3 - Location           : " + record.getLocation());
            System.out.println(" 4 - Admin metadata     : " + eq.getInventoryNumber() + " | " + eq.getSupplier()
                    + " | " + eq.getWarrantyStatus());

            if (eq instanceof Computer) {
                Computer pc = (Computer) eq;
                System.out.println(" 5 - Hostname / MAC     : " + pc.getHostname() + " / " + pc.getMacAddress());
                System.out.println(" 6 - RAM / Storage      : " + pc.getRamGb() + " GB / " + pc.getStorageGb() + " GB");
                System.out.println(" 7 - Security (Enc/AV)  : Encryption=" + (pc.isDiskEncrypted() ? "Yes" : "No")
                        + ", EDR/AV=" + (pc.isAntivirusActive() ? "Yes" : "No"));
            } else if (eq instanceof NetworkSwitch) {
                NetworkSwitch sw = (NetworkSwitch) eq;
                System.out.println(" 5 - Used ports         : " + sw.getUsedPortCount() + " / " + sw.getEthernetPortCount());
                System.out.println(" 6 - Management IP/VLAN : " + sw.getManagementIp() + " / VLAN " + sw.getManagementVlan());
                System.out.println(" 7 - PoE / Managed      : PoE=" + (sw.isPoeSupported() ? "Yes" : "No")
                        + ", Managed=" + (sw.isManaged() ? "Yes" : "No"));
            } else if (eq instanceof Phone) {
                Phone phone = (Phone) eq;
                System.out.println(" 5 - Line number        : " + phone.getLineNumber());
                System.out.println(" 6 - IMEI               : " + phone.getImei());
                System.out.println(" 7 - MDM enrollment     : " + (phone.isMdmEnrolled() ? "Yes" : "No"));
            }
            System.out.println(" 0 - Back to main menu");

            int maxOption = (eq instanceof Computer || eq instanceof NetworkSwitch || eq instanceof Phone) ? 7 : 4;
            Integer choice = readInt(scanner, "Field to update: ", 0, maxOption);
            if (choice == null || choice == 0) {
                return;
            }

            if (choice == 1) {
                EquipmentStatus[] statuses = EquipmentStatus.values();
                for (int i = 0; i < statuses.length; i++) {
                    System.out.println(" " + (i + 1) + " - " + statuses[i].getLabel());
                }
                Integer idx = readInt(scanner, "New status: ", 1, statuses.length);
                if (idx != null) {
                    record.setStatus(statuses[idx - 1]);
                    System.out.println("[OK] Status updated.");
                }
            } else if (choice == 2) {
                String user = readNonEmptyText(scanner, "Assigned user / department (or 'Unassigned'): ");
                if (user != null) {
                    record.setAssignedUser(user);
                    System.out.println("[OK] Assigned user updated.");
                }
            } else if (choice == 3) {
                String loc = readNonEmptyText(scanner, "Location (building, room, rack): ");
                if (loc != null) {
                    record.setLocation(loc);
                    System.out.println("[OK] Location updated.");
                }
            } else if (choice == 4) {
                promptAdminMetadata(scanner, eq);
                record.touchUpdatedAt();
                System.out.println("[OK] Admin metadata updated.");
            } else if (eq instanceof Computer) {
                updateComputerFields(scanner, (Computer) eq, record, choice);
            } else if (eq instanceof NetworkSwitch) {
                updateSwitchFields(scanner, (NetworkSwitch) eq, record, choice);
            } else if (eq instanceof Phone) {
                updatePhoneFields(scanner, (Phone) eq, record, choice);
            }
        }
    }

    private static void updateComputerFields(Scanner scanner, Computer pc, HelpdeskRecord record, int choice) {
        if (choice == 5) {
            try {
                System.out.print("New Hostname [Enter = keep]: ");
                String host = readLine(scanner);
                if (host != null && !host.trim().isEmpty()) {
                    pc.setHostname(host);
                }
                System.out.print("New MAC address [Enter = keep]: ");
                String mac = readLine(scanner);
                if (mac != null && !mac.trim().isEmpty()) {
                    pc.setMacAddress(mac);
                }
                record.touchUpdatedAt();
                System.out.println("[OK] Network identifiers updated.");
            } catch (IllegalArgumentException e) {
                System.out.println("[Error] " + e.getMessage());
            }
        } else if (choice == 6) {
            Integer ram = readOptionalInt(scanner, "New RAM (GB) [Enter = " + pc.getRamGb() + "]: ", 1, 4096, pc.getRamGb());
            Integer disk = readOptionalInt(scanner, "New Storage (GB) [Enter = " + pc.getStorageGb() + "]: ", 0, 102_400, pc.getStorageGb());
            if (ram != null && disk != null) {
                pc.setRamGb(ram);
                pc.setStorageGb(disk);
                record.touchUpdatedAt();
                System.out.println("[OK] Hardware resources updated.");
            }
        } else if (choice == 7) {
            pc.setDiskEncrypted(readConfirmation(scanner, "Disk encryption enabled?"));
            pc.setAntivirusActive(readConfirmation(scanner, "Antivirus / EDR active?"));
            record.touchUpdatedAt();
            System.out.println("[OK] Security compliance updated.");
        }
    }

    private static void updateSwitchFields(Scanner scanner, NetworkSwitch sw, HelpdeskRecord record, int choice) {
        if (choice == 5) {
            Integer used = readInt(scanner, "Used ports (0.." + sw.getEthernetPortCount() + "): ", 0, sw.getEthernetPortCount());
            if (used != null) {
                sw.setUsedPortCount(used);
                record.touchUpdatedAt();
                System.out.println("[OK] Port usage updated.");
            }
        } else if (choice == 6) {
            try {
                System.out.print("New management IP [Enter = keep]: ");
                String ip = readLine(scanner);
                if (ip != null && !ip.trim().isEmpty()) {
                    sw.setManagementIp(ip);
                }
                Integer vlan = readOptionalInt(scanner, "New VLAN (1..4094) [Enter = " + sw.getManagementVlan() + "]: ",
                        1, 4094, sw.getManagementVlan());
                if (vlan != null) {
                    sw.setManagementVlan(vlan);
                }
                record.touchUpdatedAt();
                System.out.println("[OK] Network configuration updated.");
            } catch (IllegalArgumentException e) {
                System.out.println("[Error] " + e.getMessage());
            }
        } else if (choice == 7) {
            sw.setPoeSupported(readConfirmation(scanner, "PoE supported?"));
            sw.setManaged(readConfirmation(scanner, "Managed switch (L2/L3)?"));
            record.touchUpdatedAt();
            System.out.println("[OK] Switch capabilities updated.");
        }
    }

    private static void updatePhoneFields(Scanner scanner, Phone phone, HelpdeskRecord record, int choice) {
        if (choice == 5) {
            System.out.print("New line number (Enter = Unassigned): ");
            String line = readLine(scanner);
            if (line != null) {
                try {
                    phone.setLineNumber(line);
                    record.touchUpdatedAt();
                    System.out.println("[OK] Line number updated.");
                } catch (IllegalArgumentException e) {
                    System.out.println("[Error] " + e.getMessage());
                }
            }
        } else if (choice == 6) {
            System.out.print("New 15-digit IMEI (Enter = Unset): ");
            String imei = readLine(scanner);
            if (imei != null) {
                try {
                    phone.setImei(imei);
                    record.touchUpdatedAt();
                    System.out.println("[OK] IMEI updated.");
                } catch (IllegalArgumentException e) {
                    System.out.println("[Error] " + e.getMessage());
                }
            }
        } else if (choice == 7) {
            phone.setMdmEnrolled(readConfirmation(scanner, "Enrolled in MDM?"));
            record.touchUpdatedAt();
            System.out.println("[OK] MDM status updated.");
        }
    }

    // ==================== CLI INPUT HELPERS ====================

    private static String readLine(Scanner scanner) {
        return scanner.hasNextLine() ? scanner.nextLine() : null;
    }

    private static Integer readInt(Scanner scanner, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = readLine(scanner);
            if (input == null) {
                return null;
            }
            try {
                int value = Integer.parseInt(input.trim());
                if (value < min || value > max) {
                    System.out.println("[Error] Please enter a number between " + min + " and " + max + ".");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("[Error] Invalid input. Integer expected.");
            }
        }
    }

    private static Integer readOptionalInt(Scanner scanner, String prompt, int min, int max, int defaultValue) {
        while (true) {
            System.out.print(prompt);
            String input = readLine(scanner);
            if (input == null) {
                return null;
            }
            if (input.trim().isEmpty()) {
                return defaultValue;
            }
            try {
                int value = Integer.parseInt(input.trim());
                if (value < min || value > max) {
                    System.out.println("[Error] Please enter a number between " + min + " and " + max + ".");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("[Error] Invalid input. Integer expected.");
            }
        }
    }

    private static Double readOptionalDouble(Scanner scanner, String prompt, double min, double defaultValue) {
        while (true) {
            System.out.print(prompt);
            String input = readLine(scanner);
            if (input == null) {
                return null;
            }
            if (input.trim().isEmpty()) {
                return defaultValue;
            }
            try {
                double val = Double.parseDouble(input.trim().replace(',', '.'));
                if (Double.isNaN(val) || Double.isInfinite(val) || val < min) {
                    System.out.println("[Error] Please enter a valid amount (>= " + min + ").");
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                System.out.println("[Error] Invalid amount (e.g., 899.90).");
            }
        }
    }

    private static LocalDate parseDateInput(String raw) {
        String trimmed = raw.trim();
        try {
            return LocalDate.parse(trimmed, DATE_FORMAT_INPUT);
        } catch (DateTimeParseException ignored) {
            return LocalDate.parse(trimmed); // Accepts ISO YYYY-MM-DD as well
        }
    }

    private static String readNonEmptyText(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = readLine(scanner);
            if (input == null) {
                return null;
            }
            String cleaned = HelpdeskService.cleanText(input, 200);
            if (!cleaned.isEmpty()) {
                return cleaned;
            }
            System.out.println("[Error] This field cannot be empty.");
        }
    }

    private static String readOptionalText(Scanner scanner, String prompt, String defaultValue) {
        System.out.print(prompt);
        String input = readLine(scanner);
        if (input == null) {
            return null;
        }
        String cleaned = HelpdeskService.cleanText(input, 120);
        return cleaned.isEmpty() ? defaultValue : cleaned;
    }

    private static boolean readConfirmation(Scanner scanner, String prompt) {
        System.out.print(prompt + " (Y/N): ");
        String reply = readLine(scanner);
        if (reply == null) {
            return false;
        }
        String trimmed = reply.trim();
        return "Y".equalsIgnoreCase(trimmed)
                || "YES".equalsIgnoreCase(trimmed)
                || "O".equalsIgnoreCase(trimmed)
                || "OUI".equalsIgnoreCase(trimmed);
    }
}
