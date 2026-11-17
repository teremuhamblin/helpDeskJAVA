/**
 * Suite de tests simples pour valider les fonctionnalités de Helpdesk.
 */
public class HelpdeskTest {
    private static void verifier(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException("ÉCHEC DU TEST : " + message);
        }
    }

    public static void main(String[] args) {
        Helpdesk hd = new Helpdesk();
        Computer pc = new Computer("PC-99", "Lenovo ThinkPad", "Ubuntu", 32);
        hd.ajouterMateriel(pc);

        verifier(hd.getParc().size() == 1, "Le parc devrait contenir 1 élément");
        verifier(hd.rechercherParId("PC-99") != null, "PC-99 devrait être trouvé");
        verifier(hd.signalerPanne("PC-99"), "Le signalement de panne doit réussir");
        verifier("En panne".equals(pc.getStatut()), "Le statut doit passer à 'En panne'");
        verifier(!hd.signalerPanne("INCONNU"), "Un ID inexistant doit renvoyer false");

        System.out.println("Tous les tests sont passés avec succès !");
    }
}
