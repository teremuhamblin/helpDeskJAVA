import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ParcInformatique {

    private static final String FICHIER_DEFAUT = "parc_helpdesk.txt";
    private static final String FICHIER_CSV_DEFAUT = "parc_rapport.csv";
    private static final long TAILLE_MAX_IMPORT_OCTETS = 10L * 1024 * 1024; // 10 Mo
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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

    public ParcInformatique() {
        this("Parc informatique principal");
    }

    public ParcInformatique(String name) {
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
        System.out.println("\n==================================================");
        System.out.println(" PARC INFORMATIQUE : " + this.name + " (" + this.materielList.size() + " équipement(s))");
        System.out.println("==================================================");
        if (this.materielList.isEmpty()) {
            System.out.println("Aucun matériel enregistré dans le parc.");
            return;
        }
        for (Materiel m : this.materielList) {
            afficherDetailsComplets(m, false);
        }
    }

    private void afficherDetailsComplets(Materiel m, boolean afficherTickets) {
        System.out.println("--------------------------------------------------");
        m.Affiche();
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
        System.out.println("Recherche des équipements de type \"" + type + "\"...");
        int compteur = 0;
        for (Materiel m : this.materielList) {
            if (m.getType() != null && m.getType().equalsIgnoreCase(type.trim())) {
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
        String cible = critere.trim().toLowerCase();
        System.out.println("Recherche des équipements affectés à \"" + critere + "\"...");
        int compteur = 0;
        for (Materiel m : this.materielList) {
            FicheHelpdesk fiche = obtenirOuCreerFiche(m.getSN());
            if (fiche.utilisateur.toLowerCase().contains(cible)) {
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
        int totalTelephones = 0;
        int telephonesMDM = 0;

        for (Materiel m : this.materielList) {
            String type = (m.getType() != null) ? m.getType() : "Inconnu";
            parType.put(type, parType.getOrDefault(type, 0) + 1);

            if (m instanceof Telephone) {
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
        System.out.println("\nIndicateurs clés :");
        System.out.println("  - Équipements non affectés       : " + nonAffectes);
        System.out.println("  - Équipements en incident/répar. : " + enPanneOuRep);
        System.out.println("  - Équipements en production      : " + enService);
        System.out.println("  - Total tickets enregistrés      : " + totalTickets);
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
            writer.write('\uFEFF'); // BOM UTF-8 pour une ouverture directe avec accents dans Excel
            writer.write("SN;Type;Statut;Utilisateur;Localisation;Derniere_MAJ;Nb_Tickets;Details");
            writer.newLine();

            for (Materiel m : this.materielList) {
                FicheHelpdesk fiche = obtenirOuCreerFiche(m.getSN());
                String details = formaterDetailsPourCsv(m);
                writer.write(m.getSN() + ";"
                        + protegerCelluleCsv(m.getType()) + ";"
                        + protegerCelluleCsv(fiche.statut.getLibelle()) + ";"
                        + protegerCelluleCsv(fiche.utilisateur) + ";"
                        + protegerCelluleCsv(fiche.localisation) + ";"
                        + protegerCelluleCsv(fiche.derniereMaj) + ";"
                        + fiche.tickets.size() + ";"
                        + protegerCelluleCsv(details));
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
        if (m instanceof Telephone) {
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
                    String contenuMat = ligne.substring(4);
                    Materiel m = parserMateriel(contenuMat);
                    if (m != null && m.getSN() != null && m.getSN() > 0) {
                        nouveauxMateriels.add(m);
                    } else {
                        erreursLignes++;
                        System.out.println("[Avertissement] Ligne " + numLigne + " ignorée (matériel invalide).");
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
                                String[] tickets = parts[6].split("\\|");
                                for (String t : tickets) {
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
                this.materielList.add(m);
                FicheHelpdesk fiche = nouvellesFiches.getOrDefault(m.getSN(), new FicheHelpdesk());
                this.fichesHelpdesk.put(m.getSN(), fiche);
                ajoutes++;
            } else {
                ignores++;
            }
        }

        System.out.println("[OK] Import terminé : " + ajoutes + " ajouté(s), "
                + ignores + " doublon(s) ignoré(s), " + erreursLignes + " ligne(s) invalide(s).");
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
                if (ram <= 0) {
                    return null;
                }
                return new Ordinateur(sn, parts[2], parts[3], ram, parts[5]);
            } else if ("Switch".equalsIgnoreCase(type) && parts.length >= 3) {
                int ports = Integer.parseInt(parts[2].trim());
                if (ports <= 0) {
                    return null;
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
        return LocalDateTime.now().format(FORMAT_DATE);
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

    // ==================== MENU PRINCIPAL INTERACTIF ====================

    private static void afficherMenu(ParcInformatique parc) {
        System.out.println("\n================ MENU HELPDESK & PARC IT ================");
        System.out.println(" Parc : " + parc.getName() + " | Équipements : " + parc.materielList.size());
        System.out.println("---------------------------------------------------------");
        System.out.println(" 1  - Renommer le parc informatique");
        System.out.println(" 2  - Ajouter un matériel (Ordinateur / Switch / Téléphone)");
        System.out.println(" 3  - Mettre à jour un matériel (Fiche Helpdesk / Téléphonie)");
        System.out.println(" 4  - Ajouter un ticket / incident sur un matériel");
        System.out.println(" 5  - Supprimer / Réformer un matériel");
        System.out.println(" 6  - Afficher tout le parc informatique");
        System.out.println(" 7  - Rechercher un matériel (SN, Type, Statut, Utilisateur)");
        System.out.println(" 8  - Tableau de bord & Statistiques du parc");
        System.out.println(" 9  - Exporter le parc (sauvegarde sécurisée UTF-8)");
        System.out.println(" 10 - Importer un parc depuis un fichier");
        System.out.println(" 11 - Exporter un rapport CSV (compatible Excel)");
        System.out.println(" 0  - Quitter");
        System.out.println("=========================================================");
    }

    private static void gererAjoutMateriel(Scanner scanner, ParcInformatique parc) {
        System.out.println("\nType de matériel à ajouter :");
        System.out.println(" 1 - Ordinateur");
        System.out.println(" 2 - Switch");
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
            String os = lireTexteNonVide(scanner, "Système d'exploitation (OS) : ");
            String cpu = lireTexteNonVide(scanner, "Processeur (CPU) : ");
            Integer ram = lireEntier(scanner, "Mémoire RAM (Go) : ", 1, 4096);
            String gpu = lireTexteNonVide(scanner, "Carte graphique (GPU) : ");
            if (os != null && cpu != null && ram != null && gpu != null) {
                nouveau = new Ordinateur(sn, os, cpu, ram, gpu);
            }
        } else if (type == 2) {
            Integer ports = lireEntier(scanner, "Nombre de ports : ", 1, 512);
            if (ports != null) {
                nouveau = new Switch(sn, ports);
            }
        } else if (type == 3) {
            String os = lireTexteNonVide(scanner, "Système d'exploitation (ex: iOS 18, Android 15) : ");
            if (os == null) {
                return;
            }
            String marque = lireTexteOptionnel(scanner, "Marque (ex: Apple, Samsung, Cisco) [Entrée = Non renseigné] : ", "Non renseigné");
            if (marque == null) {
                return;
            }
            String modele = lireTexteOptionnel(scanner, "Modèle (ex: iPhone 15, Galaxy S24) [Entrée = Non renseigné] : ", "Non renseigné");
            if (modele == null) {
                return;
            }

            Telephone tel = new Telephone(sn, os);
            tel.setMarque(marque);
            tel.setModele(modele);

            while (true) {
                System.out.print("Numéro de ligne (ex: 4012, +33 6 12 34 56 78) [Entrée = Non attribué] : ");
                String ligne = lireLigne(scanner);
                if (ligne == null) {
                    return;
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
                    return;
                }
                try {
                    tel.setImei(imei);
                    break;
                } catch (IllegalArgumentException e) {
                    System.out.println("[Erreur] " + e.getMessage());
                }
            }

            tel.setSuperviseMDM(lireConfirmation(scanner, "L'appareil est-il enrôlé dans le MDM ?"));
            nouveau = tel;
        }

        if (nouveau != null && parc.Add(nouveau)) {
            if (lireConfirmation(scanner, "Souhaitez-vous renseigner l'affectation Helpdesk maintenant ?")) {
                gererMiseAJourFiche(scanner, parc, sn);
            }
            System.out.println("[OK] Matériel ajouté avec succès.");
        }
    }

    private static void gererMiseAJourFiche(Scanner scanner, ParcInformatique parc, Integer snOptionnel) {
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
        boolean estTelephone = (m instanceof Telephone);
        int maxChoix = estTelephone ? 6 : 3;

        while (true) {
            System.out.println("\nFiche actuelle (SN " + sn + ") :");
            System.out.println(" 1 - Statut          : " + fiche.statut.getLibelle());
            System.out.println(" 2 - Utilisateur     : " + fiche.utilisateur);
            System.out.println(" 3 - Localisation    : " + fiche.localisation);
            if (estTelephone) {
                Telephone tel = (Telephone) m;
                System.out.println(" 4 - Numéro de ligne : " + tel.getNumeroLigne());
                System.out.println(" 5 - IMEI            : " + tel.getImei());
                System.out.println(" 6 - Enrôlement MDM  : " + (tel.isSuperviseMDM() ? "Oui" : "Non"));
            }
            System.out.println(" 0 - Retour au menu");

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
                    System.out.println("[OK] Statut mis à jour : " + fiche.statut.getLibelle());
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
                String lieu = lireTexteNonVide(scanner, "Localisation (bâtiment, bureau, salle) : ");
                if (lieu != null) {
                    fiche.localisation = lieu;
                    fiche.toucherMaj();
                    System.out.println("[OK] Localisation mise à jour.");
                }
            } else if (estTelephone && choix == 4) {
                Telephone tel = (Telephone) m;
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
            } else if (estTelephone && choix == 5) {
                Telephone tel = (Telephone) m;
                System.out.print("Nouvel IMEI à 15 chiffres (Entrée = Non renseigné) : ");
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
            } else if (estTelephone && choix == 6) {
                Telephone tel = (Telephone) m;
                tel.setSuperviseMDM(lireConfirmation(scanner, "L'appareil est-il enrôlé dans le MDM ?"));
                fiche.toucherMaj();
                System.out.println("[OK] Statut MDM mis à jour.");
            }
        }
    }

    private static void gererAjoutTicket(Scanner scanner, ParcInformatique parc) {
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
        String entree = "[" + horodatageActuel() + "] " + description;
        fiche.tickets.add(entree);
        fiche.toucherMaj();

        if (lireConfirmation(scanner, "Passer le statut du matériel en 'En panne' ?")) {
            fiche.statut = StatutMateriel.EN_PANNE;
        }
        System.out.println("[OK] Ticket enregistré sur le SN " + sn + ".");
    }

    private static void gererRecherche(Scanner scanner, ParcInformatique parc) {
        System.out.println("\nCritère de recherche :");
        System.out.println(" 1 - Par numéro de série (SN)");
        System.out.println(" 2 - Par type (Ordinateur, Switch, Telephone)");
        System.out.println(" 3 - Par statut Helpdesk");
        System.out.println(" 4 - Par utilisateur affecté");
        System.out.println(" 0 - Annuler");

        Integer choix = lireEntier(scanner, "Votre choix : ", 0, 4);
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
        }
    }

    public static void main(String[] args) {
        ParcInformatique parc = new ParcInformatique();

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Bienvenue dans l'outil moderne de gestion de parc informatique (Helpdesk)");

            boolean continuer = true;
            while (continuer) {
                afficherMenu(parc);
                Integer choix = lireEntier(scanner, "Votre choix : ", 0, 11);
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
                        parc.afficherStatistiques();
                        break;
                    case 9:
                        System.out.print("Nom du fichier d'export [Entrée pour '" + FICHIER_DEFAUT + "'] : ");
                        String ficExport = lireLigne(scanner);
                        if (ficExport != null) {
                            parc.exportparc(ficExport.trim().isEmpty() ? FICHIER_DEFAUT : ficExport.trim());
                        }
                        break;
                    case 10:
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
                    case 11:
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
