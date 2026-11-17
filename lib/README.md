# Dépendances (`lib/`)

Ce dossier centralise les bibliothèques externes (fichiers `.jar`) requises par le projet **helpDeskJAVA**.

## 📦 Utilisation

1. Déposez vos fichiers `.jar` directement dans ce dossier.
2. Si vous utilisez un IDE (VS Code, IntelliJ, Eclipse), pensez à ajouter ce dossier en tant que bibliothèque (*Referenced Libraries* / *Add as Library*).

## 🚀 Compiler et lancer en ligne de commande

Depuis la **racine du projet**, incluez le dossier `lib/` dans le *classpath* (`-cp`) :

**Linux / macOS :**
```bash
javac -cp "lib/*" Sources/*.java
java -cp "Sources:lib/*" Helpdesk
```

**Windows :**
```cmd
javac -cp "lib/*" Sources/*.java
java -cp "Sources;lib/*" Helpdesk
```

## 📋 Dépendances utilisées

* *(Listez ici vos `.jar` au fur et à mesure, ex : `mysql-connector-j.jar` — Connexion BDD)*
