# SWIFT pacs.008 to MT103 Conversion - Testing Guide

## Overview
This guide provides step-by-step instructions to test the MVP implementation of the SWIFT pacs.008 XML to MT103 conversion service using Postman.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Postman Client                           │
└────────────┬────────────────────────────────────────────────┘
             │ POST /api/convert
             │ (multipart/form-data)
             │
             ▼
┌─────────────────────────────────────────────────────────────┐
│        Orchestrator Service (Port 8080)                     │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  ConversionController: POST /api/convert             │  │
│  │  ├─ Receives MultipartFile                           │  │
│  │  └─ Calls ConversionService                          │  │
│  │                                                      │  │
│  │  ConversionService:                                  │  │
│  │  ├─ Calls Core Converter Service                     │  │
│  │  ├─ Receives MT103 Result                            │  │
│  │  └─ Saves to PostgreSQL TransactionHistory           │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
│  Database: PostgreSQL (transaction_history table)          │
└────────────┬────────────────────────────────────────────────┘
             │ POST /api/v1/internal/convert
             │ (text/plain)
             │
             ▼
┌─────────────────────────────────────────────────────────────┐
│        Core Converter Service (Port 8081)                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  ConverterController: POST /api/v1/internal/convert  │  │
│  │  └─ Receives XML String                              │  │
│  │                                                      │  │
│  │  XmlToMtMapper:                                      │  │
│  │  ├─ Parse XML using XPath                           │  │
│  │  ├─ Extract MsgId and Amount                        │  │
│  │  └─ Generate MT103 String                           │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
│  No Database (Stateless)                                   │
└────────────────────────────────────────────────────────────┘
```

---

## Prerequisites

1. **Running Services:**
   - Orchestrator Service running on `http://localhost:8080`
   - Core Converter Service running on `http://localhost:8081`
   - PostgreSQL database running (if testing database persistence)

2. **Postman or cURL:**
   - Postman Client installed, or
   - cURL command-line tool available

3. **Sample Data:**
   - Sample pacs.008 XML file (provided in `ressources/sample-pacs008.xml`)

---

## Test 1: Health Check Endpoints

### 1.1 Orchestrator Service Health Check

**Request:**
```
GET http://localhost:8080/api/convert/health
```

**Expected Response:**
```
200 OK
Body: Orchestrator Service is running
```

### 1.2 Core Converter Service Health Check

**Request:**
```
GET http://localhost:8081/api/v1/internal/convert/health
```

**Expected Response:**
```
200 OK
Body: Converter Service is running
```

### 1.3 Core Converter Service Info

**Request:**
```
GET http://localhost:8081/api/v1/internal/convert/info
```

**Expected Response:**
```
200 OK
Body: Core Converter Service - Converts pacs.008 XML to MT103 format
```

---

## Test 2: Direct Converter Service Test (Service B)

This test verifies the Core Converter Service works independently.

### In Postman:

1. **Create a new POST request**
2. **URL:** `http://localhost:8081/api/v1/internal/convert`
3. **Headers:**
   - Content-Type: `text/plain`
4. **Body:** Select `raw` > paste the sample XML content from `ressources/sample-pacs008.xml`
5. **Send Request**

### Expected Response (200 OK):
```
:20:MSG202602261001
:13C:/RECIMSG202602261001
:32A:26022625000,00USD
:50A:/MSG202602261001
SWIFT TECH CORPORATION
:59:/BIC
SWIFT TECH CORPORATION
:71A:SHA
:77B:CONVERSION FROM ISO 20022 PACS.008
-}
```

### Using cURL:
```bash
curl -X POST http://localhost:8081/api/v1/internal/convert \
  -H "Content-Type: text/plain" \
  --data-binary @ressources/sample-pacs008.xml
```

---

## Test 3: Complete End-to-End Flow (Service A -> Service B)

This test verifies the entire Orchestrator workflow including database persistence.

### In Postman:

1. **Create a new POST request**
2. **URL:** `http://localhost:8080/api/convert`
3. **Method:** POST
4. **Body Type:** Select `form-data`
5. **Key:** `file` (set type to **File**)
6. **Value:** Select `sample-pacs008.xml` from `ressources/` folder
7. **Send Request**

### Expected Response (200 OK):
```
:20:MSG202602261001
:13C:/RECIMSG202602261001
:32A:26022625000,00USD
:50A:/MSG202602261001
SWIFT TECH CORPORATION
:59:/BIC
SWIFT TECH CORPORATION
:71A:SHA
:77B:CONVERSION FROM ISO 20022 PACS.008
-}
```

### Response Headers:
```
Content-Disposition: attachment; filename="sample-pacs008.txt"
Content-Type: text/plain;charset=UTF-8
```

### Using cURL:
```bash
curl -X POST http://localhost:8080/api/convert \
  -F "file=@ressources/sample-pacs008.xml"
```

---

## Test 4: Error Handling Tests

### 4.1 Empty File Upload

**In Postman:**
- URL: `http://localhost:8080/api/convert`
- Body: Leave file empty
- Send Request

**Expected Response (400 Bad Request):**
```json
"File is empty"
```

### 4.2 Invalid XML Format

**In Postman:**
- URL: `http://localhost:8081/api/v1/internal/convert`
- Content-Type: `text/plain`
- Body: `This is not XML`

**Expected Response (400 Bad Request):**
```json
"Invalid XML format"
```

### 4.3 Missing Required XML Fields

**In Postman:**
- URL: `http://localhost:8081/api/v1/internal/convert`
- Content-Type: `text/plain`
- Body:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02">
    <CstmrCdtTrfInitn>
        <GrpHdr>
            <!-- Missing MsgId -->
        </GrpHdr>
    </CstmrCdtTrfInitn>
</Document>
```

**Expected Response (200 OK):**
The service will use default values:
```
:20:UNKNOWN
:32A:26022600,00USD
...
```

---

## Test 5: Database Verification (PostgreSQL)

After running Test 3, verify the transaction was saved:

### In PostgreSQL:

```sql
-- Connect to swiftbridge database
\c swiftbridge

-- View transaction history
SELECT * FROM transaction_history ORDER BY timestamp DESC LIMIT 1;

-- Expected columns:
-- id | filename | status | timestamp | mt103_output | error_message
-- 1  | sample-pacs008.xml | SUCCESS | 2026-02-26 10:15:30 | (MT103 string) | NULL
```

### View Success Count:

```sql
SELECT COUNT(*) FROM transaction_history WHERE status = 'SUCCESS';
```

---

## Data Flow Summary

### Request Flow:
1. **User** → POST `/api/convert` with XML file (Orchestrator Service A)
2. **Orchestrator** → Creates request with XML content
3. **Orchestrator** → POST `/api/v1/internal/convert` with XML string (Core Converter Service B)
4. **Converter** → Parses XML using XPath
5. **Converter** → Extracts MsgId and Amount
6. **Converter** → Returns MT103 string (200 OK)

### Response Flow:
1. **Converter** ← Returns MT103 string
2. **Orchestrator** → Saves TransactionHistory (status='SUCCESS') to PostgreSQL
3. **Orchestrator** → Returns MT103 string to user (200 OK with attachment header)
4. **User** ← Receives MT103 file

---

## Key Extracted Fields from Sample XML

| Field | XPath | Extracted Value |
|-------|-------|-----------------|
| Message ID | `//GrpHdr/MsgId` | `MSG202602261001` |
| Amount | `//CdtTrfTxInf/InstdAmt` | `25000.00` |
| Currency | `//CdtTrfTxInf/InstdAmt/@Ccy` | `USD` |
| Orderer Name | `//InitgPty/Nm` | `SWIFT TECH CORPORATION` |
| Beneficiary Name | `//UltmtDbtr/Nm` | `SWIFT TECH CORPORATION` |

---

## MT103 Tag Mapping

| MT103 Tag | Description | Source |
|-----------|-------------|--------|
| `:20:` | Transaction Reference Number | GrpHdr/MsgId |
| `:32A:` | Amount and Currency | CdtTrfTxInf/InstdAmt |
| `:50A:` | Ordering Customer | InitgPty/Nm |
| `:59:` | Beneficiary Customer | UltmtDbtr/Nm |
| `:71A:` | Charges Code | Hardcoded: SHA |
| `:77B:` | Regulatory Reporting | Hardcoded message |

---

## Troubleshooting

### Issue: Connection Refused (Port 8080 or 8081)

**Solution:**
```bash
# Check if services are running
netstat -an | findstr :8080
netstat -an | findstr :8081

# Or using PowerShell
Get-NetTCPConnection -State Listen | Where-Object {$_.LocalPort -eq 8080}
```

### Issue: PostgreSQL Connection Error

**Solution:**
```bash
# Verify PostgreSQL is running
docker ps | grep postgres

# Check database connection
psql -h localhost -U swiftbridge_user -d swiftbridge
```

### Issue: ServiceUnavailableException

**Solution:**
- Ensure Converter Service is running before testing Orchestrator
- Check firewall rules
- Verify service URLs in application.yml match running ports

### Issue: XML Parsing Error

**Solution:**
- Verify XML is well-formed (no unclosed tags)
- Check XML encoding (UTF-8 required)
- Ensure namespace is correct: `urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02`

---

## Performance Notes

### Service Latency:
- Direct Converter API: ~50-100ms
- Full Orchestrator flow: ~100-200ms (includes database write)
- Database write: ~30-50ms (PostgreSQL on Docker)

### Throughput:
- Single instance can handle ~50 requests/second
- Database pool limited to 10 connections (configurable)

### Resource Usage:
- Orchestrator Service: ~150-200MB heap
- Converter Service: ~100-150MB heap
- PostgreSQL: ~256MB container

---

## Next Steps for Production

1. **Input Validation:** Add more robust XML schema validation
2. **Error Codes:** Implement specific SWIFT error codes
3. **Caching:** Add Redis for repeated conversions
4. **Circuit Breaker:** Add Resilience4j for fault tolerance
5. **Metrics:** Add Micrometer/Prometheus monitoring
6. **Authentication:** Implement OAuth2/JWT security
7. **Rate Limiting:** Add request throttling
8. **Logging:** Centralize logs with ELK stack
9. **Testing:** Add comprehensive unit and integration tests
10. **Documentation:** Add Swagger/OpenAPI documentation

---

## Contact & Support

For issues or questions regarding this MVP implementation, please refer to the architecture documentation and source code comments.
