# Instructions du projet MiYoshiix

## Front : visuels

- **Aucun emoji**, nulle part : templates, styles, textes affichés, configuration
  des modules (`portal.modules[].icon`), données de test, messages.
- **Images, doodles, icônes et illustrations : c'est Adrien qui les fournit**
  (SVG, WebP, GIF ou PNG). Ne pas en créer, ne pas en dessiner, ne pas les
  remplacer par des emojis, des caractères Unicode décoratifs ou des icônes
  tirées d'une bibliothèque.
- Quand un écran a besoin d'un visuel, prévoir l'emplacement (composant, zone ou
  champ qui l'accueillera) et le signaler à Adrien plutôt que de le remplir.
- **Emplacement des fichiers** : `frontend/public/icons/` pour les icônes
  d'interface, `frontend/public/icons/modules/` pour les icônes de modules,
  `frontend/public/illustrations/` pour les illustrations. Garder les noms fournis
  par Adrien et les référencer à la casse près (`SUN.ico` ≠ `sun.ico` sur le
  serveur). Les noms référencés par `portal.modules[].icon` restent en minuscules,
  chiffres et tirets.
- **Image porteuse de sens** (icône seule dans un bouton, par exemple) : un `alt`
  qui décrit l'action ou la fonction. Image décorative : `alt=""`.
- **Pixel art** : afficher à un multiple entier de la grille du dessin, avec
  `image-rendering: pixelated`. Mesurer la grille avant de choisir la taille.
- **Animations** (WebP ou GIF animé, animations CSS) : toutes obéissent à la case
  « Désactiver les animations » du pied de page (`app-site-footer`, `MotionService`,
  attribut `data-motion` sur `<html>`). Pas de bouton pause par image : Adrien n'en
  veut pas. Pour une image animée, utiliser `app-animated-illustration` avec une
  image fixe `<nom>-fixe.png` (première image, extraite sans perte).

## Front : style rétro

Inspiration Windows 95 : bureau, fenêtres en relief, barres de titre, boutons
biseautés, zones enfoncées.

- **Fenêtres** : composant `app-retro-window` (barre de titre = vrai titre `h1`/`h2`).
- **Reliefs** : variables `--raised`, `--raised-strong`, `--sunken`. Pas de
  `border-radius`, pas d'ombre floue, pas de dégradé.
- **Classes globales** : `.btn`, `.btn-primary`, `.field` (zone enfoncée), `.alert`.
- **Focus** : rectangle pointillé (`outline: 2px dotted`), à conserver sur tout
  élément interactif.
- **Texte à taille lisible** : base 1rem. Ne pas reproduire les polices minuscules
  de l'époque.
- Pas de faux contrôles de fenêtre (réduire, fermer) qui ne font rien.

## Front : couleurs

Palette choisie par Adrien, définie dans `frontend/src/styles.scss` :

| Nom | Hex | Groupe |
|---|---|---|
| Space Indigo | `#2d3047` | foncée |
| Deep Mocha | `#322923` | foncée |
| Royal Plum | `#720058` | foncée |
| Emerald | `#60d394` | claire, **couleur principale** |
| Periwinkle | `#c9ddff` | claire |

- **Aucune autre couleur.** Pas de blanc, de noir, de gris ni de rouge. Seule
  exception : les nuances de relief et de face de fenêtre, obtenues avec
  `color-mix()` entre deux couleurs de la palette.
- **Les composants utilisent les rôles** (`--color-desktop`, `--color-surface`,
  `--color-field`, `--color-text`, `--color-titlebar`, `--color-primary`,
  `--color-selection`, `--color-focus`, `--bevel-*`…), jamais les couleurs brutes.
  Les rôles sont définis pour le thème clair et le thème sombre.
- **Le thème se choisit avec le bouton `app-theme-toggle`** et s'applique par
  l'attribut `data-theme` de `<html>`. Le réglage du système ne sert que tant
  qu'aucun choix n'est mémorisé. Ne pas styler avec `@media (prefers-color-scheme)`
  dans les composants : les rôles suffisent.
- **Texte : toujours une couleur foncée sur une claire, ou l'inverse.** Jamais
  deux couleurs du même groupe l'une sur l'autre (1.1 à 1.4 de contraste).
  Calculer le contraste de toute nouvelle association avant de l'utiliser.
- Un état (erreur, succès, sélection) ne passe jamais par la couleur seule :
  texte explicite, cadre ou soulignement.

## Front : accessibilité (RGAA)

- Contraste ≥ 4.5:1 pour le texte, ≥ 3:1 pour les contours de focus et les
  composants d'interface.
- Chaque page : un seul `h1`, un `title` de route explicite (« Page · MiYoshiix »),
  un lien d'évitement `app-skip-link` et un `<main id="contenu" tabindex="-1">`.
- Cibles cliquables d'au moins 44 px de haut (`.btn` le garantit).
- Aucun défilement horizontal à 320 px, avec le texte agrandi à 200 % ou
  l'espacement du texte forcé.
- Messages dynamiques : `role="status"` (information) ou `role="alert"` (erreur).
- Chaque page affiche `app-site-footer` (réglage global des animations, RGAA 13.8).
- Images décoratives : `alt=""` ou `aria-hidden="true"`.
