# Platform Module

This module demonstrates Hibernate ORM integration with Open Liberty using a containerized PostgreSQL database.

## Prerequisites

- **Docker**: Required to run the PostgreSQL database container
  - Install Docker Desktop from [docker.com](https://www.docker.com/products/docker-desktop)
  - Ensure Docker daemon is running before starting the application

## Getting Started

### 1. Test PostgreSQL Database

Before running the application, start the PostgreSQL container:

```bash
./gradlew :platform:startPostgres
```

This will create and start a PostgreSQL 18 container with:
- Container name: `postgres`
- Port: `5432`
- Database: `testDB`
- Username: `postgres`
- Password: `password`

Make sure the container starts by using the command

```bash
docker ps
CONTAINER ID   IMAGE                COMMAND                  CREATED         STATUS         PORTS                                       NAMES
5eb22becd1ee   postgres:18-alpine   "docker-entrypoint.s…"   4 seconds ago   Up 4 seconds   0.0.0.0:5432->5432/tcp, :::5432->5432/tcp   postgres
```

Now you can stop the container, gradle will automatically start/stop the container when entering/existing liberty development mode.

```bash
docker rm -f postgres
```

### 2. Start Liberty Development Mode

Run the application in development mode:

```bash
./gradlew :platform:libertyDev
```

Liberty will:
- Start the database (postgresql)
- Build and deploy the application
- Start the Open Liberty server
- Enable hot reload for code changes
- Make the application available at `http://localhost:9080/testApplication`

### 3. Stop Liberty Development Mode

You have two options to stop the server:

**Option 1: Interactive (from libertyDev console)**
- Press `q` and then `Enter`

**Option 2: Gradle task (from another terminal or if dev mode exits unexpectedly)**
```bash
./gradlew :platform:libertyStop
```

## Application Endpoints

The application provides REST endpoints for managing Person entities:

- **Create Person**: `POST http://localhost:9080/?personId={id}&value={value}`
- **Get Person**: `GET http://localhost:9080/{personId}`
- **Update Person**: `POST http://localhost:9080/{personId}?value={newValue}`

## Configuration

### Liberty Features
- `restfulWS-3.1` - JAX-RS support
- `cdi-4.0` - CDI support
- `persistenceContainer-3.1` - JPA container support
- `xmlBinding-4.0` - Required by Hibernate

### Hibernate Configuration
Configured in `persistence.xml`:
- Provider: Hibernate 7.0.9.Final
- Schema generation: Auto-create tables
- SQL logging: Enabled
- Bean Validation: Disabled (not required for this test)

## Troubleshooting

### PostgreSQL Connection Issues
- Ensure Docker is running: `docker ps`
- Check if PostgreSQL container is running: `docker ps | grep postgres`
- Verify port 5432 is not in use by another process

### Liberty Server Issues
- Check server logs: `platform/build/wlp/usr/servers/defaultServer/logs/messages.log`
- Ensure all dependencies are copied: `./gradlew :platform:copyDriver :platform:copyHibernate`

### Build Issues
- Clean and rebuild: `./gradlew :platform:clean :platform:build`

## Failing test scenario

Create a new person:

```bash
curl http://localhost:9080/testApplication/create?id=10&value=new
10
```

Verify the person was persisted to the database:

```bash
curl http://localhost:9080/testApplication/10
Person id=10  value=new
```

Attempt to update the value:

```bash
curl http://localhost:9080/testApplication/update?id=10&value=update
Person id=10  value=update
```

Verify the update was persisted to the database:

```bash
curl http://localhost:9080/testApplication/10
Person id=10  value=new
```

ERROR: The update was not persisted to the database. 
