# helpDeskJAVA

Application Java de gestion de parc informatique et de support technique (Helpdesk).

## 📁 Structure du projet

Le dossier `Sources/` contient les classes suivantes :

- `Materiel.java` : Classe représentant un équipement informatique générique.
- `Computer.java` : Gestion des ordinateurs.
- `Commutateur.java` : Gestion des commutateurs réseau (switchs).
- `Phone.java` : Gestion des téléphones.
- `Helpdesk.java` : Classe principale de l'application (gestion du support).

## ⚙️ Prérequis

- **Java JDK** (version 8 ou supérieure)

## 🚀 Compilation et exécution

Ouvrez un terminal à la racine du projet et exécutez les commandes suivantes :

```bash
# Compiler tous les fichiers Java
javac Sources/*.java

# Lancer l'application
java -cp Sources Helpdesk
```

*(Note : Si vos fichiers contiennent une déclaration `package` en haut du code ou si la méthode `main` se trouve dans une autre classe, pensez simplement à ajuster la dernière commande).*
