# Fichiers compilés (`bin/`)

Ce dossier contient les fichiers bytecode (`.class`) générés lors de la compilation des fichiers de `Sources/`.

> ⚠️ **Ne modifiez pas ces fichiers manuellement** : ils sont écrasés et régénérés à chaque compilation.

## 🚀 Compiler et exécuter avec `bin/`

Depuis la **racine du projet** :

**1. Compiler les sources dans `bin/` :**
```bash
javac -d bin -cp "lib/*" Sources/*.java
```
*(Retirez `-cp "lib/*"` si vous n'utilisez aucune bibliothèque externe.)*

**2. Lancer l'application :**
* **Linux / macOS :** `java -cp "bin:lib/*" Helpdesk`
* **Windows :** `java -cp "bin;lib/*" Helpdesk`

## 🧹 Bonnes pratiques (Git)

Si vous utilisez Git, ignorez les fichiers `.class` dans votre `.gitignore` pour ne pas versionner les binaires générés :
```gitignore
bin/*.class
```
