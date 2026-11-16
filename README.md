###### README.md >> markdown
# helpDeskJAVA

<div align="center">

### 🛠️ 𝐇𝐄𝐋𝐏𝐃𝐄𝐒𝐊
𝐆𝐄𝐒𝐓𝐈𝐎𝐍 𝐃𝐄 𝐏𝐀𝐑𝐂 𝐈𝐓

### *Solution Java moderne de gestion d'actifs informatiques (ITAM), suivi d'incidents et audit sécurité*

<br/>

![Java](https://img.shields.io/badge/Java-JDK%2011%2B%20%2F%2017%2B-FACC15?style=for-the-badge&logo=openjdk&logoColor=white&labelColor=6B21A8)
![Architecture](https://img.shields.io/badge/Architecture-POO%20%7C%20ITIL-6B21A8?style=for-the-badge&labelColor=FACC15)
![Sécurité](https://img.shields.io/badge/S%C3%A9curit%C3%A9-UTF--8%20%7C%20Validation-FACC15?style=for-the-badge&labelColor=6B21A8)
![Export](https://img.shields.io/badge/Export-TXT%20Atomique%20%26%20CSV-6B21A8?style=for-the-badge&labelColor=FACC15)
![Git](https://img.shields.io/badge/Git-Sources%2F%20Ready-FACC15?style=for-the-badge&logo=git&logoColor=white&labelColor=6B21A8)
![Licence](https://img.shields.io/badge/Licence-MIT-6B21A8?style=for-the-badge&logo=opensourceinitiative&logoColor=6B21A8&labelColor=FACC15)

<br/>

> 💡 **Un outil console complet, fiable et sécurisé** pour piloter vos **ordinateurs**, **commutateurs réseau (switchs)** et **téléphones professionnels**, suivre vos **tickets Helpdesk** et auditer votre **conformité cybersécurité**.

</div>

---

## 🧭 𝐒𝐨𝐦𝐦𝐚𝐢𝐫𝐞 𝐨𝐩é𝐫𝐚𝐭𝐢𝐨𝐧𝐧𝐞𝐥

- [📌 Présentation](#-presentation)
- [✨ Fonctionnalités clés](#-fonctionnalites)
- [🗂️ Architecture du projet](#-architecture)
- [⚙️ Prérequis & Installation](#-installation)
- [🚀 Compilation & Lancement](#-lancement)
- [📋 Guide du Menu (0 à 12)](#-menu)
- [🛡️ Sécurité & Persistance des données](#-securite)
- [📄 Licence](#-licence)

---

<a id="presentation"></a>
## 📌 𝐏𝐫é𝐬𝐞𝐧𝐭𝐚𝐭𝐢𝐨𝐧

**Helpdesk & Parc IT** est une application Java orientée objet conçue selon les bonnes pratiques **Helpdesk / ITAM** (*IT Asset Management*). Elle permet à un technicien support ou administrateur système de centraliser l'inventaire matériel, gérer le cycle de vie des équipements, enregistrer des interventions et exporter des rapports exploitables sous Excel.

---

<a id="fonctionnalites"></a>
## ✨ 𝐅𝐨𝐧𝐜𝐭𝐢𝐨𝐧𝐧𝐚𝐥𝐢𝐭é𝐬 𝐜𝐥é𝐬

| Domaine | Fonctionnalités incluses |
| :--- | :--- |
| 🖥️ **Postes (`Ordinateur`)** | OS, CPU, RAM, GPU, Stockage, Format, **Hostname**, **Adresse MAC** normalisée, suivi **BitLocker/FileVault** & **EDR/Antivirus** |
| 🌐 **Réseau (`Commutateur`)** | Nombre de ports, taux d'occupation, **PoE**, administrable L2/L3, **IPv4 de gestion**, **VLAN (802.1Q)** |
| 📱 **Mobilité (`Telephone`)** | OS, marque, modèle, numéro de ligne, validation **IMEI (15 chiffres)**, statut d'enrôlement **MDM** |
| 📦 **Actifs (`Materiel`)** | Unicité par **SN**, N° d'inventaire, fournisseur, dates d'achat/garantie, prix HT, calcul des **jours de garantie restants** |
| 🎫 **Suivi Helpdesk** | Statuts ITIL (`En stock`, `En service`, `En panne`, `En réparation`, `Réformé`), affectation utilisateur, localisation, **tickets datés** |
| 🚨 **Audit & Pilotage** | Détection des **garanties expirées (< 30 j)**, postes non conformes, switchs saturés (`>= 85%`), tableau de bord financier & technique |

---

<a id="architecture"></a>
## 🗂️ 𝐀𝐫𝐜𝐡𝐢𝐭𝐞𝐜𝐭𝐮𝐫𝐞 𝐝𝐮 𝐩𝐫𝐨𝐣𝐞𝐭

```text
📦 Projet-Helpdesk/
├── 📂 Sources/
│   ├── ☕ Materiel.java        # Classe mère : actif, fournisseur, coûts & garanties
│   ├── ☕ Ordinateur.java      # Postes de travail, MAC, Hostname & cybersécurité
│   ├── ☕ Switch.java          # Commutateurs réseau, ports, IP, VLAN & PoE
│   ├── ☕ Telephone.java       # Flotte mobile, ligne, IMEI & conformité MDM
│   └── ☕ Helpdesk.java        # Moteur principal, menu, audit & import/export
├── 📂 bin/                     # Fichiers compilés (.class) — ignorés par Git
├── 📂 lib/                     # Dépendances éventuelles (.jar)
├── 🙈 .gitignore               # Règles Git Java / VS Code / exports Helpdesk
├── 📄 LICENSE                  # Licence MIT
└── 📘 README.md                #
```

## Documentation du projet

<a id="installation"></a>
### ⚙️ 𝐏𝐫é𝐫𝐞𝐪𝐮𝐢𝐬 & 𝐈𝐧𝐬𝐭𝐚𝐥𝐥𝐚𝐭𝐢𝐨𝐧
Java Development Kit (JDK) : version 11 ou supérieure (JDK 17 / 21 recommandé).

Éditeur conseillé : Visual Studio Code avec l'extension Extension Pack for Java.

### Cloner le dépôt
```text
git clone https://github.com/teremuhamblin/helpDeskJAVA.git
cd helpDeskJAVA
```

<a id="lancement"></a>
### 🚀 𝐂𝐨𝐦𝐩𝐢𝐥𝐚𝐭𝐢𝐨𝐧 & 𝐋𝐚𝐧𝐜𝐞𝐦𝐞𝐧𝐭
>>Option 1 — Via le terminal (recommandé avec encodage UTF-8)
```bash
# Compiler toutes les classes depuis Sources/ vers bin/
javac -encoding UTF-8 -d bin Sources/*.java
```

```bash
# Lancer l'application Helpdesk
java -cp bin Helpdesk
```

>>Option 2 — Via Visual Studio Code
- Ouvrez le dossier du projet dans VS Code.
- Ouvrez Sources/Helpdesk.java.
Cliquez sur Run (ou appuyez sur F5).

<a id="menu"></a>
### 📋 𝐆𝐮𝐢𝐝𝐞 𝐝𝐮 𝐌𝐞𝐧𝐮 (𝟎 à 𝟏𝟐)
Touche	Action	Description
- 1	Renommer le parc	Définit ou modifie le nom du parc informatique
- 2	Ajouter un matériel	Assistant complet pour Ordinateur, Commutateur ou Téléphone
- 3	Mettre à jour	Modifie fiche Helpdesk, infos admin/garantie et champs techniques
- 4	Ajouter un ticket	Ajoute une intervention datée et passe en panne si besoin
- 5	Supprimer / Réformer	Suppression sécurisée par SN avec confirmation O/N
- 6	Afficher tout le parc	Liste complète triée automatiquement par numéro de série
- 7	Rechercher	Par SN, type, statut, utilisateur ou mot-clé global
- 8	Audit & Alertes	Liste garanties expirées, failles postes/MDM et saturation réseau
- 9	Tableau de bord	Statistiques complètes + valorisation financière HT
- 10	Exporter le parc	Sauvegarde complète sécurisée (parc_helpdesk.txt)
- 11	Importer un parc	Chargement transactionnel (remplacer ou fusionner sans doublons)
- 12	Exporter rapport CSV	Génère parc_rapport.csv prêt pour Microsoft Excel
- 0	Quitter	Fermeture propre avec demande de confirmation

<a id="securite"></a>
### 🛡️ 𝐒é𝐜𝐮𝐫𝐢𝐭é & 𝐏𝐞𝐫𝐬𝐢𝐬𝐭𝐚𝐧𝐜𝐞 𝐝𝐞𝐬 𝐝𝐨𝐧𝐧é𝐞𝐬
* *Ce projet n'utilise plus la sérialisation binaire Java (ObjectInputStream) afin d'écarter tout risque de faille de désérialisation*.

* *Format texte UTF-8 structuré (parc_helpdesk.txt) : enregistre les blocs PARC;, MAT;, ADMIN; et HD;.
Écriture atomique & Sauvegarde : passage par un fichier .tmp puis création automatique d'une copie de secours .bak*.

* *Protection contre le Path Traversal : validation stricte des noms de fichiers autorisés*.

* *Protection CSV Excel : neutralisation des formules dangereuses (=, +, -, @) et ajout du BOM UTF-8*.

* *Validation des entrées : contrôle d'unicité des SN, format IPv4, VLAN 1..4094, adresse MAC, IMEI et assainissement des séparateurs*.

<a id="licence"></a>
### 📄 𝐋𝐢𝐜𝐞𝐧𝐜𝐞
>Ce projet est distribué sous licence MIT
- — voir le fichier LICENSE pour plus de détails.

---

<div align="center">
Fait avec ☕ en Java — Standard Helpdesk & Parc Informatique Moderne
</div>
