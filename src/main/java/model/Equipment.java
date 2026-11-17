// SPDX-License-Identifier: MIT
// Copyright (c) 2026 teremuhamblin — helpDeskJAVA v1.2 Stable

package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class Materiel implements Serializable, Comparable<Materiel> {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMAT_DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Integer sn;
    private String type;
    private String numeroInventaire;
    private String fournisseur;
    private LocalDate dateAchat;
    private LocalDate dateFinGarantie;
    private double prixAchatHT;

    /**
     * Constructeur de base (100 % compatible avec Telephone, Switch et Ordinateur).
     */
    public Materiel(Integer sn, String type) {
        this(sn, type, "Non attribué", "Non renseigné", null, null, 0.0);
    }

    /**
     * Constructeur complet incluant les informations administratives et de garantie.
     */
    public Materiel(Integer sn, String type, String numeroInventaire, String fournisseur,
                    LocalDate dateAchat, LocalDate dateFinGarantie, double prixAchatHT) {
        setSN(sn);
        setType(type);
        setNumeroInventaire(numeroInventaire);
        setFournisseur(fournisseur);
        setDateAchat(dateAchat);
        setDateFinGarantie(dateFinGarantie);
        setPrixAchatHT(prixAchatHT);
    }

    // ==================== GETTERS & SETTERS SÉCURISÉS ====================

    public Integer getSN() {
        return sn;
    }

    public void setSN(Integer sn) {
        this.sn = validerSN(sn);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        String nettoye = nettoyerTexte(type);
        if (nettoye.isEmpty()) {
            throw new IllegalArgumentException("Le type de matériel ne peut pas être vide.");
        }
        this.type = nettoye;
    }

    public String getNumeroInventaire() {
        return numeroInventaire;
    }

    public void setNumeroInventaire(String numeroInventaire) {
        String nettoye = nettoyerTexte(numeroInventaire);
        this.numeroInventaire = nettoye.isEmpty() ? "Non attribué" : nettoye;
    }

    public String getFournisseur() {
        return fournisseur;
    }

    public void setFournisseur(String fournisseur) {
        String nettoye = nettoyerTexte(fournisseur);
        this.fournisseur = nettoye.isEmpty() ? "Non renseigné" : nettoye;
    }

    public LocalDate getDateAchat() {
        return dateAchat;
    }

    public void setDateAchat(LocalDate dateAchat) {
        if (dateAchat != null && this.dateFinGarantie != null && this.dateFinGarantie.isBefore(dateAchat)) {
            throw new IllegalArgumentException(
                    "La date d'achat ne peut pas être postérieure à la date de fin de garantie.");
        }
        this.dateAchat = dateAchat;
    }

    public LocalDate getDateFinGarantie() {
        return dateFinGarantie;
    }

    public void setDateFinGarantie(LocalDate dateFinGarantie) {
        if (dateFinGarantie != null && this.dateAchat != null && dateFinGarantie.isBefore(this.dateAchat)) {
            throw new IllegalArgumentException(
                    "La date de fin de garantie ne peut pas être antérieure à la date d'achat.");
        }
        this.dateFinGarantie = dateFinGarantie;
    }

    public double getPrixAchatHT() {
        return prixAchatHT;
    }

    public void setPrixAchatHT(double prixAchatHT) {
        if (Double.isNaN(prixAchatHT) || Double.isInfinite(prixAchatHT) || prixAchatHT < 0.0) {
            throw new IllegalArgumentException("Le prix d'achat HT doit être un nombre positif ou nul.");
        }
        this.prixAchatHT = prixAchatHT;
    }

    // ==================== MÉTHODES MÉTIER GARANTIE & ASSET ====================

    public boolean estSousGarantie() {
        return dateFinGarantie != null && !LocalDate.now().isAfter(dateFinGarantie);
    }

    public String getStatutGarantie() {
        if (dateFinGarantie == null) {
            return "Non renseignée";
        }
        LocalDate aujourdHui = LocalDate.now();
        if (!aujourdHui.isAfter(dateFinGarantie)) {
            long joursRestants = ChronoUnit.DAYS.between(aujourdHui, dateFinGarantie);
            return "Sous garantie (" + joursRestants + " jour(s) restant(s), jusqu'au "
                    + dateFinGarantie.format(FORMAT_DATE_FR) + ")";
        }
        return "Expirée depuis le " + dateFinGarantie.format(FORMAT_DATE_FR);
    }

    public void afficherInfosAdministratives() {
        System.out.println("  [Admin]    Inventaire   : " + numeroInventaire + " | Fournisseur : " + fournisseur);
        System.out.println("  [Admin]    Achat        : "
                + (dateAchat != null ? dateAchat.format(FORMAT_DATE_FR) : "Non renseignée")
                + (prixAchatHT > 0 ? String.format(" (%.2f € HT)", prixAchatHT) : ""));
        System.out.println("  [Admin]    Garantie     : " + getStatutGarantie());
    }

    // ==================== AFFICHAGE & EXPORT ====================

    public void Affiche() {
        System.out.println("  Type         : " + type + " | SN : " + sn);
    }

    public String export() {
        return sn + ";" + type;
    }

    // ==================== UTILITAIRES PROTÉGÉS & CONTRATS JAVA ====================

    protected static Integer validerSN(Integer sn) {
        if (sn == null || sn <= 0) {
            throw new IllegalArgumentException("Le numéro de série (SN) doit être un entier positif non nul.");
        }
        return sn;
    }

    protected static String nettoyerTexte(String texte) {
        if (texte == null) {
            return "";
        }
        return texte.trim()
                .replace(";", ",")
                .replace("|", "/")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    @Override
    public int compareTo(Materiel autre) {
        if (autre == null) {
            return 1;
        }
        return Integer.compare(this.sn, autre.sn);
    }

    @Override
    public String toString() {
        return "Materiel{SN=" + sn + ", type='" + type + "', inventaire='" + numeroInventaire + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Materiel)) {
            return false;
        }
        Materiel other = (Materiel) o;
        return Objects.equals(this.sn, other.sn);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sn);
    }
}
