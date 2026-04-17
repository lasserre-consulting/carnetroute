# Carnet Route V2 — Architecture & Plan de Refonte Complet

> Synthèse de 20 agents (10 audits + 10 experts technos)  
> Date : Avril 2026

---

## Table des matières

1. [Diagnostic de l'existant](#1-diagnostic-de-lexistant)
2. [Architecture cible](#2-architecture-cible)
3. [Stack technologique recommandée](#3-stack-technologique-recommandée)
4. [Bounded Contexts DDD](#4-bounded-contexts-ddd)
5. [Sécurité](#5-sécurité)
6. [Intégrations tierces](#6-intégrations-tierces)
7. [Frontend](#7-frontend)
8. [Messaging — Kafka vs NATS](#8-messaging--kafka-vs-nats)
9. [Observabilité & monitoring](#9-observabilité--monitoring)
10. [Mobile & PWA](#10-mobile--pwa)
11. [Infrastructure & déploiement](#11-infrastructure--déploiement)
12. [Roadmap en 3 phases](#12-roadmap-en-3-phases)
13. [Checklist qualité](#13-checklist-qualité)

---

## 1. Diagnostic de l'existant

### Points forts de V1

- Stack moderne cohérent : Kotlin/Ktor 3.4 + Angular 21 standalone
- Architecture hexagonale proprement structurée (domain / application / infrastructure)
- Kafka intégré pour les prix carburant temps réel
- Use cases bien isolés (`SimulationUseCases`, `GenerateHeatmapUseCase`)
- Infrastructure as Code : Docker Compose + Kubernetes + Helm
- Tests unitaires présents : Kotest + MockK

### Problèmes critiques identifiés

#### Sécurité (OWASP Top 10)

| Vulnérabilité | Sévérité | Description |
|---|---|---|
| Pas d'authentification | **CRITIQUE** | Toute l'API est publique, aucun concept d'utilisateur |
| Pas de rate limiting | **CRITIQUE** | Vulnérable aux attaques DoS / brute-force |
| CORS wildcard | **HAUTE** | `allowAnyHost = true` en production |
| Pas de HTTPS forcé | **HAUTE** | Pas de HSTS, redirection HTTP→HTTPS absente |
| Injection potentielle | **HAUTE** | Paramètres routage non validés côté backend |
| Secrets en dur | **HAUTE** | Clés Kafka dans `application.conf` non vaultées |
| Pas de CSP | **MOYENNE** | Headers Content-Security-Policy absents |
| Pas d'audit trail | **MOYENNE** | Aucun log des accès / actions sensibles |

#### Tests & qualité

- **Frontend : 0% de couverture de tests** (aucun spec existant)
- Backend : ~8,7% de couverture (uniquement `SimulationServiceTest`)
- Pas de tests d'intégration avec base réelle (Testcontainers)
- Pas de lint / SonarQube configuré

#### Architecture & performance

- **Pas de persistance** : tout en mémoire, stateless, aucune base de données
- **Pas de cache distribué** : prix Kafka stockés uniquement en mémoire locale
- **Kafka surdimensionné** : 768 Mo RAM pour des prix simulés toutes les 30s
- **Carte SVG custom trop limitée** : pas de zoom, pas d'interaction, pas de géocodage visuel
- **WebSocket sessions non distribuées** : empêche le scaling horizontal
- **Pas d'Angular OnPush** ni de Signals : re-renders inutiles
- **Pas de lazy loading** : tout le bundle chargé au démarrage
- **Memory leaks** : subscriptions RxJS non unsubscribed dans les composants
- **Pas de circuit breaker** : si OSRM/api-adresse tombent, le backend plante

#### Domaine métier manquant

- Pas de compte utilisateur ni de gestion de profil
- Pas d'historique des simulations
- Pas de profils de véhicules personnalisés
- Pas de prix carburant officiels (données statiques codées en dur)
- Pas de partage de trajet
- Pas de calcul CO2 comparatif

---

## 2. Architecture cible

### Choix : Monolithe modulaire évolutif

**Justification pour dev solo :**
- Déploiement unique simplifié
- Transactions ACID possibles entre domaines
- Découplage progressif vers microservices possible sans refonte

```
nouveau-carnetroute/
├── backend/
│   ├── domain/
│   │   ├── user/            # Utilisateurs et profils
│   │   ├── vehicle/         # Véhicules et profils carburant
│   │   ├── simulation/      # Moteur de simulation (core)
│   │   ├── fuel-price/      # Prix carburant temps réel
│   │   └── history/         # Historique et statistiques
│   ├── application/
│   │   ├── user-service/
│   │   ├── vehicle-service/
│   │   ├── simulation-service/
│   │   ├── fuel-price-service/
│   │   └── history-service/
│   └── infrastructure/
│       ├── persistence/     # PostgreSQL + jOOQ + Flyway
│       ├── cache/           # Redis
│       ├── messaging/       # Kafka (ou NATS JetStream)
│       ├── routing/         # TomTom + haversine fallback
│       ├── geocoding/       # IGN + Nominatim fallback
│       ├── fuel-api/        # SPSE + EssenceAndCo
│       ├── security/        # JWT + OAuth2
│       └── monitoring/      # Micrometer + OpenTelemetry
├── frontend/
│   ├── src/app/
│   │   ├── core/
│   │   │   ├── auth/        # Guards, interceptors JWT
│   │   │   └── services/
│   │   ├── features/
│   │   │   ├── auth/        # Login, Register
│   │   │   ├── vehicles/    # Gestion mes véhicules
│   │   │   ├── simulation/  # Simulateur (amélioré)
│   │   │   ├── history/     # Mes trajets
│   │   │   └── dashboard/   # Analytics
│   │   └── shared/
├── k8s/
├── helm/
├── .github/workflows/
└── docker-compose.yml
```

### Diagramme d'architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                        Angular 21 (PWA)                          │
│  features: auth / vehicles / simulation / history / dashboard    │
│  MapLibre GL JS │ Tailwind v4 │ Angular Signals │ TanStack Query │
└────────────────────────┬─────────────────────────────────────────┘
                         │ HTTPS / JWT
                         ▼
┌──────────────────────────────────────────────────────────────────┐
│                   Nginx Ingress (rate limiting + SSL)            │
└───────────────┬──────────────────────────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────────────┐
│                    Kotlin/Ktor Backend                           │
│                                                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐   │
│  │   User   │ │ Vehicle  │ │Simulation│ │   FuelPrice      │   │
│  │ Service  │ │ Service  │ │ Service  │ │   Service        │   │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────────┬─────────┘   │
│       │             │            │                 │             │
│  ┌────▼─────────────▼────────────▼─────────────────▼──────────┐ │
│  │              Domain Layer (Hexagonal Architecture)          │ │
│  └────┬──────────────────────────────────────────────┬────────┘ │
│       │                                               │          │
│  ┌────▼───────┐ ┌──────────┐ ┌────────┐ ┌───────────▼───────┐  │
│  │ PostgreSQL │ │  Redis   │ │ Kafka  │ │  TomTom / IGN     │  │
│  │ 16+PostGIS │ │    7     │ │ 3.9   │ │  SPSE / OSRM      │  │
│  └────────────┘ └──────────┘ └────────┘ └───────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────────┐
│           Observabilité (Prometheus + Grafana + Loki)            │
└──────────────────────────────────────────────────────────────────┘
```

---

## 3. Stack technologique recommandée

### Backend

| Couche | Technologie | Justification |
|--------|-------------|---------------|
| Framework | **Ktor 3.4+ (Netty)** | Coroutines natives, léger, performant |
| Sérialisation | **kotlinx-serialization** | JSON natif, zero-copy possible |
| Persistance | **PostgreSQL 16 + jOOQ** | Typage SQL à la compilation, migrations versionées |
| Migrations | **Flyway 10+** | SQL versionné, rollback atomique |
| Cache | **Redis 7** | TTL, pub/sub, sessions distribuées |
| Auth | **JWT (HS256/RS256)** | Stateless, refresh token via Redis |
| Validation | **Konform ou Valiktor** | Règles data classes Kotlin |
| Tests | **Kotest + MockK + Testcontainers** | PostgreSQL/Redis réels dans les tests |
| Monitoring | **Micrometer + Prometheus** | Métriques applicatives (latence, cache) |
| Tracing | **OpenTelemetry + Tempo** | Distributed tracing Kafka→DB→API |
| Logs | **Logback + Loki** | Logs centralisés, corrélation traces |
| Circuit breaker | **Resilience4j** | Protège les appels TomTom / SPSE |

### Frontend

| Couche | Technologie | Justification |
|--------|-------------|---------------|
| Framework | **Angular 21+ standalone** | Reactive, Signals natifs depuis v17 |
| Gestion état | **Angular Signals + TanStack Query** | Réactivité fine-grained, cache serveur |
| UI | **Tailwind CSS v4** | Accessible, performant, customizable |
| Cartes | **MapLibre GL JS** | Fork libre Mapbox, tuiles OSM, GeoJSON |
| HTTP | **HttpClient + Interceptors** | Auto-refresh JWT, retry logic |
| Tests | **Jest + Testing Library** | Composants isolés, snapshots |
| Build | **esbuild (Angular CLI 21)** | Bundles optimisés, splitting auto |
| PWA | **Service Worker + Workbox** | Offline mode, cache-first |
| Change detection | **OnPush + Signals** | Re-renders uniquement si nécessaire |

### Infrastructure

| Composant | Technologie | Justification |
|-----------|-------------|---------------|
| Conteneurisation | **Docker 25+** | Reproductibilité |
| Orchestration | **Kubernetes 1.30+** | Autoscaling, auto-healing |
| Charts | **Helm 3.14+** | Secrets chiffrés, templating réutilisable |
| Secrets | **Sealed Secrets (K8s)** | Chiffrement at-rest dans etcd |
| CI/CD | **GitHub Actions** | Matrix builds, cache layers Docker |
| Registry | **GitHub Container Registry** | Intégré GH Actions, gratuit pour public |
| CDN | **Cloudflare** | Cache assets, protection DDoS, SSL gratuit |
| Load balancer | **Nginx Ingress Controller** | Rate limiting, SSL termination |

---

## 4. Bounded Contexts DDD

### BC1 — UserManagement

```
Entity: User
  userId: UUID
  email: Email (value object, unicité)
  password: PasswordHash (bcrypt)
  profile: UserProfile { name, avatar, locale }
  vehicles: List<VehicleId>
  preferences: UserPreferences {
    defaultFuelType: FuelType
    defaultVehicle: VehicleId?
    alertsEnabled: Boolean
    theme: "light" | "dark"
  }
  status: "active" | "inactive" | "suspended"

Events:
  UserCreatedEvent, UserProfileUpdatedEvent, UserDeletedEvent

Ports:
  UserRepository: findByEmail, findById, save, delete
  PasswordEncoder: hash(raw), verify(raw, hash)
  EmailService: sendWelcome, sendPasswordReset
```

### BC2 — VehicleManagement

```
Entity: Vehicle (appartient à un User)
  vehicleId: UUID
  userId: UserId
  name: String ("Ma Peugeot 308")
  fuelProfile: FuelProfile {
    fuelType: FuelType
    consumption: Double (L/100km ou kWh/100km)
    motorization: String
  }
  tankCapacity: Double
  emissions: Double (gCO2/km)
  yearMake: Int
  isDefault: Boolean

Events:
  VehicleCreatedEvent, VehicleProfileUpdatedEvent, VehicleDeletedEvent

Ports:
  VehicleRepository: findByUserId, save, delete
  FuelProfileCache: getCurrent(fuelType) [Redis TTL 1h]
```

### BC3 — RouteSimulation (core)

```
Entity: Simulation (immutable, append-only)
  simulationId: UUID
  userId: UserId? (null = anonyme)
  vehicleId: VehicleId?
  route: Route {
    from: Coordinates
    to: Coordinates
    distanceKm: Double
    durationBaseMin: Double
    geometry: LineString (GeoJSON)
  }
  traffic: TrafficConditions {
    mode: "manual" | "auto"
    factor: Double (1.0–2.0)
    departureTime: Instant
  }
  costs: CostBreakdown {
    fuelConsumed: Double
    costTotal: Double
    costPer100km: Double
    comparison: Map<FuelType, Cost>
  }
  createdAt: Instant

Domain Policies:
  ConsumptionPolicy: calculateAdjustedConsumption(base, trafficFactor)
  TrafficPolicy: estimateFactor(day, hour)
  CostPolicy: calculateTotal(consumption, price, distance)

Events:
  SimulationCreatedEvent, SimulationSavedEvent

Ports:
  SimulationRepository: save, findById, findByUserId (paginé)
  RoutingService: getRoute(from, to) → Route
  TrafficService: estimateFactor(time) → Double
```

### BC4 — FuelPriceManagement

```
Entity: FuelPrice (time-series, versioned)
  fuelType: FuelType
  price: Money (€/L ou €/kWh)
  source: FuelPriceSource { provider, lastUpdate, confidence }
  historicalPrices: List<(Instant, Money)>
  priceChangePercent: Double
  trend: "↑" | "↓" | "→"

Events:
  FuelPriceUpdatedEvent, FuelPriceAlertEvent

Adapters:
  SPSEAdapter: fetchPrices() [API data.economie.gouv.fr]
  EssenceAndCoAdapter: fetchPrices()
  FuelPriceCache: [Redis TTL 5min]
  FuelPricePublisher: → Kafka + WebSocket
```

### BC5 — JourneyHistory

```
Entity: JourneyHistory (log append-only)
  journeyHistoryId: UUID
  userId: UserId
  simulationId: SimulationId
  executedAt: Instant
  status: "planned" | "completed" | "cancelled"
  actualCost: Money?
  carbonEmissionSaved: Double?
  tags: List<String>

Aggregate: UserStatistics
  totalJourneys, totalDistance, totalCost
  averageConsumption, carbonSaved
  monthlyStats: Map<YearMonth, Statistics>

Ports:
  JourneyHistoryRepository: save, findByUserId (paginé)
  UserStatisticsRepository: computeMonthly(userId)
```

---

## 5. Sécurité

### Correctifs prioritaires (à implémenter en Phase 1)

```kotlin
// 1. Authentification JWT
install(Authentication) {
    jwt("auth-jwt") {
        realm = "carnetroute"
        verifier(JWT.require(algorithm).withIssuer("carnetroute").build())
        validate { credential ->
            if (credential.payload.getClaim("userId").asString() != null)
                JWTPrincipal(credential.payload) else null
        }
    }
}

// 2. Rate limiting (Nginx Ingress)
// annotations:
//   nginx.ingress.kubernetes.io/limit-rps: "20"
//   nginx.ingress.kubernetes.io/limit-connections: "10"

// 3. CORS restrictif
install(CORS) {
    allowHost("carnetroute.fr", schemes = listOf("https"))
    allowMethod(HttpMethod.Get)
    allowMethod(HttpMethod.Post)
    allowHeader(HttpHeaders.Authorization)
    allowHeader(HttpHeaders.ContentType)
}

// 4. Headers de sécurité
install(DefaultHeaders) {
    header("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
    header("Content-Security-Policy", "default-src 'self'; script-src 'self'")
    header("X-Frame-Options", "DENY")
    header("X-Content-Type-Options", "nosniff")
}
```

### Checklist sécurité complète

- [ ] JWT + refresh token rotation (stocké Redis, révocable)
- [ ] Rate limiting : 50 req/min par IP (Nginx), 200 req/min par user authentifié
- [ ] CORS whitelist (jamais `allowAnyHost`)
- [ ] HSTS headers + redirection HTTP→HTTPS
- [ ] Content-Security-Policy strict
- [ ] SQL injection : requêtes paramétrées uniquement (jOOQ / Exposed)
- [ ] Secrets Vault (Sealed Secrets K8s, jamais en clair dans git)
- [ ] Gitleaks pre-commit hook
- [ ] Validation entrées à la frontière (Konform)
- [ ] Audit trail : toutes les actions sensibles loggées vers Loki

---

## 6. Intégrations tierces

### Prix carburant — SPSE (data.economie.gouv.fr)

```kotlin
class SPSEFuelPriceAdapter(
    private val httpClient: HttpClient,
    private val cache: RedisCache
) : FuelPricePort {
    // Dataset officiel : "Prix des carburants en France"
    // Refresh : quotidien via Kafka Producer @ 14h00 CEST
    override suspend fun fetchLatestPrices(): Map<FuelType, FuelPrice> {
        return cache.getOrSet("fuel_prices:spse", duration = 1.hour) {
            httpClient.get("https://prix-carburants.economie.gouv.fr/api/prices/latest")
                .body<Map<String, Double>>()
                .mapKeys { FuelType.from(it.key) }
                .mapValues { FuelPrice(it.value, source = "SPSE") }
        }
    }
}
```

**Fallback :** EssenceAndCo → prix Redis historiques + interpolation de tendance

### Routage

| Cas d'usage | Adapter primaire | Fallback |
|---|---|---|
| Itinéraire optimal | **TomTom Routing API** | OSRM public + haversine |
| Trafic temps réel | **TomTom Flow API** | Heatmap historique locale |
| Géocodage France | **IGN API Géoplateforme** | api-adresse.data.gouv.fr |
| Géocodage mondial | **Nominatim** | — |

**Cache strategy (Redis) :**
- Routes : LRU 24h (routes populaires)
- Trafic : TTL 5min (fraîcheur critique)
- Géocodage : TTL 7 jours (adresses quasi-statiques)

### Circuit breaker (Resilience4j)

```kotlin
@CircuitBreaker(name = "tomtom", fallbackMethod = "haversineFallback")
suspend fun getRoute(from: Coordinates, to: Coordinates): Route

@CircuitBreaker(name = "spse", fallbackMethod = "redisHistoricalPrices")
suspend fun fetchPrices(): Map<FuelType, FuelPrice>
```

---

## 7. Frontend

### Problèmes actuels à corriger

1. **OnPush manquant** sur tous les composants → re-renders excessifs
2. **Memory leaks** : subscriptions RxJS non unsubscribed → utiliser `takeUntilDestroyed()`
3. **Pas de lazy loading** → charger chaque feature module à la demande
4. **Carte SVG custom** trop limitée → migrer vers MapLibre GL JS
5. **0% coverage** → ajouter Jest + Testing Library dès Phase 1

### Migration vers MapLibre GL JS

```typescript
// map.component.ts (nouvelle version)
import maplibregl from 'maplibre-gl';

@Component({
  selector: 'app-map',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div #mapContainer style="width:100%;height:400px"></div>`
})
export class MapComponent implements AfterViewInit, OnDestroy {
  private map: maplibregl.Map;
  route = input<GeoJSON.LineString>();

  ngAfterViewInit() {
    this.map = new maplibregl.Map({
      container: this.mapContainer.nativeElement,
      style: 'https://tiles.openfreemap.org/styles/liberty',
      center: [2.35, 46.8],
      zoom: 5
    });
  }

  ngOnDestroy() { this.map.remove(); }
}
```

### Optimisations Angular

```typescript
// Signals pour état local (Angular 17+)
export class SimulationComponent {
  private simService = inject(SimulationService);

  // Signal-based state
  result = signal<SimulationResult | null>(null);
  loading = signal(false);

  async simulate(request: SimulationRequest) {
    this.loading.set(true);
    const res = await firstValueFrom(this.simService.simulate(request));
    this.result.set(res);
    this.loading.set(false);
  }
}

// takeUntilDestroyed pour cleanup auto
export class AlertComponent {
  private destroyRef = inject(DestroyRef);

  ngOnInit() {
    this.alertService.alerts$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(alert => this.alerts.push(alert));
  }
}
```

---

## 8. Messaging — Kafka vs NATS

### Comparatif

| Critère | Kafka (actuel) | NATS JetStream |
|---|---|---|
| RAM footprint | ~768 Mo (Kafka + Zookeeper) | ~64 Mo |
| Latence | ~5ms | ~0.5ms |
| Config | Complexe (topics, offsets, CG) | Simple (streams, consumers) |
| Rétention des messages | Oui | Oui |
| Exactly-once | Oui | Oui (v2.10+) |
| Cas d'usage adapté | Volume massif (millions/sec) | Alertes, prix carburant (faible volume) |

### Recommandation

- **Phase 1-2 :** Migrer vers **NATS JetStream** (8x moins de RAM, 10x moins de latence, configuration simplifiée)
- **Phase 3 :** Réévaluer si le volume justifie Kafka (>1 000 messages/sec)

### Migration

```kotlin
// Remplace KafkaProducer par NATSPublisher
class NATSFuelPricePublisher(private val nats: NatsConnection) : FuelPricePublisher {
    private val js = nats.jetStream()

    override suspend fun publishUpdate(event: FuelPriceUpdatedEvent) {
        val data = Json.encodeToByteArray(event)
        js.publish("carnetroute.fuel.prices", data)
    }
}

// Consumer
class NATSFuelPriceConsumer(nats: NatsConnection) {
    init {
        val js = nats.jetStream()
        val sub = js.subscribe("carnetroute.fuel.prices") { msg ->
            val event = Json.decodeFromByteArray<FuelPriceUpdatedEvent>(msg.data)
            handlePriceUpdate(event)
            msg.ack()
        }
    }
}
```

---

## 9. Observabilité & monitoring

### Stack observabilité

```
Logs    → Logback → Loki → Grafana
Métriques → Micrometer → Prometheus → Grafana
Traces  → OpenTelemetry → Tempo → Grafana
Alertes → AlertManager → Slack / Email
```

### Métriques clés (Micrometer)

```kotlin
class SimulationMetrics(private val registry: MeterRegistry) {
    val simulationDuration = Timer.builder("simulation.duration")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(registry)

    val cacheHitRate = Counter.builder("cache.hits")
        .tag("type", "simulation")
        .register(registry)

    val routingAPIErrors = Counter.builder("routing.api.errors")
        .tag("provider", "tomtom")
        .register(registry)
}
```

### Alertes Prometheus

```yaml
groups:
  - name: carnetroute
    rules:
      - alert: HighSimulationLatency
        expr: histogram_quantile(0.95, simulation_duration_ms) > 500
        for: 5m
        annotations:
          summary: "Latence simulation p95 > 500ms"

      - alert: LowCacheHitRate
        expr: cache_hit_rate < 0.7
        for: 10m
        annotations:
          summary: "Taux de cache hit < 70%"

      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.001
        for: 5m
        annotations:
          summary: "Taux d'erreur HTTP 5xx > 0.1%"
```

### Dashboards Grafana

1. **System Health** : JVM heap, GC, PostgreSQL connexions, Redis mémoire
2. **Application Performance** : latence simulation (p50/p95/p99), cache hit rate, erreurs API
3. **Business Metrics** : utilisateurs actifs, simulations/jour, CO2 économisé cumulatif

---

## 10. Mobile & PWA

### Phase 1 — PWA Angular

```typescript
// app.config.ts
export const appConfig: ApplicationConfig = {
  providers: [
    provideServiceWorker('ngsw-worker.js', {
      enabled: !isDevMode(),
      registrationStrategy: 'registerWhenStable:30000'
    })
  ]
};
```

**Stratégies de cache (ngsw-config.json) :**
- Assets statiques : `cacheFirst` (TTL 30 jours)
- Appels API GET : `networkFirst` (TTL 5 min, fallback cache)
- Simulations offline : stockées en IndexedDB, synchronisées au retour en ligne

**Cible Lighthouse :**
- Performance ≥ 90, Accessibility ≥ 95
- LCP < 2.5s, FID < 100ms, CLS < 0.1
- Service Worker installable, icônes, manifest.json

### Phase 2 — Capacitor (optionnel, mois 8+)

```bash
npm install @capacitor/core @capacitor/cli @capacitor/geolocation
npx cap add android
npx cap add ios
```

Fonctionnalités natives :
- `@capacitor/geolocation` → détection automatique position départ
- `@capacitor/push-notifications` → alertes prix carburant en temps réel
- `@capacitor/share` → partage de trajets

---

## 11. Infrastructure & déploiement

### Docker Compose (développement local)

```yaml
services:
  backend:
    build: ./backend
    ports: ["8080:8080"]
    depends_on: [postgres, redis, nats]
    environment:
      DB_URL: postgres://postgres:password@postgres:5432/carnetroute
      REDIS_URL: redis://redis:6379
      NATS_URL: nats://nats:4222

  postgres:
    image: postgres:16
    volumes: [postgres_data:/var/lib/postgresql/data]

  redis:
    image: redis:7-alpine

  nats:
    image: nats:2.10-alpine
    command: ["-js"]  # JetStream activé

  frontend:
    build: ./frontend
    ports: ["4200:80"]

  grafana:
    image: grafana/grafana:latest
    ports: ["3000:3000"]

  prometheus:
    image: prom/prometheus:latest
    ports: ["9090:9090"]
```

### CI/CD GitHub Actions

```yaml
name: CI/CD

on:
  push:
    branches: [main]
  pull_request:

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16
        env: { POSTGRES_PASSWORD: password }
      redis:
        image: redis:7-alpine
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: corretto }
      - run: cd backend && ./gradlew test jacocoTestReport
      - run: cd frontend && npm ci && npm run test:ci

  build-and-push:
    needs: test
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - uses: docker/build-push-action@v5
        with:
          context: ./backend
          push: true
          tags: ghcr.io/${{ github.repository }}/backend:${{ github.sha }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  deploy:
    needs: build-and-push
    runs-on: ubuntu-latest
    steps:
      - run: |
          helm upgrade --install carnetroute ./helm/carnetroute \
            --kubeconfig=${{ secrets.KUBECONFIG }} \
            --set backend.image.tag=${{ github.sha }} \
            --set frontend.image.tag=${{ github.sha }}
```

### Helm values (production)

```yaml
replicaCount:
  backend: 3
  frontend: 2

ingress:
  enabled: true
  host: carnetroute.fr
  tls:
    enabled: true
    issuer: letsencrypt-prod

postgresql:
  persistence: { size: 50Gi }
  backup: { enabled: true, schedule: "0 2 * * *" }

redis:
  persistence: { enabled: true, size: 20Gi }

monitoring:
  prometheus: true
  grafana: true
  loki: true
```

---

## 12. Roadmap en 3 phases

### Phase 1 — Fondations (Semaines 1–3, ~100h)

**Objectif :** Infrastructure BD + Auth + simulations persistées

| Tâche | Effort |
|---|---|
| Setup PostgreSQL + Flyway (users, vehicles, fuel_prices, simulations, history) | 5h |
| BC UserManagement : User entity + JWT auth + /api/auth/* | 15h |
| BC VehicleManagement : CRUD véhicules + FuelProfile overrides | 12h |
| Migration SimulationEngine vers BC Simulation + persistance DB | 20h |
| Setup Redis + caching layer (TTL + invalidation event-driven) | 15h |
| Tests intégration Testcontainers (PostgreSQL + Redis) | 15h |
| Frontend : composants Login/Register + guards JWT + My Vehicles | 18h |
| **Total** | **100h** |

**Livrables :**
- Backend déployable avec persistance complète
- Authentification JWT fonctionnelle
- Gestion des véhicules utilisateur
- CI/CD GitHub Actions opérationnel

---

### Phase 2 — Intégrations (Semaines 4–7, ~120h)

**Objectif :** Prix réels, routage précis, monitoring, analytics

| Tâche | Effort |
|---|---|
| SPSE + EssenceAndCo adapters + Kafka/NATS producer quotidien | 20h |
| TomTom Routing API + cache LRU Redis 24h | 25h |
| Trafic prédictif + heatmap données réelles | 20h |
| IGN géocodage prioritaire + Nominatim fallback | 15h |
| Monitoring : Prometheus + Grafana + Loki + alertes Slack | 20h |
| Analytics : UserStatistics + leaderboard CO2 + Kafka events BI | 15h |
| Frontend : My Journeys (paginé) + Stats dashboard + CO2 calc | 15h |
| Load tests K6 (100 users concurrent) + optimisation requêtes | 10h |
| **Total** | **120h** |

**Livrables :**
- Prix carburant officiels temps réel
- Routage précis avec TomTom
- Analytics utilisateur complètes
- Monitoring production-ready
- Performance validée : p95 < 500ms

---

### Phase 3 — Polish & scalabilité (Semaines 8–10, ~80h)

**Objectif :** PWA, CQRS optionnel, OAuth2 social, performance avancée

| Tâche | Effort |
|---|---|
| PWA Angular (Service Worker + Workbox + offline mode) | 10h |
| OAuth2 social login (Google, GitHub) | 10h |
| CQRS optionnel si simulations deviennent bottleneck | 20h |
| Batchs asynchrones (export PDF/CSV, digest mensuel email) | 10h |
| MapLibre GL JS complet (tuiles vectorielles, styles custom) | 15h |
| Découplage FuelPrice Service (microservice indépendant optionnel) | 10h |
| Performance avancée : gzip, HTTP/2, indexation DB fine | 5h |
| **Total** | **80h** |

---

### Résumé timeline

| Phase | Durée | Effort | Mode full-time | Mode part-time (20h/sem) |
|---|---|---|---|---|
| Phase 1 | 3 semaines | 100h | Sem. 1–3 | Sem. 1–5 |
| Phase 2 | 4 semaines | 120h | Sem. 4–7 | Sem. 6–12 |
| Phase 3 | 3 semaines | 80h | Sem. 8–10 | Sem. 13–16 |
| Overhead (review, docs, hotfixes) | 2 semaines | 60h | — | — |
| **TOTAL** | **~10 semaines** | **360h** | **2.5 mois** | **4–5 mois** |

---

## 13. Checklist qualité

### Code quality
- [ ] Couverture tests > 70% (Jacoco backend, Jest frontend)
- [ ] SonarQube quality gates (code smells, bugs, duplications)
- [ ] Gitleaks pre-commit hook (détection secrets)
- [ ] API OpenAPI auto-générée (Swagger UI)
- [ ] CHANGELOG.md (semver)

### Sécurité
- [ ] CORS whitelist (pas de `allowAnyHost`)
- [ ] CSP + HSTS headers
- [ ] SQL injection : requêtes paramétrées uniquement
- [ ] Rate limiting API (50 req/min par IP)
- [ ] JWT refresh token rotation
- [ ] Secrets dans Sealed Secrets K8s (jamais en clair git)
- [ ] OWASP Top 10 checklist complète

### Performance
- [ ] Requêtes DB < 100ms p95 (`EXPLAIN ANALYZE`)
- [ ] Index PostgreSQL : email, userId, createdAt, fuelType
- [ ] Cache hit rate > 80%
- [ ] Load test validé : 100 users concurrent, p95 < 500ms
- [ ] Angular OnPush sur tous les composants
- [ ] Lazy loading de tous les feature modules
- [ ] Pas de memory leaks (`takeUntilDestroyed`)

### Operations
- [ ] Health check `/api/health` + `/metrics`
- [ ] Graceful shutdown (drain requêtes, fermeture DB)
- [ ] HPA K8s : min 2, max 10 replicas
- [ ] Backup PostgreSQL quotidien (snapshot S3/Ceph)
- [ ] Procédure de rollback documentée
- [ ] Runbook incidents (latence, erreurs DB, Kafka lag)
