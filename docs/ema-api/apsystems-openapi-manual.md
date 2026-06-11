# APsystems OpenAPI User Manual

**Version:** V1.8 (2025-07-18)  
**Source:** `Apsystems_OpenAPI_User_Manual_End_User_EN.pdf` in this directory

## Version History

| Version | Date | Changes |
|---|---|---|
| V1.0 | 2022-09-16 | First Document |
| V1.1 | 2023-03-24 | Edit token URL; change refresh token expiration time |
| V1.2 | 2023-10-07 | Change JWT Token auth to signature auth |
| V1.3 | 2023-10-31 | Add meter interface |
| V1.5 | 2023-11-17 | Add inverter-level data API |
| V1.6 | 2024-02-07 | Optimize interface for end user |
| V1.7 | 2025-04-17 | Add Storage-level Data API |
| V1.8 | 2025-07-18 | Adapt to the shared sub user system |

---

## 1. Overview

REST API delivering JSON over HTTPS. Six endpoint categories:

- System Details API
- System-level Data API
- ECU-level Data API
- Meter-level Data API
- Inverter-level Data API
- Storage-level Data API *(added V1.7)*

---

## 2. Authentication & Authorization

### 2.1 Obtaining Credentials

Email APsystems to register an OpenAPI account. Provide who you are, why you need access, and what you'll do with the data. You'll receive:

| Parameter | Type | Description |
|---|---|---|
| App Id | string | 32-character alphanumeric, unique per account, immutable |
| App Secret | string | 12-character alphanumeric, changeable via email to APsystems |

**Keep credentials confidential.**

Credentials are also available in the [APsystems dashboard](https://apsystemsema.com/apsystems/web/setting/personalSetting/openAPIService) under **OpenAPI Settings**.

### 2.2 Request Signing

Every request requires HMAC signing. Include these headers on every request:

| Header | Required | Description |
|---|---|---|
| `X-CA-AppId` | Y | App Id |
| `X-CA-Timestamp` | Y | Unix timestamp in **milliseconds** |
| `X-CA-Nonce` | Y | UUID without dashes, 32 chars (e.g. `5e36eab8295911ee90751eff13c2920b`) |
| `X-CA-Signature-Method` | Y | `HmacSHA256` or `HmacSHA1` |
| `X-CA-Signature` | Y | Computed signature (see below) |

**Computing the signature:**

```
# Step 1: Build the string to sign
stringToSign = X-CA-Timestamp + "/" + X-CA-Nonce + "/" + X-CA-AppId + "/" + RequestPath + "/" + HTTPMethod + "/" + X-CA-Signature-Method
```

`RequestPath` is the **last path segment** of the URL after substituting all path parameters.  
Example: `GET /user/api/v2/systems/details/XYZ123` → `RequestPath = XYZ123`

```java
// Step 2: Compute HmacSHA256 (Java)
Mac hmacSha256 = Mac.getInstance("HmacSHA256");
byte[] appSecretBytes = appSecret.getBytes(Charset.forName("UTF-8"));
hmacSha256.init(new SecretKeySpec(appSecretBytes, 0, appSecretBytes.length, "HmacSHA256"));
byte[] result = hmacSha256.doFinal(stringToSign.getBytes(Charset.forName("UTF-8")));
String signature = Base64.getEncoder().encodeToString(result);
```

### 2.3 Base URL

```
https://api.apsystemsema.com:9282
```

All paths are prefixed with:
- `/user/api/v2/` — end-user data (System, ECU, Meter, Inverter)
- `/installer/api/v2/` — installer data (Storage)

### 2.4 Authorization & Billing

Default data access is included with AppId + AppSecret. Additional data categories require separate authorization. **API usage is billed based on access count and data range.**

---

## 3. API Reference

### 3.1 System Details API

#### 3.1.1 Get System Details

```
GET /user/api/v2/systems/details/{sid}
```

| Parameter | Required | Description |
|---|---|---|
| sid | Y | System identifier |

**Response:**

```json
{
  "code": 0,
  "data": {
    "sid": "AZ12649A3DFF",
    "create_date": "2022-09-01",
    "capacity": "1.28",
    "type": 1,
    "timezone": "Asia/Shanghai",
    "ecu": ["203000001234"],
    "light": 1,
    "authorization_code": "ff80808155..."
  }
}
```

| Field | Type | Description |
|---|---|---|
| sid | string | System identifier |
| create_date | string (yyyy-MM-dd) | Registration date in EMA |
| capacity | string | System size in kW |
| type | int | `1`=PV, `2`=Storage, `3`=PV+Storage |
| timezone | string | ECU timezone |
| ecu | list | ECU ids. **Shared sub-user format:** `2030000001236-002405253708` — main ECU before `-`, virtual ECU after. Use the virtual ECU id in subsequent device queries. |
| light | int | `1`=Green (normal), `2`=Yellow (inverter alarms), `3`=Red (ECU network issue), `4`=Grey (no data yet) |
| authorization_code | string | Generated when "Allow visitors to access this system" is enabled; used for embedding the EMA portal |

---

#### 3.1.2 Get System Inverters

```
GET /user/api/v2/systems/inverters/{sid}
```

**Response:**

```json
{
  "code": 0,
  "data": [{
    "eid": "203000001234",
    "type": 0,
    "timezone": "Asia/Shanghai",
    "model": null,
    "capacity": null,
    "inverter": [
      {"uid": "902000001234", "type": "QT2D"},
      {"uid": "902000001235", "type": "QT2D"}
    ]
  }]
}
```

**ECU `type` values:** `0`=ECU, `1`=ECU with meter, `2`=ECU with storage  
`model` and `capacity` (kWh) are populated for storage-activated ECUs.

---

#### 3.1.3 Get System Meters

```
GET /user/api/v2/systems/meters/{sid}
```

**Response:** `data` is a list of meter ECU ids.

```json
{"code": 0, "data": ["203000001234"]}
```

---

### 3.2 System-level Data API

#### 3.2.1 System Energy Summary

```
GET /user/api/v2/systems/summary/{sid}
```

**Response:** All values in kWh.

```json
{"code": 0, "data": {"today": "12.28", "month": "12.28", "year": "12.28", "lifetime": "12.28"}}
```

---

#### 3.2.2 System Energy in Period

```
GET /user/api/v2/systems/energy/{sid}?energy_level=daily&date_range=2025-07
```

| Parameter | Required | Description |
|---|---|---|
| sid | Y | System id |
| energy_level | Y | `hourly`, `daily`, `monthly`, or `yearly` |
| date_range | N | Required for all except `yearly` |

**date_range format by energy_level:**

| energy_level | Format | Example |
|---|---|---|
| hourly | yyyy-MM-dd | 2025-07-20 |
| daily | yyyy-MM | 2025-07 |
| monthly | yyyy | 2025 |
| yearly | (omit) | — |

**Response:** `data` is an array of kWh strings.

| energy_level | Array length |
|---|---|
| hourly | 24 (one per hour, 0–23) |
| daily | days in the month |
| monthly | 12 |
| yearly | years since installation |

```json
{"code": 0, "data": ["567.23", "550.32", "320.12"]}
```

---

### 3.3 ECU-level Data API

#### 3.3.1 ECU Energy Summary

```
GET /user/api/v2/systems/{sid}/devices/ecu/summary/{eid}
```

| Parameter | Required | Description |
|---|---|---|
| sid | Y | System id |
| eid | Y | ECU id |

**Response:** Same shape as system summary — `{today, month, year, lifetime}` in kWh.

---

#### 3.3.2 ECU Energy in Period

```
GET /user/api/v2/systems/{sid}/devices/ecu/energy/{eid}?energy_level=minutely&date_range=2025-07-20
```

| Parameter | Required | Description |
|---|---|---|
| sid | Y | System id |
| eid | Y | ECU id |
| energy_level | Y | `minutely`, `hourly`, `daily`, `monthly`, `yearly` |
| date_range | N | See system energy for formats; `minutely` uses yyyy-MM-dd |

**Response — non-minutely:** Flat array of kWh strings (same shape as system energy).

**Response — minutely (5-minute power telemetry):**

```json
{
  "code": 0,
  "data": {
    "today": "18.90",
    "time": ["05:55", "06:00", "06:05"],
    "power": [0, 20, 19],
    "energy": ["0.00", "0.00", "0.00"]
  }
}
```

`time`, `power`, and `energy` are parallel arrays. Length varies (only intervals with data included). `power` in W, `energy` in kWh per 5-minute interval.

---

### 3.4 Meter-level Data API

#### 3.4.1 Meter Energy Summary

```
GET /user/api/v2/systems/{sid}/devices/meter/summary/{eid}
```

| Parameter | Required | Description |
|---|---|---|
| sid | Y | System id |
| eid | Y | Meter ECU id |

**Response:**

```json
{
  "code": 0,
  "data": {
    "today":    {"consumed": "394.41", "exported": "0.00", "imported": "560.52", "produced": "833.88"},
    "month":    {"consumed": "394.41", "exported": "0.00", "imported": "560.52", "produced": "833.88"},
    "year":     {"consumed": "6394.41", "exported": "0.00", "imported": "4560.52", "produced": "1833.88"},
    "lifetime": {"consumed": "6394.46", "exported": "0.00", "imported": "4561.64", "produced": "1833.89"}
  }
}
```

All values in kWh.

---

#### 3.4.2 Meter Energy in Period

```
GET /user/api/v2/systems/{sid}/devices/meter/period/{eid}?energy_level=daily&date_range=2025-07
```

**Parameters:** `sid`, `eid`, `energy_level` (`minutely`, `hourly`, `daily`, `monthly`, `yearly`), `date_range`

**Response — non-minutely levels:**

```json
{
  "code": 0,
  "data": {
    "time":     ["01", "02"],
    "produced": ["40.300", "50.016"],
    "consumed": ["40.300", "50.016"],
    "imported": ["40.300", "50.016"],
    "exported": ["40.300", "50.016"]
  }
}
```

**Response — minutely level:**

```json
{
  "code": 0,
  "data": {
    "today": {"consumed": "5.997", "exported": "0.072", "imported": "3.712", "produced": "2.356"},
    "time": ["23:57"],
    "power": {
      "consumed": ["167.96"],
      "imported_exported": ["167.96"],
      "produced": ["0.00"]
    },
    "energy": {
      "consumed": ["0.015620"],
      "exported": ["0"],
      "imported": ["0.01562"],
      "produced": ["0.00000"]
    }
  }
}
```

---

### 3.5 Inverter-level Data API

#### 3.5.1 Inverter Energy Summary

```
GET /user/api/v2/systems/{sid}/devices/inverter/summary/{uid}
```

| Parameter | Required | Description |
|---|---|---|
| sid | Y | System id |
| uid | Y | Inverter id |

**Response — per-channel fields** (`{period}{channel}` naming):

```json
{
  "code": 0,
  "data": {
    "d1": "12.28", "m1": "12.28", "y1": "12.28", "t1": "12.28",
    "d2": "12.28", "m2": "12.28", "y2": "12.28", "t2": "12.28",
    "d3": "12.28", "m3": "12.28", "y3": "12.28", "t3": "12.28",
    "d4": "12.28", "m4": "12.28", "y4": "12.28", "t4": "12.28"
  }
}
```

Period prefix: `d`=today, `m`=this month, `y`=this year, `t`=lifetime  
Channel suffix: `1`–`4` (varies by inverter model)  
All values in kWh.

---

#### 3.5.2 Inverter Energy in Period

```
GET /user/api/v2/systems/{sid}/devices/inverter/energy/{uid}?energy_level=hourly&date_range=2025-07-20
```

**Parameters:** `sid`, `uid`, `energy_level` (`minutely`, `hourly`, `daily`, `monthly`, `yearly`), `date_range`

**Response — non-minutely levels:** Per-channel arrays.

```json
{
  "code": 0,
  "data": {
    "e1": ["567.23", "550.32", "320.12"],
    "e2": ["567.23", "550.32", "320.12"]
  }
}
```

**Response — minutely level:** Full DC/AC telemetry.

```json
{
  "code": 0,
  "data": {
    "t":    ["06:00", "06:05"],
    "dc_p1": [...], "dc_p2": [...], "dc_p3": [...], "dc_p4": [...],
    "dc_i1": [...], "dc_i2": [...], "dc_i3": [...], "dc_i4": [...],
    "dc_v1": [...], "dc_v2": [...], "dc_v3": [...], "dc_v4": [...],
    "dc_e1": [...], "dc_e2": [...], "dc_e3": [...], "dc_e4": [...],
    "ac_v1": [...], "ac_v2": [...], "ac_v3": [...],
    "ac_t":  [...],
    "ac_p":  [...],
    "ac_f":  [...]
  }
}
```

| Field | Description |
|---|---|
| `t` | Time list (HH:mm) |
| `dc_p{n}` | DC Power on channel n (W) |
| `dc_i{n}` | DC Current on channel n |
| `dc_v{n}` | DC Voltage on channel n |
| `dc_e{n}` | DC Energy on channel n |
| `ac_v{n}` | AC Voltage on channel n |
| `ac_t` | AC Temperature |
| `ac_p` | AC Power |
| `ac_f` | AC Frequency |

---

#### 3.5.3 Batch Inverter Energy for ECU

```
GET /user/api/v2/systems/{sid}/devices/inverter/batch/energy/{eid}?energy_level=power&date_range=2025-07-24
```

| Parameter | Required | Description |
|---|---|---|
| sid | Y | System id |
| eid | Y | ECU id |
| energy_level | Y | `power` or `energy` |
| date_range | Y | yyyy-MM-dd |

**Response — energy_level=energy:**

```json
{
  "code": 0,
  "data": {
    "energy": ["701000001234-1-1.24", "701000001234-2-0.95"]
  }
}
```

Format per entry: `"{uid}-{channel}-{kWh}"`

**Response — energy_level=power:**

```json
{
  "code": 0,
  "data": {
    "time": ["06:00", "06:05"],
    "power": {
      "701000001234-1": [45, 56],
      "701000001234-2": [30, 45]
    }
  }
}
```

`power` map keys are `"{uid}-{channel}"`. Array lengths match `time`.

---

### 3.6 Storage-level Data API

> Storage endpoints use the **`/installer/api/v2/`** path prefix (not `/user/api/v2/`).

#### 3.6.1 Get Latest Storage Power

```
GET /installer/api/v2/systems/{sid}/devices/storage/latest/{eid}
```

| Parameter | Required | Description |
|---|---|---|
| sid | Y | System id |
| eid | Y | Storage ECU id |

**Response:**

```json
{
  "code": 0,
  "data": {
    "mode":      "4",
    "soc":       "97",
    "time":      "23:57",
    "discharge": "394.408",
    "charge":    "0.000",
    "produced":  "560.523",
    "consumed":  "560.523",
    "exported":  "560.523",
    "imported":  "833.884"
  }
}
```

| Field | Description |
|---|---|
| mode | Storage operation mode |
| soc | Battery State of Charge (%) |
| discharge | Last discharge power (W) |
| charge | Last charge power (W) |
| produced | Last produced power (W) |
| consumed | Last consumed power (W) |
| exported | Last exported power (W) |
| imported | Last imported power (W) |

---

#### 3.6.2 Storage Energy Summary

```
GET /installer/api/v2/systems/{sid}/devices/storage/summary/{eid}
```

**Response:**

```json
{
  "code": 0,
  "data": {
    "today":    {"discharge": "394.408", "charge": "0.000", "produced": "560.523", "consumed": "560.523", "exported": "560.523", "imported": "833.884"},
    "month":    {"discharge": "394.408", "charge": "0.000", "produced": "560.523", "consumed": "560.523", "exported": "560.523", "imported": "833.884"},
    "year":     {"discharge": "394.408", "charge": "0.000", "produced": "560.523", "consumed": "560.523", "exported": "560.523", "imported": "833.884"},
    "lifetime": {"discharge": "394.408", "charge": "0.000", "produced": "560.523", "consumed": "560.523", "exported": "560.523", "imported": "833.884"}
  }
}
```

All values in kWh.

---

#### 3.6.3 Storage Energy in Period

```
GET /installer/api/v2/systems/{sid}/devices/storage/period/{eid}?energy_level=hourly&date_range=2025-07-20
```

**Parameters:** `sid`, `eid`, `energy_level` (`minutely`, `hourly`, `daily`, `monthly`, `yearly`), `date_range`

**Response — non-minutely levels:**

```json
{
  "code": 0,
  "data": {
    "time":      ["01", "02"],
    "discharge": ["40.300", "50.016"],
    "charge":    ["40.300", "50.016"],
    "produced":  ["40.300", "50.016"],
    "consumed":  ["40.300", "50.016"],
    "exported":  ["40.300", "50.016"],
    "imported":  ["40.300", "50.016"]
  }
}
```

**Response — minutely level:**

```json
{
  "code": 0,
  "data": {
    "today": {"discharge": "394.408", "charge": "0.000", "produced": "560.523", "consumed": "560.523", "exported": "560.523", "imported": "833.884"},
    "time": ["23:57"],
    "power": {
      "discharge": ["167.961"], "charge": ["167.961"], "produced": ["167.961"],
      "consumed": ["167.961"], "exported": ["167.961"], "imported": ["0.000"]
    },
    "energy": {
      "discharge": ["167.961"], "charge": ["167.961"], "produced": ["167.961"],
      "consumed": ["167.961"], "exported": ["167.961"], "imported": ["0.000"]
    }
  }
}
```

---

## 4. Response Codes

| Code | Description |
|---|---|
| 0 | Success |
| 1000 | Data exception |
| 1001 | No data |
| 2000 | Application account exception |
| 2001 | Invalid application account |
| 2002 | Application account not authorized |
| 2003 | Application account authorization expired |
| 2004 | Application account has no permission |
| 2005 | Access limit exceeded |
| 3000 | Access token exception |
| 3001 | Missing access token |
| 3002 | Unable to verify access token |
| 3003 | Access token timeout |
| 3004 | Refresh token timeout |
| 4000 | Request parameter exception |
| 4001 | Invalid request parameter |
| 5000 | Internal server exception |
| 6000 | Communication exception |
| 7000 | Server access restriction exception |
| 7001 | Server access limit exceeded |
| 7002 | Too many requests |
| 7003 | System busy |
