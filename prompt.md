Projet de gestion de musique et génération de playlists
Partie Backend (Applications Standalone)
Objectif général
Le système doit traiter automatiquement les fichiers MP3 déposés dans un répertoire d'import.
Le traitement est découpé en trois programmes indépendants communiquant via RabbitMQ.
Le flux global est :
1. Détection des nouveaux MP3.
2. Extraction des métadonnées.
3. Envoi du MP3 et des métadonnées à une API.
4. Stockage du fichier et des métadonnées par l'API.
5. Suppression du fichier du répertoire d'import après succès.

Programme 1 : Scanner de répertoire
Rôle
Surveiller un répertoire contenant les musiques à importer.
Fonctionnement
* Lire périodiquement le contenu du répertoire (par exemple toutes les 5 minutes).
* Ne traiter que les fichiers ayant l'extension .mp3.
* Ignorer tous les autres types de fichiers.
* Détecter les nouveaux morceaux ajoutés depuis le dernier scan.
Entrée
Répertoire d'import :
incoming/
Sortie
Liste des fichiers MP3 détectés :
* Nom du fichier
* Chemin absolu du fichier
Exemple :
C:/incoming/song1.mp3
C:/incoming/song2.mp3
Cette liste est envoyée dans une file RabbitMQ dédiée.

Programme 2 : Extracteur de métadonnées
Rôle
Extraire les informations disponibles dans chaque fichier MP3.
Entrée
Liste des MP3 reçue depuis le Programme 1.
Traitement
Utiliser une bibliothèque spécialisée (par exemple jaudiotagger ou mp3agic) afin d'extraire :
* Titre
* Artiste
* Album
* Genre
* Durée
* Année
* Date
* Toute autre métadonnée disponible
Sortie
Pour chaque fichier :
{
  "path": "C:/incoming/song1.mp3",
  "title": "Imagine",
  "artist": "John Lennon",
  "album": "Imagine",
  "genre": "Rock",
  "duration": 183
}
Ces informations sont envoyées dans une autre file RabbitMQ.

Programme 3 : Uploader API
Rôle
Transmettre les fichiers MP3 et leurs métadonnées à l'API.
Entrée
Informations reçues depuis le Programme 2 :
* Chemin du fichier
* Métadonnées extraites
Traitement
Appeler l'API en envoyant :
* Le fichier MP3
* Les métadonnées associées
Réponse attendue
Succès
L'API :
* Enregistre les métadonnées dans la base de données.
* Copie le fichier MP3 dans son espace de stockage permanent.
Une fois la confirmation reçue :
* Le Programme 3 supprime le fichier du répertoire d'import (incoming).
Ainsi, lors du prochain scan, le Programme 1 ne verra plus ce fichier.
Échec
* Le fichier n'est pas supprimé.
* Une ou plusieurs tentatives de réenvoi sont effectuées.
Si toutes les tentatives échouent :
* Déplacer le fichier dans un dossier d'échec.
Exemple :
failed/

Gestion des logs
Les trois programmes doivent écrire dans un fichier de log.
Les logs doivent contenir :
* Les actions réalisées.
* Les erreurs rencontrées.
* Les exceptions.
Exemple Programme 1
10:00 Début du scan
10:01 Nouveau fichier détecté : song1.mp3
Exemple Programme 2
10:02 Début extraction : song1.mp3
10:02 Métadonnées extraites avec succès
Exemple Programme 3
10:03 Début envoi API : song1.mp3
10:03 Envoi en cours
10:04 Envoi terminé avec succès
10:04 Suppression du fichier source

Communication entre programmes
Principe
Les programmes ne communiquent pas directement entre eux.
La communication se fait via RabbitMQ.
Fonctionnement
Programme 1 :
Scan MP3
      ↓
Queue Scan
Programme 2 :
Queue Scan
      ↓
Extraction métadonnées
      ↓
Queue Metadata
Programme 3 :
Queue Metadata
      ↓
Envoi API
Chaque programme :
* Écoute uniquement sa propre queue.
* Consomme les messages qui lui sont destinés.
* Produit un nouveau message à destination du programme suivant.
Cette architecture permet un fonctionnement asynchrone et découplé.

API Backend
Rôle
Recevoir les fichiers MP3 et les métadonnées.
Responsabilités
* Stocker les métadonnées dans la base de données.
* Conserver une copie permanente du fichier MP3.
* Fournir les données nécessaires à l'application web.
Remarque importante
Le répertoire incoming n'est qu'un répertoire temporaire d'import.
Les fichiers MP3 utilisés par l'application web proviennent du stockage permanent géré par l'API.
Cela permet de supprimer les fichiers du répertoire d'import après traitement tout en conservant les morceaux disponibles pour les utilisateurs.

Partie Web
Objectif
Permettre aux utilisateurs de générer, enregistrer et utiliser des playlists.

Authentification
Fonctionnalités :
* Login utilisateur
* Gestion des comptes utilisateurs

Gestion des MP3
CRUD complet :
* Ajouter un MP3
* Consulter les MP3
* Modifier les informations
* Supprimer un MP3

Interface 1 : Génération de playlist
L'utilisateur définit des critères :
Critères possibles
* Durée totale souhaitée
* Genre musical
* Artiste
* Album
* Autres critères éventuels
Exemple
Durée demandée :
60 minutes
Le système doit proposer automatiquement une playlist dont la somme des durées est proche ou égale à 60 minutes.
Après génération, l'utilisateur peut :
* Ajouter des morceaux.
* Retirer des morceaux.
* Modifier librement la playlist même si elle ne respecte plus la durée initiale.

Interface 2 : Sauvegarde de playlist
Après génération ou modification :
* L'utilisateur peut enregistrer sa playlist.

Interface 3 : Gestion des playlists
L'utilisateur peut :
* Voir ses playlists enregistrées.

Actions disponibles sur une playlist
Lecture
Lecture des morceaux de la playlist.
Téléchargement
Téléchargement de tous les morceaux de la playlist dans une archive ZIP.
Exemple :
playlist.zip
 ├── song1.mp3
 ├── song2.mp3
 └── song3.mp3