# Screenshots

Dieser Ordner enthält Screenshots für die Dokumentation:

## Benötigte Screenshots

1. **swagger-ui.png** - Swagger UI mit allen API Endpoints
2. **minio-console.png** - MinIO Console mit hochgeladenen JAR-Dateien
3. **docker-compose.png** - Docker Compose mit laufenden Services
4. **github-actions.png** - GitHub Actions Pipeline (grün)
5. **function-execution.png** - Erfolgreiche Function Execution

## Wie Screenshots erstellen

### Swagger UI
```bash
# Services starten
cd serverless && docker-compose up -d
# Öffne: http://localhost:8080/swagger-ui.html
```

### MinIO Console
```bash
# Öffne: http://localhost:9001
# Login: minioadmin / minioadmin
```

### Docker Compose Status
```bash
docker-compose ps
# Terminal Screenshot machen
```

### GitHub Actions
```bash
# Gehe zu: https://github.com/CC-X5/Serverless-Function-Runner/actions
# Screenshot der grünen Pipeline machen
```

---
*Platziere die Screenshots hier im PNG-Format*
