import java.util.Locale;
import java.util.Objects;

public class Ordinateur extends Materiel {

    private static final int MAX_RAM_GO = 4096;
    private static final int MAX_STOCKAGE_GO = 102400; // 100 To
    private static final String VALEUR_NON_RENSEIGNEE = "Non renseigné";
    private static final String REGEX_HOSTNAME = "^[a-zA-Z0-9._-]{1,63}$";
    private static final String REGEX_MAC = "^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$";

    private String os;
    private String cpu;
    private int ramGo;
    private String gpu;
    private String marque;
    private String modele;
    private String formatPoste;
    private int stockageGo;
    private String nomHost;
    private String adresseMac;
    private boolean chiffrementDisque;
    private boolean antivirusActif;

    /**
     * Constructeur minimal (100 % compatible avec ParcInformatique.java).
     */
    public Ordinateur(Integer sn, String os, String cpu, Integer ram, String gpu) {
        this(sn, os, cpu, ram, gpu, VALEUR_NON_RENSEIGNEE, VALEUR_NON_RENSEIGNEE,
                VALEUR_NON_RENSEIGNEE, 0, VALEUR_NON_RENSEIGNEE, "Non renseignée", false, false);
    }

    /**
     * Constructeur complet pour une fiche poste de travail détaillée.
     */
    public Ordinateur(Integer sn, String os, String cpu, Integer ramGo, String gpu,
                      String marque, String modele, String formatPoste, int stockageGo,
                      String nomHost, String adresseMac, boolean chiffrementDisque, boolean antivirusActif) {
        super(validerSN(sn), "Ordinateur");
        setOs(os);
        setCpu(cpu);
        setRamGo(ramGo);
        setGpu(gpu);
        setMarque(marque);
        setModele(modele);
        setFormatPoste(formatPoste);
        setStockageGo(stockageGo);
        setNomHost(nomHost);
        setAdresseMac(adresseMac);
        this.chiffrementDisque = chiffrementDisque;
        this.antivirusActif = antivirusActif;
    }

    // ==================== GETTERS & SETTERS SÉCURISÉS ====================

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        String nettoye = nettoyerTexte(os);
        this.os = nettoye.isEmpty() ? "Inconnu" : nettoye;
    }

    public String getCpu() {
        return cpu;
    }

    public void setCpu(String cpu) {
        String nettoye = nettoyerTexte(cpu);
        this.cpu = nettoye.isEmpty() ? "Inconnu" : nettoye;
    }

    public int getRamGo() {
        return ramGo;
    }

    public void setRamGo(Integer ramGo) {
        if (ramGo == null || ramGo < 1 || ramGo > MAX_RAM_GO) {
            throw new IllegalArgumentException("La mémoire RAM doit être comprise entre 1 et " + MAX_RAM_GO + " Go.");
        }
        this.ramGo = ramGo;
    }

    public String getGpu() {
        return gpu;
    }

    public void setGpu(String gpu) {
        String nettoye = nettoyerTexte(gpu);
        this.gpu = nettoye.isEmpty() ? "Intégré / Inconnu" : nettoye;
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

    public String getFormatPoste() {
        return formatPoste;
    }

    public void setFormatPoste(String formatPoste) {
        String nettoye = nettoyerTexte(formatPoste);
        this.formatPoste = nettoye.isEmpty() ? VALEUR_NON_RENSEIGNEE : nettoye;
    }

    public int getStockageGo() {
        return stockageGo;
    }

    public void setStockageGo(int stockageGo) {
        if (stockageGo < 0 || stockageGo > MAX_STOCKAGE_GO) {
            throw new IllegalArgumentException(
                    "La capacité de stockage doit être comprise entre 0 et " + MAX_STOCKAGE_GO + " Go.");
        }
        this.stockageGo = stockageGo;
    }

    public String getNomHost() {
        return nomHost;
    }

    public void setNomHost(String nomHost) {
        String nettoye = nettoyerTexte(nomHost);
        if (nettoye.isEmpty() || VALEUR_NON_RENSEIGNEE.equalsIgnoreCase(nettoye)) {
            this.nomHost = VALEUR_NON_RENSEIGNEE;
            return;
        }
        if (!nettoye.matches(REGEX_HOSTNAME)) {
            throw new IllegalArgumentException(
                    "Hostname invalide (lettres, chiffres, '.', '-' et '_' uniquement, max 63 caractères).");
        }
        this.nomHost = nettoye;
    }

    public String getAdresseMac() {
        return adresseMac;
    }

    public void setAdresseMac(String adresseMac) {
        String nettoye = nettoyerTexte(adresseMac);
        if (nettoye.isEmpty() || "Non renseignée".equalsIgnoreCase(nettoye)
                || VALEUR_NON_RENSEIGNEE.equalsIgnoreCase(nettoye)) {
            this.adresseMac = "Non renseignée";
            return;
        }
        if (!nettoye.matches(REGEX_MAC)) {
            throw new IllegalArgumentException(
                    "Adresse MAC invalide (format attendu : AA:BB:CC:DD:EE:FF ou AA-BB-CC-DD-EE-FF).");
        }
        this.adresseMac = nettoye.replace("-", ":").toUpperCase(Locale.ROOT);
    }

    public boolean isChiffrementDisque() {
        return chiffrementDisque;
    }

    public void setChiffrementDisque(boolean chiffrementDisque) {
        this.chiffrementDisque = chiffrementDisque;
    }

    public boolean isAntivirusActif() {
        return antivirusActif;
    }

    public void setAntivirusActif(boolean antivirusActif) {
        this.antivirusActif = antivirusActif;
    }

    // ==================== MÉTHODES MÉTIER SÉCURITÉ ====================

    public boolean estConformeSecurite() {
        return chiffrementDisque && antivirusActif;
    }

    // ==================== AFFICHAGE & EXPORT ====================

    @Override
    public void Affiche() {
        System.out.println("  Type         : " + getType() + " (" + formatPoste + ") | SN : " + getSN());
        System.out.println("  Matériel     : " + marque + " " + modele + " | Hostname : " + nomHost);
        System.out.println("  Système      : " + os + " | CPU : " + cpu);
        System.out.println("  Ressources   : RAM " + ramGo + " Go | Stockage : "
                + (stockageGo > 0 ? stockageGo + " Go" : VALEUR_NON_RENSEIGNEE) + " | GPU : " + gpu);
        System.out.println("  Réseau / Séc.: MAC " + adresseMac
                + " | Chiffrement : " + (chiffrementDisque ? "Actif" : "Inactif")
                + " | EDR/AV : " + (antivirusActif ? "Actif" : "Inactif"));
    }

    /**
     * Format rétrocompatible :
     * parts[0]=SN ; parts[1]=Type ; parts[2]=OS ; parts[3]=CPU ; parts[4]=RAM ; parts[5]=GPU ;
     * suivis des champs étendus.
     */
    @Override
    public String export() {
        return getSN() + ";" + getType() + ";" + os + ";" + cpu + ";" + ramGo + ";" + gpu + ";"
                + marque + ";" + modele + ";" + formatPoste + ";" + stockageGo + ";"
                + nomHost + ";" + adresseMac + ";" + chiffrementDisque + ";" + antivirusActif;
    }

    // ==================== UTILITAIRES & CONTRATS JAVA ====================

    private static Integer validerSN(Integer sn) {
        if (sn == null || sn <= 0) {
            throw new IllegalArgumentException("Le numéro de série (SN) d'un ordinateur doit être positif et non nul.");
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
        return "Ordinateur{SN=" + getSN() + ", host='" + nomHost + "', modele='" + marque + " " + modele
                + "', os='" + os + "', ram=" + ramGo + "Go}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ordinateur)) {
            return false;
        }
        Ordinateur other = (Ordinateur) o;
        return Objects.equals(getSN(), other.getSN());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getSN());
    }
}
