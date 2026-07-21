# SOA-503 — Conversion XLSX des erreurs Sudoc

## Objectif

Remplacer, pour les nouveaux traitements, les rapports texte cumulés
`ErreursInsertion469.txt` et `ErreursCreations.txt` par des classeurs XLSX
directement exploitables dans Excel, LibreOffice Calc et Google Sheets.

Les fichiers TXT historiques ne sont ni migrés, ni modifiés, ni supprimés.

## Périmètre

Le changement concerne uniquement `kafka2sudoc` :

- génération de `ErreursInsertion469.xlsx` ;
- génération de `ErreursCreations.xlsx` ;
- adaptation des emails existants pour joindre les fichiers XLSX ;
- ajout d'Apache POI ;
- tests unitaires de génération, d'ajout cumulatif et d'intégration email.

Le mode d'envoi reste une pièce jointe. Cette correction ne transforme pas les
pièces jointes en liens HTTP.

## Architecture

Un nouveau service `ErrorWorkbookService` porte la responsabilité de produire
et de mettre à jour les classeurs. Il reçoit les erreurs déjà filtrées, le nom
du fichier KBART et les lignes KBART disponibles dans le `WorkInProgress`.

`EmailService` conserve les responsabilités suivantes :

- filtrer les erreurs selon leur type ;
- demander au service XLSX d'ajouter les lignes correspondantes ;
- joindre le classeur produit à l'email.

Le service XLSX assure :

- l'extraction du PPN et de la nature de l'erreur ;
- la recherche de la ligne KBART associée ;
- la construction des sept colonnes ;
- la création et l'ajout cumulatif dans le classeur ;
- la mise en forme ;
- l'écriture sûre du fichier.

Cette séparation évite d'ajouter la manipulation Apache POI au service d'email
et permet de tester la génération indépendamment de l'envoi.

## Structure des classeurs

Chaque classeur contient une feuille portant le même nom que le fichier, sans
l'extension :

- `ErreursInsertion469` ;
- `ErreursCreations`.

Les colonnes sont, dans cet ordre :

| Colonne | En-tête | Contenu |
|---|---|---|
| A | PPN | PPN extrait du message d'erreur |
| B | Commande WinIBW | `che ppn <PPN>` si le PPN existe |
| C | Bouquet | Nom issu du fichier KBART |
| D | Erreur | Nature ou message de l'erreur |
| E | Titre | Titre de publication KBART |
| F | ISSN imprimé | Identifiant imprimé KBART |
| G | ISSN en ligne | Identifiant en ligne KBART |

Toutes les cellules de données sont écrites comme chaînes et reçoivent le
format Excel explicite `Texte` (`@`). Cela évite la conversion des identifiants
longs en notation scientifique.

L'en-tête est affiché en gras, en blanc sur fond bleu foncé. La première ligne
est figée, un filtre automatique couvre toutes les colonnes et des largeurs
bornées rendent les valeurs lisibles.

## Association entre erreur et ligne KBART

Pour `LigneKbartConnect`, la recherche utilise le PPN de l'erreur et le champ
`BEST_PPN`.

Pour `LigneKbartImprime`, la recherche utilise le PPN de l'erreur et le champ
`ppn`.

Quand une ligne est trouvée, les colonnes Titre, ISSN imprimé et ISSN en ligne
proviennent de cette ligne.

Pour les erreurs globales sans PPN, notamment les erreurs de connexion ou de
date, les colonnes PPN, Commande WinIBW, Titre, ISSN imprimé et ISSN en ligne
restent vides. Les colonnes Bouquet et Erreur restent renseignées.

Si un PPN est présent mais qu'aucune ligne KBART ne correspond, le PPN, la
commande, le bouquet et l'erreur sont conservés ; les trois colonnes KBART
restent vides.

## Nom du bouquet

Le bouquet est construit avec les fonctions existantes de `CheckFiles`, à
partir du fournisseur, du package et de la date du fichier.

Le comportement historique du suffixe `_FORCE` est conservé. Aucun changement
fonctionnel supplémentaire sur le nom du bouquet n'est introduit dans cette
partie.

## Écriture cumulative et gestion des erreurs

Les appels d'ajout sont synchronisés dans l'instance du service.

Pour chaque ajout :

1. le classeur existant est ouvert, ou un nouveau classeur est créé ;
2. les nouvelles lignes sont ajoutées après la dernière ligne ;
3. le filtre est ajusté à la totalité des lignes ;
4. le classeur est écrit dans un fichier temporaire voisin ;
5. le fichier temporaire remplace le classeur avec un déplacement atomique
   lorsque le système de fichiers le permet ;
6. un remplacement standard est utilisé si le déplacement atomique n'est pas
   pris en charge.

Une liste d'erreurs vide ne crée pas de classeur. Si la génération échoue,
l'ancien classeur reste inchangé et l'exception remonte à l'appelant selon le
contrat actuel en `IOException`.

## Emails

Les trois flux existants sont conservés :

- erreurs de liens 469 ;
- erreurs de création ex nihilo ;
- erreurs de création par dérivation depuis l'imprimé.

Les deux flux de création alimentent le même fichier
`ErreursCreations.xlsx`.

L'email est envoyé uniquement si le classeur attendu existe. La pièce jointe
porte désormais l'extension `.xlsx`. Le sujet et le texte fonctionnels des
emails restent inchangés, hormis la référence implicite au nouveau nom de
fichier.

## Tests

Les tests doivent couvrir :

- la création de chaque classeur avec les sept en-têtes ;
- la valeur et le type texte de chaque colonne ;
- l'association d'une erreur avec une `LigneKbartConnect` ;
- l'association d'une erreur avec une `LigneKbartImprime` ;
- les cellules vides pour une erreur globale ou une ligne KBART absente ;
- l'ajout cumulatif sans écraser les lignes existantes ;
- la conservation des anciens fichiers TXT ;
- la mise en forme, le gel de la première ligne et le filtre ;
- l'absence de classeur pour une liste vide ;
- l'utilisation des noms `.xlsx` dans les trois flux d'email.

La suite de référence contient 59 tests et doit rester entièrement verte.

## Hors périmètre

- migration du contenu des fichiers TXT historiques ;
- suppression automatique des fichiers TXT ;
- stockage cloud ;
- déploiement diplo2 ;
- modification des sujets d'email ;
- ajout du contenu des notices Sudoc dans les classeurs ;
- déduplication des lignes cumulées.
