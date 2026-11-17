import model.Computer;
import model.EquipmentStatus;
import model.HelpdeskRecord;
import model.NetworkSwitch;
import model.Phone;
import service.HelpdeskService;

/**
 * Suite de tests unitaires (sans dépendance externe) pour valider Helpdesk v1.2 Stable.
 */
public class HelpdeskTest {

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("ÉCHEC DU TEST : " + message);
        }
    }

    public static void main(String[] args) {
        HelpdeskService service = new HelpdeskService("Parc Test");

        // 1. Test création & ajout d'un Computer (Builder v1.2)
        Computer pc = Computer.builder(99, "Ubuntu 24.04", "AMD Ryzen 7", 32, "Integrated")
                .brand("Lenovo")
                .model("ThinkPad T14")
                .hostname("PC-99")
                .macAddress("aa-bb-cc-dd-ee-ff")
                .diskEncrypted(true)
                .antivirusActive(true)
                .build();

        assertTrue(service.addEquipment(pc), "L'ajout du PC-99 doit réussir");
        assertTrue(!service.addEquipment(pc), "Un doublon de SN ne doit pas être ajouté");
        assertTrue(service.getEquipmentCount() == 1, "Le parc devrait contenir 1 équipement");
        assertTrue(service.findBySerialNumber(99) != null, "Le SN 99 devrait être trouvé");
        assertTrue("AA:BB:CC:DD:EE:FF".equals(pc.getMacAddress()), "La MAC doit être normalisée");
        assertTrue(pc.isSecurityCompliant(), "Le PC doit être conforme sécurité");

        // 2. Test gestion Helpdesk (ticket + panne)
        HelpdeskRecord record = service.getOrCreateRecord(99);
        record.addTicket("Écran noir au démarrage");
        record.setStatus(EquipmentStatus.BROKEN);

        assertTrue(record.getTickets().size() == 1, "1 ticket doit être enregistré");
        assertTrue(record.getStatus() == EquipmentStatus.BROKEN, "Le statut doit passer à BROKEN");
        assertTrue(service.findByStatus(EquipmentStatus.BROKEN).size() == 1, "Recherche par statut BROKEN");

        // 3. Test NetworkSwitch & Phone (validations + Luhn IMEI)
        NetworkSwitch sw = NetworkSwitch.builder(200, 24)
                .usedPortCount(21)
                .managementIp("192.168.10.2")
                .build();
        assertTrue(sw.getPortUtilizationPercent() >= 85.0, "Le switch doit dépasser le seuil d'alerte 85%");

        Phone phone = Phone.builder(300, "iOS 18")
                .imei("490154203237518")
                .mdmEnrolled(true)
                .build();
        assertTrue(phone.hasValidImeiChecksum(), "L'IMEI valide doit passer le contrôle Luhn");
        assertTrue("***********7518".equals(phone.getMaskedImei()), "L'IMEI masqué doit cacher les 11 premiers chiffres");

        // 4. Test sécurité / rejet d'entrées invalides
        boolean invalidMacRejected = false;
        try {
            pc.setMacAddress("INVALID-MAC");
        } catch (IllegalArgumentException e) {
            invalidMacRejected = true;
        }
        assertTrue(invalidMacRejected, "Une adresse MAC invalide doit lever IllegalArgumentException");

        // 5. Test suppression
        assertTrue(!service.removeEquipment(999999), "Un SN inexistant doit renvoyer false");
        assertTrue(service.removeEquipment(99), "La suppression du SN 99 doit réussir");

        System.out.println("Tous les tests v1.2 sont passés avec succès !");
    }
}
