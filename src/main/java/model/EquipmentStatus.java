package helpdesk.model;

/**
 * Standard ITIL / Helpdesk lifecycle statuses for IT equipment.
 */
public enum EquipmentStatus {

    IN_STOCK("In stock"),
    IN_USE("In use"),
    BROKEN("Broken / Faulty"),
    IN_REPAIR("In repair"),
    RETIRED("Retired / Decommissioned");

    private final String label;

    EquipmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Parses either English (v1.2) or legacy French (v1.1) status names and labels.
     */
    public static EquipmentStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return IN_STOCK;
        }
        String normalized = value.trim();
        for (EquipmentStatus status : values()) {
            if (status.name().equalsIgnoreCase(normalized) || status.label.equalsIgnoreCase(normalized)) {
                return status;
            }
        }
        // Legacy v1.1 French compatibility
        if ("EN_STOCK".equalsIgnoreCase(normalized) || "En stock".equalsIgnoreCase(normalized)) {
            return IN_STOCK;
        }
        if ("EN_SERVICE".equalsIgnoreCase(normalized) || "En service".equalsIgnoreCase(normalized)) {
            return IN_USE;
        }
        if ("EN_PANNE".equalsIgnoreCase(normalized) || "En panne".equalsIgnoreCase(normalized)) {
            return BROKEN;
        }
        if ("EN_REPARATION".equalsIgnoreCase(normalized) || "En réparation".equalsIgnoreCase(normalized)) {
            return IN_REPAIR;
        }
        if ("REFORME".equalsIgnoreCase(normalized) || "Réformé / Hors service".equalsIgnoreCase(normalized)) {
            return RETIRED;
        }
        return IN_STOCK;
    }
}
