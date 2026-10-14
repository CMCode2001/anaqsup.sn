# ADR-0006 : Comptes simples gérés par le backend pour le back-office

- **Statut** : accepté et implémenté (décisions de l'ANAQ-Sup du 2026-10-13), remplace la proposition « Keycloak »
- **Date** : 2026-10-13

## Contexte

Les agents de l'ANAQ-Sup doivent se connecter au back-office pour publier les actualités, les événements, les publications, les pages et les avis. Une première version proposait Keycloak (OIDC, MFA). L'ANAQ-Sup a choisi des **comptes simples**.

## Décision

- Les comptes (identifiant et mot de passe) sont **gérés directement par le backend Spring Boot** : il n'y a pas de fournisseur d'identité séparé.
- Keycloak est retiré de la pile, de Nginx, des sauvegardes et de la supervision.
- En attendant la livraison des comptes, `/api/v1/admin/**` est **fermé** : toute requête reçoit 401.
- Le back-office est servi sur `admin.anaqsup.sn`, en même origine que l'API, avec un accès restreint par liste d'IP.

## Décisions de l'ANAQ-Sup (13/10/2026)

| Sujet | Décision |
|---|---|
| Rôles | **Administrateur** (tous les droits, non modifiable) et **Chargé de communication**. L'administrateur peut **créer d'autres rôles** en cochant leurs permissions. |
| Création des comptes | Par l'administrateur, avec un **mot de passe provisoire** à changer à la première connexion. |
| Mots de passe | **8 caractères minimum**, sans verrouillage de compte. |
| Adresse d'accès | **Non devinable** : le back-office n'existe que sous `https://admin.anaqsup.sn/<ADMIN_PATH>/` ; toute autre adresse répond 404. |

## Mise en œuvre (module `comptes`)

- **Données.** La migration V7 crée `admin_role` et `admin_account`. Permissions : `COMPTES`, `ACTUALITES`, `EVENEMENTS`, `DOCUMENTS`, `PAGES`, `AVIS`, plus `PARTENAIRES`, ajoutée par V8 et accordée à l'Administrateur.
- **Droits par écran.** Chaque route `/api/v1/admin/<rubrique>/**` est réservée à sa permission (`AdminSecurityConfig`). Le téléversement d'images est ouvert à toute personne connectée.
- **Connexion.** Elle se fait par e-mail et mot de passe, haché en bcrypt.
  - Session côté serveur, avec le cookie `ANAQSUP_SESSION` (HttpOnly, SameSite=Strict, Secure en production), perdu à la fermeture du navigateur.
  - Déconnexion après **3 heures d'inactivité** (décision ANAQ-Sup du 14/10/2026, au lieu de 30 minutes).
  - Sessions **enregistrées dans PostgreSQL** (Spring Session JDBC, tables `spring_session*`, migration V13) : un redémarrage ou une mise à jour du serveur ne déconnecte personne. Le hachage du mot de passe n'y est jamais enregistré.
  - Nouvel identifiant de session à chaque connexion.
  - Même message pour un e-mail inconnu, un mauvais mot de passe ou un compte désactivé.
- **CSRF.** Le jeton est stocké dans le cookie `XSRF-TOKEN` et renvoyé dans l'en-tête `X-XSRF-TOKEN`. Un nouveau jeton est émis à chaque connexion.
- **Droits relus en base à chaque requête.** Un compte désactivé est déconnecté immédiatement, et un changement de rôle s'applique sans reconnexion.
- **Règles de protection.**
  - Il reste toujours au moins un administrateur actif.
  - On ne peut ni se supprimer, ni se désactiver, ni changer son propre rôle.
  - Le rôle Administrateur n'est pas modifiable.
- **Mot de passe provisoire.** Tant qu'il n'est pas changé, seules les routes `/auth/*` répondent (403 `PASSWORD_CHANGE_REQUIRED`).
- **Premier administrateur.** Il est créé au démarrage à partir de `ADMIN_BOOTSTRAP_EMAIL` et `ADMIN_BOOTSTRAP_PASSWORD`, seulement si aucun compte n'existe.
- **Freinage des essais en série.** Sans verrouillage de compte, Nginx limite la connexion à 10 tentatives par minute et par IP.
- **Contrat.** Il est décrit dans `contracts/openapi/admin-api.v1.yaml`. Les types du back-office en sont générés.

## Conséquences

- La pile est plus simple à exploiter : un service de moins.
- La double authentification n'est pas prévue. Elle pourra être ajoutée plus tard si l'ANAQ-Sup le demande.
