# Instructions du projet MiYoshiix

## Front : visuels

- **Aucun emoji**, nulle part : templates, styles, textes affichés, configuration
  des modules (`portal.modules[].icon`), données de test, messages.
- **Images, doodles, icônes et illustrations : c'est Adrien qui les fournit**
  (SVG, WebP, GIF ou PNG). Ne pas en créer, ne pas en dessiner, ne pas les
  remplacer par des emojis, des caractères Unicode décoratifs ou des icônes
  tirées d'une bibliothèque.
- **Exception : les Uicons de Flaticon qu'Adrien désigne** par leur classe
  (`fi fi-tr-caret-circle-right`). Extraire le glyphe de la police du paquet npm
  `@flaticon/flaticon-uicons` en un SVG autonome (`viewBox="0 0 24 24"`, sans
  couleur) dans `frontend/public/icons/` (ou `icons/modules/` pour une icône de
  module), nommé d'après la classe sans préfixe (`caret-circle-right.svg`,
  `knitting.svg`). `fi-tr-` = thin rounded, `fi-ts-` = thin straight : prendre
  la police du style indiqué. Pas de police d'icônes, pas de CDN, pas de
  dépendance ajoutée. L'afficher par masque CSS (`mask: url(...)` +
  `background: currentColor`) pour qu'il suive le thème. Garder le crédit
  « Uicons par Flaticon » du pied de page : la licence gratuite l'exige.
- Quand un écran a besoin d'un visuel, prévoir l'emplacement (composant, zone ou
  champ qui l'accueillera) et le signaler à Adrien plutôt que de le remplir.
- **Emplacement des fichiers** : `frontend/public/icons/` pour les icônes
  d'interface, `frontend/public/icons/modules/` pour les icônes de modules,
  `frontend/public/illustrations/` pour les illustrations. Garder les noms fournis
  par Adrien et les référencer à la casse près (`Logo.svg` ≠ `logo.svg` sur le
  serveur). `portal.modules[].icon` donne le nom du fichier avec son extension
  (`crochedex.svg`) : minuscules, chiffres, tirets, extension png, webp, gif ou svg.
  L'icône s'affiche dans un cadre carré cerné d'un filet (vide sans icône) ;
  une icône `.svg` y est affichée en masque (couleur du texte).
- **Image porteuse de sens** (icône seule dans un bouton, par exemple) : un `alt`
  qui décrit l'action ou la fonction. Image décorative : `alt=""`.
- **Pas de pixel art** : retiré du site, le style des visuels est à redéfinir.
  En attendant, les emplacements restent vides (tuile sans icône, bouton de thème
  en texte).
- **Animations** (WebP ou GIF animé, animations CSS) : toutes obéissent à la case
  « Désactiver les animations » du pied de page (`app-site-footer`, `MotionService`,
  attribut `data-motion` sur `<html>`). Pas de bouton pause par image : Adrien n'en
  veut pas. Pour une image animée, utiliser `app-animated-illustration` avec une
  image fixe `<nom>-fixe.png` (première image, extraite sans perte).

## Front : style moderne / cyberpunk

Ambiance : l'intranet d'une grande corporation dans un futur proche. Froid,
précis, maîtrisé, cher. Le futur se lit dans la structure (grille, filets fins,
coins coupés, données étiquetées), jamais dans les effets. Références : les menus
d'Arasaka dans Cyberpunk 2077, Sarif Industries dans Deus Ex: Human Revolution,
les interfaces de Blade Runner 2049. **En cas de doute, choisir la version la plus
sobre.**

- **Interdits** : néon et halo (`box-shadow` ou `text-shadow` lumineux), glitch,
  décalage RGB, scanlines, bruit, texte qui clignote ou s'écrit lettre par lettre,
  texte « hacker » qui défile, dégradés, flou (`backdrop-filter`), `border-radius`,
  ombres portées.
- **Panneaux** : composant `app-panel` (en-tête = vrai titre `h1`/`h2`, étiquette
  codifiée facultative).
- **Structure** : grille stricte, alignements nets, espaces généreux. Panneaux à
  fond plein, délimités par des filets de 1 px. La hiérarchie passe par l'espace,
  la taille et la graisse du texte, pas par la décoration.
- **Coins coupés** : un seul angle chanfreiné (`--chamfer`, 8 px) par panneau ou
  bouton principal. `clip-path` rogne l'`outline` de l'élément : le fond chanfreiné
  se dessine sur un `::before` (`z-index: -1`, `isolation: isolate` sur l'élément),
  jamais sur l'élément focalisable lui-même.
- **Accent** : le vert (`--color-primary`) est réservé à l'action principale, à
  l'état actif et au focus. Une seule action principale par écran. Tout le reste en tons de fond.
- **Typographie** : une sans-serif géométrique pour le texte, une monospace pour
  les données (identifiants, dates, valeurs, codes). Les étiquettes courtes
  peuvent être en majuscules espacées (`letter-spacing: 0.08em`), jamais une
  phrase entière. Base 1rem. Pour l'instant, polices du système (`system-ui`,
  `ui-monospace`) ; les fichiers de police viendront d'Adrien, auto-hébergés dans
  `frontend/public/fonts/`. Pas de CDN.
- **Détails** (avec parcimonie, deux ou trois par écran au plus) : étiquette de
  section codifiée (`MOD-01`), repère d'angle, numéro de référence. Ces détails
  sont décoratifs : `aria-hidden="true"`.
- **Mouvement** : transitions courtes (150 à 200 ms, `ease-out`), opacité ou
  translation de quelques pixels. Pas de rebond, pas de boucle, pas d'animation
  au chargement. Soumis à `data-motion`.
- **Classes globales** : `.btn`, `.btn-primary`, `.field`, `.alert`.
- **Focus** : contour plein de 2 px en `--color-focus`, décalé de 2 px
  (`outline-offset`), sur tout élément interactif.
- Pas d'élément factice : faux voyants, fausses jauges, fausses données qui
  défilent, faux contrôles qui ne font rien.


## Front : couleurs

Palette choisie par Adrien, définie dans `frontend/src/styles.scss` :

| Nom | Hex | Groupe |
|---|---|---|
| Emerald | `#60d394` | claire, **couleur principale** |
| Noir | `#0c0f0e` | foncée |
| Blanc | `#f3f6f4` | claire |

- **Aucune autre couleur.** Pas de gris pur, de bleu ni de rouge. Seule
  exception : les nuances (gris de fond et de filet, vert foncé du thème clair),
  obtenues avec `color-mix()` entre deux couleurs de la palette.
- **Les composants utilisent les rôles** (`--color-background`, `--color-surface`,
  `--color-field`, `--color-text`, `--color-text-muted`, `--color-line`,
  `--color-line-strong`, `--color-primary`, `--color-selection`, `--color-focus`…),
  jamais les couleurs brutes. Les rôles sont définis pour le thème clair et le
  thème sombre.
- **Filets** : `--color-line` pour les séparations décoratives (contraste faible,
  volontaire) ; `--color-line-strong` (≥ 3:1) pour tout contour qui délimite un
  contrôle (bouton, champ, tuile cliquable).
- **Le vert en thème clair** : Emerald n'a que 1.7:1 sur Blanc. En clair, l'action
  principale, la sélection et le focus utilisent un vert foncé (40 % Emerald +
  Noir, 7:1 sur Blanc) ; Emerald pur ne s'y emploie jamais seul.
- **Le thème se choisit avec le bouton `app-theme-toggle`** et s'applique par
  l'attribut `data-theme` de `<html>`. Le réglage du système ne sert que tant
  qu'aucun choix n'est mémorisé. Ne pas styler avec `@media (prefers-color-scheme)`
  dans les composants : les rôles suffisent.
- **Texte : toujours une couleur foncée sur une claire, ou l'inverse.** Jamais
  deux couleurs du même groupe l'une sur l'autre (Emerald sur Blanc : 1.7).
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
