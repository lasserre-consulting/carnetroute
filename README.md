# Carnet Route V2

Application de simulation et de planification de trajets routiers. La V2 est une refonte complète, orientée microservices, avec un backend Kotlin/Ktor, un frontend Angular, et une stack d'observabilité intégrée (Prometheus, Grafana, Loki).

---

## Stack technique

| Couche         | Technologie              | Version |
|----------------|--------------------------|---------|
| Backend        | Kotlin / Ktor            | 2.x     |
| Frontend       | Angular                  | 18+     |
| Base de données| PostgreSQL               | 16      |
| Cache          | Redis                    | 7       |
| Messaging      | NATS JetStream           | 2.10    |
| Métriques      | Prometheus               | latest  |
| Dashboards     | Grafana                  | latest  |
| Logs           | Loki                     | latest  |
| Runtime JVM    | Java                     | 21      |
| Conteneurisation| Docker / Compose        | 3.9     |

---

## Prérequis

- **Docker** >= 24.x et **Docker Compose** >= 2.x
- **Java 21** (JDK, pour le développement backend local)
- **Node.js 22** et **npm** (pour le développement frontend local)
- **Gradle** >= 8.x (ou utiliser le wrapper `./gradlew` inclus)

---

## Démarrage rapide

```bash
# Cloner le dépôt
git clone <url-du-repo> carnetroute-v2
cd carnetroute-v2

# Démarrer tous les services
docker compose up -d

# Suivre les logs
docker compose logs -f backend
```

> Les images backend et frontend sont construites automatiquement depuis les `Dockerfile` locaux.

---

## URLs de développement

| Service             | URL                                      |
|---------------------|------------------------------------------|
| Frontend (Angular)  | http://localhost:4200                    |
| Backend (API)       | http://localhost:8080/api                |
| Backend (Health)    | http://localhost:8080/api/health         |
| Backend (Metrics)   | http://localhost:8080/metrics            |
| Grafana             | http://localhost:3000 (admin / admin)    |
| Prometheus          | http://localhost:9090                    |
| NATS Monitoring     | http://localhost:8222                    |
| Loki                | http://localhost:3100                    |

---

## Structure du projet

```
carnetroute-v2/
├── backend/                    # Backend Kotlin / Ktor
│   ├── src/
│   │   └── main/kotlin/
│   ├── build.gradle.kts
│   └── Dockerfile
├── frontend/                   # Frontend Angular
│   ├── src/
│   ├── package.json
│   └── Dockerfile
├── monitoring/                 # Observabilité
│   ├── prometheus.yml          # Configuration scrape
│   ├── alerts.yml              # Règles d'alertes
│   └── grafana/
│       ├── provisioning/
│       │   ├── datasources/    # Prometheus + Loki
│       │   └── dashboards/     # Provider de dashboards
│       └── dashboards/
│           └── carnetroute.json
├── docker-compose.yml          # Stack développement
├── docker-compose.prod.yml     # Override production
├── .dockerignore
├── README.md
└── ARCHITECTURE.md
```

---

## Développement local

### Backend (Kotlin / Ktor)

```bash
cd backend

# Démarrer uniquement les services d'infrastructure
docker compose up -d postgres redis nats

# Lancer le backend en hot-reload
./gradlew run --continuous

# Tests unitaires
./gradlew test

# Build de la fat-jar
./gradlew shadowJar
```

### Frontend (Angular)

```bash
cd frontend

# Installer les dépendances
npm install

# Serveur de développement avec proxy vers le backend
npm start
# ou
npx ng serve --proxy-config proxy.conf.json

# Tests unitaires
npm test

# Build de production
npm run build
```

---

## Variables d'environnement

### Backend

| Variable            | Description                              | Défaut (dev)                                      |
|---------------------|------------------------------------------|---------------------------------------------------|
| `PORT`              | Port d'écoute HTTP                       | `8080`                                            |
| `APP_ENV`           | Environnement (`dev` / `production`)     | `dev`                                             |
| `DATABASE_URL`      | JDBC URL PostgreSQL                      | `jdbc:postgresql://postgres:5432/carnetroute`     |
| `DATABASE_USER`     | Utilisateur PostgreSQL                   | `carnetroute`                                     |
| `DATABASE_PASSWORD` | Mot de passe PostgreSQL                  | `carnetroute_dev`                                 |
| `REDIS_URL`         | URL Redis                                | `redis://redis:6379`                              |
| `NATS_URL`          | URL NATS                                 | `nats://nats:4222`                                |
| `JWT_SECRET`        | Secret JWT (min 32 chars)                | `dev-secret-change-in-production-min-32-chars`    |

### Production (docker-compose.prod.yml)

| Variable              | Description                              |
|-----------------------|------------------------------------------|
| `GITHUB_REPOSITORY`   | Organisation/repo pour les images GHCR   |
| `IMAGE_TAG`           | Tag d'image à déployer (défaut: `latest`)|
| `POSTGRES_PASSWORD`   | Mot de passe PostgreSQL de production    |
| `REDIS_PASSWORD`      | Mot de passe Redis de production         |

---

## Architecture

Voir [ARCHITECTURE.md](./ARCHITECTURE.md) pour le détail des choix techniques, des flux de données et des diagrammes de séquence.

---

## Commandes utiles

```bash
# Rebuild uniquement le backend
docker compose up -d --build backend

# Réinitialiser les volumes (perte de données locale)
docker compose down -v

# Déploiement en production
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Vérifier l'état des services
docker compose ps
```
