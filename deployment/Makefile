.PHONY: help build build-all push clean \
        infra-up infra-down infra-logs infra-ps \
        services-up services-down services-logs services-ps \
        dev-up dev-down dev-logs dev-ps \
        all-up all-down all-logs all-ps \
        db-shell redis-cli kafka-topics \
        test lint health urls

DOCKER_DIR := docker
COMPOSE_INFRA := $(DOCKER_DIR)/docker-compose.infra.yml
COMPOSE_SERVICES := $(DOCKER_DIR)/docker-compose.services.yml
COMPOSE_INFRA_DEV := $(DOCKER_DIR)/docker-compose.infra.dev.yml
COMPOSE_SERVICES_DEV := $(DOCKER_DIR)/docker-compose.services.dev.yml

VERSION ?= latest
DOCKER_REGISTRY ?= pictorium

SERVICES := config-server eureka-server api-gateway user-service content-service \
            storage-service chat-service notification-service search-service

GREEN := \033[0;32m
YELLOW := \033[0;33m
RED := \033[0;31m
BLUE := \033[0;34m
NC := \033[0m

# =============================================================================
# HELP
# =============================================================================

help:
	@echo ""
	@echo "$(GREEN)Pictorium - Makefile Commands$(NC)"
	@echo ""
	@echo "$(YELLOW)Сборка:$(NC)"
	@echo "  make build SERVICE=<name>  - Собрать конкретный сервис"
	@echo "  make build-all             - Собрать все сервисы"
	@echo "  make build-jar             - Собрать только JAR файлы"
	@echo "  make clean                 - Очистить все"
	@echo ""
	@echo "$(YELLOW)Инфраструктура:$(NC)"
	@echo "  make infra-up              - Запустить инфраструктуру"
	@echo "  make infra-down            - Остановить инфраструктуру"
	@echo "  make infra-logs            - Логи инфраструктуры"
	@echo "  make infra-ps              - Статус контейнеров"
	@echo ""
	@echo "$(YELLOW)Сервисы:$(NC)"
	@echo "  make services-up           - Запустить сервисы"
	@echo "  make services-down         - Остановить сервисы"
	@echo "  make services-logs         - Логи сервисов"
	@echo "  make services-ps           - Статус сервисов"
	@echo ""
	@echo "$(YELLOW)Dev режим:$(NC)"
	@echo "  make dev-up                - Запустить всё в dev режиме"
	@echo "  make dev-down              - Остановить всё"
	@echo "  make dev-logs              - Логи в dev режиме"
	@echo "  make dev-infra-up          - Только инфра в dev режиме"
	@echo ""
	@echo "$(YELLOW)Production:$(NC)"
	@echo "  make all-up                - Запустить всё"
	@echo "  make all-down              - Остановить всё"
	@echo ""
	@echo "$(YELLOW)Утилиты:$(NC)"
	@echo "  make db-shell              - Подключиться к PostgreSQL"
	@echo "  make redis-cli             - Подключиться к Redis"
	@echo "  make kafka-topics          - Список топиков Kafka"
	@echo "  make health                - Проверить здоровье сервисов"
	@echo "  make urls                  - Показать все URL"
	@echo ""

# =============================================================================
# BUILD
# =============================================================================

build:
ifndef SERVICE
	$(error SERVICE is not set. Usage: make build SERVICE=user-service)
endif
	@echo "$(GREEN)Building $(SERVICE)...$(NC)"
	cd ../$(SERVICE) && ./gradlew bootJar --no-daemon
	docker build -t $(DOCKER_REGISTRY)/$(SERVICE):$(VERSION) ../$(SERVICE)

build-all:
	@echo "$(GREEN)Building all services...$(NC)"
	@for service in $(SERVICES); do \
		echo "$(YELLOW)Building $$service...$(NC)"; \
		cd ../$$service && ./gradlew bootJar --no-daemon && cd ../deployment; \
		docker build -t $(DOCKER_REGISTRY)/$$service:$(VERSION) ../$$service; \
	done
	@echo "$(GREEN)All services built!$(NC)"

build-jar:
	@echo "$(GREEN)Building JARs...$(NC)"
	@for service in $(SERVICES); do \
		echo "$(YELLOW)Building $$service JAR...$(NC)"; \
		cd ../$$service && ./gradlew bootJar --no-daemon && cd ../deployment; \
	done

push:
ifndef SERVICE
	$(error SERVICE is not set)
endif
	docker push $(DOCKER_REGISTRY)/$(SERVICE):$(VERSION)

push-all:
	@for service in $(SERVICES); do \
		docker push $(DOCKER_REGISTRY)/$$service:$(VERSION); \
	done

clean:
	@echo "$(YELLOW)Cleaning...$(NC)"
	@for service in $(SERVICES); do \
		cd ../$$service && ./gradlew clean && cd ../deployment; \
	done
	docker system prune -f
	@echo "$(GREEN)Cleaned!$(NC)"

# =============================================================================
# INFRASTRUCTURE
# =============================================================================

infra-up:
	@echo "$(GREEN)Starting infrastructure...$(NC)"
	docker compose -f $(COMPOSE_INFRA) up -d
	@echo "$(GREEN)Infrastructure started!$(NC)"
	@$(MAKE) --no-print-directory urls-infra

infra-down:
	@echo "$(YELLOW)Stopping infrastructure...$(NC)"
	docker compose -f $(COMPOSE_INFRA) down

infra-down-v:
	@echo "$(RED)Stopping infrastructure and removing volumes...$(NC)"
	docker compose -f $(COMPOSE_INFRA) down -v

infra-logs:
	docker compose -f $(COMPOSE_INFRA) logs -f

infra-ps:
	docker compose -f $(COMPOSE_INFRA) ps

# =============================================================================
# SERVICES
# =============================================================================

services-up:
	@echo "$(GREEN)Starting services...$(NC)"
	docker compose -f $(COMPOSE_SERVICES) up -d
	@echo "$(GREEN)Services started!$(NC)"

services-down:
	@echo "$(YELLOW)Stopping services...$(NC)"
	docker compose -f $(COMPOSE_SERVICES) down

services-logs:
	docker compose -f $(COMPOSE_SERVICES) logs -f

services-logs-service:
ifndef SERVICE
	$(error SERVICE is not set)
endif
	docker compose -f $(COMPOSE_SERVICES) logs -f $(SERVICE)

services-ps:
	docker compose -f $(COMPOSE_SERVICES) ps

services-restart:
ifndef SERVICE
	$(error SERVICE is not set)
endif
	docker compose -f $(COMPOSE_SERVICES) restart $(SERVICE)

# =============================================================================
# DEV MODE
# =============================================================================

dev-up:
	@echo "$(GREEN)Starting in DEV mode...$(NC)"
	docker compose -f $(COMPOSE_INFRA) -f $(COMPOSE_INFRA_DEV) up -d
	@echo "$(YELLOW)Waiting for infrastructure...$(NC)"
	sleep 10
	docker compose -f $(COMPOSE_SERVICES) -f $(COMPOSE_SERVICES_DEV) up -d
	@echo "$(GREEN)DEV environment started!$(NC)"
	@$(MAKE) --no-print-directory urls

dev-down:
	@echo "$(YELLOW)Stopping DEV environment...$(NC)"
	docker compose -f $(COMPOSE_SERVICES) -f $(COMPOSE_SERVICES_DEV) down
	docker compose -f $(COMPOSE_INFRA) -f $(COMPOSE_INFRA_DEV) down

dev-logs:
	docker compose -f $(COMPOSE_INFRA) -f $(COMPOSE_INFRA_DEV) \
	               -f $(COMPOSE_SERVICES) -f $(COMPOSE_SERVICES_DEV) logs -f

dev-ps:
	@echo "$(YELLOW)Infrastructure:$(NC)"
	docker compose -f $(COMPOSE_INFRA) -f $(COMPOSE_INFRA_DEV) ps
	@echo ""
	@echo "$(YELLOW)Services:$(NC)"
	docker compose -f $(COMPOSE_SERVICES) -f $(COMPOSE_SERVICES_DEV) ps

dev-infra-up:
	@echo "$(GREEN)Starting infrastructure in DEV mode...$(NC)"
	docker compose -f $(COMPOSE_INFRA) -f $(COMPOSE_INFRA_DEV) up -d
	@$(MAKE) --no-print-directory urls-infra

dev-infra-down:
	docker compose -f $(COMPOSE_INFRA) -f $(COMPOSE_INFRA_DEV) down

# =============================================================================
# ALL (PRODUCTION)
# =============================================================================

all-up: infra-up
	@echo "$(YELLOW)Waiting for infrastructure...$(NC)"
	sleep 15
	$(MAKE) services-up
	@echo "$(GREEN)All services started!$(NC)"
	@$(MAKE) --no-print-directory urls

all-down: services-down infra-down
	@echo "$(GREEN)All stopped!$(NC)"

all-logs:
	docker compose -f $(COMPOSE_INFRA) -f $(COMPOSE_SERVICES) logs -f

all-ps: infra-ps services-ps

# =============================================================================
# URLS
# =============================================================================

urls: urls-infra urls-services

urls-infra:
	@echo ""
	@echo "$(BLUE)Infrastructure:$(NC)"
	@echo "  PostgreSQL:      localhost:$${POSTGRES_PORT:-5432}"
	@echo "  PostgreSQL KC:   localhost:$${POSTGRES_KEYCLOAK_PORT:-5433}"
	@echo "  Keycloak:        http://localhost:$${KEYCLOAK_PORT:-9090}"
	@echo "  MinIO Console:   http://localhost:$${MINIO_CONSOLE_PORT:-9001}"
	@echo "  Redis:           localhost:$${REDIS_PORT:-6379}"
	@echo "  Elasticsearch:   http://localhost:$${ELASTICSEARCH_PORT:-9200}"
	@echo "  Kafka:           localhost:$${KAFKA_PORT:-9092}"

urls-services:
	@echo ""
	@echo "$(BLUE)Services:$(NC)"
	@echo "  Config Server:   http://localhost:$${CONFIG_SERVER_PORT:-8888}"
	@echo "  Eureka:          http://localhost:$${EUREKA_PORT:-8761}"
	@echo "  API Gateway:     http://localhost:$${GATEWAY_PORT:-8222}"
	@echo "  User Service:    http://localhost:$${USER_SERVICE_PORT:-8010}"
	@echo "  Content Service: http://localhost:$${CONTENT_SERVICE_PORT:-8020}"
	@echo "  Storage Service: http://localhost:$${STORAGE_SERVICE_PORT:-8088}"
	@echo "  Chat Service:    http://localhost:$${CHAT_SERVICE_PORT:-8030}"
	@echo "  Notification:    http://localhost:$${NOTIFICATION_SERVICE_PORT:-8040}"
	@echo "  Search Service:  http://localhost:$${SEARCH_SERVICE_PORT:-8050}"
	@echo ""

# =============================================================================
# UTILITIES
# =============================================================================

db-shell:
	docker exec -it pictorium-postgres psql -U postgres

db-shell-users:
	docker exec -it pictorium-postgres psql -U user_service -d users_db

db-shell-content:
	docker exec -it pictorium-postgres psql -U content_user -d content_db

db-shell-keycloak:
	docker exec -it pictorium-postgres-keycloak psql -U keycloak -d keycloak

redis-cli:
	docker exec -it pictorium-redis redis-cli

kafka-topics:
	docker exec -it pictorium-kafka kafka-topics --bootstrap-server localhost:9092 --list

kafka-create-topics:
	docker exec -it pictorium-kafka kafka-topics --bootstrap-server localhost:9092 --create --if-not-exists --topic user-events --partitions 3 --replication-factor 1
	docker exec -it pictorium-kafka kafka-topics --bootstrap-server localhost:9092 --create --if-not-exists --topic content-events --partitions 3 --replication-factor 1
	docker exec -it pictorium-kafka kafka-topics --bootstrap-server localhost:9092 --create --if-not-exists --topic chat-events --partitions 3 --replication-factor 1

es-health:
	curl -s http://localhost:9200/_cluster/health?pretty

es-indices:
	curl -s http://localhost:9200/_cat/indices?v

# =============================================================================
# HEALTH
# =============================================================================

health:
	@echo ""
	@echo "$(YELLOW)Checking health...$(NC)"
	@echo ""
	@echo "$(BLUE)Infrastructure:$(NC)"
	@echo "  PostgreSQL:      $$(docker exec pictorium-postgres pg_isready -U postgres >/dev/null 2>&1 && echo '$(GREEN)UP$(NC)' || echo '$(RED)DOWN$(NC)')"
	@echo "  PostgreSQL KC:   $$(docker exec pictorium-postgres-keycloak pg_isready -U keycloak >/dev/null 2>&1 && echo '$(GREEN)UP$(NC)' || echo '$(RED)DOWN$(NC)')"
	@echo "  Keycloak:        $$(curl -sf http://localhost:9090/health/ready >/dev/null 2>&1 && echo '$(GREEN)UP$(NC)' || echo '$(RED)DOWN$(NC)')"
	@echo "  Redis:           $$(docker exec pictorium-redis redis-cli ping >/dev/null 2>&1 && echo '$(GREEN)UP$(NC)' || echo '$(RED)DOWN$(NC)')"
	@echo "  Elasticsearch:   $$(curl -sf http://localhost:9200/_cluster/health >/dev/null 2>&1 && echo '$(GREEN)UP$(NC)' || echo '$(RED)DOWN$(NC)')"
	@echo "  Kafka:           $$(docker exec pictorium-kafka kafka-topics --bootstrap-server localhost:9092 --list >/dev/null 2>&1 && echo '$(GREEN)UP$(NC)' || echo '$(RED)DOWN$(NC)')"
	@echo "  MinIO:           $$(curl -sf http://localhost:9000/minio/health/live >/dev/null 2>&1 && echo '$(GREEN)UP$(NC)' || echo '$(RED)DOWN$(NC)')"
	@echo ""
	@echo "$(BLUE)Services:$(NC)"
	@echo "  Config Server:   $$(curl -sf http://localhost:8888/actuator/health 2>&1 | grep -q UP && echo '$(GREEN)UP$(NC)' || echo '$(RED)DOWN$(NC)')"
	@echo "  Eureka:          $$(curl -sf http://localhost:8761/actuator/health 2>&1 | grep -q UP && echo '$(GREEN)UP$(NC)' || echo '$(RED)DOWN$(NC)')"
	@echo "  API Gateway:     $$(curl -sf http://localhost:8222/actuator/health 2>&1 | grep -q UP && echo '$(GREEN)UP$(NC)' || echo '$(RED)DOWN$(NC)')"
	@echo "  User Service:    $$(curl -sf http://localhost:8010/actuator/health 2>&1 | grep -q UP && echo '$(GREEN)UP$(NC)' || echo '$(RED)DOWN$(NC)')"
	@echo "  Content Service: $$(curl -sf http://localhost:8020/actuator/health 2>&1 | grep -q UP && echo '$(GREEN)UP$(NC)' || echo '$(RED)DOWN$(NC)')"
	@echo "  Storage Service: $$(curl -sf http://localhost:8088/actuator/health 2>&1 | grep -q UP && echo '$(GREEN)UP$(NC)' || echo '$(RED)DOWN$(NC)')"
	@echo "  Chat Service:    $$(curl -sf http://localhost:8030/actuator/health 2>&1 | grep -q UP && echo '$(GREEN)UP$(NC)' || echo '$(RED)DOWN$(NC)')"
	@echo "  Notification:    $$(curl -sf http://localhost:8040/actuator/health 2>&1 | grep -q UP && echo '$(GREEN)UP$(NC)' || echo '$(RED)DOWN$(NC)')"
	@echo "  Search Service:  $$(curl -sf http://localhost:8050/actuator/health 2>&1 | grep -q UP && echo '$(GREEN)UP$(NC)' || echo '$(RED)DOWN$(NC)')"
	@echo ""

# =============================================================================
# DEV HELPERS
# =============================================================================

run-local:
ifndef SERVICE
	$(error SERVICE is not set)
endif
	cd ../$(SERVICE) && ./gradlew bootRun

test:
ifdef SERVICE
	cd ../$(SERVICE) && ./gradlew test
else
	@for service in $(SERVICES); do \
		echo "$(YELLOW)Testing $$service...$(NC)"; \
		cd ../$$service && ./gradlew test && cd ../deployment; \
	done
endif

# =============================================================================
# QUICK ALIASES
# =============================================================================

up: dev-up
down: dev-down
logs: dev-logs
ps: dev-ps
restart: dev-down dev-up