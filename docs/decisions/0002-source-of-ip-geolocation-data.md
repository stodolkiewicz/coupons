---
status: accepted
date: 2026-11-22
---

# Source of IP geolocation data

## Context and Problem Statement

Every coupon has a `country_code` with an ISO 3166-1 alpha-2 code.
Only a user from that country can redeem the coupon.
The service determines the country from the IP address of the redemption request.
The service must handle many concurrent redemptions of one coupon.

Every redemption needs one country lookup before it changes the database.

Problems:

1. **Latency.** The lookup runs inside every redemption request, so its time adds to the time of every redemption.
2. **Limits.** A geolocation API can limit how many lookups a client may make.
3. **Availability.** When the lookup fails, the service cannot decide whether the user may redeem the coupon.

## Considered Options

### 1. Local geolocation database file (chosen)

The service keeps a geolocation database file and looks up the country in it.
The service downloads a new version of the file regularly.

Pros:

- A lookup sends no network request, so it adds no network round trip to a redemption.
- The number of lookups has no limit, because the lookups never reach the provider.
- A failed download keeps the current file, so redemptions continue on slightly older data.
- The IP addresses of users stay inside the service.

Cons:

- The data is as old as the last downloaded file. For example, DB-IP publishes its free Lite file once a month.
- The service must download the file regularly and switch to the new file while it runs.
- Each instance of the service keeps its own copy of the file.

Lower latency of every redemption matters more than slightly stale data, so this option wins.

Possible extension:

When the file says that the country of the user differs from the country of the coupon, the service calls an external geolocation HTTP API before it rejects the redemption.
The extra call happens only for rejections, so accepted redemptions keep the latency of a file lookup.
A user whose IP address has moved to another country since the last file is not rejected because of stale data.

### 2. External geolocation HTTP API

The service sends the IP address of each redemption to a geolocation API, for example the MaxMind GeoLite web services, and reads the country from the JSON response.

Pros:

- The provider updates the data, so each lookup uses current data.
- The service stores no file and needs no download code.

Cons:

- Each redemption waits for one HTTP request to the provider.
- Free plans limit the number of lookups. For example, the free MaxMind GeoLite web services allow 1000 lookups per day per service, so the service could check at most 1000 redemptions per day.
- When the provider is down or slow, no redemption succeeds. The service needs a timeout, retries and a circuit breaker to limit the damage.
- The IP address of every user leaves the service and reaches a third party.

## More Information

- [DB-IP IP to Country Lite](https://db-ip.com/db/download/ip-to-country-lite): monthly releases, MMDB format, CC BY 4.0.
- [MaxMind GeoLite free geolocation data](https://dev.maxmind.com/geoip/geolite2-free-geolocation-data): limit of the free GeoLite web services.
