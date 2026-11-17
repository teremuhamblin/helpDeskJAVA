# helpDeskJAVA
## CONSEILS_JAVA

### 💡 Bonnes pratiques & Conseils pour `src/main/java/`

Guide de référence pour maintenir la **qualité, sécurité et fiabilité** du code Java de **helpDeskJAVA**.

---

## 1. Aligner strictement les `package` avec les dossiers (Prioritaire)

En Java, la déclaration `package` en haut de chaque fichier doit correspondre exactement au chemin depuis `src/main/java/` :

* **Avec ta structure actuelle (`src/main/java/model/`, `service/`, `Helpdesk.java`) :**
  * `model/*.java` → `package model;`
  * `service/*.java` → `package service;`
  * `Helpdesk.java` → sans déclaration `package` (*default package*) et avec `import model.*;` / `import service.*;`.
* **Recommandation pro (à envisager pour la suite) :**
  Éviter le *default package* en ajoutant un dossier racine `helpdesk/` (ex: `src/main/java/helpdesk/Helpdesk.java` avec `package helpdesk;`). Cela permet aux tests unitaires (JUnit) ou à d'autres outils d'importer `Helpdesk` proprement.

---

## 2. Nom du fichier = Nom exact de la classe `public`

* En Java, `public class NetworkSwitch` impose impérativement que le fichier se nomme **`NetworkSwitch.java`** (attention aux fautes de frappe comme `NetworkSwtich.java` qui bloquent la compilation `javac`).

---

## 3. Respecter la séparation des responsabilités (SoC)

| Dossier | Rôle autorisé | Strictement interdit |
| :--- | :--- | :--- |
| **`model/`** | Données, validations (regex, bornes), calculs métier (`isUnderWarranty`, `isSecurityCompliant`) | `Scanner`, lecture/écriture disque, menus |
| **`service/`** | Stockage (`Map`), recherches, stats, audit, import/export UTF-8 | `Scanner`, `System.out.println` (préférer retourner des résultats ou `String`) |
| **Racine (`Helpdesk.java`)** | Menus, lecture `Scanner`, affichage console, gestion des erreurs utilisateur | Logique métier complexe, accès direct aux fichiers |

---

## 4. Ne jamais polluer `src/main/java/`

* **Aucun fichier compilé (`.class`)** : toujours utiliser `javac -d bin ...` pour séparer les sources des binaires.
* **Aucun fichier de données (`.txt`, `.csv`, `.tmp`, `.bak`)** : les sauvegardes générées à l'exécution (`parc_helpdesk.txt`, `parc_rapport.csv`) doivent rester en dehors de `src/main/java/` (à la racine ou dans un dossier `data/`).
* **`.gitignore` indispensable à la racine du dépôt :**
  ```gitignore
  bin/
  out/
  target/
  *.class
  *.tmp
  *.bak

## 5. Sécurité & Performance : les réflexes à conserver
* **Collections adaptées** : garder Map<Integer, Equipment> (O(1)) pour les recherches par SN plutôt qu'une List (O(n)).
* **Regex pré-compilées** : toujours déclarer private static final Pattern ... = Pattern.compile(...) au lieu d'utiliser String.matches(...) dans les setters.
* **Immutabilité des identifiants** : ne jamais modifier le serialNumber après création (final), car il sert de clé dans les Map et equals()/hashCode().
* **Précision financière** : utiliser BigDecimal (jamais float/double) pour les prix et la valeur totale du parc.
* **Entrées/Sorties sécurisées** : conserver l'encodage StandardCharsets.UTF_8, la validation anti-Path Traversal, les sauvegardes atomiques (.tmp → .bak → cible) et la protection contre l'injection CSV (=, +, -, @).

6. Préparer la suite (Tests & Build)
Puisque tu utilises déjà la convention standard src/main/java/, le projet est prêt pour :
Ajouter des tests unitaires dans src/test/java/ (avec JUnit 5) pour tester automatiquement Computer (MAC/Hostname), Phone (IMEI/Luhn), Equipment (garanties) et HelpdeskService (import/export).
Passer à Maven ou Gradle plus tard en ajoutant simplement un pom.xml ou build.gradle à la racine, sans avoir à déplacer tes fichiers Java.

✅ Check-list rapide avant chaque git commit
Bash
# 1. Recompiler from scratch pour vérifier qu'il n'y a aucune erreur
rm -rf bin && mkdir bin
javac -Xlint:all -d bin src/main/java/model/*.java src/main/java/service/*.java src/main/java/*.java

# 2. Vérifier que git status ne contient ni .class ni fichiers .tmp/.bak
git status
Code
---

> **Petite vigilance repérée dans ton message :** tu as écrit `NetworkSwtich.java` (inversion de `i` et `t`). Pense bien à vérifier que le fichier s'appelle **`NetworkSwitch.java`** sur ton disque pour que `javac` le compile sans erreur avec `public class NetworkSwitch` !
