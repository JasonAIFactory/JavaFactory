# REST API 면접 답변

CMiC 채용공고에 Jersey + Oracle ADF BC REST 명시. 면접에서 거의 확실히 나옴.

영어는 단순 SVO, 짧은 문장.

---

## 1. What is a REST API?

🧠 **REST = Representational State Transfer**. HTTP 기반의 리소스 중심 API 스타일.

### 핵심 원칙 5가지

1. **Resource-based URL**: `/users/123`, `/orders/456`
2. **HTTP methods**: GET, POST, PUT, PATCH, DELETE
3. **Stateless**: 서버가 클라이언트 세션 안 가짐, 모든 요청이 독립
4. **JSON or XML**: 데이터 형식
5. **Status codes**: 200, 201, 400, 404, 500 등

🗣️ "REST is a style for building web APIs. It uses HTTP methods on resources. Each resource has a URL. The server is stateless — every request carries all needed info. We usually exchange JSON. Status codes tell the client what happened."

---

## 2. HTTP Methods (가장 많이 묻는 부분)

| Method | 용도 | Idempotent? | 예시 |
| --- | --- | --- | --- |
| **GET** | 조회 | ✅ | `GET /users/1` |
| **POST** | 생성 | ❌ | `POST /users` |
| **PUT** | 전체 교체 | ✅ | `PUT /users/1` |
| **PATCH** | 부분 수정 | ✅ (보통) | `PATCH /users/1` |
| **DELETE** | 삭제 | ✅ | `DELETE /users/1` |

> **Idempotent** = 여러 번 요청해도 결과 같음.

🗣️ "GET reads. POST creates. PUT replaces the whole resource. PATCH updates part of it. DELETE removes. GET, PUT, PATCH, DELETE are idempotent — calling them many times gives the same result. POST is not, because each call creates a new resource."

---

## 3. PUT vs PATCH vs POST (자주 묻는 차이점)

```
POST /users           Body: {name: "Alice", age: 30}
   → 새 user 생성 (id는 서버가 부여)
   → 같은 요청 두 번 보내면 user 두 명 생김 (not idempotent)

PUT /users/1          Body: {name: "Alice", age: 31}
   → user 1을 통째로 교체. age만 바꾸려 해도 모든 필드 보내야 함
   → 같은 요청 100번 보내도 결과 같음 (idempotent)

PATCH /users/1        Body: {age: 31}
   → 변경할 필드만 보냄. age만 31로 바뀜
   → idempotent (같은 입력에 같은 결과)
```

🗣️ "POST creates a new resource. PUT replaces the whole resource — you must send all fields. PATCH updates only the fields you send. Use POST to create, PUT to replace, PATCH to modify part."

---

## 4. Status Codes (필수 5개)

| 코드 | 의미 | 언제? |
| --- | --- | --- |
| **200 OK** | 성공 | GET, PUT, PATCH 성공 |
| **201 Created** | 생성됨 | POST 성공 |
| **204 No Content** | 성공, 본문 없음 | DELETE 성공 |
| **400 Bad Request** | 잘못된 요청 | 필수 필드 누락 등 |
| **401 Unauthorized** | 인증 안 됨 | 로그인 필요 |
| **403 Forbidden** | 권한 없음 | 로그인은 됐지만 접근 불가 |
| **404 Not Found** | 리소스 없음 | `/users/999` 없음 |
| **500 Internal Server Error** | 서버 에러 | 코드 버그, DB 문제 |

🗣️ "200 means success. 201 means created. 204 means success without body. 400 means bad input. 401 means not logged in. 403 means logged in but not allowed. 404 means resource not found. 500 means server error."

---

## 5. API 설계 — Meeting + Participants (라이브 디자인 단골)

### 리소스 식별

- **Meeting** (회의)
- **Participant** (참가자) — Meeting에 종속

### URL 설계

| Method | URL | 동작 |
| --- | --- | --- |
| GET | `/meetings` | 모든 회의 목록 |
| GET | `/meetings/{id}` | 회의 상세 |
| POST | `/meetings` | 새 회의 생성 |
| PUT | `/meetings/{id}` | 회의 정보 전체 교체 |
| PATCH | `/meetings/{id}` | 일부만 수정 (제목만 등) |
| DELETE | `/meetings/{id}` | 회의 삭제 |
| GET | `/meetings/{id}/participants` | 참가자 목록 |
| POST | `/meetings/{id}/participants` | 참가자 추가 |
| DELETE | `/meetings/{id}/participants/{userId}` | 참가자 제거 |

### JSON 예시

**Request: POST /meetings**
```json
{
  "title": "Sprint Planning",
  "startTime": "2026-05-04T10:00:00Z",
  "duration": 60
}
```

**Response: 201 Created**
```json
{
  "id": 123,
  "title": "Sprint Planning",
  "startTime": "2026-05-04T10:00:00Z",
  "duration": 60,
  "createdAt": "2026-05-03T09:00:00Z"
}
```

**Response: GET /meetings/123/participants**
```json
{
  "meetingId": 123,
  "participants": [
    { "userId": "u1", "name": "Alice", "role": "ORGANIZER" },
    { "userId": "u2", "name": "Bob",   "role": "ATTENDEE"  }
  ]
}
```

### 🗣️ 면접 영어 답변

> "First I identify resources. Meeting is the main resource. Participant is a sub-resource of meeting. So I design nested URLs like meetings slash id slash participants.
>
> For HTTP methods I use GET to read, POST to create, PUT to replace, PATCH to update part, DELETE to remove. I use 201 Created when POST succeeds, 204 No Content for DELETE.
>
> For the response body I use JSON. I include the resource ID and timestamps. For errors I use 400 for bad input, 404 for missing resource, 500 for server problem.
>
> I keep the API stateless. Each request includes auth token in the header. The server does not store session state."

---

## 6. JDBC와 차이 (혹시 물어볼 경우)

🗣️ "JDBC is for talking to a database. REST API is for talking to another service over HTTP. In a Java app, the controller layer exposes REST endpoints and uses JDBC inside to talk to the database."

---

## 🎯 한 줄 카드

| 주제 | 한 줄 |
| --- | --- |
| REST | resource-based, stateless, HTTP-driven |
| GET | read |
| POST | create (not idempotent) |
| PUT | replace whole (idempotent) |
| PATCH | update part (idempotent) |
| DELETE | remove |
| 200 | OK |
| 201 | Created |
| 204 | No Content (DELETE) |
| 400 | Bad Request |
| 404 | Not Found |
| 500 | Server Error |
| Stateless | server holds no session |
| Idempotent | same call, same result |
