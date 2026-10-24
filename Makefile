COMPOSE = docker compose -f dev/integration/compose.yml

.PHONY: dev-up dev-down dev-test dev-stock dev-quote dev-invalid dev-status dev-replay
dev-up:
	$(COMPOSE) up -d --build app
dev-down:
	$(COMPOSE) down
dev-test:
	$(COMPOSE) run --rm --build smoke
dev-stock dev-quote dev-invalid dev-status dev-replay:
	$(COMPOSE) run --rm --build --no-deps smoke python /tools/events.py $(patsubst dev-%,%,$@)
