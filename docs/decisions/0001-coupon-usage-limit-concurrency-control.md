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

- If the user has already used the coupon → the `INSERT` violates `UNIQUE (coupon_id, user_id)` and fails, and the `UPDATE` does not run.
- If `current_uses` has reached `max_uses` → the `UPDATE` updates 0 rows, because `current_uses < max_uses` is false.
  The service then throws a new custom `CouponExhaustedException`, which extends `RuntimeException`.
  Spring rolls back the transaction on a `RuntimeException`, so the row inserted into `coupon_usage` is removed.

Pros:

- The lock on the `coupon` row is shorter than in option 2.
  It lasts from the `UPDATE` to the `COMMIT`, not from the `SELECT ... FOR UPDATE` to the `COMMIT`.

Cons:

- Every redemption of an exhausted coupon runs the `INSERT` and then a rollback.
  If many users keep redeeming a coupon after it is exhausted, the database runs many rollbacks.
  A shorter lock on the `coupon` row matters more than these rollbacks.

### 2. Pessimistic locking with `SELECT ... FOR UPDATE`

A redemption of a coupon locks the `coupon` row when it reads the row, before it changes anything.
`SELECT ... FOR UPDATE` reads the row and locks it until `COMMIT` or `ROLLBACK`.
The transaction of one redemption:

```sql
SELECT current_uses, max_uses
FROM coupon
WHERE id = :couponId
FOR UPDATE;
-- Java checks current_uses < max_uses

INSERT INTO coupon_usage (id, coupon_id, user_id, used_at)
VALUES (:id, :couponId, :userId, :usedAt);

UPDATE coupon
SET current_uses = current_uses + 1
WHERE id = :couponId;

COMMIT;
```

The `UPDATE` has no `current_uses < max_uses` condition, because Java checks `max_uses` between the `SELECT` and the `INSERT`.

- If the `SELECT` returns 0 rows → the coupon does not exist.
- If `current_uses` has reached `max_uses` → Java throws `CouponExhaustedException` before the `INSERT`.
  The transaction has run only the `SELECT`, so the rollback has no row to remove.

Pros:

- The limit rule is readable in domain code.

Cons:

- The lock is held longer than in option 1: through the read, the application round trip and the write.
- Requests still queue on one row, with lower throughput than option 1.

### 3. Optimistic locking with `@Version`

A redemption of a coupon reads the `coupon` row without a lock and detects a conflict only when it writes the row.
The `coupon` table gets a `version BIGINT` column.
The transaction of one redemption:

```sql
SELECT id, current_uses, max_uses, version
FROM coupon
WHERE id = :couponId;
-- Java checks current_uses < max_uses
-- Java sets current_uses = current_uses + 1 on the Coupon entity

INSERT INTO coupon_usage (id, coupon_id, user_id, used_at)
VALUES (:id, :couponId, :userId, :usedAt);

UPDATE coupon
SET current_uses = :newCurrentUses,
    version = :readVersion + 1
WHERE id = :couponId
  AND version = :readVersion;

COMMIT;
```

Hibernate generates the `UPDATE` with the `version = :readVersion` condition.
`:readVersion` is the version that the `SELECT` read.
If the `UPDATE` updates 0 rows → Spring throws `ObjectOptimisticLockingFailureException` and rolls back the transaction, including the row inserted into `coupon_usage`.

Cons:

- If two requests read the same `version` of the `coupon` row, the second request rolls back, even when the coupon has unused redemptions left.
  When many requests redeem one coupon at the same time, most of them roll back and the service must retry them.
  This con rules option 3 out.

### Estimates

These numbers are estimates, not measurements.
The estimate uses the formula `max requests/s for one coupon = 1 s / time the lock on the coupon row is held`.
The lock times in the table are assumptions.
The service and PostgreSQL run in the same network, and the database uses an SSD.
The `COMMIT` time depends most on the disk and on `synchronous_commit`, so it can shift the results of options 1 and 2 several times.

| Option | Time the lock on the `coupon` row is held | Requests/s for one coupon |
|---|---|---|
| 1 | `UPDATE` + `COMMIT` with a WAL write (1–5 ms) | about 200–1000 |
| 2 | option 1 plus `SELECT ... FOR UPDATE`, `INSERT` and 2 extra network round trips (2–10 ms) | about 100–500 |
| 3 | no lock, but conflicts on `version` | tens without problems; above that, most requests are retries |

A load test must measure the real numbers.
The load test sends N concurrent redemptions of one coupon in each option and measures the successful redemptions per second.

If the load test shows too few requests/s for one coupon, two extensions are possible.
Both extensions spread the redemptions of one coupon across many rows.
Concurrent redemptions then lock different rows, so they do not wait for one lock.

#### Slot pool with `SKIP LOCKED`

A `coupon_slot` table holds `max_uses` rows for each coupon.
Each row is one free redemption with `used_by = NULL`.
A redemption takes one free slot:

```sql
SELECT id
FROM coupon_slot
WHERE coupon_id = :couponId
  AND used_by IS NULL
LIMIT 1
FOR UPDATE SKIP LOCKED;

UPDATE coupon_slot
SET used_by = :userId
WHERE id = :slotId;
```

`SKIP LOCKED` makes the `SELECT ... FOR UPDATE` skip the rows that other transactions have locked, instead of waiting for them.

- If requests A and B redeem coupon `WIOSNA` at the same time → A locks slot 1, and B skips slot 1 and locks slot 2.
- If the `SELECT` returns 0 rows → every slot is used or locked by another transaction, and the service treats the coupon as exhausted.

Cons:

- The slot pool needs `max_uses` rows in `coupon_slot` for each coupon.
  A coupon with `max_uses = 10 000 000` needs 10 000 000 rows in `coupon_slot`.

#### Sharded counter

A `coupon_counter_shard` table splits `max_uses` of one coupon across several rows, called shards.
Each shard has its own `current_uses` and `max_uses`.
For example, coupon `WIOSNA` with `max_uses = 10` has 4 shards:

| `shard_no` | `current_uses` | `max_uses` |
|---|---|---|
| 0 | 0 | 3 |
| 1 | 0 | 3 |
| 2 | 0 | 2 |
| 3 | 0 | 2 |

A redemption runs 3 steps:

1. A plain `SELECT` reads all shards of the coupon.
2. The service picks a random shard among the shards with `current_uses < max_uses`.
3. The service runs the transaction of option 1 or option 2 on the picked shard.

- If no shard has `current_uses < max_uses` in step 1 → the coupon is exhausted.
- If the transaction in step 3 finds the picked shard exhausted → the service picks another shard from step 2.

The plain `SELECT` in step 1 does not lock the shards, so the check in step 2 guarantees nothing.
The transaction in step 3 keeps `current_uses` of the shard from exceeding `max_uses` of the shard.

Cons:

- The service is slightly more complex.
  It reads the shards, picks a random free shard and tries another shard when the picked shard is exhausted.

## Other approaches

### Counter in Redis

Redis holds two keys for each coupon, for example for coupon `WIOSNA`:

- `coupon:WIOSNA:users` is a set with the IDs of the users who have used coupon `WIOSNA`.
- `coupon:WIOSNA:remaining` is a counter. The service sets it to `max_uses` when it creates the coupon.

A Lua script checks the user, runs `DECR` on the counter and `SADD` on the set of users.

Pros:

- Redis handles a hot counter without row locks in the database.

Cons:

- Redis and PostgreSQL both hold coupon state. A failure between the Redis write and the database write leaves them inconsistent and needs compensation or reconciliation.
- Redis is extra infrastructure to operate.

## More Information

- PostgreSQL 17, [13.2.1. Read Committed Isolation Level](https://www.postgresql.org/docs/17/transaction-iso.html) and [13.3.2. Row-Level Locks](https://www.postgresql.org/docs/17/explicit-locking.html#LOCKING-ROWS).
