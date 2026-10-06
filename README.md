# cvm-recommendation-service

Hobby microservice: `POST /api/v1/get-offers` → gets a Kong token → calls Kong `POST /offers/recommendations` → transforms the response.

```
client ──► offers-service ──► (Kong) POST /oauth/token
                         └──► (Kong) POST /offers/recommendations   (Bearer token)
```

Kong is a dummy: `mock-kong` is a tiny Spring Boot app with the same two endpoints. Switching to real Kong only needs `KONG_BASE_URL` (and credentials) to change.

| Folder | What |
|---|---|
| [offers-service](offers-service) | The real microservice (Spring Boot 3.5, Java 17), port 8080 |
| [mock-kong](mock-kong) | Dummy Kong, port 9090 locally, 8080 on OpenShift |
| [openshift](openshift) | ConfigMap + Secrets YAML |

## API

`POST /api/v1/get-offers`

```json
{ "msisdn": "628970196650" }
```

Success (`data` is an array sorted by Kong `order`):

```json
{ "status": true, "message": "Success", "code": 1,
  "data": [ { "offerId": "234589222", "offerShortDesc": "10GB 28hari 40rb", "registrationKey": "RCRP1MB30D50K",
              "validity": "30", "Description": "...", "retailerIncentive": "2000", "starred": "true",
              "bestDeal": "true", "cashbackValue": "0", "order": 1, "price": 38000 } ] }
```

| Case | HTTP | Body |
|---|---|---|
| Invalid msisdn / bad JSON | 400 | `status:false, code:0` |
| Kong status != 200 or no offers | 200 | `status:false, code:0, data:[]` |
| Kong 4xx/5xx (after one 401 retry) | 502 | `status:false, code:0` |
| Kong timeout/unreachable | 504 | `status:false, code:0` |

Mapping (Kong → BE): `id→offerId`, `shortMessage→offerShortDesc`, `order`, `price` as is, and the first value of `additional-params`: `registrationKey`, `validity`, `Description`, `retailerIncentive`, `starred`, `bestDeal`, `cashbackValue`.
Kong request: `transid` = `yyyyMMddHHmmssSSS` + 3 random digits, `servicedata.lang` = `EN`, `servicedata.channel` = `3id` (both configurable).

Swagger UI: `/swagger-ui.html` · OpenAPI JSON: `/v3/api-docs` · Health: `/actuator/health/readiness`.

## Configuration (env vars, offers-service)

| Variable | Default |
|---|---|
| `KONG_BASE_URL` | `http://localhost:9090` |
| `KONG_TOKEN_PATH` | `/oauth/token` |
| `KONG_RECOMMENDATIONS_PATH` | `/offers/recommendations` |
| `KONG_CLIENT_ID` / `KONG_CLIENT_SECRET` | `demo-client` / `demo-secret` |
| `KONG_LANG` / `KONG_CHANNEL` | `EN` / `3id` |
| `KONG_CONNECT_TIMEOUT_MS` / `KONG_READ_TIMEOUT_MS` | `3000` / `5000` |

mock-kong: `MOCK_CLIENT_ID`, `MOCK_CLIENT_SECRET`, `MOCK_TOKEN_TTL_SECONDS` (default `300`).

## Run locally (JDK 17 + Maven)

```powershell
cd mock-kong;     mvn spring-boot:run          # terminal 1 (port 9090)
cd offers-service; mvn spring-boot:run         # terminal 2 (port 8080)
curl.exe -X POST http://localhost:8080/api/v1/get-offers -H "Content-Type: application/json" -d "{\"msisdn\":\"628970196650\"}"
```

Tests: `mvn test` in each folder.

## Deploy on OpenShift Developer Sandbox (browser only, no oc)

1. Push this repo to GitHub.
2. Console → Developer perspective → select your `<user>-dev` project → **+Add → Import YAML** → paste [openshift/config.yaml](openshift/config.yaml) → Create.
3. **+Add → Import from Git**
   - Git repo URL: your repo; **Context dir** `/mock-kong`; build strategy **Dockerfile**.
   - Name `mock-kong`; target port `8080`; uncheck *Create a route*.
   - Deployment → Environment → add from Secret `mock-kong-secret`.
4. **+Add → Import from Git** again
   - **Context dir** `/offers-service`; strategy **Dockerfile**; name `offers-service`; target port `8080`; keep *Create a route* checked (Secure route, edge TLS).
   - Deployment → Environment → add from ConfigMap `offers-service-config` and Secret `offers-service-secret`.
5. Wait for both builds (Builds → Logs) and pods to be Running. Optional: set readiness probe to `/actuator/health/readiness` and liveness to `/actuator/health/liveness` on port 8080.
6. Test: `curl.exe -X POST https://<offers-service-route>/api/v1/get-offers -H "Content-Type: application/json" -d "{\"msisdn\":\"628970196650\"}"`, or open `https://<route>/swagger-ui.html`.

Troubleshooting: if the Dockerfile build fails, re-import choosing **Builder Image → Java (Red Hat OpenJDK 17)** instead; set `MAVEN_ARGS_APPEND=-DskipTests` and `ARTIFACT_DIR=target`. Sandbox pods are scaled down after ~12h idle; scale them up again from the Topology view.
