# Partie 3 — Application Web (Vue.js)

> Front **Vue.js 3** consommant l'API Backend : authentification, CRUD MP3,
> génération / sauvegarde / gestion de playlists, lecture et téléchargement ZIP.

## Stack
Vue 3 + Vite, Vue Router, Pinia (store), Axios (HTTP), un lecteur audio HTML5,
(option : Tailwind / Vuetify pour l'UI).

---

## 0. Mise en place

- [ ] Initialiser le projet : `npm create vite@latest` (template Vue).
- [ ] Installer : `vue-router`, `pinia`, `axios`.
- [ ] Configurer l'URL de base de l'API (variable d'environnement `.env`).
- [ ] Instance Axios centralisée (intercepteur d'erreurs + injection du JWT).
- [ ] Mettre en place le routeur et le store Pinia.
- [ ] Layout global (header, navigation, zone de contenu).

---

## 1. Authentification

- [ ] Store `auth` (token JWT, user, état connecté).
- [ ] Page **Login**.
- [ ] Page **Inscription** (création de compte).
- [ ] Stockage du token (localStorage) + ajout automatique dans les requêtes.
- [ ] **Guard de navigation** : protéger les routes nécessitant une connexion.
- [ ] Déconnexion (logout).
- [ ] Gestion des comptes utilisateurs (profil).

---

## 2. Gestion des MP3 (CRUD)

- [ ] Page **Liste des MP3** (tableau / cartes) avec recherche et filtres
      (genre, artiste, album).
- [ ] Vue **Détail** d'un MP3.
- [ ] Formulaire **Ajouter** un MP3.
- [ ] Formulaire **Modifier** les informations.
- [ ] Action **Supprimer** (avec confirmation).
- [ ] **Lecteur audio** pour écouter un MP3 (endpoint `/stream`).

---

## 3. Interface 1 — Génération de playlist

- [ ] Formulaire de **critères** :
  - [ ] Durée totale souhaitée (ex : 60 minutes).
  - [ ] Genre musical.
  - [ ] Artiste.
  - [ ] Album.
  - [ ] Autres critères éventuels.
- [ ] Appel `POST /api/playlists/generate`.
- [ ] Afficher la playlist proposée + la **durée totale** obtenue.
- [ ] Permettre d'**ajouter** des morceaux.
- [ ] Permettre de **retirer** des morceaux.
- [ ] Permettre de **réordonner** / modifier librement
      (même si la durée n'est plus respectée).

---

## 4. Interface 2 — Sauvegarde de playlist

- [ ] Bouton **Enregistrer** (saisie d'un nom).
- [ ] Appel `POST /api/playlists` (depuis génération ou modification).
- [ ] Feedback de confirmation.

---

## 5. Interface 3 — Gestion des playlists

- [ ] Page **Mes playlists** (liste des playlists enregistrées).
- [ ] Vue **Détail** d'une playlist (morceaux ordonnés).
- [ ] Action **Lecture** : lire les morceaux de la playlist (lecteur + file d'attente).
- [ ] Action **Téléchargement ZIP** : `GET /api/playlists/{id}/download`
      → télécharger `playlist.zip` (song1.mp3, song2.mp3, ...).
- [ ] Action **Modifier** (réutilise l'interface de génération/édition).
- [ ] Action **Supprimer** une playlist.

---

## 6. Qualité & transverse

- [ ] Composants réutilisables (carte morceau, lecteur, modale, etc.).
- [ ] Gestion des états de chargement / erreurs (UX).
- [ ] Messages de notification (succès / erreur).
- [ ] Design responsive.
- [ ] (Option) Tests de composants (Vitest).

---

## 7. Tests & validation

- [ ] Parcours complet : login → liste MP3 → générer playlist → sauvegarder.
- [ ] Vérifier la lecture audio.
- [ ] Vérifier le téléchargement ZIP.
- [ ] Vérifier les filtres et le CRUD MP3.
- [ ] Vérifier la protection des routes (utilisateur non connecté).
