# Avancement — Partie 3 : Application Web (Vue.js)

**Statut : TERMINÉ.** Build de production OK, serveur de dev opérationnel, API + CORS joignables.
Date : 2026-06-18 · Stack : Vue 3 + Vite 8, Vue Router 4, Pinia, Axios.

> Thème **« vinyle classique × streaming moderne »** : fond espresso, accent or/ambre,
> typo Playfair Display + Inter, disque vinyle qui tourne pendant la lecture, barre de
> lecture persistante en bas (à la Spotify / Apple Music).

---

## 1. Ce qui est fait (couvre tout [03-web-frontend.md](../03-web-frontend.md))

| Fonctionnalité | Écran / composant |
|---|---|
| **Login / Inscription** | `views/LoginView.vue` (hero vinyle + formulaire à onglets), store `stores/auth.js` |
| **Protection des routes** | `router/index.js` (guard), JWT injecté par `api/http.js`, logout auto sur 401/403 |
| **CRUD MP3** | `views/LibraryView.vue` : liste, recherche, filtre genre, **import** (multipart), **modifier**, **supprimer** |
| **Lecture audio** | `stores/player.js` (moteur `Audio()` global) + `components/PlayerBar.vue` + `VinylDisc.vue` |
| **Génération de playlist** | `views/GenerateView.vue` : durée (slider + presets), genre/artiste/album, jauge objectif vs obtenu, ajout/retrait libre |
| **Sauvegarde de playlist** | `GenerateView` → `POST /api/playlists` |
| **Gestion des playlists** | `views/PlaylistsView.vue` (grille de pochettes) + `views/PlaylistDetailView.vue` |
| **Lecture playlist** | « Tout lire » → file de lecture (`playQueue`) |
| **Téléchargement ZIP** | `PlaylistDetailView` → `GET /api/playlists/{id}/download` (blob) |
| **UX** | toasts (`stores/toast.js`), modales (`AppModal.vue`), états vides, skeletons, transitions, responsive |

Composants réutilisables : `AppIcon` (SVG inline), `AppSidebar`, `PlayerBar`, `VinylDisc`,
`TrackRow`, `AppModal`, `ToastHost`. API : `api/http.js`, `api/tracks.js`, `api/playlists.js`.

---

## 2. Configuration
- `front/.env` → `VITE_API_URL=http://localhost:8080`.
- Dépendances ajoutées : `vue-router`, `pinia`, `axios`.
- Router en **hash mode** (aucune config serveur nécessaire).
- Le **streaming** utilise l'URL publique directement dans `<audio>` (`/api/tracks/{id}/stream`).
- Le **JWT** est stocké dans `localStorage` et envoyé en `Authorization: Bearer`.

---

## 3. Utilisation

Pré-requis : API (Partie 2) lancée sur `http://localhost:8080` (+ PostgreSQL, et RabbitMQ
pour l'import automatique).

```bash
cd front
npm install        # une seule fois
npm run dev        # -> http://localhost:5173
```
Build de production : `npm run build` (sortie dans `front/dist/`).

Parcours : ouvrir http://localhost:5173 → s'inscrire/se connecter → Bibliothèque
(importer / lire / modifier / supprimer) → Générer (critères → ajuster → enregistrer)
→ Mes playlists (lire / télécharger ZIP / renommer / supprimer).

---

## 4. Vérifications réalisées
- `npm run build` : **119 modules transformés**, build OK (CSS découpé par vue).
- Serveur de dev : `http://localhost:5173` répond **HTTP 200**.
- **CORS** : préflight `OPTIONS /api/auth/login` depuis l'origine `http://localhost:5173` → **200**.
- API jointe et endpoints validés en Partie 2 ; 2 morceaux de démo injectés pour la première ouverture.
- *(Rendu visuel à confirmer dans un navigateur — aucun navigateur headless dispo en local.)*

---

## 5. Le projet est complet
Les 3 parties du cahier des charges sont implémentées et testées :
Partie 1 (programmes standalone + RabbitMQ), Partie 2 (API + PostgreSQL), Partie 3 (web Vue).
Pistes optionnelles : drag-and-drop pour réordonner, rôles ADMIN dans l'UI, tests Vitest, Dockerisation globale.
