# OrangeHRM Performance Testing & Monitoring

Suite complète pour les tests de performance et le monitoring de l'application OrangeHRM.

## 🎯 Objectifs

- Tests de performance automatisés avec Gatling
- Monitoring en temps réel avec Prometheus & Grafana
- Analyse des métriques de performance
- Détection des seuils critiques

## 📁 Structure du projet

```
├── gatling-maven-plugin-demo-java/     # Tests Gatling Maven
├── prometheus/           # Stack Prometheus (Grafana reste à implémenter)
└── docs/                # Documentation
```

## 🚀 Quick Start

### Tests de performance
```bash
cd gatling-maven-plugin-demo-java
mvn gatling:test
```

### Monitoring
```bash
cd prometheus
docker-compose up -d
```

Accès aux interfaces :
- **Grafana** : http://localhost:3000 (admin/admin)(à implémenter)
- **Prometheus** : http://localhost:9090

## 📊 Scénarios de test

### Répartition de charge
- **75%** : Recherche employé → Consultation coordonnées
- **20%** : Ajout employé → Saisie détails
- **5%** : Recherche → Suppression employé

## 📖 Documentation

- [Scénarios de test](docs/test-scenarios.md)
- [Configuration monitoring](docs/monitoring-setup.md)
- [Tests performance](performance-tests/README.md)
- [Stack monitoring](monitoring/README.md)

## 🛠️ Prérequis

- Java 11+
- Maven 3.6+
- Docker & Docker Compose
- OrangeHRM instance

## 🤝 Contribution

1. Fork le projet
2. Crée une branche feature
3. Commit tes changements
4. Push et ouvre une PR