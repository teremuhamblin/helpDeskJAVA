import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class Helpdesk {

    private static final String FICHIER_DEFAUT = "parc_helpdesk.txt";
    private static final String FICHIER_CSV_DEFAUT = "parc_rapport.csv";
    private static final long TAILLE_MAX_IMPORT_OCTETS = 10L * 1024 * 1024; // 10 Mo
    private static final int SEUIL_ALERTE_GARANTIE_JOURS = 30;
    private static final double SEUIL_ALERTE_SWITCH_PCT = 85.0;

    private static final DateTimeFormatter FORMAT_DATE_HEURE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FORMAT_DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Statuts standards ITIL / Helpdesk pour le cycle de vie d'un équipement.
     */
    public enum StatutMateriel {
        EN_STOCK("En stock"),
        EN_SERVICE("En service"),
        EN_PANNE("En panne"),
        EN_REPARATION("En réparation"),
        REFORME("Réformé / Hors service");

        private final String libelle;

        StatutMateriel(String libelle) {
            this.libelle = libelle;
        }

        public String getLibelle() {
            return libelle;
        }

        public static StatutMateriel depuisChaine(String valeur) {
            if (valeur == null) {
                return EN_STOCK;
            }
            for (StatutMateriel s : values()) {
                if (s.name().equalsIgnoreCase(valeur.trim()) || s.libelle.equalsIgnoreCase(valeur.trim())) {
                    return s;
                }
            }
            return EN_STOCK;
        }
    }

    /**
     * Fiche Helpdesk associée à chaque matériel (indexée par SN).
     */
    private static class FicheHelpdesk {
        private StatutMateriel statut;
        private String utilisateur;
        private String localisation;
        private String derniereMaj;
        private final List<String> tickets;

        public FicheHelpdesk() {
            this.statut = StatutMateriel.EN_STOCK;
            this.utilisateur = "Non affecté";
            this.localisation = "Stock IT";
            this.derniereMaj = horodatageActuel();
            this.tickets = new ArrayList<>();
        }

        public void toucherMaj() {
            this.derniereMaj = horodatageActuel();
        }
    }

    private String name;
    private final ArrayList<Materiel> materielList;
    private final Map<Integer, FicheHelpdesk> fichesHelpdesk;

    public Helpdesk() {
        this("Parc informatique principal");
    }

    public Helpdesk(String name) {
        this.name = (name == null || name.trim().isEmpty()) ? "Parc informatique principal" : nettoyerTexte(name);
        this.materielList = new ArrayList<>();
        this.fichesHelpdesk = new LinkedHashMap<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            System.out.println("[Erreur] Le nom du parc ne peut pas être vide.");
            return;
        }
        this.name = nettoyerTexte(name);
    }

    // ==================== GESTION DU MATÉRIEL ====================

    public boolean Add(Materiel materiel) {
        if (materiel == null || materiel.getSN() == null || materiel.getSN() <= 0) {
            System.out.println("[Erreur] Matériel invalide ou numéro de série (SN) incorrect.");
            return false;
        }
        if (trouverParSN(materiel.getSN()) != null) {
            System.out.println("[Erreur] Un matériel avec le SN " + materiel.getSN() + " existe déjà dans le parc.");
            return false;
        }
        this.materielList.add(materiel);
        this.fichesHelpdesk.putIfAbsent(materiel.getSN(), new FicheHelpdesk());
        return true;
    }

    public boolean Remove(Integer sn) {
        if (sn == null) {
            return false;
        }
        for (int i = 0; i < this.materielList.size(); i++) {
            Materiel m = this.materielList.get(i);
            if (m.getSN() != null && m.getSN().equals(sn)) {
                this.materielList.remove(i);
                this.fichesHelpdesk.remove(sn);
                System.out.println("[OK] Matériel (SN " + sn + ") supprimé avec succès.");
                return true;
            }
        }
        System.out.println("[Info] Aucun matériel trouvé avec le SN " + sn + ".");
        return false;
    }

    private Materiel trouverParSN(Integer sn) {
        if (sn == null) {
            return null;
        }
        for (Materiel m : this.materielList) {
            if (m.getSN() != null && m.getSN().equals(sn)) {
                return m;
            }
        }
        return null;
    }

    private FicheHelpdesk obtenirOuCreerFiche(Integer sn) {
        return this.fichesHelpdesk.computeIfAbsent(sn, k -> new FicheHelpdesk());
    }

    // ==================== AFFICHAGE & RECHERCHE ====================

    public void Affiche() {
        System.out.println("\n=========================================================");
        System.out.println(" PARC INFORMATIQUE : " + this.name + " (" + this.materielList.size() + " équipement(s))");
        System.out.println("=========================================================");
        if (this.materielList.isEmpty()) {
            System.out.println("Aucun matériel enregistré dans le parc.");
            return;
        }
        List<Materiel> tries = new ArrayList<>(this.materielList);
        Collections.sort(tries);
        for (Materiel m : tries) {
            afficherDetailsComplets(m, false);
        }
    }

    private void afficherDetailsComplets(Materiel m, boolean afficherTickets) {
        System.out.println("---------------------------------------------------------");
        m.Affiche();
        m.afficherInfosAdministratives();
        FicheHelpdesk fiche = obtenirOuCreerFiche(m.getSN());
        System.out.println("  [Helpdesk] Statut       : " + fiche.statut.getLibelle());
        System.out.println("  [Helpdesk] Utilisateur  : " + fiche.utilisateur);
        System.out.println("  [Helpdesk] Localisation : " + fiche.localisation);
        System.out.println("  [Helpdesk] Dernière MAJ : " + fiche.derniereMaj);
        System.out.println("  [Helpdesk] Tickets      : " + fiche.tickets.size());
        if (afficherTickets && !fiche.tickets.isEmpty()) {
            System.out.println("  Historique des incidents / interventions :");
            for (String ticket : fiche.tickets) {
                System.out.println("    - " + ticket);
            }
        }
    }

    public void Search(Integer sn) {
        System.out.println("Recherche du SN " + sn + "...");
        Materiel m = trouverParSN(sn);
        if (m != null) {
            System.out.println("[OK] Matériel trouvé :");
            afficherDetailsComplets(m, true);
        } else {
            System.out.println("[Info] Matériel introuvable, merci de vérifier le SN.");
        }
    }

    public void Search(String type) {
        if (type == null || type.trim().isEmpty()) {
            System.out.println("[Erreur] Type de recherche invalide.");
            return;
        }
        String cible = type.trim();
        if ("Commutateur".equalsIgnoreCase(cible) || "Commutateur réseau".equalsIgnoreCase(cible)) {
            cible = "Switch";
        }
        System.out.println("Recherche des équipements de type \"" + cible + "\"...");
        int compteur = 0;
        for (Materiel m : this.materielList) {
            if (m.getType() != null && m.getType().equalsIgnoreCase(cible)) {
                afficherDetailsComplets(m, false);
                compteur++;
            }
        }
        if (compteur == 0) {
            System.out.println("[Info] Aucun équipement trouvé pour le type \"" + type + "\".");
        } else {
            System.out.println("Total trouvé : " + compteur);
        }
    }

    public void rechercherParStatut(StatutMateriel statut) {
        System.out.println("Recherche des équipements au statut \"" + statut.getLibelle() + "\"...");
        int compteur = 0;
        for (Materiel m : this.materielList) {
            FicheHelpdesk fiche = obtenirOuCreerFiche(m.getSN());
            if (fiche.statut == statut) {
                afficherDetailsComplets(m, false);
                compteur++;
            }
        }
        if (compteur == 0) {
            System.out.println("[Info] Aucun équipement avec le statut \"" + statut.getLibelle() + "\".");
        } else {
            System.out.println("Total trouvé : " + compteur);
        }
    }

    public void rechercherParUtilisateur(String critere) {
        String cible = critere.trim().toLowerCase(Locale.ROOT);
        System.out.println("Recherche des équipements affectés à \"" + critere + "\"...");
        int compteur = 0;
        for (Materiel m : this.materielList) {
            FicheHelpdesk fiche = obtenirOuCreerFiche(m.getSN());
            if (fiche.utilisateur.toLowerCase(Locale.ROOT).contains(cible)) {
                afficherDetailsComplets(m, false);
                compteur++;
            }
        }
        if (compteur == 0) {
            System.out.println("[Info] Aucun équipement affecté à cet utilisateur.");
        } else {
            System.out.println("Total trouvé : " + compteur);
        }
    }

    public void rechercherParMotCle(String motCle) {
        String cible = motCle.trim().toLowerCase(Locale.ROOT);
        System.out.println("Recherche globale pour \"" + motCle + "\"...");
        int compteur = 0;
        for (Materiel m : this.materielList) {
            FicheHelpdesk fiche = obtenirOuCreerFiche(m.getSN());
            String texteGlobal = (m.export() + " " + m.getNumeroInventaire() + " " + m.getFournisseur() + " "
                    + fiche.utilisateur + " " + fiche.localisation + " " + fiche.statut.getLibelle())
                    .toLowerCase(Locale.ROOT);
            if (texteGlobal.contains(cible)) {
                afficherDetailsComplets(m, false);
                compteur++;
            }
        }
        if (compteur == 0) {
            System.out.println("[Info] Aucun équipement ne correspond à \"" + motCle + "\".");
        } else {
            System.out.println("Total trouvé : " + compteur);
        }
    }

    public void afficherAuditAlertes() {
        System.out.println("\n=== AUDIT & ALERTES HELPDESK : " + this.name + " ===");
        if (this.materielList.isEmpty()) {
            System.out.println("Aucun matériel enregistré.");
            return;
        }
        LocalDate aujourdHui = LocalDate.now();
        int nbAlertes = 0;

        for (Materiel m : this.materielList) {
            List<String> alertes = new ArrayList<>();
            FicheHelpdesk fiche = obtenirOuCreerFiche(m.getSN());

            if (fiche.statut == StatutMateriel.EN_PANNE || fiche.statut == StatutMateriel.EN_REPARATION) {
                alertes.add("Statut critique : " + fiche.statut.getLibelle());
            }

            if (m.getDateFinGarantie() != null && fiche.statut != StatutMateriel.REFORME) {
                long jours = ChronoUnit.DAYS.between(aujourdHui, m.getDateFinGarantie());
                if (jours < 0) {
                    alertes.add("Garantie expirée (" + m.getDateFinGarantie().format(FORMAT_DATE_FR) + ")");
                } else if (jours <= SEUIL_ALERTE_GARANTIE_JOURS) {
                    alertes.add("Garantie expire bientôt (" + jours + " j)");
                }
            }

            if (m instanceof Ordinateur) {
                Ordinateur ordi = (Ordinateur) m;
                if (!ordi.estConformeSecurite() && fiche.statut == StatutMateriel.EN_SERVICE) {
                    alertes.add("Sécurité poste non conforme (Chiffrement=" + ordi.isChiffrementDisque()
                            + ", EDR/AV=" + ordi.isAntivirusActif() + ")");
                }
            } else if (m instanceof Switch) {
                Switch sw = (Switch) m;
                if (sw.getTauxOccupation() >= SEUIL_ALERTE_SWITCH_PCT) {
                    alertes.add(String.format("Commutateur presque saturé (%.0f%% des ports)", sw.getTauxOccupation()));
                }
            } else if (m instanceof Telephone) {
                Telephone tel = (Telephone) m;
                if (!tel.isSuperviseMDM() && fiche.statut == StatutMateriel.EN_SERVICE) {
                    alertes.add("Téléphone en production non enrôlé dans le MDM");
                }
            }

            if (!alertes.isEmpty()) {
                nbAlertes++;
                System.out.println("  [!] SN " + m.getSN() + " (" + m.getType() + " - " + fiche.utilisateur + ") :");
                for (String a : alertes) {
                    System.out.println("      -> " + a);
                }
            }
        }

        if (nbAlertes == 0) {
            System.out.println("[OK] Aucune alerte détectée : le parc est sain et conforme.");
        } else {
            System.out.println("\nTotal équipements avec alerte : " + nbAlertes);
        }
    }

    public void afficherStatistiques() {
        System.out.println("\n=== TABLEAU DE BORD HELPDESK : " + this.name + " ===");
        int total = this.materielList.size();
        System.out.println("Nombre total d'équipements : " + total);
        if (total == 0) {
            return;
        }

        Map<String, Integer> parType = new LinkedHashMap<>();
        Map<StatutMateriel, Integer> parStatut = new LinkedHashMap<>();
        for (StatutMateriel s : StatutMateriel.values()) {
            parStatut.put(s, 0);
        }

        int totalTickets = 0;
        int nonAffectes = 0;
        double valeurTotaleHT = 0.0;

        int totalOrdis = 0;
        int ordisConformes = 0;
        int totalTelephones = 0;
        int telephonesMDM = 0;
        int totalSwitchs = 0;
        int totalPortsSwitch = 0;
        int totalPortsUtilises = 0;
        int sousGarantie = 0;
        int garantieExpiree = 0;

        LocalDate aujourdHui = LocalDate.now();

        for (Materiel m : this.materielList) {
            String type = (m.getType() != null) ? m.getType() : "Inconnu";
            parType.put(type, parType.getOrDefault(type, 0) + 1);
            valeurTotaleHT += m.getPrixAchatHT();

            if (m.getDateFinGarantie() != null) {
                if (!aujourdHui.isAfter(m.getDateFinGarantie())) {
                    sousGarantie++;
                } else {
                    garantieExpiree++;
                }
            }

            if (m instanceof Ordinateur) {
                totalOrdis++;
                if (((Ordinateur) m).estConformeSecurite()) {
                    ordisConformes++;
                }
            } else if (m instanceof Switch) {
                totalSwitchs++;
                Switch sw = (Switch) m;
                totalPortsSwitch += sw.getNbPortsEthernet();
                totalPortsUtilises += sw.getPortsUtilises();
            } else if (m instanceof Telephone) {
                totalTelephones++;
                if (((Telephone) m).isSuperviseMDM()) {
                    telephonesMDM++;
                }
            }

            FicheHelpdesk fiche = obtenirOuCreerFiche(m.getSN());
            parStatut.put(fiche.statut, parStatut.get(fiche.statut) + 1);
            totalTickets += fiche.tickets.size();
            if ("Non affecté".equalsIgnoreCase(fiche.utilisateur)) {
                nonAffectes++;
            }
        }

        System.out.printf("Valeur cumulée du parc     : %.2f € HT%n", valeurTotaleHT);

        System.out.println("\nRépartition par type :");
        for (Map.Entry<String, Integer> entry : parType.entrySet()) {
            System.out.printf("  - %-15s : %d%n", entry.getKey(), entry.getValue());
        }

        System.out.println("\nRépartition par statut :");
        for (Map.Entry<StatutMateriel, Integer> entry : parStatut.entrySet()) {
            double pct = (entry.getValue() * 100.0) / total;
            System.out.printf("  - %-24s : %d (%.1f%%)%n", entry.getKey().getLibelle(), entry.getValue(), pct);
        }

        int enService = parStatut.get(StatutMateriel.EN_SERVICE);
        int enPanneOuRep = parStatut.get(StatutMateriel.EN_PANNE) + parStatut.get(StatutMateriel.EN_REPARATION);
        System.out.println("\nIndicateurs Helpdesk & Sécurité :");
        System.out.println("  - Équipements en production      : " + enService);
        System.out.println("  - Équipements non affectés       : " + nonAffectes);
        System.out.println("  - Équipements en panne / répar.  : " + enPanneOuRep);
        System.out.println("  - Total tickets enregistrés      : " + totalTickets);
        System.out.println("  - Garanties (actives / expirées) : " + sousGarantie + " / " + garantieExpiree);

        if (totalOrdis > 0) {
            System.out.println("  - Conformité Cyber (Ordinateurs) : " + ordisConformes + " / " + totalOrdis
                    + " (Chiffrement + EDR/AV)");
        }
        if (totalSwitchs > 0) {
            System.out.println("  - Capacité réseau (Commutateurs) : " + totalPortsUtilises + " / "
                    + totalPortsSwitch + " ports utilisés");
        }
        if (totalTelephones > 0) {
            System.out.println("  - Conformité MDM (Téléphones)    : " + telephonesMDM + " / " + totalTelephones);
        }
    }

    // ==================== EXPORT / IMPORT SÉCURISÉS (UTF-8) ====================

    public void exportparc() {
        exportparc(FICHIER_DEFAUT);
    }

    public void exportparc(String nomFichier) {
        Path cible;
        try {
            cible = resoudreCheminSecurise(nomFichier);
        } catch (IllegalArgumentException e) {
            System.out.println("[Erreur sécurité] " + e.getMessage());
            return;
        }

        Path fichierTemp = cible.resolveSibling(cible.getFileName() + ".tmp");
        Path fichierBackup = cible.resolveSibling(cible.getFileName() + ".bak");

        try (BufferedWriter writer = Files.newBufferedWriter(fichierTemp, StandardCharsets.UTF_8)) {
            writer.write("PARC;" + nettoyerTexte(this.name));
            writer.newLine();

            for (Materiel m : this.materielList) {
                writer.write("MAT;" + m.export());
                writer.newLine();

                String dAchat = (m.getDateAchat() != null) ? m.getDateAchat().toString() : "";
                String dGarantie = (m.getDateFinGarantie() != null) ? m.getDateFinGarantie().toString() : "";
                writer.write("ADMIN;" + m.getSN() + ";"
                        + nettoyerTexte(m.getNumeroInventaire()) + ";"
                        + nettoyerTexte(m.getFournisseur()) + ";"
                        + dAchat + ";"
                        + dGarantie + ";"
                        + String.format(Locale.ROOT, "%.2f", m.getPrixAchatHT()));
                writer.newLine();

                FicheHelpdesk fiche = obtenirOuCreerFiche(m.getSN());
                String ticketsSerialises = String.join("|", fiche.tickets);
                writer.write("HD;" + m.getSN() + ";" + fiche.statut.name() + ";"
                        + nettoyerTexte(fiche.utilisateur) + ";"
                        + nettoyerTexte(fiche.localisation) + ";"
                        + nettoyerTexte(fiche.derniereMaj) + ";"
                        + ticketsSerialises);
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            System.out.println("[Erreur] Échec lors de l'écriture du fichier temporaire : " + e.getMessage());
            return;
        }

        try {
            if (Files.exists(cible)) {
                Files.copy(cible, fichierBackup, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(fichierTemp, cible, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[OK] Parc informatique exporté avec succès vers : " + cible.getFileName());
        } catch (IOException e) {
            System.out.println("[Erreur] Impossible de finaliser la sauvegarde : " + e.getMessage());
        }
    }

    public void exporterRapportCsv(String nomFichier) {
        Path cible;
        try {
            cible = resoudreCheminSecurise(nomFichier);
        } catch (IllegalArgumentException e) {
            System.out.println("[Erreur sécurité] " + e.getMessage());
            return;
        }

        Path fichierTemp = cible.resolveSibling(cible.getFileName() + ".tmp");

        try (BufferedWriter writer = Files.newBufferedWriter(fichierTemp, StandardCharsets.UTF_8)) {
            writer.write('\uFEFF'); // BOM UTF-8 pour Excel
            writer.write("SN;Type;Inventaire;Statut;Utilisateur;Localisation;Fournisseur;Fin_Garantie;Prix_HT;Nb_Tickets;Details");
            writer.newLine();

            for (Materiel m : this.materielList) {
                FicheHelpdesk fiche = obtenirOuCreerFiche(m.getSN());
                String finGarantie = (m.getDateFinGarantie() != null)
                        ? m.getDateFinGarantie().format(FORMAT_DATE_FR)
                        : "Non renseignée";
                String prix = String.format(Locale.FRANCE, "%.2f", m.getPrixAchatHT());
                writer.write(m.getSN() + ";"
                        + protegerCelluleCsv(m.getType()) + ";"
                        + protegerCelluleCsv(m.getNumeroInventaire()) + ";"
                        + protegerCelluleCsv(fiche.statut.getLibelle()) + ";"
                        + protegerCelluleCsv(fiche.utilisateur) + ";"
                        + protegerCelluleCsv(fiche.localisation) + ";"
                        + protegerCelluleCsv(m.getFournisseur()) + ";"
                        + protegerCelluleCsv(finGarantie) + ";"
                        + protegerCelluleCsv(prix) + ";"
                        + fiche.tickets.size() + ";"
                        + protegerCelluleCsv(formaterDetailsPourCsv(m)));
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            System.out.println("[Erreur] Échec de génération du rapport CSV : " + e.getMessage());
            return;
        }

        try {
            Files.move(fichierTemp, cible, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[OK] Rapport Excel CSV généré avec succès : " + cible.getFileName());
        } catch (IOException e) {
            System.out.println("[Erreur] Impossible de finaliser le fichier CSV : " + e.getMessage());
        }
    }

    private String formaterDetailsPourCsv(Materiel m) {
        if (m instanceof Ordinateur) {
            Ordinateur o = (Ordinateur) m;
            return o.getMarque() + " " + o.getModele() + " (" + o.getFormatPoste() + ") | Host: " + o.getNomHost()
                    + " | OS: " + o.getOs() + " | CPU: " + o.getCpu() + " | RAM: " + o.getRamGo() + "Go"
                    + " | MAC: " + o.getAdresseMac()
                    + " | Chiffrement: " + (o.isChiffrementDisque() ? "Oui" : "Non")
                    + " | EDR/AV: " + (o.isAntivirusActif() ? "Oui" : "Non");
        } else if (m instanceof Switch) {
            Switch sw = (Switch) m;
            return sw.getMarque() + " " + sw.getModele() + " (" + sw.getVitessePorts() + ")"
                    + " | Ports: " + sw.getPortsUtilises() + "/" + sw.getNbPortsEthernet()
                    + " | PoE: " + (sw.isPoe() ? "Oui" : "Non")
                    + " | IP: " + sw.getAdresseIpGestion() + " | VLAN: " + sw.getVlanGestion();
        } else if (m instanceof Telephone) {
            Telephone t = (Telephone) m;
            return t.getMarque() + " " + t.getModele() + " | OS: " + t.getOs()
                    + " | Ligne: " + t.getNumeroLigne() + " | IMEI: " + t.getImei()
                    + " | MDM: " + (t.isSuperviseMDM() ? "Oui" : "Non");
        }
        return m.export().replace(";", " | ");
    }

    public void importparc() throws IOException {
        importparc(FICHIER_DEFAUT, true);
    }

    public void importparc(String nomFichier, boolean remplacerExistant) throws IOException {
        Path source;
        try {
            source = resoudreCheminSecurise(nomFichier);
        } catch (IllegalArgumentException e) {
            System.out.println("[Erreur sécurité] " + e.getMessage());
            return;
        }

        if (!Files.exists(source) || !Files.isRegularFile(source)) {
            System.out.println("[Erreur] Le fichier \"" + source.getFileName() + "\" est introuvable.");
            return;
        }
        if (Files.size(source) > TAILLE_MAX_IMPORT_OCTETS) {
            System.out.println("[Erreur sécurité] Le fichier dépasse la taille maximale autorisée (10 Mo).");
            return;
        }

        String nouveauNom = this.name;
        List<Materiel> nouveauxMateriels = new ArrayList<>();
        Map<Integer, FicheHelpdesk> nouvellesFiches = new LinkedHashMap<>();
        Map<Integer, String[]> infosAdminBrutes = new LinkedHashMap<>();
        int erreursLignes = 0;

        try (BufferedReader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            String ligne;
            int numLigne = 0;
            while ((ligne = reader.readLine()) != null) {
                numLigne++;
                if (ligne.trim().isEmpty()) {
                    continue;
                }
                if (ligne.startsWith("PARC;")) {
                    String[] p = ligne.split(";", 2);
                    if (p.length == 2 && !p[1].trim().isEmpty()) {
                        nouveauNom = nettoyerTexte(p[1]);
                    }
                } else if (ligne.startsWith("MAT;")) {
                    Materiel m = parserMateriel(ligne.substring(4));
                    if (m != null && m.getSN() != null && m.getSN() > 0) {
                        nouveauxMateriels.add(m);
                    } else {
                        erreursLignes++;
                        System.out.println("[Avertissement] Ligne " + numLigne + " ignorée (matériel invalide).");
                    }
                } else if (ligne.startsWith("ADMIN;")) {
                    String[] parts = ligne.split(";", -1);
                    if (parts.length >= 7) {
                        try {
                            int sn = Integer.parseInt(parts[1].trim());
                            infosAdminBrutes.put(sn, parts);
                        } catch (NumberFormatException e) {
                            erreursLignes++;
                            System.out.println("[Avertissement] Ligne " + numLigne + " ignorée (SN Admin invalide).");
                        }
                    } else {
                        erreursLignes++;
                    }
                } else if (ligne.startsWith("HD;")) {
                    String[] parts = ligne.split(";", -1);
                    if (parts.length >= 6) {
                        try {
                            int sn = Integer.parseInt(parts[1].trim());
                            FicheHelpdesk fiche = new FicheHelpdesk();
                            fiche.statut = StatutMateriel.depuisChaine(parts[2]);
                            fiche.utilisateur = parts[3].isEmpty() ? "Non affecté" : parts[3];
                            fiche.localisation = parts[4].isEmpty() ? "Stock IT" : parts[4];
                            fiche.derniereMaj = parts[5].isEmpty() ? horodatageActuel() : parts[5];
                            if (parts.length >= 7 && !parts[6].trim().isEmpty()) {
                                for (String t : parts[6].split("\\|")) {
                                    if (!t.trim().isEmpty()) {
                                        fiche.tickets.add(t.trim());
                                    }
                                }
                            }
                            nouvellesFiches.put(sn, fiche);
                        } catch (NumberFormatException e) {
                            erreursLignes++;
                            System.out.println("[Avertissement] Ligne " + numLigne + " ignorée (SN Helpdesk invalide).");
                        }
                    }
                } else {
                    erreursLignes++;
                    System.out.println("[Avertissement] Ligne " + numLigne + " non reconnue.");
                }
            }
        }

        if (remplacerExistant) {
            this.materielList.clear();
            this.fichesHelpdesk.clear();
            this.name = nouveauNom;
        }

        int ajoutes = 0;
        int ignores = 0;
        for (Materiel m : nouveauxMateriels) {
            if (trouverParSN(m.getSN()) == null) {
                String[] admin = infosAdminBrutes.get(m.getSN());
                if (admin != null) {
                    appliquerInfosAdmin(m, admin);
                }
                this.materielList.add(m);
                this.fichesHelpdesk.put(m.getSN(), nouvellesFiches.getOrDefault(m.getSN(), new FicheHelpdesk()));
                ajoutes++;
            } else {
                ignores++;
            }
        }

        System.out.println("[OK] Import terminé : " + ajoutes + " ajouté(s), "
                + ignores + " doublon(s) ignoré(s), " + erreursLignes + " ligne(s) invalide(s).");
    }

    private void appliquerInfosAdmin(Materiel m, String[] parts) {
        try {
            m.setNumeroInventaire(parts[2]);
            m.setFournisseur(parts[3]);
            m.setDateAchat(parts[4].trim().isEmpty() ? null : LocalDate.parse(parts[4].trim()));
            m.setDateFinGarantie(parts[5].trim().isEmpty() ? null : LocalDate.parse(parts[5].trim()));
            if (!parts[6].trim().isEmpty()) {
                m.setPrixAchatHT(Double.parseDouble(parts[6].trim().replace(',', '.')));
            }
        } catch (DateTimeParseException | IllegalArgumentException e) {
            System.out.println("[Avertissement] Métadonnées admin partielles/invalides pour SN " + m.getSN() + ".");
        }
    }

    private Materiel parserMateriel(String ligneExport) {
        String[] parts = ligneExport.split(";", -1);
        if (parts.length < 3) {
            return null;
        }
        try {
            int sn = Integer.parseInt(parts[0].trim());
            if (sn <= 0) {
                return null;
            }
            String type = parts[1].trim();
            if ("Ordinateur".equalsIgnoreCase(type) && parts.length >= 6) {
                int ram = Integer.parseInt(parts[4].trim());
                if (parts.length >= 14) {
                    return new Ordinateur(
                            sn, parts[2], parts[3], ram, parts[5],
                            parts[6], parts[7], parts[8], Integer.parseInt(parts[9].trim()),
                            parts[10], parts[11], Boolean.parseBoolean(parts[12].trim()),
                            Boolean.parseBoolean(parts[13].trim())
                    );
                }
                return new Ordinateur(sn, parts[2], parts[3], ram, parts[5]);
            } else if ("Switch".equalsIgnoreCase(type) && parts.length >= 3) {
                int ports = Integer.parseInt(parts[2].trim());
                if (parts.length >= 11) {
                    return new Switch(
                            sn, ports, parts[3], parts[4], parts[5],
                            Boolean.parseBoolean(parts[6].trim()), Boolean.parseBoolean(parts[7].trim()),
                            parts[8], Integer.parseInt(parts[9].trim()), Integer.parseInt(parts[10].trim())
                    );
                }
                return new Switch(sn, ports);
            } else if (("Telephone".equalsIgnoreCase(type) || "Téléphone".equalsIgnoreCase(type)) && parts.length >= 3) {
                if (parts.length >= 8) {
                    return new Telephone(
                            sn, parts[2], parts[3], parts[4],
                            parts[5], parts[6], Boolean.parseBoolean(parts[7].trim())
                    );
                }
                return new Telephone(sn, parts[2]);
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        return null;
    }

    // ==================== UTILITAIRES DE SÉCURITÉ & SAISIE ====================

    private static Path resoudreCheminSecurise(String nomFichier) {
        String nomNettoye = (nomFichier == null) ? "" : nomFichier.trim();
        if (nomNettoye.isEmpty() || !nomNettoye.matches("^[a-zA-Z0-9_.-]+$") || nomNettoye.contains("..")) {
            throw new IllegalArgumentException(
                    "Nom de fichier invalide. Utilisez uniquement lettres, chiffres, '.', '-' et '_'.");
        }
        return Paths.get("").toAbsolutePath().resolve(nomNettoye).normalize();
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

    private static String protegerCelluleCsv(String valeur) {
        String nettoye = nettoyerTexte(valeur);
        if (!nettoye.isEmpty()) {
            char premier = nettoye.charAt(0);
            if (premier == '=' || premier == '+' || premier == '-' || premier == '@' || premier == '\t') {
                nettoye = "'" + nettoye;
            }
        }
        return "\"" + nettoye.replace("\"", "\"\"") + "\"";
    }

    private static String horodatageActuel() {
        return LocalDateTime.now().format(FORMAT_DATE_HEURE);
    }

    private static String lireLigne(Scanner scanner) {
        if (!scanner.hasNextLine()) {
            return null;
        }
        return scanner.nextLine();
    }

    private static Integer lireEntier(Scanner scanner, String message, int min, int max) {
        while (true) {
            System.out.print(message);
            String saisie = lireLigne(scanner);
            if (saisie == null) {
                return null;
            }
            try {
                int valeur = Integer.parseInt(saisie.trim());
                if (valeur < min || valeur > max) {
                    System.out.println("[Erreur] Veuillez saisir un nombre entre " + min + " et " + max + ".");
                    continue;
                }
                return valeur;
            } catch (NumberFormatException e) {
                System.out.println("[Erreur] Saisie invalide. Un nombre entier est attendu.");
            }
        }
    }

    private static Integer lireEntierOptionnel(Scanner scanner, String message, int min, int max, int defaut) {
        while (true) {
            System.out.print(message);
            String saisie = lireLigne(scanner);
            if (saisie == null) {
                return null;
            }
            if (saisie.trim().isEmpty()) {
                return defaut;
            }
            try {
                int valeur = Integer.parseInt(saisie.trim());
                if (valeur < min || valeur > max) {
                    System.out.println("[Erreur] Veuillez saisir un nombre entre " + min + " et " + max + ".");
                    continue;
                }
                return valeur;
            } catch (NumberFormatException e) {
                System.out.println("[Erreur] Saisie invalide. Un nombre entier est attendu.");
            }
        }
    }

    private static Double lireDoubleOptionnel(Scanner scanner, String message, double min, double defaut) {
        while (true) {
            System.out.print(message);
            String saisie = lireLigne(scanner);
            if (saisie == null) {
                return null;
            }
            if (saisie.trim().isEmpty()) {
                return defaut;
            }
            try {
                double val = Double.parseDouble(saisie.trim().replace(',', '.'));
                if (Double.isNaN(val) || Double.isInfinite(val) || val < min) {
                    System.out.println("[Erreur] Veuillez saisir un montant valide (>= " + min + ").");
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                System.out.println("[Erreur] Montant invalide (ex: 899.90).");
            }
        }
    }

    private static LocalDate parserDateSaisie(String texte) {
        String nettoye = texte.trim();
        try {
            return LocalDate.parse(nettoye, FORMAT_DATE_FR);
        } catch (DateTimeParseException ignored) {
            return LocalDate.parse(nettoye); // accepte aussi AAAA-MM-JJ
        }
    }

    private static String lireTexteNonVide(Scanner scanner, String message) {
        while (true) {
            System.out.print(message);
            String saisie = lireLigne(scanner);
            if (saisie == null) {
                return null;
            }
            String nettoye = nettoyerTexte(saisie);
            if (!nettoye.isEmpty()) {
                return nettoye;
            }
            System.out.println("[Erreur] Ce champ ne peut pas être vide.");
        }
    }

    private static String lireTexteOptionnel(Scanner scanner, String message, String valeurParDefaut) {
        System.out.print(message);
        String saisie = lireLigne(scanner);
        if (saisie == null) {
            return null;
        }
        String nettoye = nettoyerTexte(saisie);
        return nettoye.isEmpty() ? valeurParDefaut : nettoye;
    }

    private static boolean lireConfirmation(Scanner scanner, String message) {
        System.out.print(message + " (O/N) : ");
        String rep = lireLigne(scanner);
        return rep != null && (rep.trim().equalsIgnoreCase("O") || rep.trim().equalsIgnoreCase("OUI"));
    }

    // ==================== ASSISTANTS DE SAISIE & MISE À JOUR ====================

    private static void afficherMenu(Helpdesk parc) {
        System.out.println("\n================ MENU HELPDESK & PARC IT ================");
        System.out.println(" Parc : " + parc.getName() + " | Équipements : " + parc.materielList.size());
        System.out.println("---------------------------------------------------------");
        System.out.println(" 1  - Renommer le parc informatique");
        System.out.println(" 2  - Ajouter un matériel (Ordinateur / Commutateur / Téléphone)");
        System.out.println(" 3  - Mettre à jour un matériel (Helpdesk / Admin / Technique)");
        System.out.println(" 4  - Ajouter un ticket / incident sur un matériel");
        System.out.println(" 5  - Supprimer / Réformer un matériel");
        System.out.println(" 6  - Afficher tout le parc informatique (trié par SN)");
        System.out.println(" 7  - Rechercher (SN, Type, Statut, Utilisateur, Mot-clé)");
        System.out.println(" 8  - Audit & Alertes (Garanties, Cybersécurité, Réseau)");
        System.out.println(" 9  - Tableau de bord & Statistiques du parc");
        System.out.println(" 10 - Exporter le parc (sauvegarde sécurisée UTF-8)");
        System.out.println(" 11 - Importer un parc depuis un fichier");
        System.out.println(" 12 - Exporter un rapport CSV (compatible Excel)");
        System.out.println(" 0  - Quitter");
        System.out.println("=========================================================");
    }

    private static void gererAjoutMateriel(Scanner scanner, Helpdesk parc) {
        System.out.println("\nType de matériel à ajouter :");
        System.out.println(" 1 - Ordinateur");
        System.out.println(" 2 - Commutateur réseau (Switch)");
        System.out.println(" 3 - Téléphone");
        System.out.println(" 0 - Annuler");
        Integer type = lireEntier(scanner, "Votre choix : ", 0, 3);
        if (type == null || type == 0) {
            return;
        }

        Integer sn = lireEntier(scanner, "Numéro de série (SN > 0) : ", 1, Integer.MAX_VALUE);
        if (sn == null) {
            return;
        }
        if (parc.trouverParSN(sn) != null) {
            System.out.println("[Erreur] Le SN " + sn + " est déjà enregistré dans le parc.");
            return;
        }

        Materiel nouveau = null;
        if (type == 1) {
            nouveau = saisirOrdinateur(scanner, sn);
        } else if (type == 2) {
            nouveau = saisirSwitch(scanner, sn);
        } else if (type == 3) {
            nouveau = saisirTelephone(scanner, sn);
        }

        if (nouveau != null && parc.Add(nouveau)) {
            if (lireConfirmation(scanner, "Souhaitez-vous renseigner les infos administratives (Inventaire, Garantie, Prix) ?")) {
                saisirInfosAdministratives(scanner, nouveau);
            }
            if (lireConfirmation(scanner, "Souhaitez-vous renseigner l'affectation Helpdesk maintenant ?")) {
                gererMiseAJourFiche(scanner, parc, sn);
            }
            System.out.println("[OK] Matériel (SN " + sn + ") ajouté avec succès.");
        }
    }

    private static Ordinateur saisirOrdinateur(Scanner scanner, int sn) {
        String os = lireTexteNonVide(scanner, "Système d'exploitation (ex: Windows 11 Pro, Ubuntu 24.04) : ");
        String cpu = lireTexteNonVide(scanner, "Processeur (CPU) : ");
        Integer ram = lireEntier(scanner, "Mémoire RAM (Go) : ", 1, 4096);
        String gpu = lireTexteNonVide(scanner, "Carte graphique (GPU) : ");
        if (os == null || cpu == null || ram == null || gpu == null) {
            return null;
        }

        Ordinateur ordi = new Ordinateur(sn, os, cpu, ram, gpu);
        String marque = lireTexteOptionnel(scanner, "Marque (ex: Dell, Lenovo, HP) [Entrée = Non renseigné] : ", "Non renseigné");
        String modele = lireTexteOptionnel(scanner, "Modèle (ex: Latitude 5540) [Entrée = Non renseigné] : ", "Non renseigné");
        String format = lireTexteOptionnel(scanner, "Format (PC Portable / PC Fixe / Station) [Entrée = Non renseigné] : ", "Non renseigné");
        Integer stockage = lireEntierOptionnel(scanner, "Stockage SSD/HDD en Go [Entrée = Non renseigné] : ", 0, 102400, 0);
        if (marque == null || modele == null || format == null || stockage == null) {
            return null;
        }
        ordi.setMarque(marque);
        ordi.setModele(modele);
        ordi.setFormatPoste(format);
        ordi.setStockageGo(stockage);

        while (true) {
            System.out.print("Hostname (ex: PC-COMPTA-01) [Entrée = Non renseigné] : ");
            String host = lireLigne(scanner);
            if (host == null) {
                return null;
            }
            try {
                ordi.setNomHost(host);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("[Erreur] " + e.getMessage());
            }
        }

        while (true) {
            System.out.print("Adresse MAC (ex: AA:BB:CC:DD:EE:FF) [Entrée = Non renseignée] : ");
            String mac = lireLigne(scanner);
            if (mac == null) {
                return null;
            }
            try {
                ordi.setAdresseMac(mac);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("[Erreur] " + e.getMessage());
            }
        }

        ordi.setChiffrementDisque(lireConfirmation(scanner, "Chiffrement disque actif (BitLocker / FileVault) ?"));
        ordi.setAntivirusActif(lireConfirmation(scanner, "Antivirus / EDR actif et à jour ?"));
        return ordi;
    }

    private static Switch saisirSwitch(Scanner scanner, int sn) {
        Integer ports = lireEntier(scanner, "Nombre de ports Ethernet (1 à 512) : ", 1, 512);
        if (ports == null) {
            return null;
        }
        Switch sw = new Switch(sn, ports);

        String marque = lireTexteOptionnel(scanner, "Marque (ex: Cisco, Aruba, Ubiquiti) [Entrée = Non renseigné] : ", "Non renseigné");
        String modele = lireTexteOptionnel(scanner, "Modèle (ex: Catalyst 2960-X) [Entrée = Non renseigné] : ", "Non renseigné");
        String vitesse = lireTexteOptionnel(scanner, "Vitesse des ports [Entrée = 1 Gbps] : ", "1 Gbps");
        if (marque == null || modele == null || vitesse == null) {
            return null;
        }
        sw.setMarque(marque);
        sw.setModele(modele);
        sw.setVitessePorts(vitesse);
        sw.setAdministrable(lireConfirmation(scanner, "Commutateur administrable (L2/L3) ?"));
        sw.setPoe(lireConfirmation(scanner, "Supporte-t-il le PoE (Power over Ethernet) ?"));

        while (true) {
            System.out.print("IP de gestion (ex: 192.168.10.2 ou DHCP) [Entrée = Non assignée] : ");
            String ip = lireLigne(scanner);
            if (ip == null) {
                return null;
            }
            try {
                sw.setAdresseIpGestion(ip);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("[Erreur] " + e.getMessage());
            }
        }

        Integer vlan = lireEntierOptionnel(scanner, "VLAN de gestion (1 à 4094) [Entrée = 1] : ", 1, 4094, 1);
        Integer utilises = lireEntierOptionnel(scanner, "Nombre de ports déjà utilisés (0 à " + ports + ") [Entrée = 0] : ", 0, ports, 0);
        if (vlan == null || utilises == null) {
            return null;
        }
        sw.setVlanGestion(vlan);
        sw.setPortsUtilises(utilises);
        return sw;
    }

    private static Telephone saisirTelephone(Scanner scanner, int sn) {
        String os = lireTexteNonVide(scanner, "Système d'exploitation (ex: iOS 18, Android 15) : ");
        String marque = lireTexteOptionnel(scanner, "Marque (ex: Apple, Samsung, Cisco) [Entrée = Non renseigné] : ", "Non renseigné");
        String modele = lireTexteOptionnel(scanner, "Modèle (ex: iPhone 15, Galaxy S24) [Entrée = Non renseigné] : ", "Non renseigné");
        if (os == null || marque == null || modele == null) {
            return null;
        }

        Telephone tel = new Telephone(sn, os);
        tel.setMarque(marque);
        tel.setModele(modele);

        while (true) {
            System.out.print("Numéro de ligne (ex: 4012, +33 6 12 34 56 78) [Entrée = Non attribué] : ");
            String ligne = lireLigne(scanner);
            if (ligne == null) {
                return null;
            }
            try {
                tel.setNumeroLigne(ligne);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("[Erreur] " + e.getMessage());
            }
        }

        while (true) {
            System.out.print("IMEI (15 chiffres) [Entrée = Non renseigné] : ");
            String imei = lireLigne(scanner);
            if (imei == null) {
                return null;
            }
            try {
                tel.setImei(imei);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("[Erreur] " + e.getMessage());
            }
        }

        tel.setSuperviseMDM(lireConfirmation(scanner, "L'appareil est-il enrôlé dans le MDM ?"));
        return tel;
    }

    private static void saisirInfosAdministratives(Scanner scanner, Materiel m) {
        String inv = lireTexteOptionnel(scanner, "Numéro d'inventaire (ex: INV-2026-001) [Entrée = conserver] : ", m.getNumeroInventaire());
        String fourn = lireTexteOptionnel(scanner, "Fournisseur (ex: Dell, LDLC Pro) [Entrée = conserver] : ", m.getFournisseur());
        if (inv != null) {
            m.setNumeroInventaire(inv);
        }
        if (fourn != null) {
            m.setFournisseur(fourn);
        }

        while (true) {
            System.out.print("Date d'achat (JJ/MM/AAAA) [Entrée = ignorer] : ");
            String s = lireLigne(scanner);
            if (s == null) {
                return;
            }
            if (s.trim().isEmpty()) {
                break;
            }
            try {
                m.setDateAchat(parserDateSaisie(s));
                break;
            } catch (DateTimeParseException | IllegalArgumentException e) {
                System.out.println("[Erreur] Format attendu : JJ/MM/AAAA (ou date antérieure à la fin de garantie).");
            }
        }

        while (true) {
            System.out.print("Date de fin de garantie (JJ/MM/AAAA) [Entrée = ignorer] : ");
            String s = lireLigne(scanner);
            if (s == null) {
                return;
            }
            if (s.trim().isEmpty()) {
                break;
            }
            try {
                m.setDateFinGarantie(parserDateSaisie(s));
                break;
            } catch (DateTimeParseException | IllegalArgumentException e) {
                System.out.println("[Erreur] " + e.getMessage());
            }
        }

        Double prix = lireDoubleOptionnel(scanner, "Prix d'achat HT en € [Entrée = " + m.getPrixAchatHT() + "] : ", 0.0, m.getPrixAchatHT());
        if (prix != null) {
            m.setPrixAchatHT(prix);
        }
    }

    private static void gererMiseAJourFiche(Scanner scanner, Helpdesk parc, Integer snOptionnel) {
        Integer sn = snOptionnel;
        if (sn == null) {
            sn = lireEntier(scanner, "Entrez le SN du matériel à mettre à jour : ", 1, Integer.MAX_VALUE);
            if (sn == null) {
                return;
            }
        }
        Materiel m = parc.trouverParSN(sn);
        if (m == null) {
            System.out.println("[Info] Aucun matériel trouvé avec le SN " + sn + ".");
            return;
        }

        FicheHelpdesk fiche = parc.obtenirOuCreerFiche(sn);

        while (true) {
            System.out.println("\n--- Mise à jour SN " + sn + " (" + m.getType() + ") ---");
            System.out.println(" 1 - Statut Helpdesk    : " + fiche.statut.getLibelle());
            System.out.println(" 2 - Utilisateur        : " + fiche.utilisateur);
            System.out.println(" 3 - Localisation       : " + fiche.localisation);
            System.out.println(" 4 - Infos admin        : " + m.getNumeroInventaire() + " | " + m.getFournisseur() + " | " + m.getStatutGarantie());

            if (m instanceof Ordinateur) {
                Ordinateur o = (Ordinateur) m;
                System.out.println(" 5 - Hostname / MAC     : " + o.getNomHost() + " / " + o.getAdresseMac());
                System.out.println(" 6 - RAM / Stockage     : " + o.getRamGo() + " Go / " + o.getStockageGo() + " Go");
                System.out.println(" 7 - Sécurité (BitL/AV) : Chiffrement=" + (o.isChiffrementDisque() ? "Oui" : "Non")
                        + ", EDR/AV=" + (o.isAntivirusActif() ? "Oui" : "Non"));
            } else if (m instanceof Switch) {
                Switch sw = (Switch) m;
                System.out.println(" 5 - Ports utilisés     : " + sw.getPortsUtilises() + " / " + sw.getNbPortsEthernet());
                System.out.println(" 6 - IP / VLAN gestion  : " + sw.getAdresseIpGestion() + " / VLAN " + sw.getVlanGestion());
                System.out.println(" 7 - PoE / Admin        : PoE=" + (sw.isPoe() ? "Oui" : "Non")
                        + ", Admin=" + (sw.isAdministrable() ? "Oui" : "Non"));
            } else if (m instanceof Telephone) {
                Telephone tel = (Telephone) m;
                System.out.println(" 5 - Numéro de ligne    : " + tel.getNumeroLigne());
                System.out.println(" 6 - IMEI               : " + tel.getImei());
                System.out.println(" 7 - Enrôlement MDM     : " + (tel.isSuperviseMDM() ? "Oui" : "Non"));
            }
            System.out.println(" 0 - Retour au menu");

            int maxChoix = (m instanceof Ordinateur || m instanceof Switch || m instanceof Telephone) ? 7 : 4;
            Integer choix = lireEntier(scanner, "Champ à modifier : ", 0, maxChoix);
            if (choix == null || choix == 0) {
                return;
            }

            if (choix == 1) {
                StatutMateriel[] statuts = StatutMateriel.values();
                for (int i = 0; i < statuts.length; i++) {
                    System.out.println(" " + (i + 1) + " - " + statuts[i].getLibelle());
                }
                Integer idx = lireEntier(scanner, "Nouveau statut : ", 1, statuts.length);
                if (idx != null) {
                    fiche.statut = statuts[idx - 1];
                    fiche.toucherMaj();
                    System.out.println("[OK] Statut mis à jour.");
                }
            } else if (choix == 2) {
                String user = lireTexteNonVide(scanner, "Nom de l'utilisateur / service (ou 'Non affecté') : ");
                if (user != null) {
                    fiche.utilisateur = user;
                    if (!"Non affecté".equalsIgnoreCase(user) && fiche.statut == StatutMateriel.EN_STOCK) {
                        fiche.statut = StatutMateriel.EN_SERVICE;
                    }
                    fiche.toucherMaj();
                    System.out.println("[OK] Utilisateur mis à jour.");
                }
            } else if (choix == 3) {
                String lieu = lireTexteNonVide(scanner, "Localisation (bâtiment, bureau, baie) : ");
                if (lieu != null) {
                    fiche.localisation = lieu;
                    fiche.toucherMaj();
                    System.out.println("[OK] Localisation mise à jour.");
                }
            } else if (choix == 4) {
                saisirInfosAdministratives(scanner, m);
                fiche.toucherMaj();
                System.out.println("[OK] Informations administratives mises à jour.");
            } else if (m instanceof Ordinateur) {
                Ordinateur o = (Ordinateur) m;
                if (choix == 5) {
                    try {
                        System.out.print("Nouveau Hostname [Entrée = inchangé] : ");
                        String host = lireLigne(scanner);
                        if (host != null && !host.trim().isEmpty()) {
                            o.setNomHost(host);
                        }
                        System.out.print("Nouvelle adresse MAC [Entrée = inchangée] : ");
                        String mac = lireLigne(scanner);
                        if (mac != null && !mac.trim().isEmpty()) {
                            o.setAdresseMac(mac);
                        }
                        fiche.toucherMaj();
                        System.out.println("[OK] Identifiants réseau mis à jour.");
                    } catch (IllegalArgumentException e) {
                        System.out.println("[Erreur] " + e.getMessage());
                    }
                } else if (choix == 6) {
                    Integer ram = lireEntierOptionnel(scanner, "Nouvelle RAM Go [Entrée = " + o.getRamGo() + "] : ", 1, 4096, o.getRamGo());
                    Integer disk = lireEntierOptionnel(scanner, "Nouveau Stockage Go [Entrée = " + o.getStockageGo() + "] : ", 0, 102400, o.getStockageGo());
                    if (ram != null && disk != null) {
                        o.setRamGo(ram);
                        o.setStockageGo(disk);
                        fiche.toucherMaj();
                        System.out.println("[OK] Ressources matérielles mises à jour.");
                    }
                } else if (choix == 7) {
                    o.setChiffrementDisque(lireConfirmation(scanner, "Chiffrement disque actif ?"));
                    o.setAntivirusActif(lireConfirmation(scanner, "Antivirus / EDR actif ?"));
                    fiche.toucherMaj();
                    System.out.println("[OK] Conformité sécurité mise à jour.");
                }
            } else if (m instanceof Switch) {
                Switch sw = (Switch) m;
                if (choix == 5) {
                    Integer utilises = lireEntier(scanner, "Ports utilisés (0 à " + sw.getNbPortsEthernet() + ") : ", 0, sw.getNbPortsEthernet());
                    if (utilises != null) {
                        sw.setPortsUtilises(utilises);
                        fiche.toucherMaj();
                        System.out.println("[OK] Occupation des ports mise à jour.");
                    }
                } else if (choix == 6) {
                    try {
                        System.out.print("Nouvelle IP de gestion [Entrée = inchangée] : ");
                        String ip = lireLigne(scanner);
                        if (ip != null && !ip.trim().isEmpty()) {
                            sw.setAdresseIpGestion(ip);
                        }
                        Integer vlan = lireEntierOptionnel(scanner, "Nouveau VLAN (1..4094) [Entrée = " + sw.getVlanGestion() + "] : ", 1, 4094, sw.getVlanGestion());
                        if (vlan != null) {
                            sw.setVlanGestion(vlan);
                        }
                        fiche.toucherMaj();
                        System.out.println("[OK] Configuration réseau mise à jour.");
                    } catch (IllegalArgumentException e) {
                        System.out.println("[Erreur] " + e.getMessage());
                    }
                } else if (choix == 7) {
                    sw.setPoe(lireConfirmation(scanner, "Support PoE actif ?"));
                    sw.setAdministrable(lireConfirmation(scanner, "Commutateur administrable (L2/L3) ?"));
                    fiche.toucherMaj();
                    System.out.println("[OK] Options du commutateur mises à jour.");
                }
            } else if (m instanceof Telephone) {
                Telephone tel = (Telephone) m;
                if (choix == 5) {
                    System.out.print("Nouveau numéro de ligne (Entrée = Non attribué) : ");
                    String ligne = lireLigne(scanner);
                    if (ligne != null) {
                        try {
                            tel.setNumeroLigne(ligne);
                            fiche.toucherMaj();
                            System.out.println("[OK] Numéro de ligne mis à jour.");
                        } catch (IllegalArgumentException e) {
                            System.out.println("[Erreur] " + e.getMessage());
                        }
                    }
                } else if (choix == 6) {
                    System.out.print("Nouvel IMEI 15 chiffres (Entrée = Non renseigné) : ");
                    String imei = lireLigne(scanner);
                    if (imei != null) {
                        try {
                            tel.setImei(imei);
                            fiche.toucherMaj();
                            System.out.println("[OK] IMEI mis à jour.");
                        } catch (IllegalArgumentException e) {
                            System.out.println("[Erreur] " + e.getMessage());
                        }
                    }
                } else if (choix == 7) {
                    tel.setSuperviseMDM(lireConfirmation(scanner, "L'appareil est-il enrôlé dans le MDM ?"));
                    fiche.toucherMaj();
                    System.out.println("[OK] Statut MDM mis à jour.");
                }
            }
        }
    }

    private static void gererAjoutTicket(Scanner scanner, Helpdesk parc) {
        Integer sn = lireEntier(scanner, "Entrez le SN du matériel concerné : ", 1, Integer.MAX_VALUE);
        if (sn == null) {
            return;
        }
        if (parc.trouverParSN(sn) == null) {
            System.out.println("[Info] Aucun matériel trouvé avec le SN " + sn + ".");
            return;
        }
        String description = lireTexteNonVide(scanner, "Description de l'incident / intervention : ");
        if (description == null) {
            return;
        }

        FicheHelpdesk fiche = parc.obtenirOuCreerFiche(sn);
        fiche.tickets.add("[" + horodatageActuel() + "] " + description);
        fiche.toucherMaj();

        if (lireConfirmation(scanner, "Passer le statut du matériel en 'En panne' ?")) {
            fiche.statut = StatutMateriel.EN_PANNE;
        }
        System.out.println("[OK] Ticket enregistré sur le SN " + sn + ".");
    }

    private static void gererRecherche(Scanner scanner, Helpdesk parc) {
        System.out.println("\nCritère de recherche :");
        System.out.println(" 1 - Par numéro de série (SN + historique tickets)");
        System.out.println(" 2 - Par type (Ordinateur, Commutateur/Switch, Telephone)");
        System.out.println(" 3 - Par statut Helpdesk");
        System.out.println(" 4 - Par utilisateur affecté");
        System.out.println(" 5 - Recherche globale par mot-clé (Hostname, IP, Modèle, Ligne...)");
        System.out.println(" 0 - Annuler");

        Integer choix = lireEntier(scanner, "Votre choix : ", 0, 5);
        if (choix == null || choix == 0) {
            return;
        }

        if (choix == 1) {
            Integer sn = lireEntier(scanner, "SN à rechercher : ", 1, Integer.MAX_VALUE);
            if (sn != null) {
                parc.Search(sn);
            }
        } else if (choix == 2) {
            String type = lireTexteNonVide(scanner, "Type (Ordinateur / Switch / Telephone) : ");
            if (type != null) {
                parc.Search(type);
            }
        } else if (choix == 3) {
            StatutMateriel[] statuts = StatutMateriel.values();
            for (int i = 0; i < statuts.length; i++) {
                System.out.println(" " + (i + 1) + " - " + statuts[i].getLibelle());
            }
            Integer idx = lireEntier(scanner, "Statut à filtrer : ", 1, statuts.length);
            if (idx != null) {
                parc.rechercherParStatut(statuts[idx - 1]);
            }
        } else if (choix == 4) {
            String user = lireTexteNonVide(scanner, "Nom (ou partie du nom) de l'utilisateur : ");
            if (user != null) {
                parc.rechercherParUtilisateur(user);
            }
        } else if (choix == 5) {
            String motCle = lireTexteNonVide(scanner, "Mot-clé à rechercher : ");
            if (motCle != null) {
                parc.rechercherParMotCle(motCle);
            }
        }
    }

    public static void main(String[] args) {
        Helpdesk parc = new Helpdesk();

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Bienvenue dans l'outil moderne Helpdesk & Gestion de Parc Informatique");

            boolean continuer = true;
            while (continuer) {
                afficherMenu(parc);
                Integer choix = lireEntier(scanner, "Votre choix : ", 0, 12);
                if (choix == null) {
                    break;
                }

                switch (choix) {
                    case 0:
                        if (lireConfirmation(scanner, "Voulez-vous vraiment quitter l'application ?")) {
                            continuer = false;
                            System.out.println("Fermeture de l'outil Helpdesk. À bientôt !");
                        }
                        break;
                    case 1:
                        String nom = lireTexteNonVide(scanner, "Nouveau nom du parc informatique : ");
                        if (nom != null) {
                            parc.setName(nom);
                            System.out.println("[OK] Parc renommé : " + parc.getName());
                        }
                        break;
                    case 2:
                        gererAjoutMateriel(scanner, parc);
                        break;
                    case 3:
                        gererMiseAJourFiche(scanner, parc, null);
                        break;
                    case 4:
                        gererAjoutTicket(scanner, parc);
                        break;
                    case 5:
                        Integer snSuppr = lireEntier(scanner, "Entrez le SN du matériel à supprimer : ", 1, Integer.MAX_VALUE);
                        if (snSuppr != null) {
                            if (parc.trouverParSN(snSuppr) == null) {
                                System.out.println("[Info] Aucun matériel trouvé avec le SN " + snSuppr + ".");
                            } else if (lireConfirmation(scanner, "Confirmer la suppression définitive du SN " + snSuppr + " ?")) {
                                parc.Remove(snSuppr);
                            } else {
                                System.out.println("Suppression annulée.");
                            }
                        }
                        break;
                    case 6:
                        parc.Affiche();
                        break;
                    case 7:
                        gererRecherche(scanner, parc);
                        break;
                    case 8:
                        parc.afficherAuditAlertes();
                        break;
                    case 9:
                        parc.afficherStatistiques();
                        break;
                    case 10:
                        System.out.print("Nom du fichier d'export [Entrée pour '" + FICHIER_DEFAUT + "'] : ");
                        String ficExport = lireLigne(scanner);
                        if (ficExport != null) {
                            parc.exportparc(ficExport.trim().isEmpty() ? FICHIER_DEFAUT : ficExport.trim());
                        }
                        break;
                    case 11:
                        System.out.print("Nom du fichier à importer [Entrée pour '" + FICHIER_DEFAUT + "'] : ");
                        String ficImport = lireLigne(scanner);
                        if (ficImport != null) {
                            String cible = ficImport.trim().isEmpty() ? FICHIER_DEFAUT : ficImport.trim();
                            boolean remplacer = lireConfirmation(scanner, "Remplacer le parc actuel (N = fusionner sans écraser) ?");
                            try {
                                parc.importparc(cible, remplacer);
                            } catch (IOException e) {
                                System.out.println("[Erreur] Problème lors de la lecture du fichier : " + e.getMessage());
                            }
                        }
                        break;
                    case 12:
                        System.out.print("Nom du rapport CSV [Entrée pour '" + FICHIER_CSV_DEFAUT + "'] : ");
                        String ficCsv = lireLigne(scanner);
                        if (ficCsv != null) {
                            parc.exporterRapportCsv(ficCsv.trim().isEmpty() ? FICHIER_CSV_DEFAUT : ficCsv.trim());
                        }
                        break;
                    default:
                        System.out.println("[Erreur] Choix non reconnu.");
                }
            }
        }
    }
}
