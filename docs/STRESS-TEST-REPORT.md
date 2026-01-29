# Stress Test Report — Serverless Function Runner

> Automatisierter Lasttest der REST API

## Testbedingungen

| Parameter | Wert |
|-----------|------|
| Datum | 2026-01-29 16:11:27 |
| Dauer | 46 Sekunden |
| Betriebssystem | Windows-10-10.0.19045-SP0 |
| Prozessor | AMD64 Family 25 Model 97 Stepping 2, AuthenticAMD |
| Python | 3.12.3 |
| Docker Services | PostgreSQL 16, MinIO, RabbitMQ 3, Registry (8080), Executor (8081), Gateway (8082) |
| Memory Limits | Registry 512M, Executor 768M, Gateway 384M |

## Test-Szenarien

1. **Health Check** — GET /actuator/health (Baseline, 50 Requests)
2. **List Functions** — GET /api/v1/functions (50 Requests)
3. **Execute Functions** — POST /api/v1/execute (hello, reverse, sum — je 10 Requests)
4. **Concurrent Load** — 10, 25, 50, 100 gleichzeitige Requests
5. **Sustained Load** — 200 Requests @ ~20 req/s

## Ergebnisse

| Szenario | Requests | Avg (ms) | Min (ms) | Max (ms) | P95 (ms) | P99 (ms) | Req/s | Fehlerrate |
|----------|----------|----------|----------|----------|----------|----------|-------|------------|
| Health Check | 50 | 1.2 | 1.1 | 1.7 | 1.5 | 1.6 | 667.2 | 0.0% |
| List Functions | 50 | 2.0 | 1.7 | 3.0 | 2.6 | 3.0 | 453.8 | 0.0% |
| Execute Functions | 30 | 381.9 | 339.8 | 421.8 | 415.1 | 420.9 | 2.7 | 0.0% |
| Concurrent-10 | 10 | 1028.1 | 927.0 | 1083.0 | 1081.6 | 1082.7 | 1665.0 | 0.0% |
| Concurrent-25 | 25 | 1981.4 | 1481.0 | 2256.8 | 2233.0 | 2251.4 | 1388.8 | 0.0% |
| Concurrent-50 | 50 | 3819.7 | 2177.3 | 4259.0 | 4253.7 | 4257.9 | 1515.1 | 0.0% |
| Concurrent-100 | 100 | 8272.5 | 6244.9 | 9336.8 | 9168.5 | 9309.9 | 1098.6 | 0.0% |
| Sustained Load | 200 | 3210.4 | 1916.1 | 4199.9 | 4049.0 | 4195.9 | 13.2 | 0.0% |

### Zusammenfassung

- **Gesamt-Requests:** 515
- **Gesamt-Fehler:** 0 (0.0%)
- **Testdauer:** 46s

## Diagramme

### 01 Endpoint Response Times

![01_endpoint_response_times](stress-test-results/01_endpoint_response_times.png)

### 02 Latency Vs Concurrency

![02_latency_vs_concurrency](stress-test-results/02_latency_vs_concurrency.png)

### 03 Sustained Over Time

![03_sustained_over_time](stress-test-results/03_sustained_over_time.png)

### 04 Success Vs Error

![04_success_vs_error](stress-test-results/04_success_vs_error.png)

### 05 Throughput

![05_throughput](stress-test-results/05_throughput.png)

## Fazit

Der Stress Test hat die Serverless Function Runner API mit insgesamt **515 Requests**
getestet. Die Fehlerrate lag bei **0.0%**.

**Beobachtungen:**

- **Health Check / List Functions:** Diese Lightweight-Endpoints antworten schnell und skalieren
  gut, da sie keine Container-Erstellung erfordern.
- **Function Execution:** Die Ausfuehrung von Funktionen dauert laenger, da fuer jede Ausfuehrung
  ein Docker-Container erstellt, gestartet und wieder entfernt wird.
- **Concurrent Load:** Bei steigender Parallelitaet erhoehen sich die Antwortzeiten, da der
  Executor-Service mehrere Docker-Container gleichzeitig verwalten muss.
- **Sustained Load:** Ueber laengere Zeit bleibt das System stabil, wobei die Antwortzeiten
  von der Container-Erstellung dominiert werden.

---

*Generiert am 2026-01-29 16:12:14 mit `stress-test.py`*
