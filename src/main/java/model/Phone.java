// SPDX-License-Identifier: MIT
// Copyright (c) 2026 teremuhamblin — helpDeskJAVA v1.2 Stable

package model;

import java.util.Objects;

public class Telephone extends Materiel {

    private static final String VALEUR_NON_RENSEIGNEE = "Non renseigné";
    private static final String REGEX_IMEI = "^[0-9]{15}$";
    private static final String REGEX_LIGNE = "^[+0-9 .-]{3,20}$";

    private String os;
    private String marque;
    private String modele;
    private String numeroLigne;
    private String imei;
    private boolean superviseMDM;

    /**
     * Constructeur minimal (100 % compatible avec ParcInformatique.java).
     */
    public Telephone(Integer sn, String os) {
        this(sn, os, VALEUR_NON_RENSEIGNEE, VALEUR_NON_RENSEIGNEE, "Non attribué", VALEUR_NON_RENSEIGNEE, false);
    }

    /**
     * Constructeur complet pour une fiche téléphonie / MDM détaillée.
     */
    public Telephone(Integer sn, String os, String marque, String modele,
                     String numeroLigne, String imei, boolean superviseMDM) {
        super(validerSN(sn), "Telephone");
        setOs(os);
        setMarque(marque);
        setModele(modele);
        setNumeroLigne(numeroLigne);
        setImei(imei);
        this.superviseMDM = superviseMDM;
    }

    // ==================== GETTERS & SETTERS SÉCURISÉS ====================

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        String nettoye = nettoyerTexte(os);
        this.os = nettoye.isEmpty() ? "Inconnu" : nettoye;
    }

    public String getMarque() {
        return marque;
    }

    public void setMarque(String marque) {
        String nettoye = nettoyerTexte(marque);
        this.marque = nettoye.isEmpty() ? VALEUR_NON_RENSEIGNEE : nettoye;
    }

    public String getModele() {
        return modele;
    }

    public void setModele(String modele) {
        String nettoye = nettoyerTexte(modele);
        this.modele = nettoye.isEmpty() ? VALEUR_NON_RENSEIGNEE : nettoye;
    }

    public String getNumeroLigne() {
        return numeroLigne;
    }

    public void setNumeroLigne(String numeroLigne) {
        String nettoye = nettoyerTexte(numeroLigne);
        if (nettoye.isEmpty() || "Non attribué".equalsIgnoreCase(nettoye)
                || VALEUR_NON_RENSEIGNEE.equalsIgnoreCase(nettoye)) {
            this.numeroLigne = "Non attribué";
            return;
        }
        if (!nettoye.matches(REGEX_LIGNE)) {
            throw new IllegalArgumentException(
                    "Numéro de ligne invalide (exemples valides : '4012', '0612345678', '+33 6 12 34 56 78').");
        }
        this.numeroLigne = nettoye;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        String nettoye = nettoyerTexte(imei);
        if (nettoye.isEmpty() || VALEUR_NON_RENSEIGNEE.equalsIgnoreCase(nettoye)) {
            this.imei = VALEUR_NON_RENSEIGNEE;
            return;
        }
        if (!nettoye.matches(REGEX_IMEI)) {
            throw new IllegalArgumentException("IMEI invalide : exactement 15 chiffres sont attendus.");
        }
        this.imei = nettoye;
    }

    public boolean isSuperviseMDM() {
        return superviseMDM;
    }

    public void setSuperviseMDM(boolean superviseMDM) {
        this.superviseMDM = superviseMDM;
    }

    // ==================== AFFICHAGE & EXPORT ====================

    @Override
    public void Affiche() {
        System.out.println("  Type         : " + getType() + " | SN : " + getSN());
        System.out.println("  Appareil     : " + marque + " " + modele + " (OS : " + os + ")");
        System.out.println("  Ligne / IMEI : " + numeroLigne + " | IMEI : " + imei);
        System.out.println("  Sécurité MDM : " + (superviseMDM ? "Enrôlé (Géré)" : "Non enrôlé"));
    }

    /**
     * Format rétrocompatible :
     * parts[0]=SN ; parts[1]=Type ; parts[2]=OS ; suivis des champs étendus.
     */
    @Override
    public String export() {
        return getSN() + ";" + getType() + ";" + os + ";"
                + marque + ";" + modele + ";" + numeroLigne + ";"
                + imei + ";" + superviseMDM;
    }

    // ==================== UTILITAIRES & CONTRATS JAVA ====================

    private static Integer validerSN(Integer sn) {
        if (sn == null || sn <= 0) {
            throw new IllegalArgumentException("Le numéro de série (SN) d'un téléphone doit être positif et non nul.");
        }
        return sn;
    }

    private static String nettoyerTexte(String texte) {
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
    public String toString() {
        return "Telephone{SN=" + getSN() + ", marque='" + marque + "', modele='" + modele
                + "', os='" + os + "', ligne='" + numeroLigne + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Telephone)) {
            return false;
        }
        Telephone other = (Telephone) o;
        return Objects.equals(getSN(), other.getSN());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getSN());
    }
}
