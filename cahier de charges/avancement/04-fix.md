# Avancement — Partie 4 : Corrections & ajouts

**Statut : TERMINÉ.** Compilation backend OK.
Date : 2026-06-30 · Stack inchangée (Spring Boot 4.1 + RabbitMQ côté back, Vue 3 côté front).

Ce document couvre deux évolutions postérieures à la livraison initiale :

1. la **génération de playlist 100 % côté client** (nouveau JS dans `GenerateView.vue`) ;
2. la **liste noire d'import** par artiste (`blacklist/artiste.txt`) et par genre
   (`blacklist/genre.txt`).

---

## 1. Génération de playlist côté client (`front/src/views/GenerateView.vue`)

### 1.1 Le principe

La génération **ne passe plus par le backend** : toute la bibliothèque est déjà chargée
en mémoire (`tracksApi.list()` au montage), et les contraintes sont appliquées en JS
dans `buildPlaylist()`. Avantage : réponse instantanée, ajustement libre, et aucune
route serveur dédiée à maintenir.

### 1.2 Les critères

Trois familles de filtres, chacune avec **Inclure** / **Exclure**, plus une option
**« uniquement »** sur les artistes et les albums :

| Critère | Effet |
|---|---|
| `includeGenres` | restreint le pool aux genres listés |
| `excludeGenres` / `excludeArtists` / `excludeAlbums` | exclusions **absolues** (jamais retenus) |
| `includeArtists` / `includeAlbums` | **graines** : prioritaires mais pas exclusives |
| `onlyArtists` | restreint le pool à ces artistes **uniquement** |
| `onlyAlbums` | l'album entier devient **obligatoire** |
| `minMinutes` / `maxMinutes` | fourchette de durée cible (sliders + présets) |

Les sliders min/max sont contraints l'un par l'autre (`watch`) : le minimum ne peut
pas dépasser le maximum.

### 1.3 L'algorithme (`buildPlaylist`)

1. **Pool autorisé** — on filtre la bibliothèque : durée > 0, on retire les exclusions
   (artiste/album/genre), on applique le genre inclus et la restriction
   « artiste uniquement ».
2. **Albums obligatoires** — si un album est marqué « uniquement », **tous** ses
   morceaux sont ajoutés d'office. Si c'est le seul critère actif, on s'arrête là
   (`stopAfterMandatory`).
3. **Graines puis reste** — les morceaux des artistes/albums « inclus » (mais non
   « uniquement ») passent en tête ; le reste du pool suit. Chaque sous-liste est
   **mélangée** (Fisher–Yates) pour varier les playlists d'une fois à l'autre.
4. **Remplissage glouton** — on ajoute des morceaux sans dépasser `maxSec`.
5. **Comblement du minimum** — si on est sous `minSec`, on ajoute le morceau dont la
   durée comble le mieux l'écart (léger dépassement toléré).

Comparaisons **insensibles à la casse** via `lowerSet()` / `inSet()` ; déduplication
par `id` (`used`).

### 1.4 UX

- **Jauge** « obtenu vs cible » (`matchPct`) ; indicateur `belowTarget` quand les
  contraintes priment sur la durée (playlist plus courte que demandée — c'est assumé).
- Après génération : **Ajouter** / **retirer** librement, **Lire** tout, puis
  **Enregistrer** (`POST /api/playlists`).

---

## 2. Liste noire d'import (artistes & genres)

### 2.1 Objectif

Empêcher l'import des morceaux de certains **artistes** ou de certains **genres**.
Les fichiers concernés ne sont **pas** envoyés à l'API : ils sont **mis de côté** dans
`blacklisted/`, dans un **sous-dossier** selon la cause (`artistes/` ou `genres/`).

### 2.2 Les fichiers `back/blacklist/`

À côté de `back/incoming/`, le dossier `blacklist/` contient **deux** listes — un
**motif par ligne**, même logique pour les deux :

| Fichier | Bloque selon le champ | Exemple |
|---|---|---|
| `blacklist/artiste.txt` | **artiste** | `skaiz` → `Skaiz Official` |
| `blacklist/genre.txt` | **genre** | `rock` → `Pop-Rock` |

- Comparaison **insensible à la casse** et par **sous-chaîne**.
- Lignes **vides** et lignes commençant par `#` (commentaires) **ignorées**.
- Chaque fichier est **rechargé automatiquement** s'il a été modifié (basé sur la date
  de dernière modification) — pas besoin de redémarrer le programme pour ajouter une entrée.

Exemple `artiste.txt` :
```
skaiz
mahaleo
```

### 2.3 Où la règle s'applique — l'Extracteur (Programme 2)

Artiste et genre ne sont connus **qu'après extraction** des métadonnées. La règle est
donc appliquée dans l'**Extracteur** (`MetadataExtractor`), juste avant la publication
vers `queue.metadata`. L'**artiste est testé en premier**, puis le genre :

```
[Scanner] ─► queue.scan ─► [Extracteur] ─► artiste ou genre blacklisté ?
                                              ├─ artiste ─► blacklisted/artistes/  (stop)
                                              ├─ genre   ─► blacklisted/genres/    (stop)
                                              └─ non     ─► queue.metadata ─► [Uploader] ─► API
```

Conséquence : un morceau blacklisté **ne génère aucun message** pour l'Uploader, n'est
jamais envoyé à l'API et n'apparaît pas dans la bibliothèque.

### 2.4 Fichiers / réglages

| Élément | Détail |
|---|---|
| `back/blacklist/artiste.txt` | liste des artistes bloqués |
| `back/blacklist/genre.txt` | liste des genres bloqués |
| `extractor/BlacklistFilter.java` | charge les 2 listes, expose `check(artist, genre)` → `Reason` (ARTIST / GENRE / null), recharge à chaud |
| `extractor/MetadataExtractor.java` | teste le morceau ; si bloqué → `moveToBlacklisted(file, sous-dossier)` |
| `back/blacklisted/artistes/`, `back/blacklisted/genres/` | dossiers de destination (créés automatiquement au besoin) |

Propriétés (`application.properties`, surchargables en CLI) :
```properties
app.blacklist-artist-file=blacklist/artiste.txt
app.blacklist-genre-file=blacklist/genre.txt
app.blacklisted-dir=blacklisted
```

### 2.5 Logs (profil `extractor`)

- Au démarrage / rechargement : `Liste noire (artistes) chargee : N entree(s) depuis …`
  (idem pour `genres`).
- Sur un morceau bloqué :
  `Sur liste noire (artistes) : Skaiz Official (fichier.mp3) -> non importe`
  puis `Fichier blackliste deplace vers : …/blacklisted/artistes/fichier.mp3`.

### 2.6 Exemple sur la bibliothèque du sujet

La comparaison porte sur le **champ artiste** (et non le titre). Avec `skaiz` + `mahaleo`,
après un scan sont mis de côté (non importés) **5 morceaux** :

- artiste **Skaiz Official** → 3 morceaux ;
- artiste **Mahaleo Officiel** → 2 morceaux (*HIARAKA ISIKA…*, *VOLOLONA…*).

⚠️ Les morceaux qui ont « MAHALEO » dans le **titre** mais un **autre artiste** sont
**importés normalement** — ce sont des reprises par d'autres artistes :
*Mianatra Misintaka - Mahaleo* (**Hira Gasy**), *SOMAMBISAMBY MAHALEO* (**RH**),
*Andro ririnina-MAHALEO* (**Ny Ando Fanantenana Ratsimikatry**),
*LENDREMA "MAHALEO"* (**Doda Randriantsoa**). Pour les bloquer aussi, il faudrait
ajouter ces artistes à la liste (ou étendre le filtre au titre — non demandé ici).
