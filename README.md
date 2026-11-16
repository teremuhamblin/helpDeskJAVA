<div align="center">

# 🛠️ helpDeskJAVA
### Gestion de parc informatique & Helpdesk en Java

Une application console pour inventorier les équipements, suivre les incidents et surveiller les principaux indicateurs du parc IT.

![Java](https://img.shields.io/badge/Java-JDK%2011%2B-FACC15?style=for-the-badge&logo=openjdk&logoColor=white&labelColor=6B21A8)
![Architecture](https://img.shields.io/badge/Architecture-POO%20%7C%20ITIL-6B21A8?style=for-the-badge&labelColor=FACC15)
![Données](https://img.shields.io/badge/Donn%C3%A9es-UTF--8-FACC15?style=for-the-badge&labelColor=6B21A8)
![Exports](https://img.shields.io/badge/Exports-TXT%20%7C%20CSV-6B21A8?style=for-the-badge&labelColor=FACC15)
![Licence](https://img.shields.io/badge/Licence-MIT-FACC15?style=for-the-badge&logo=opensourceinitiative&logoColor=white&labelColor=6B21A8)

</div>

---

## Sommaire

- [Présentation](#présentation)
- [Fonctionnalités](#fonctionnalités)
- [Documentation et structure](#documentation-et-structure)
- [Prérequis et installation](#prérequis-et-installation)
- [Compilation et lancement](#compilation-et-lancement)
- [Guide du menu](#guide-du-menu)
- [Sécurité et persistance](#sécurité-et-persistance)
- [Licence](#licence)

## Présentation

**helpDeskJAVA** est une application Java orientée objet de gestion de parc informatique (ITAM) et de support Helpdesk. Elle permet de centraliser les informations sur les équipements, de suivre leur état et leurs interventions, puis d’exporter les données et rapports.

L’application se pilote depuis un menu en console et prend en charge les équipements suivants :

- ordinateurs,
- commutateurs réseau,
- téléphones professionnels.

## Fonctionnalités

| Domaine | Capacités |
| :--- | :--- |
| **Ordinateurs** | Système d’exploitation, caractéristiques matérielles, hostname, adresse MAC, suivi BitLocker/FileVault et EDR/antivirus |
| **Commutateurs** | Ports, occupation, PoE, gestion L2/L3, IPv4 de gestion et VLAN 802.1Q |
| **Téléphones** | Système d’exploitation, marque, modèle, ligne, validation de l’IMEI et statut MDM |
| **Inventaire** | Numéro de série unique, numéro d’inventaire, fournisseur, achat, garantie et prix HT |
| **Helpdesk** | Affectation, localisation, statuts de cycle de vie et tickets datés |
| **Audit et pilotage** | Alertes de garantie, contrôles de conformité, occupation réseau et tableau de bord |
| **Import / export** | Sauvegarde texte, import avec remplacement ou fusion, export de rapport CSV |

## Documentation et structure

La documentation de la structure du projet se trouve dans [`Docs/structure.md`](Docs/structure.md).

- `Sources/` — code source Java
- `Docs/` — documentation du projet
- `LICENSE` — licence MIT

## Prérequis et installation

- JDK 11 ou supérieur (JDK 17 ou 21 recommandé)
- Git
- Facultatif : Visual Studio Code avec l’Extension Pack for Java

Clonez le dépôt :

```bash
git clone https://github.com/teremuhamblin/helpDeskJAVA.git
cd helpDeskJAVA
```

## Compilation et lancement

Depuis la racine du dépôt, créez le dossier de sortie, compilez les sources puis lancez l’application :

```bash
mkdir -p bin
javac -encoding UTF-8 -d bin Sources/*.java
java -cp bin Helpdesk
```

Dans Visual Studio Code, ouvrez le dossier du projet, puis `Sources/Helpdesk.java` et lancez la classe avec **Run** ou **F5**.

## Guide du menu

| Touche | Action | Description |
| :---: | :--- | :--- |
| 1 | Renommer le parc | Définit ou modifie le nom du parc |
| 2 | Ajouter un matériel | Ajoute un ordinateur, un commutateur ou un téléphone |
| 3 | Mettre à jour | Modifie les informations Helpdesk, administratives, de garantie ou techniques |
| 4 | Ajouter un ticket | Enregistre une intervention datée |
| 5 | Supprimer / réformer | Supprime un matériel par son numéro de série après confirmation |
| 6 | Afficher le parc | Affiche les équipements triés par numéro de série |
| 7 | Rechercher | Recherche par numéro de série, type, statut, utilisateur ou mot-clé |
| 8 | Audit et alertes | Consulte les alertes de garantie, de conformité et de saturation réseau |
| 9 | Tableau de bord | Affiche les statistiques et la valorisation financière HT |
| 10 | Exporter le parc | Enregistre les données dans `parc_helpdesk.txt` |
| 11 | Importer un parc | Importe les données en remplaçant ou en fusionnant le parc |
| 12 | Exporter un rapport CSV | Génère `parc_rapport.csv` pour tableur |
| 0 | Quitter | Ferme l’application après confirmation |

## Sécurité et persistance

- Les données sont enregistrées dans un format texte structuré en UTF-8 ; le projet n’utilise pas `ObjectInputStream`.
- L’écriture du fichier de parc passe par un fichier temporaire et une copie de secours `.bak`.
- Les noms de fichiers autorisés sont validés pour limiter les risques de traversée de chemin (*path traversal*).
- Les champs exportés en CSV sont neutralisés contre les formules de tableur, et un BOM UTF-8 est ajouté pour la compatibilité.
- Les entrées font l’objet de contrôles, notamment pour les numéros de série, les adresses IPv4 et MAC, les VLAN et les IMEI.

## Licence

Ce projet est distribué sous licence MIT. Consultez le fichier [`LICENSE`](LICENSE) pour plus de détails.

---

<div align="center">
Fait avec ☕ en Java
</div>
