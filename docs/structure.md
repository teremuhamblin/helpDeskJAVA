###### structure.md  >> markdown 
# helpDeskJAVA

## 🗂️ 𝐀𝐫𝐜𝐡𝐢𝐭𝐞𝐜𝐭𝐮𝐫𝐞 𝐝𝐮 𝐩𝐫𝐨𝐣𝐞𝐭
### Base Stable v1.0
```text
📦 helpDeskJAVA/
├── 📂 .github/ # Fichier de gouvernance et de sécurité 
├── 📂 Sources/
│   ├── ☕ README.md 
│   ├── ☕ Materiel.java       # Classe mère : actif, fournisseur, coûts & garanties
│   ├── ☕ Computer.java       # Postes de travail, MAC, Hostname & cybersécurité
│   ├── ☕ Commutateur.java    # Commutateurs réseau, ports, IP, VLAN & PoE
│   ├── ☕ Phone.java          # Flotte mobile, ligne, IMEI & conformité MDM
│   └── ☕ Helpdesk.java       # Moteur principal, menu, audit & import/export
├── 📂 Docs/
│   ├── ☕ README.md           # Documentation generale
├── 📂 bin/
│   ├── ☕ README.md           # Fichiers compilés (.class) — ignorés par Git
├── 📂 lib/
│   ├── ☕ README.md           # Dépendances éventuelles (.jar)
├── 🙈 .gitignore               # Règles Git Java / VS Code / exports Helpdesk
├── 📄 LICENSE                  # Licence MIT
└── 📘 README.md                #
```

### 📦 Structure du dossier Sources/
### Mise à jour v1.1 Stable
```text
Sources/
    ├── README.md 
    ├── Main.java                 # Point d'entrée (ou Helpdesk.java)
    ├── model/
    │   ├── README.md            # Les entités / objets du parc informatique
    │   ├── Materiel.java
    │   ├── Computer.java
    │   ├── Commutateur.java
    │   └── Phone.java
    └── service/                  # Logique métier (ajout, recherche, gestion des tickets...)
        └── ParcService.java
```

### Convention standard
#### Mise à jour v1.2 Stable
- Adopter la convention standard Java (*optionnel, pour aller plus loin*)
**Si vous envisagez d'utiliser un IDE (IntelliJ, Eclipse, VS Code) ou un outil de build comme Maven ou Gradle**, la convention standard consiste à remplacer Sources/ par :

```text
src/
├── main/
│   ├── java/       # Vos packages et fichiers .java
│   └── resources/  # Fichiers de config, base de données locale, CSV/JSON...
└── test/
    └── java/       # Vos tests unitaires (ex: JUnit)
```
