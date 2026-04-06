# 🚗 Carnet Route

Simulateur de trajet avec calcul de coût carburant, modèle d'affluence et comparatif multi-motorisations.

## Stack technique

| Couche | Technologie |
|--------|-------------|
| **Backend** | Kotlin + Ktor (API REST) |
| **Frontend** | Angular 21 (standalone components) |
| **Autocomplétion** | api-adresse.data.gouv.fr (proxy via backend) |
| **Conteneurisation** | Docker + Docker Compose |
| **Orchestration** | Kubernetes + Helm |

## Fonctionnalités

- **Saisie d'adresse libre** — autocomplétion via l'API nationale des adresses françaises
- **6 motorisations** — SP95, SP98, Diesel, E85 (éthanol), GPL, Électrique
- **Prix actualisés** — avril 2026, modifiables manuellement
- **Affluence double mode** :
  - Manuel (curseur fluide → embouteillage)
  - Automatique par heure de départ avec heatmap hebdomadaire cliquable
- **Dashboard** — distance, durée, consommation, coût total, comparatif, jauges

## Lancement rapide (Docker Compose)

```bash
# Cloner et lancer
cd carnetroute
docker-compose up --build

# Accéder à l'application
open http://localhost:4200
```

Le frontend est servi sur le port **4200**, le backend API sur le port **8080**.

## Développement local

### Backend (Kotlin/Ktor)

```bash
cd backend

# Avec Gradle wrapper
./gradlew run

# L'API démarre sur http://localhost:8080
# Endpoints :
#   GET  /api/fuels          — Liste des carburants
#   POST /api/simulate       — Simulation de trajet
#   POST /api/heatmap        — Heatmap hebdomadaire
#   GET  /api/geocode?q=...  — Proxy autocomplétion adresses
#   GET  /api/health         — Health check
```

### Frontend (Angular)

```bash
cd frontend
npm install
npm start

# Angular dev server sur http://localhost:4200
# Les appels /api sont proxifiés vers localhost:8080 (proxy.conf.json)
```

## Déploiement Kubernetes

### Avec les manifestes bruts

```bash
# Créer le namespace et déployer
kubectl apply -f k8s/carnetroute.yaml

# Vérifier
kubectl get pods -n carnetroute
kubectl get svc -n carnetroute
```

### Avec Helm

```bash
# Installer
helm install carnetroute ./helm/carnetroute

# Mettre à jour
helm upgrade carnetroute ./helm/carnetroute

# Personnaliser
helm install carnetroute ./helm/carnetroute \
  --set replicaCount.backend=3 \
  --set ingress.host=carnetroute.mondomaine.fr
```

## Architecture

```
┌─────────────┐     ┌──────────────┐     ┌──────────────────────┐
│   Angular    │────▶│  Nginx       │────▶│  Ktor Backend        │
│   Frontend   │     │  (reverse    │     │                      │
│              │     │   proxy /api)│     │  ├─ SimulationService │
│  Components: │     └──────────────┘     │  ├─ GeocodingService  │
│  ├─ Address  │                          │  └─ Routes            │
│  ├─ Fuel     │                          │        │              │
│  ├─ Traffic  │                          │        ▼              │
│  └─ Dashboard│                          │  api-adresse.data.   │
└─────────────┘                           │  gouv.fr             │
                                          └──────────────────────┘
```

## Structure du projet

```
carnetroute/
├── backend/
│   ├── src/main/kotlin/com/carnetroute/
│   │   ├── Application.kt           # Point d'entrée Ktor
│   │   ├── models/Models.kt         # Data classes
│   │   ├── services/
│   │   │   ├── SimulationService.kt  # Calculs trajet/coût/trafic
│   │   │   └── GeocodingService.kt   # Proxy API adresses
│   │   └── routes/SimulationRoutes.kt
│   ├── build.gradle.kts
│   └── Dockerfile
├── frontend/
│   ├── src/app/
│   │   ├── components/
│   │   │   ├── address-input/        # Autocomplétion adresses
│   │   │   ├── fuel-selector/        # Sélection motorisation
│   │   │   ├── traffic/              # Affluence + heatmap
│   │   │   └── dashboard/            # Résultats + comparatif
│   │   ├── services/simulation.service.ts
│   │   ├── models/simulation.model.ts
│   │   └── app.component.ts
│   ├── angular.json
│   ├── nginx.conf
│   └── Dockerfile
├── k8s/
│   └── carnetroute.yaml         # Manifestes K8s
├── helm/carnetroute/
│   ├── Chart.yaml
│   ├── values.yaml
│   └── templates/
├── docker-compose.yml
└── README.md
```

## Prix carburant (avril 2026)

| Carburant | Prix par défaut | Consommation moy. |
|-----------|----------------|-------------------|
| SP95 (E10) | 1.85 €/L | 7.0 L/100km |
| SP98 (E5) | 1.96 €/L | 7.2 L/100km |
| Diesel (B7) | 2.19 €/L | 5.8 L/100km |
| Éthanol E85 | 0.73 €/L | 9.5 L/100km |
| GPL | 1.05 €/L | 9.8 L/100km |
| Électrique | 0.44 €/kWh | 17.0 kWh/100km |
