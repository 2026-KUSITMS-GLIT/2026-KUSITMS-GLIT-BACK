# Local Monitoring Stack

Prometheus + Grafana 로컬 모니터링 스택.

## 사전 조건

- Spring 서버가 `local` 프로파일로 실행 중이어야 함 (`http://localhost:8080`)
- Docker Desktop 실행 중

## 실행

```bash
cd monitoring
docker compose up -d
```

| 서비스     | URL                    | 계정           |
|-----------|------------------------|---------------|
| Prometheus | http://localhost:9090  | -             |
| Grafana    | http://localhost:3001  | admin / admin |

## 4701 대시보드 import

1. Grafana 접속 → 좌측 메뉴 **Dashboards → Import**
2. **Import via grafana.com** 입력란에 `4701` 입력 → **Load**
3. Datasource를 `Prometheus` 선택 → **Import**

## 종료

```bash
docker compose down
```

볼륨까지 삭제하려면:

```bash
docker compose down -v
```
