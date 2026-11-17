// SPDX-License-Identifier: MIT
// Copyright (c) 2026 teremuhamblin — helpDeskJAVA v1.2 Stable

package model;

package helpdesk.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Helpdesk assignment, status, and incident ticket history linked to an equipment item.
 */
public class HelpdeskRecord implements Serializable {

    private static final long serialVersionUID = 2L;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static final String DEFAULT_ASSIGNEE = "Unassigned";
    public static final String DEFAULT_LOCATION = "IT Stock";

    private EquipmentStatus status;
    private String assignedUser;
    private String location;
    private String lastUpdated;
    private final List<String> tickets;

    public HelpdeskRecord() {
        this.status = EquipmentStatus.IN_STOCK;
        this.assignedUser = DEFAULT_ASSIGNEE;
        this.location = DEFAULT_LOCATION;
        this.lastUpdated = currentTimestamp();
        this.tickets = new ArrayList<>();
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public void setStatus(EquipmentStatus status) {
        this.status = Objects.requireNonNull(status, "Status cannot be null.");
        touchUpdatedAt();
    }

    public String getAssignedUser() {
        return assignedUser;
    }

    public void setAssignedUser(String assignedUser) {
        String cleaned = Equipment.cleanText(assignedUser, "", Equipment.MAX_TEXT_LENGTH);
        if (cleaned.isEmpty() || "Non affecté".equalsIgnoreCase(cleaned)
                || DEFAULT_ASSIGNEE.equalsIgnoreCase(cleaned)) {
            this.assignedUser = DEFAULT_ASSIGNEE;
        } else {
            this.assignedUser = cleaned;
            if (this.status == EquipmentStatus.IN_STOCK) {
                this.status = EquipmentStatus.IN_USE;
            }
        }
        touchUpdatedAt();
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        String cleaned = Equipment.cleanText(location, "", Equipment.MAX_TEXT_LENGTH);
        if (cleaned.isEmpty() || "Stock IT".equalsIgnoreCase(cleaned)
                || DEFAULT_LOCATION.equalsIgnoreCase(cleaned)) {
            this.location = DEFAULT_LOCATION;
        } else {
            this.location = cleaned;
        }
        touchUpdatedAt();
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        String cleaned = Equipment.cleanText(lastUpdated, "", 32);
        this.lastUpdated = cleaned.isEmpty() ? currentTimestamp() : cleaned;
    }

    public List<String> getTickets() {
        return Collections.unmodifiableList(tickets);
    }

    public void addTicket(String description) {
        String cleaned = Equipment.cleanText(description, "", 250);
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Ticket description cannot be empty.");
        }
        this.tickets.add("[" + currentTimestamp() + "] " + cleaned);
        touchUpdatedAt();
    }

    /**
     * Restores an already-timestamped ticket during file import without re-timestamping it.
     */
    public void addImportedTicket(String rawTicket) {
        String cleaned = Equipment.cleanText(rawTicket, "", 280);
        if (!cleaned.isEmpty()) {
            this.tickets.add(cleaned);
        }
    }

    public void touchUpdatedAt() {
        this.lastUpdated = currentTimestamp();
    }

    public static String currentTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }
}
