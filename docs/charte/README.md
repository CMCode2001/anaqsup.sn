# Charte : logotype et couleurs

## Logotype de l'ANAQ-Sup

Les règles sont celles de la charte officielle :

1. Le logotype **ne peut en aucun cas être modifié**.
2. Il est **utilisé tel quel** sur tous les supports de communication.
3. Il est **uniquement utilisé sur fond blanc** et **n'a pas de variantes**.

| Fichier | Usage |
|---|---|
| [`logo-anaqsup-original.png`](logo-anaqsup-original.png) | Fichier officiel, conservé à l'identique (801 × 824, fond blanc). SHA-256 `e2591d7d…5876b7`. |
| `frontend/apps/*/public/brand/logo-anaqsup.png` | Même fichier, **marges blanches recadrées** pour l'en-tête (689 × 294). Dessin, couleurs et proportions inchangés. |

Dans le code, le logotype passe **toujours** par le composant `BrandLogo` (`@anaqsup/ui/components/brand-logo`). Le composant impose :

- le fichier officiel, sans filtre ni recoloration ;
- le ratio d'origine ;
- un fond blanc, y compris sur une surface colorée.

Il est donc interdit :

- de le redessiner en SVG ;
- de le placer sur un fond bleu ou sombre (pied de page, barre latérale du back-office) ;
- d'en créer une version monochrome ou « mode sombre ».

> Un fichier vectoriel officiel (SVG, PDF ou AI) serait préférable pour la netteté sur les écrans haute densité. Il faut le demander au service communication, puis le substituer sans retouche.

## Couleurs (validées le 13/10/2026) : uniquement celles du logotype

| Rôle | Couleur | Contraste | Usage |
|---|---|---|---|
| Couleur principale | Or du logotype `#C7B62E` | texte anthracite dessus : 7,3:1 | Boutons, filtres actifs, pastilles |
| Titres et liens | Or assombri `#6E6414` (même teinte) | 6,0:1 sur blanc | L'or pur est illisible en texte sur blanc (2,07:1) |
| Fonds sombres | Anthracite neutre `#24272B` | blanc dessus : 15:1 | Bandeau supérieur, pied de page |
| Accents | Vert `#00853D`, rouge `#E21B22`, jaune `#FDEE41` (drapeau) | — | Surtitres et avis favorable ou défavorable ; jaune réservé au drapeau |

**Bande drapeau** : `frontend/apps/portail/public/brand/drapeau.svg` (vert `#109939`, jaune `#FFE500` avec l'étoile, rouge `#DE2627`), 6 px, en haut du pied de page. Le composant `FlagBand` la reproduit en SVG inline : les trois bandes couvrent toute la largeur et l'étoile reste centrée, sans déformation.

**Police** : Poppins, pour les titres et le texte, sur tout le site et le back-office.

**Formes** : boutons, champs, badges et filtres en pilule (`rounded-full`). Cartes très arrondies (`rounded-3xl`) avec un léger zoom au survol.
