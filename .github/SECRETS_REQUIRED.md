# GitHub Secrets requis

## Pour le déploiement (CD workflow)

| Secret | Description | Exemple |
|--------|-------------|---------|
| `KUBECONFIG` | Contenu du kubeconfig encodé en base64 | `base64 ~/.kube/config` |
| `JWT_SECRET` | Clé secrète JWT (min 32 chars) | Générer avec `openssl rand -hex 32` |
| `DATABASE_PASSWORD` | Mot de passe PostgreSQL de production | Mot de passe fort |
| `GITLEAKS_LICENSE` | Licence Gitleaks (optionnel) | Clé de licence |

## Configuration

1. Aller dans Settings → Secrets and variables → Actions
2. Ajouter chaque secret avec sa valeur de production
3. Pour KUBECONFIG : `cat ~/.kube/config | base64 -w 0`

## Environnements GitHub

Créer un environnement "production" dans Settings → Environments avec :
- Reviewers obligatoires avant déploiement
- Branches autorisées : `main` uniquement
