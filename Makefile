COMPOSE = docker compose -f dev/integration/compose.yml

.PHONY: dev-up dev-down dev-test dev-test-messaging dev-stock dev-quote dev-invalid dev-status dev-replay
dev-up:
	$(COMPOSE) up -d --build app
dev-down:
	$(COMPOSE) down
dev-test:
	$(COMPOSE) run --rm --build smoke
dev-test-messaging:
	$(COMPOSE) run --rm --build --no-deps smoke python /tools/smoke_messaging.py
dev-stock dev-quote dev-invalid dev-status dev-replay:
	$(COMPOSE) run --rm --build --no-deps smoke python /tools/events.py $(patsubst dev-%,%,$@)
