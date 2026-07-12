# 🛰️ OrbitaMarket

**Платформа для торговли спутниковыми данными** с микросервисной архитектурой, асинхронной оплатой через Kafka и гарантированной доставкой событий.

![Java|58](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)
![Kafka](https://img.shields.io/badge/Kafka-3.6-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![Docker](https://img.shields.io/badge/Docker-Compose-blue)

---

## 📋 О проекте

OrbitaMarket — это платформа для покупки спутниковых данных (ДЗЗ). Клиенты могут:
- 📸 Покупать **архивные снимки** (ARCHIVE)

Будет добавлено позже:
- 🎯 Заказывать **будущую съёмку** (TASKING)
- 📊 Оформлять **подписки на мониторинг** (MONITORING)

Оплата происходит в **геокредитах** — внутренней валюте платформы.

---
## 🛠️ Технологии

| Категория            | Технология                     |
| -------------------- | ------------------------------ |
| **Язык**             | Java 21                        |
| **Фреймворк**        | Spring Boot 3.2.5              |
| **Gateway**          | Spring Cloud Gateway           |
| **Брокер сообщений** | Apache Kafka 3.6               |
| **База данных**      | PostgreSQL 15                  |
| **Контейнеризация**  | Docker + Docker Compose        |
| **Документация API** | SpringDoc OpenAPI (Swagger UI) |
| **Тестирование**     | Postman + Allure               |
| **Безопасность**     | Gitleaks, Semgrep              |


---

## 🌟 Особенности проекта

| Особенность                        | Реализация                                    |
| ---------------------------------- | --------------------------------------------- |
| 🏗️ **Микросервисная архитектура** | Payments Service, Orders Service, API Gateway |
| 📨 **Асинхронная оплата**          | Kafka + Outbox/Inbox паттерны                 |
| 🔒 **Идемпотентность**             | Защита от двойного списания по `order_id`     |
| ⚡ **Конкурентность**               | Optimistic Locking через `@Version`           |
| 🔄 **Надёжная доставка**           | Transactional Outbox + retry механизм         |
| 🛡️ **Безопасность**               | Gitleaks + Semgrep анализ                     |

---
## 🚀 Запуск

### Предварительные требования

- Docker & Docker Compose
- Java 21 (для локальной разработки)
### Запуск проекта

```bash
# Клонировать репозиторий
git clone git@github.com:Nikita20q/OrbitaMarket.git
cd OrbitaMarket
```

```bash
# Запустить все сервисы
docker compose up -d --build
```

### Доступные сервисы

| Сервис                 | URL                                   | Описание            |
| ---------------------- | ------------------------------------- | ------------------- |
| **API Gateway**        | http://localhost:8080                 | Единая точка входа  |
| **Payments Service**   | http://localhost:8081                 | Управление счетами  |
| **Orders Service**     | http://localhost:8082                 | Управление заказами |
| **Swagger UI (общий)** | http://localhost:8080/swagger-ui.html | Документация API    |
| **Kafka UI**           | http://localhost:8085/                | Мониторинг Kafka    |

---
## 🏛️ Архитектура

### C4 Model

Диаграммы архитектуры находятся в папке `docs/`:

- 📄 [C1 System Context](docs/c4-context.puml) — внешние акторы и система
![C1](C1.png)
- 📦 [C2 Containers](docs/c4-container.puml) — внутренние компоненты
![C2](C2.png)
### Схема взаимодействия

**Описание потока:**

1. **Создание заказа** (синхронно):
   - Клиент → API Gateway → Orders Service
   - Orders Service сохраняет заказ + событие в outbox (атомарно)
   - Возвращает 201 Created с orderId

2. **Отправка в Kafka** (асинхронно):
   - OutboxPollingPublisher каждую секунду проверяет PENDING записи
   - Отправляет событие в топик `order-payment-requested`
   - Обновляет статус outbox на SENT

3. **Обработка платежа** (асинхронно):
   - Payments Service получает событие из Kafka
   - Проверяет inbox (идемпотентность)
   - Списывает баланс, сохраняет в inbox
   - Отправляет `order-payment-completed` или `order-payment-failed`

4. **Обновление статуса** (асинхронно):
   - Orders Service получает результат из Kafka
   - Обновляет статус заказа на PAID или PAYMENT_FAILED

![Поток данных](data_flow.png)
### Контракты событий

#### OrderPaymentRequested (Orders → Payments)

```json
{
	"eventId": "uuid",
	"orderId": "uuid",
	"userId": "uuid",
	"amount": 120,
	"occurredAt": "2026-07-11T12:00:00"
}
```
#### OrderPaymentCompleted (Payments → Orders)
```json
{
	"eventId": "uuid",
	"orderId": "uuid",
	"userId": "uuid",
	"amount": 120,
	"newBalance": 880,
	"occurredAt": "2026-07-11T12:00:01"
}
```
#### OrderPaymentFailed (Payments → Orders)
```json
{
	"eventId": "uuid",
	"orderId": "uuid",
	"userId": "uuid",
	"reason": "INSUFFICIENT_BALANCE",
	"amount": 120,
	"occurredAt": "2026-07-11T12:00:01"
}
```
---
## 
## 🔐 Механизмы защиты данных

### Optimistic Locking (`@Version`)

Для защиты баланса от **lost update** (потери обновлений при параллельных запросах) используется оптимистичная блокировка через аннотацию `@Version`:

```java
@Entity
@Table(name = "account")
public class Account {
    @Id
    private UUID id;
    
    @Column(nullable = false)
    private BigDecimal balance;
    
    @Version
    private Integer version;
}
```

### Transactional Outbox (Orders Service)

Событие в Kafka и заказ сохраняются **в одной транзакции** — это гарантирует, что не возникнет ситуация "заказ создан, но событие не отправлено":
```java
@Transactional
public OrderResponse createOrder(UUID userId, OrderRequest request) {
    Order order = orderRepository.save(...);           // ← в транзакции
    OrderOutbox outbox = OrderOutbox.builder()
        .eventId(UUID.randomUUID())
        .payload(eventJson)
        .status(OutboxStatus.PENDING)
        .build();
    orderOutboxRepository.save(outbox);                // ← в той же транзакции
}
```

### Inbox Pattern (Payments Service)

Защита от **двойного списания** при повторной доставке события из Kafka:

```java
@Transactional
public void processPaymentRequest(OrderPaymentRequested event) {
    if (inboxRepository.existsById(event.getEventId())) {
        return;  // Уже обработано, идемпотентность
    }
    
    account.setBalance(account.getBalance().subtract(event.getAmount()));
    accountRepository.save(account);
    
    inboxRepository.save(PaymentInbox.builder()
        .eventId(event.getEventId())
        .build());  // Фиксируем, что обработали
}
```

---
## 📨 Топики Apache Kafka

Платформа использует Kafka для асинхронного взаимодействия между микросервисами.

### Список топиков

| Имя топика                | Отправитель (Producer) | Получатель (Consumer) | Назначение                                                 |     |
| ------------------------- | ---------------------- | --------------------- | ---------------------------------------------------------- | --- |
| `order-payment-requested` | Orders Service         | Payments Service      | Запрос на списание геокредитов за заказ                    |     |
| `order-payment-completed` | Payments Service       | Orders Service        | Успешное списание — статус заказа меняется на PAID         |     |
| `order-payment-failed`    | Payments Service       | Orders Service        | Ошибка списания — статус заказа меняется на PAYMENT_FAILED |     |

---
## 📚 API Документация

### Идентификация пользователя

Все запросы требуют заголовок `X-User-Id`.

### Тестовые пользователи

Для локальной разработки и тестирования используются следующие UUID:

| Пользователь          | UUID                                   | Назначение                                        |
| --------------------- | -------------------------------------- | ------------------------------------------------- |
| **User-1 (основной)** | `7ade5318-a459-4cc5-9b62-c1b15ec56c3d` | Основные сценарии тестирования                    |
| **User-2 (тестовый)** | `7ade3378-a459-4cc5-9b62-c1b25ec56c3d` | Тесты на изоляцию данных, проверка PAYMENT_FAILED |

### Пример использования

#### Payments API

##### Создать счёт
```bash
curl -X 'POST' \
  'http://localhost:8080/payments/api/v1/payments/accounts' \
  -H 'accept: */*' \
  -H 'X-User-Id: 7ade5318-a459-4cc5-9b62-c1b15ec56c3d' \
  -d ''
```

##### Пополнить счёт
```bash
curl -X 'POST' \
  'http://localhost:8080/payments/api/v1/payments/accounts/top-up' \
  -H 'accept: */*' \
  -H 'X-User-Id: 7ade5318-a459-4cc5-9b62-c1b15ec56c3d' \
  -H 'Content-Type: application/json' \
  -d '{
  "amount": 1000
}'
```

##### Получить баланс пользователя
```bash
curl -X 'GET' \
  'http://localhost:8080/payments/api/v1/payments/accounts/balance' \
  -H 'accept: */*' \
  -H 'X-User-Id: 7ade5318-a459-4cc5-9b62-c1b15ec56c3d'
```

#### Orders API

##### Получить список заказов пользователя
```bash
curl -X 'GET' \
  'http://localhost:8080/orders/api/v1/orders/orders' \
  -H 'accept: */*' \
  -H 'X-User-Id: 7ade5318-a459-4cc5-9b62-c1b15ec56c3d'
```
##### Создать заказ
```bash
curl -X 'POST' \
  'http://localhost:8080/orders/api/v1/orders/orders' \
  -H 'accept: */*' \
  -H 'X-User-Id: 7ade5318-a459-4cc5-9b62-c1b15ec56c3d' \
  -H 'Content-Type: application/json' \
  -d '{
  "productType": "ARCHIVE",
  "price": 120,
  "payload": {
    "aoi": "string",
    "capture_date": "string",
    "sensor_type": "string"
  }
}'
```
##### Получить детали заказа
```bash
curl -X 'GET' \
  'http://localhost:8080/orders/api/v1/orders/orders/3fa85f64-5717-4562-b3fc-2c963f66afa6' \
  -H 'accept: */*' \
  -H 'X-User-Id: 7ade5318-a459-4cc5-9b62-c1b15ec56c3d'
```

---
## 🛡️ Информационная безопасность

### Сканирование

```bash
# Поиск утечек секретов
gitleaks detect --source . --report-format json --report-path security-reports/gitleaks-history-report.json --verbose --no-git

# Статический анализ кода
semgrep scan --config auto --json --output security-reports/semgrep-report.json
```

📄 [Результаты и триаж](./security-reports/Triaj) — Результаты сканирования (./security-reports/Triaj)

---
## 📊 Аналитика (SQL)

SQL-запросы для бизнес-аналитики находятся в `docs/analytics.sql`.

**Пример одного из запросов:**

![Аналитика](sql-analytics.png)

---
```
OrbitaMarket/
├── api-gateway/                    # API Gateway
│   ├── src/main/java/
│   └── Dockerfile
├── orders-service/                 # Сервис заказов
│   ├── src/main/java/
│   │   ├── controller/            # REST контроллеры
│   │   ├── domain/                # Entity, DTO, Enums
│   │   ├── exception/             # Обработчики ошибок
│   │   ├── kafka/                 # Outbox Publisher, Consumers
│   │   ├── repository/            # JPA репозитории
│   │   ├── service/               # Бизнес-логика
│   │   └── validation/            # Валидаторы payload
│   └── Dockerfile
├── payments-service/               # Сервис платежей
│   ├── src/main/java/
│   │   ├── controller/
│   │   ├── domain/
│   │   ├── exception/
│   │   ├── kafka/                 # Inbox, Producer
│   │   ├── repository/
│   │   └── service/
│   └── Dockerfile
├── docs/                           # Документация
│   ├── c4-context.puml            # C1 System Context
│   ├── c4-container.puml          # C2 Containers
│   └── analytics.sql              # SQL-запросы аналитики
├── security-reports/               # Отчёты безопасности
│   ├── gitleaks-report.json
│   └── semgrep-report.json
├── orbitamarket-collection.json    # Postman коллекция
├── orbitamarket-environment.json   # Postman окружение
├── PROJECT.md                      # Планирование проекта
├── docker-compose.yml              # Docker Compose конфигурация
└── README.md                       # Данный файл
```

---

## 🗺️ Roadmap

### ✅ Реализовано (MVP)

- Payments Service с идемпотентным созданием счёта
- Orders Service с поддержкой ARCHIVE
- API Gateway с маршрутизацией
- Асинхронная оплата через Kafka
- Outbox/Inbox паттерны
- Optimistic Locking для баланса
- Swagger UI документация
- Тесты Postman + Allure
- Анализ безопасности (Gitleaks, Semgrep)
- C4 диаграммы

### 🔄 В планах

- JWT-аутентификация
- Типы заказов: TASKING, MONITORING
- Кеширование данных
- Отдельный сервис для аналитики
- Сервис выдачи заказанных данных