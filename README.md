# Mini Search Engine

Мини-поисковик на Java, который обходит один указанный сайт, собирает до 1000 страниц, извлекает текст, индексирует данные в OpenSearch и позволяет искать по страницам через REST API.

Проект разрабатывается поэтапно. Главная цель — получить понятный backend-проект для защиты, портфолио и демонстрации архитектуры crawler → parser → indexer → search.

## Стек

* Java 21
* Spring Boot 3
* Maven
* PostgreSQL
* OpenSearch
* Jsoup
* Flyway
* Swagger/OpenAPI
* Docker Compose

## Архитектура

Используется простая layered architecture:

```text
controller → service → repository/client
```

Основной поток данных:

```text
REST API
  → CrawlJob
  → Crawler
  → Parser
  → PostgreSQL
  → OpenSearch Indexer
  → Search API
```

PostgreSQL хранит состояние обхода, задания, URL, ошибки и метаданные страниц.

OpenSearch хранит поисковый индекс и выполняет полнотекстовый поиск.

## Что проект должен уметь

* Создавать задачу обхода сайта.
* Ограничивать обход одним доменом.
* Ограничивать количество страниц до 1000.
* Загружать HTML через Jsoup.
* Извлекать title, description, h1, canonical URL и body text.
* Индексировать страницы в OpenSearch.
* Искать по страницам через REST API.
* Возвращать title, url, snippet, score и indexedAtUtc.
* Показывать статус crawl job.
* Обрабатывать ошибки загрузки и парсинга.

## Этапы разработки

## Этап 1 — Spring Boot каркас и инфраструктура

Цель этапа — поднять минимальный рабочий backend-каркас.

Что добавляется:

* Spring Boot 3 проект.
* Maven конфигурация.
* PostgreSQL в Docker Compose.
* OpenSearch в Docker Compose.
* application.yml.
* Swagger/OpenAPI.
* Actuator.
* Custom health endpoint `/api/health`.

Файлы:

```text
pom.xml
docker-compose.yml
src/main/resources/application.yml
src/main/java/com/example/minisearch/MiniSearchApplication.java
src/main/java/com/example/minisearch/api/HealthController.java
```

Проверка:

```bash
mvn clean package
docker compose up -d
java -jar target/mini-search-0.0.1-SNAPSHOT.jar
curl http://localhost:8080/api/health
curl http://localhost:8080/actuator/health
curl http://localhost:9200/_cluster/health
```

Ожидаемый результат:

```text
Spring Boot запускается.
PostgreSQL доступен.
OpenSearch доступен.
Swagger доступен.
Health endpoint возвращает UP.
```

## Этап 2 — CrawlJob и PostgreSQL модель

Цель этапа — добавить хранение crawl job и базовые endpoints.

Что добавляется:

* Flyway migration.
* Таблица `crawl_jobs`.
* Entity `CrawlJobEntity`.
* Repository `CrawlJobRepository`.
* Service `CrawlJobService`.
* Controller `CrawlController`.
* DTO для создания и получения crawl job.
* Endpoint `POST /api/crawl`.
* Endpoint `GET /api/crawl/{jobId}`.

Основные поля `crawl_jobs`:

```text
id
start_url
root_domain
status
max_pages
pages_discovered
pages_fetched
pages_indexed
pages_failed
created_at
started_at
finished_at
last_error
```

Проверка:

```bash
mvn clean package
docker compose up -d
mvn spring-boot:run
curl -X POST http://localhost:8080/api/crawl \
  -H "Content-Type: application/json" \
  -d '{"startUrl":"https://example.com","maxPages":100}'
```

Ожидаемый результат:

```text
Создается crawl job.
Данные сохраняются в PostgreSQL.
GET /api/crawl/{jobId} возвращает статус задачи.
```

## Этап 3 — Crawler на Jsoup

Цель этапа — реализовать обход сайта.

Что добавляется:

* Загрузка HTML через Jsoup.
* Обход только одного домена.
* Ограничение `maxPages <= 1000`.
* Нормализация URL.
* visited URLs.
* timeout.
* user-agent.
* обработка ошибок.
* извлечение ссылок со страницы.

Основные классы:

```text
crawler/CrawlerService.java
crawler/UrlNormalizer.java
crawler/DomainPolicy.java
crawler/JsoupPageFetcher.java
crawler/LinkExtractor.java
```

Crawler должен:

```text
1. Взять стартовый URL.
2. Скачать страницу.
3. Извлечь ссылки.
4. Отфильтровать внешние домены.
5. Нормализовать URL.
6. Не обходить повторно уже посещенные URL.
7. Продолжать обход до maxPages.
```

Проверка:

```bash
curl -X POST http://localhost:8080/api/crawl \
  -H "Content-Type: application/json" \
  -d '{"startUrl":"https://example.com","maxPages":10}'
```

Ожидаемый результат:

```text
Crawler загружает страницы.
Внешние ссылки игнорируются.
Дубликаты URL не обходятся повторно.
Ошибки сохраняются в статусе crawl job.
```

## Этап 4 — Parser

Цель этапа — извлекать полезные данные из HTML.

Что добавляется:

* title.
* meta description.
* h1.
* body text.
* canonical URL.
* очистка script/style/nav/footer.
* нормализация пробелов.

Основные классы:

```text
parser/HtmlParser.java
parser/ParsedPage.java
```

Parser должен возвращать:

```text
url
canonicalUrl
title
description
h1
bodyText
```

Проверка:

```bash
curl http://localhost:8080/api/crawl/{jobId}
```

Ожидаемый результат:

```text
После обхода у страниц есть title, description, h1 и body text.
```

## Этап 5 — OpenSearch indexing

Цель этапа — индексировать распарсенные страницы.

Что добавляется:

* OpenSearch client.
* Создание индекса.
* Mapping для страниц.
* Indexer service.
* Обновление документа при повторной индексации.

Индекс:

```text
search_pages_v1
```

Поля документа:

```text
id
url
canonicalUrl
title
description
h1
body
indexedAtUtc
```

Основные классы:

```text
indexer/OpenSearchClientConfig.java
indexer/OpenSearchIndexService.java
indexer/PageIndexerService.java
```

Проверка:

```bash
curl http://localhost:9200/search_pages_v1/_search
```

Ожидаемый результат:

```text
Проиндексированные страницы появляются в OpenSearch.
При повторной индексации документ обновляется.
```

## Этап 6 — Search endpoint

Цель этапа — добавить REST API для поиска.

Endpoint:

```text
GET /api/search?q=...&page=1&pageSize=10
```

Результат:

```text
title
url
snippet
score
indexedAtUtc
```

Основные классы:

```text
api/SearchController.java
search/SearchService.java
search/dto/SearchResponse.java
search/dto/SearchResultItem.java
```

Проверка:

```bash
curl "http://localhost:8080/api/search?q=example&page=1&pageSize=10"
```

Ожидаемый результат:

```text
API возвращает найденные страницы из OpenSearch.
```

## Этап 7 — Ranking и snippet

Цель этапа — улучшить качество поисковой выдачи.

Что добавляется:

* boost для title.
* boost для h1.
* boost для description.
* обычный score для body.
* highlight/snippet.

Ranking:

```text
title       высокий boost
h1          средний boost
description средний boost
body        обычный score
```

OpenSearch query:

```text
multi_match:
  title^3
  h1^2
  description^1.5
  body^1
```

Snippet:

```text
Используется OpenSearch highlight.
Если highlight пустой, берется фрагмент из body.
```

Проверка:

```bash
curl "http://localhost:8080/api/search?q=search&page=1&pageSize=10"
```

Ожидаемый результат:

```text
Страницы с совпадением в title и h1 находятся выше.
В ответе есть snippet с подсветкой найденного текста.
```

## Этап 8 — README и финальная документация

Цель этапа — подготовить проект для защиты и портфолио.

Что добавляется:

* описание архитектуры;
* описание crawler;
* описание parser;
* описание indexer;
* описание search;
* команды запуска;
* curl-примеры;
* описание масштабирования;
* ограничения проекта.

Финальный README должен объяснять:

```text
1. Что делает проект.
2. Как запустить.
3. Как создать crawl job.
4. Как проверить статус.
5. Как выполнить поиск.
6. Как устроены PostgreSQL и OpenSearch.
7. Как масштабировать crawler и indexer.
8. Какие ограничения есть у MVP.
```

## Локальный запуск

Поднять инфраструктуру:

```bash
docker compose up -d
```

Собрать проект:

```bash
mvn clean package
```

Запустить приложение:

```bash
java -jar target/mini-search-0.0.1-SNAPSHOT.jar
```

Или:

```bash
mvn spring-boot:run
```

Проверить health:

```bash
curl http://localhost:8080/api/health
```

Проверить Swagger:

```text
http://localhost:8080/swagger-ui.html
```

Проверить OpenSearch:

```bash
curl http://localhost:9200/_cluster/health
```

## Пример будущего сценария использования

Создать задачу обхода:

```bash
curl -X POST http://localhost:8080/api/crawl \
  -H "Content-Type: application/json" \
  -d '{"startUrl":"https://example.com","maxPages":100}'
```

Получить статус задачи:

```bash
curl http://localhost:8080/api/crawl/{jobId}
```

Выполнить поиск:

```bash
curl "http://localhost:8080/api/search?q=example&page=1&pageSize=10"
```

## Масштабирование

MVP запускается как один Spring Boot application:

```text
API + crawler + parser + indexer + search
```

Для портфолио этого достаточно.

Дальше можно масштабировать так:

```text
API instance 1
API instance 2
Crawler worker 1
Crawler worker 2
Indexer worker 1
PostgreSQL
OpenSearch
RabbitMQ или Redis Streams
```

Возможное разделение:

```text
API отвечает только за REST.
Crawler workers обходят сайты.
Indexer workers пишут документы в OpenSearch.
PostgreSQL хранит состояние.
OpenSearch выполняет поиск.
Queue связывает crawler и indexer.
```

Для MVP RabbitMQ/Kafka не нужны. Их стоит добавлять только после рабочей версии crawler + parser + indexer.

## Ограничения MVP

* Jsoup не выполняет JavaScript.
* SPA-сайты могут плохо индексироваться.
* Поисковик работает только по одному сайту.
* Ограничение до 1000 страниц.
* Нет semantic search.
* Нет ML-ranking.
* Нет полноценного PageRank.
* Нет распределенного crawling в первой версии.
* Нет авторизации в первой версии.

## План разработки по дням

## День 1

* Создать Spring Boot проект.
* Добавить Maven зависимости.
* Добавить Docker Compose.
* Поднять PostgreSQL и OpenSearch.
* Добавить `/api/health`.
* Подключить Swagger.

## День 2

* Добавить Flyway.
* Создать таблицу `crawl_jobs`.
* Добавить `POST /api/crawl`.
* Добавить `GET /api/crawl/{jobId}`.

## День 3

* Добавить URL normalization.
* Добавить domain policy.
* Добавить visited URL set.
* Добавить Jsoup fetcher.

## День 4

* Добавить parser.
* Извлекать title, description, h1, canonical URL и body.
* Сохранять parsed page.

## День 5

* Добавить OpenSearch index.
* Добавить mapping.
* Добавить indexer service.
* Индексировать страницы после парсинга.

## День 6

* Добавить `/api/search`.
* Реализовать поиск по OpenSearch.
* Добавить pagination.

## День 7

* Добавить ranking.
* Добавить snippets/highlighting.
* Улучшить ошибки API.

## День 8

* Добавить README.
* Добавить curl-примеры.
* Добавить описание архитектуры.
* Подготовить demo-сценарий.

## Статус проекта

Текущий этап:

```text
Этап 1: Spring Boot каркас и инфраструктура
```

Готово:

```text
Spring Boot
Maven
PostgreSQL Docker
OpenSearch Docker
Swagger
Actuator
/api/health
```

Следующий этап:

```text
Этап 2: CrawlJob model, PostgreSQL tables, Flyway migration, POST /api/crawl, GET /api/crawl/{jobId}
```
