# helpDeskJAVA
>> — Code source (`src/main/java/`)

Application Java de gestion de parc informatique et de support technique (Helpdesk) — **v1.2 Stable**.

## 📁 Structure du dossier

```text
src/main/java/
├── Helpdesk.java                # Point d'entrée de l'application (interface CLI)
├── model/                       # Modèles métier & entités du parc
│   ├── Equipment.java           # Classe mère générique (SN, garantie, infos admin)
│   ├── Computer.java            # Postes de travail (OS, CPU, RAM, MAC, sécurité)
│   ├── NetworkSwitch.java       # Commutateurs réseau (ports, VLAN, PoE, IP)
│   ├── Phone.java               # Téléphones fixes/mobiles (ligne, IMEI, MDM)
│   ├── EquipmentStatus.java     # Statuts ITIL (En stock, En service, En panne...)
│   └── HelpdeskRecord.java      # Fiche d'affectation et historique des tickets
└── service/                     # Logique métier, audit et persistance
    └── HelpdeskService.java     # Gestion O(1), recherche, stats, import/export UTF-8
```

## ⚙️ Prérequis
- Java JDK (version 8 ou supérieure)

### 🚀 Compilation et exécution
Depuis la racine du projet :
```bash
# Compiler toutes les sources vers un dossier dédié (bin/)
javac -d bin src/main/java/model/*.java src/main/java/service/*.java src/main/java/*.java
```

```bash
# Lancer l'application
java -cp bin Helpdesk
(Note : Si vous utilisez un package racine commun comme helpdesk, pensez à adapter la commande d'exécution en java -cp bin helpdesk.Helpdesk).
```
