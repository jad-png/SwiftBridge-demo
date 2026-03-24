# Backend API Contract Audit (March 24, 2026)

Scope reviewed:
- Orchestrator controllers, services, repositories, entities, DTOs
- Core converter API surface used by orchestrator

Key sources:
- [Orchestrator-Service/src/main/java/com/swiftbridge/orchestrator/controller/ConversionController.java](Orchestrator-Service/src/main/java/com/swiftbridge/orchestrator/controller/ConversionController.java)
- [Orchestrator-Service/src/main/java/com/swiftbridge/orchestrator/controller/ConversionHistoryController.java](Orchestrator-Service/src/main/java/com/swiftbridge/orchestrator/controller/ConversionHistoryController.java)
- [Orchestrator-Service/src/main/java/com/swiftbridge/orchestrator/controller/AdminController.java](Orchestrator-Service/src/main/java/com/swiftbridge/orchestrator/controller/AdminController.java)
- [Orchestrator-Service/src/main/java/com/swiftbridge/orchestrator/controller/UserController.java](Orchestrator-Service/src/main/java/com/swiftbridge/orchestrator/controller/UserController.java)
- [Orchestrator-Service/src/main/java/com/swiftbridge/orchestrator/dto/ConversionResponse.java](Orchestrator-Service/src/main/java/com/swiftbridge/orchestrator/dto/ConversionResponse.java)
- [Orchestrator-Service/src/main/java/com/swiftbridge/orchestrator/entity/TransactionHistory.java](Orchestrator-Service/src/main/java/com/swiftbridge/orchestrator/entity/TransactionHistory.java)
- [Orchestrator-Service/src/main/java/com/swiftbridge/orchestrator/repository/TransactionHistoryRepository.java](Orchestrator-Service/src/main/java/com/swiftbridge/orchestrator/repository/TransactionHistoryRepository.java)
- [Orchestrator-Service/src/main/resources/db/migration/V1__create_transaction_history_metadata_schema.sql](Orchestrator-Service/src/main/resources/db/migration/V1__create_transaction_history_metadata_schema.sql)

---

Endpoint: /api/users/stats
Status: ❌ Missing
Issues:
- Endpoint does not exist in current controllers. User API is under `/api/v1/users` and exposes list/profile/update/delete only.
- No DTO matching required payload fields:
  - `totalConversions`
  - `successfulConversions`
  - `failedConversions`
  - `successRate`
  - `recentActivity[]` (`id`, `status`, `createdAt`)
  - `activityTrend[]` (`day`, `total`, `success`, `successRate`)
- Current history model has no user linkage (`transaction_history` has no `user_id`), so per-user stats cannot be computed correctly from persisted data.

Suggested Fix:
- Add `GET /api/users/stats` endpoint (prefer versioning strategy alignment, e.g., `/api/users/stats` or `/api/v1/users/stats` based on frontend contract).
- Add dedicated DTOs for `UserStatsResponse`, `RecentActivityItem`, `ActivityTrendItem`.
- Add data model linkage between conversion history and authenticated user (e.g., `user_id` in `transaction_history`) and populate it during conversion logging.
- Add repository queries grouped by user and day to compute trend metrics.

---

Endpoint: /api/admin/stats
Status: ⚠️ Partial
Issues:
- Similar route exists only as `GET /api/admin` (not `/api/admin/stats`).
- Current response is a generic map with `message` and `username`, not contract payload.
- Missing `metrics` object fields:
  - `metrics.totalUsers`
  - `metrics.totalConversions`
  - `metrics.totalSuccessfulConversions`
  - `metrics.conversionSuccessRate`
  - `metrics.totalGuests`
- Missing admin analytics collections required by frontend:
  - `roleDistribution[]` (`role`, `count`, `percentage`)
  - `conversionVolume[]` (`day`, `total`, `success`, `successRate`)
  - `successRateTrend[]` (`day`, `successRate`, `total`)
- `totalGuests` cannot be computed from current role model (`AppRole` has only `ROLE_USER`, `ROLE_ADMIN`).

Suggested Fix:
- Add/rename endpoint to `GET /api/admin/stats`.
- Return typed DTO with nested `metrics` and analytics arrays (avoid `Map<String,Object>`).
- Add repository aggregation queries for user counts by role and conversion metrics by day.
- Define how `guests` are represented (new role or separate tracking source), then implement `totalGuests` logic.

---

Endpoint: /api/convert
Status: ⚠️ Partial
Issues:
- Endpoint exists (`/api/convert` and `/convert`) and conversion flow works.
- Response includes:
  - `processingTimeMs` ✅
  - `warnings[]` ✅
  - `mt103` ✅
- Missing field in response:
  - `messageReference` ❌
- `messageReference` is currently only persisted in history (`saveSuccess/saveFailureInNewTransaction`) and not returned to client.

Suggested Fix:
- Extend conversion response DTO to include `messageReference`.
- Populate `messageReference` from validation output (`instructionId`) or extracted business reference from MT103 mapping logic.
- Keep existing fields unchanged for backward compatibility.

---

Endpoint: /api/history
Status: ⚠️ Partial
Issues:
- Endpoint exists via `GET /api/history` (also aliased as `/api/conversions`).
- Required fields are present in returned entity for each item:
  - `conversionStatus` ✅
  - `requestTimestamp` ✅
  - `processingDurationMs` ✅
  - `messageType` ✅
- Response shape mismatch:
  - Returns raw Spring `Page<TransactionHistory>` serialization.
  - Contract requires `pagination.total`; current Spring page exposes `totalElements` (and other paging keys) at top level, not `pagination.total`.
- Returning JPA entity directly couples API to internal schema and may expose extra/internal fields (`id`, `transactionId`, `messageReference`).

Suggested Fix:
- Introduce `HistoryListResponse` DTO:
  - `items[]` mapped to contract fields
  - `pagination.total` (plus optional page/size if needed)
- Map `Page<TransactionHistory>` into DTO in service/controller layer.

---

Endpoint: /api/history/{txnId}
Status: ⚠️ Partial
Issues:
- Route exists as `GET /api/history/{id}` (path variable type `Long`) and fetches by DB primary key.
- Contract expects `{txnId}`; current API does not expose lookup by business transaction id.
- Field availability in response entity includes required set:
  - `conversionStatus` ✅
  - `requestTimestamp` ✅
  - `processingDurationMs` ✅
  - `messageType` ✅
- Naming/identifier mismatch can break frontend tracking flows expecting transaction identifier semantics.

Suggested Fix:
- Add endpoint `GET /api/history/{txnId}` where `txnId` is transaction/business id (`transaction_id`).
- Add repository method `findByTransactionId(String txnId)`.
- Return a DTO (not entity) aligned with frontend contract.
- Optionally preserve existing `/{id}` as legacy route if needed.

---

## Frontend-impact inconsistencies

- Path mismatch: frontend contract requires `/api/users/stats` and `/api/admin/stats`, backend currently serves `/api/v1/users` and `/api/admin`.
- Identifier mismatch in history details: frontend expects transaction id lookup (`txnId`), backend uses numeric DB id.
- Payload mismatch for list pagination: frontend expects `pagination.total`, backend returns Spring Page default JSON shape.
- Missing analytics/statistics endpoints and aggregations prevent dashboard rendering.
- Missing `messageReference` in convert response blocks traceability between conversion result and history timeline.

## Overall contract coverage summary

- ✅ Implemented: core `/api/convert` execution and core conversion payload fields except one; `/api/history` and `/api/history/{id}` base retrieval.
- ⚠️ Partial: `/api/convert`, `/api/history`, `/api/history/{txnId}`, `/api/admin/stats` (route-adjacent but wrong payload/path).
- ❌ Missing: `/api/users/stats` and all user/admin analytics data model + response contracts required by frontend.
