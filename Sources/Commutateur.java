import java.util.Objects;

public class Switch extends Materiel {

    private static final int MAX_PORTS = 512;
    private static final int VLAN_MIN = 1;
    private static final int VLAN_MAX = 4094;
    private static final String VALEUR_NON_RENSEIGNEE = "Non renseigné";
    private static final String REGEX_IPV4 =
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

    private int nbPortsEthernet;
    private int portsUtilises;
    private String marque;
    private String modele;
    private String vitessePorts;
    private boolean administrable;
    private boolean poe;
    private String adresseIpGestion;
    private int vlanGestion;

    /**
     * Constructeur minimal (100 % compatible avec ParcInformatique.java).
     */
    public Switch(Integer sn, Integer nbPortsEthernet) {
        this(sn, nbPortsEthernet, VALEUR_NON_RENSEIGNEE, VALEUR_NON_RENSEIGNEE,
                "1 Gbps", true, false, "Non assignée", 1, 0);
    }

    /**
     * Constructeur complet pour une fiche commutateur réseau détaillée.
     */
    public Switch(Integer sn, Integer nbPortsEthernet, String marque, String modele,
                  String vitessePorts, boolean administrable, boolean poe,
                  String adresseIpGestion, int vlanGestion, int portsUtilises) {
        super(validerSN(sn), "Switch");
        setNbPortsEthernet(nbPortsEthernet);
        setMarque(marque);
        setModele(modele);
        setVitessePorts(vitessePorts);
        this.administrable = administrable;
        this.poe = poe;
        setAdresseIpGestion(adresseIpGestion);
        setVlanGestion(vlanGestion);
        setPortsUtilises(portsUtilises);
    }

    // ==================== GETTERS & SETTERS SÉCURISÉS ====================

    public int getNbPortsEthernet() {
        return nbPortsEthernet;
    }

    public void setNbPortsEthernet(Integer nbPortsEthernet) {
        if (nbPortsEthernet == null || nbPortsEthernet < 1 || nbPortsEthernet > MAX_PORTS) {
            throw new IllegalArgumentException(
                    "Le nombre de ports Ethernet doit être compris entre 1 et " + MAX_PORTS + ".");
        }
        if (nbPortsEthernet < this.portsUtilises) {
            throw new IllegalArgumentException(
                    "Le nombre total de ports ne peut pas être inférieur aux ports déjà utilisés ("
                            + this.portsUtilises + ").");
        }
        this.nbPortsEthernet = nbPortsEthernet;
    }

    public int getPortsUtilises() {
        return portsUtilises;
    }

    public void setPortsUtilises(int portsUtilises) {
        if (portsUtilises < 0 || portsUtilises > this.nbPortsEthernet) {
            throw new IllegalArgumentException(
                    "Le nombre de ports utilisés doit être compris entre 0 et " + this.nbPortsEthernet + ".");
        }
        this.portsUtilises = portsUtilises;
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

    public String getVitessePorts() {
        return vitessePorts;
    }

    public void setVitessePorts(String vitessePorts) {
        String nettoye = nettoyerTexte(vitessePorts);
        this.vitessePorts = nettoye.isEmpty() ? "1 Gbps" : nettoye;
    }

    public boolean isAdministrable() {
        return administrable;
    }

    public void setAdministrable(boolean administrable) {
        this.administrable = administrable;
    }

    public boolean isPoe() {
        return poe;
    }

    public void setPoe(boolean poe) {
        this.poe = poe;
    }

    public String getAdresseIpGestion() {
        return adresseIpGestion;
    }

    public void setAdresseIpGestion(String adresseIpGestion) {
        String nettoye = nettoyerTexte(adresseIpGestion);
        if (nettoye.isEmpty() || "Non assignée".equalsIgnoreCase(nettoye)
                || VALEUR_NON_RENSEIGNEE.equalsIgnoreCase(nettoye)) {
            this.adresseIpGestion = "Non assignée";
            return;
        }
        if ("DHCP".equalsIgnoreCase(nettoye)) {
            this.adresseIpGestion = "DHCP";
            return;
        }
        if (!nettoye.matches(REGEX_IPV4)) {
            throw new IllegalArgumentException(
                    "Adresse IPv4 invalide (exemples valides : '192.168.10.2', 'DHCP' ou vide).");
        }
        this.adresseIpGestion = nettoye;
    }

    public int getVlanGestion() {
        return vlanGestion;
    }

    public void setVlanGestion(int vlanGestion) {
        if (vlanGestion < VLAN_MIN || vlanGestion > VLAN_MAX) {
            throw new IllegalArgumentException(
                    "Le VLAN de gestion doit être compris entre " + VLAN_MIN + " et " + VLAN_MAX + ".");
        }
        this.vlanGestion = vlanGestion;
    }

    // ==================== MÉTHODES MÉTIER RÉSEAU ====================

    public int getPortsDisponibles() {
        return nbPortsEthernet - portsUtilises;
    }

    public double getTauxOccupation() {
        return (portsUtilises * 100.0) / nbPortsEthernet;
    }

    public boolean estSature() {
        return portsUtilises >= nbPortsEthernet;
    }

    // ==================== AFFICHAGE & EXPORT ====================

    @Override
    public void Affiche() {
        System.out.println("  Type         : Commutateur réseau (" + getType() + ") | SN : " + getSN());
        System.out.println("  Matériel     : " + marque + " " + modele + " (" + vitessePorts + ")");
        System.out.printf("  Ports        : %d / %d utilisés (%d libre(s), %.0f%%) | PoE : %s%n",
                portsUtilises, nbPortsEthernet, getPortsDisponibles(), getTauxOccupation(),
                (poe ? "Oui" : "Non"));
        System.out.println("  Réseau       : " + (administrable ? "Administrable (L2/L3)" : "Non administrable")
                + " | IP : " + adresseIpGestion + " | VLAN : " + vlanGestion);
    }

    /**
     * Format rétrocompatible :
     * parts[0]=SN ; parts[1]=Type ; parts[2]=NbPorts ; suivis des champs réseau étendus.
     */
    @Override
    public String export() {
        return getSN() + ";" + getType() + ";" + nbPortsEthernet + ";"
                + marque + ";" + modele + ";" + vitessePorts + ";"
                + administrable + ";" + poe + ";" + adresseIpGestion + ";"
                + vlanGestion + ";" + portsUtilises;
    }

    // ==================== UTILITAIRES & CONTRATS JAVA ====================

    private static Integer validerSN(Integer sn) {
        if (sn == null || sn <= 0) {
            throw new IllegalArgumentException("Le numéro de série (SN) d'un commutateur doit être positif et non nul.");
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
        return "Switch{SN=" + getSN() + ", modele='" + marque + " " + modele
                + "', ports=" + portsUtilises + "/" + nbPortsEthernet
                + ", ip='" + adresseIpGestion + "', vlan=" + vlanGestion + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Switch)) {
            return false;
        }
        Switch other = (Switch) o;
        return Objects.equals(getSN(), other.getSN());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getSN());
    }
}
