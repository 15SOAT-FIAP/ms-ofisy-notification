COMPOSE = docker compose -f dev/integration/compose.yml

.PHONY: dev-up dev-down dev-test dev-test-messaging dev-stock dev-quote dev-quote-created dev-stock-failure dev-invalid dev-status dev-replay
dev-up:
	$(COMPOSE) up -d --build app
dev-down:
	$(COMPOSE) down
dev-test:
	$(COMPOSE) up -d --wait localstack
	DYNAMODB_TEST_ENDPOINT=http://127.0.0.1:14566 mvn clean verify
	python3 dev/integration/assert_reports.py
dev-test-messaging:
	mvn test-compile failsafe:integration-test failsafe:verify -Dit.test=NotificationEventListenerIT
dev-stock dev-quote dev-quote-created dev-stock-failure dev-invalid dev-status dev-replay:
	$(COMPOSE) run --rm --build --no-deps tools $(patsubst dev-%,%,$@) $(if $(SERVICE_ORDER_ID),--service-order-id $(SERVICE_ORDER_ID))
