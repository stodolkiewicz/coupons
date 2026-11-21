---
status: accepted
date: 2026-11-21
---

# Redeem coupons with an atomic conditional UPDATE in PostgreSQL

## Context and Problem Statement

The `coupon` table stores one row per coupon code.  
Column `max_uses` holds the usage limit and column `current_uses` holds the number of redemptions so far.  
The `coupon_usage` table stores one row per redemption, with the coupon and the user who redeemed it.  
Coupons are first come, first served: a coupon must never be used more than `max_uses` times.  
A single user may use a coupon only once.

Problems:

1. **Correctness.** Two requests can both read `current_uses = 9` with `max_uses = 10`, both pass the limit check in Java, and the coupon ends up used 11 times.
2. **Throughput.** Many users redeem a popular public code such as `WIOSNA` at the same time, so many concurrent requests hit one row in the `coupon` table (a hot row).

## Considered Options

### 1. Atomic conditional `UPDATE` (chosen)

A redemption of a coupon runs as one transaction with two statements:

```sql
INSERT INTO coupon_usage (id, coupon_id, user_id, used_at)
VALUES (:id, :couponId, :userId, :usedAt);

UPDATE coupon
SET current_uses = current_uses + 1
WHERE id = :couponId
  AND current_uses < max_uses;
```

### 2. Pessimistic locking with `SELECT ... FOR UPDATE`

Pros:

- The limit rule is readable in domain code.

Cons:

- The lock is held longer than in option 1: through the read, the application round trip and the write.
- Requests still queue on one row, with lower throughput than option 1.

### 3. Optimistic locking with `@Version`

Cons:

- First come, first served means high contention on a single row, so most writes conflict and retry.
- Retries can run out under heavy load.

### 4. Counter in Redis

A Lua script checks the user, runs `DECR` on the counter and `SADD` on the set of users.

Pros:

- Redis handles a hot counter without row locks in the database.

Cons:

- Redis and PostgreSQL both hold coupon state. A failure between the Redis write and the database write leaves them inconsistent and needs compensation or reconciliation.
- Redis is extra infrastructure to operate.

### 5. Counter spread across multiple rows

A slot pool with `SKIP LOCKED` or a sharded counter spreads the uses of one coupon across many rows.

Pros:

- Concurrent requests hit different rows, so there is no hot row.

Cons:

- The data model is more complex. A slot pool needs `max_uses` rows per coupon.

## More Information

- PostgreSQL 17, [13.2.1. Read Committed Isolation Level](https://www.postgresql.org/docs/17/transaction-iso.html) and [13.3.2. Row-Level Locks](https://www.postgresql.org/docs/17/explicit-locking.html#LOCKING-ROWS).
