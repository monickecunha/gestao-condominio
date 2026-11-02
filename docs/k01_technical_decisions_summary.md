# K01 — Technical Decisions & API Specifications

This document consolidates the technical definitions required to complete issue **K01** for the Backend API.

---

## 1. Authentication Mechanism & Session Expiration

* **Technical Solution:** JWT (JSON Web Token) provided via HTTP Header: `Authorization: Bearer <token>`.
* **Signing Algorithm:** HMAC-SHA256 (`HS256`).
* **Token Claim Payload:**
  * `sub`: User Identifier (`Long` / `UUID`).
  * `role`: User Profile (`RESIDENT` or `MANAGER`).
  * `condoId`: Associated Condominium Identifier.
  * `iat`: Issued At timestamp.
  * `exp`: Expiration timestamp.
* **Expiration Policy:**
  * **Access Token:** Valid for **2 hours** (tailored for active sessions and API test execution via Postman/cURL).
  * **Refresh Token:** Omitted for MVP to maintain a lean scope.
* **Logout Behavior:**
  * **Stateless:** Logout is managed on the client side by discarding the token. No token blacklisting database table is maintained in the H2 instance for the MVP.

---

## 2. Unit Creation & Primary Resident Identification

* **Required Unit Fields (`Unit`):**
  * `block` (String, max 20 chars — optional; e.g., "Block A").
  * `number` (String, max 20 chars — required; e.g., "101", "House 12").
  * `condoId` (Foreign Key — required).
* **Unit Creation Workflow:**
  * Units are created beforehand by a **Manager (`MANAGER`)** through the management endpoint (`POST /api/v1/condominiums/{condoId}/units`).
* **Resident Binding Workflow:**
  * **Primary Resident:** The first user requesting a link to an unassigned unit becomes the primary applicant. Upon approval by the Manager (`POST /api/v1/bindings/{id}/approve`), they become the unit's official primary resident.
  * **Additional Residents (K16):** An approved primary resident can invite or register additional family members/roommates for their unit. New bindings remain pending until final approval by the Manager.

---

## 3. Complaint Search by Protocol, Access Scope & Field Breakdown

* **Protocol Access Policy:**
  * **Public Access (No Login Required):** Protocol tracking (`GET /api/v1/complaints/protocol/{protocolCode}`) is publicly accessible with a valid unpredictably generated protocol string (UUID v4 format). This enables anonymous reporters to track complaint progress without logging into an account.
* **Field Visibility & Payload Breakdown:**

| Scope / Context | Visible JSON Fields |
| :--- | :--- |
| **Public Scope**<br>*(Protocol Search)* | • `protocol`<br>• `category`<br>• `status` (`RECEIVED`, `UNDER_REVIEW`, `IN_PROGRESS`, `RESOLVED`, `REJECTED`) <br>• `createdAt`<br>• `updatedAt`<br>• `statusHistory`: List of items containing `previousStatus`, `nextStatus`, `changedAt`, and `publicJustification` (e.g., rejection reason when applicable) |
| **Internal / Administrative Scope**<br>*(`MANAGER` Role)* | • All Public Scope fields above +<br>• `fullDescription`<br>• `location` (optional)<br>• `attachments`: List of items containing `imageId`, `url`<br>• `internalActions`: List of items containing `actionDescription`, `executedAt`, `registeredBy` (Manager Name/ID) |

* **Privacy Assurance:** Payloads strictly exclude `userId`, `unitId`, or network origin attributes across all public and management endpoints.

---

## 4. Image Upload Constraints & Validation

* **Allowed MIME Types:** `image/jpeg`, `image/jpg`, `image/png`, `image/webp` (validated via MIME headers and binary magic numbers).
* **Maximum File Count:** Up to **3 images** per complaint submission.
* **Maximum File Size:** **5 MB** per individual file.
* **Maximum Request Size:** **15 MB** overall for multipart requests.
* **Sanitization:** Uploaded files receive randomly generated UUID names on storage to overwrite original filenames and remove metadata (including EXIF location/camera information).