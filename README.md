# TransactionAggregator

Projet qui à pour but de reconstituer des chaines de transactions désordonnées, de les afficher, et de voir leurs incohérences
## Tech Stack

**Front :** Angular

**Back :** Java, Kafka, PostGreSQL
## Installation

### Prérequis

- Java : https://www.oracle.com/fr/java/technologies/downloads/ (ou n'importe quel jdk 17+)
- NodeJS : https://nodejs.org/en/download
- Kafka : https://kafka.apache.org/downloads
- PostGreSQL : https://www.postgresql.org/download/
## Lancer localement

Aller dans le répertoire du projet

**Pour lancer Kafka :**

```bash
  cd C:\kafka
  bin\windows\zookeeper-server-start.bat config\zookeeper.properties
  cd C:\kafka
  bin\windows\kafka-server-start.bat config\server.properties
```

**Pour lancer le serveur back :**

Placer le fichier JSON de transactions dans "TransactionAggregator-back\src\main\resources\data", puis lancer les commandes suivantes

```bash
  mvn clean install
  mvn spring-boot:run
```

**Pour lancer le serveur front :**

```bash
  npm install
  ng build
  ng serve --proxy-config proxy.conf.json
```