# Firestore Security Specification — Habit Radar

## 1. Access Control Model
- **Authentication**: All operations require a signed-in user (`request.auth != null`).
- **Path Ownership**: All documents live under `/users/{userId}/habits/{habitId}` and `/users/{userId}/entries/{entryId}`. Only the owner (`request.auth.uid == userId`) can read (`get`, `list`), `create`, `update`, or `delete` documents in their own namespace.
- **Document ID Validation**: `habitId` and `entryId` must match `^[a-zA-Z0-9_-]{1,128}$`.

## 2. Schema & Field Invariants

### `/users/{userId}/habits/{habitId}` (`CloudHabit`)
- **Required & Allowed Keys**: `['userId', 'title', 'category', 'iconName', 'colorHex', 'frequencyType', 'targetPerDay', 'isArchived', 'createdAt', 'updatedAt']`
- **Field Types & Bounds**:
  - `userId`: `string`, must equal `request.auth.uid` and `userId` path parameter (immutable on update).
  - `title`: `string`, `1 <= size <= 200`
  - `category`: `string`, `1 <= size <= 100`
  - `iconName`: `string`, `1 <= size <= 64`
  - `colorHex`: `string`, `1 <= size <= 16`
  - `frequencyType`: `string`, in `['DAILY', 'DAYS_OF_WEEK', 'INTERVAL', 'X_PER_PERIOD']`
  - `targetPerDay`: `int`, `1 <= targetPerDay <= 1000`
  - `isArchived`: `bool`
  - `createdAt`: `timestamp`, `<= request.time` (immutable on update)
  - `updatedAt`: `timestamp`, `<= request.time`

### `/users/{userId}/entries/{entryId}` (`CloudEntry`)
- **Required & Allowed Keys**: `['userId', 'habitId', 'date', 'status', 'count', 'note', 'createdAt', 'updatedAt']`
- **Field Types & Bounds**:
  - `userId`: `string`, must equal `request.auth.uid` and `userId` path parameter (immutable on update).
  - `habitId`: `string`, `1 <= size <= 128` (immutable on update).
  - `date`: `string`, `size == 10` (YYYY-MM-DD).
  - `status`: `string`, in `['COMPLETED', 'PARTIAL', 'SKIPPED', 'MISSED']`
  - `count`: `int`, `0 <= count <= 10000`
  - `note`: `string`, `0 <= size <= 500`
  - `createdAt`: `timestamp`, `<= request.time` (immutable on update)
  - `updatedAt`: `timestamp`, `<= request.time`
