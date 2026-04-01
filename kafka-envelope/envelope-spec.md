# Kafka Envelope Specification

CloudEvents-compatible envelope for Kafka messages. Based on ADR "Kafka Message Struktur".
All CloudEvents attributes are transported in Kafka message headers with `ce_` prefix
(CloudEvents Kafka Protocol Binding, binary content mode).

## Attribute Reference

### Required (MUST)

| Attribute | Kafka Header | Type | Description | Example |
|---|---|---|---|---|
| id | `ce_id` | String (UUID) | Unique event ID. source + id must be globally unique. Does NOT identify the entity. | `44e149f7-8533-4d2c-814c-70e4fc8d4841` |
| source | `ce_source` | URI | Context in which the event occurs. Use business process context, NOT API endpoints or environments. | `https://netrtl.com/assets/asset-management` |
| specversion | `ce_specversion` | String | CloudEvents spec version. Always `1.0`. | `1.0` |
| type | `ce_type` | String | Describes the domain event (e.g. created, assigned, released). Must NOT imply entity-updates or snapshots. Version recommended. | `assets.material.asset-created.v1` |
| time | `ce_time` | Timestamp (RFC 3339) | Business timestamp of the event occurrence. NOT the infrastructure/Kafka timestamp. | `2025-04-12T23:20:50.52Z` |
| traceparent | `ce_traceparent` | String (W3C) | W3C Trace Context. Must be propagated or created if none exists. | `00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-00` |

### Recommended (SHOULD)

| Attribute | Kafka Header | Type | Description | Example |
|---|---|---|---|---|
| correlationid | `ce_correlationid` | String (UUID) | Business correlation across system/service/topic boundaries. Propagate if business process context exists. | `2b9e7524-bbd9-469e-a510-95c10795d561` |
| causationid | `ce_causationid` | String | ID of the causing event. Must NOT be modified, must be forwarded. | See `id` |

### Optional (MAY)

| Attribute | Kafka Header | Type | Description | Example |
|---|---|---|---|---|
| datacontenttype | `content-type` | String (RFC 2046) | Media type of payload. Exception: NO `ce_` prefix. | `application/avro` |
| subject | `ce_subject` | String | Business identity + grouping for downstream filtering. | `small-material/287fc583-cc46-4d72-93ba-cad3640f1894` |
| messagekey | Kafka Message Key | String | Partitioning key, derived from subject. Not a header — it IS the Kafka message key. | `287fc583-cc46-4d72-93ba-cad3640f1894` |

## Rules

### MUST

1. Every Kafka message MUST contain an event envelope in message headers
2. Required attributes: id, source, specversion, type, time, traceparent
3. All CloudEvents headers MUST use `ce_` prefix. Exception: `datacontenttype` maps to `content-type` (no prefix)
4. All header values MUST be UTF-8 encoded strings
5. Message value MUST NOT contain technical metadata (tracing, correlation, governance)
6. `type` MUST describe a domain event (created, assigned, released) — no entity-updates or snapshots
7. Kafka messages MUST NOT be implicitly treated as events when they are entity-streams or snapshots
8. `messagekey` SHOULD be derived from `subject` to guarantee ordering per entity
9. `traceparent` MUST be propagated when a tracing context exists or can be created

### SHOULD

1. Set `correlationid` and propagate across system/service/topic boundaries
2. Set `causationid` when a causing event is identifiable
3. Version event types (e.g. `.v1`, `.v2`)

### MAY

1. Existing entity-streams may continue but MUST follow the envelope standard
2. Snapshots MUST be explicitly recognizable (via type or topic naming)
3. Additional metadata via CloudEvents extensions if justified and documented
4. Existing envelopes in message value should be migrated incrementally

## Complete Message Example

```
------------------ Message -------------------
Topic Name: assets.material.events

------------------- key ----------------------
Key: 287fc583-cc46-4d72-93ba-cad3640f1894

------------------ headers -------------------
ce_specversion: "1.0"
ce_id: "ca39745a-2b82-4097-9cdf-c40814492c1c"
ce_source: "https://netrtl.com/assets/asset-management"
ce_type: "assets.material.asset-created.v1"
ce_time: "2026-01-05T14:26:50.524Z"
ce_subject: "small-material/287fc583-cc46-4d72-93ba-cad3640f1894"
ce_traceparent: "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
ce_correlationid: "287fc583-cc46-4d72-93ba-cad3640f1894"
ce_causationid: "44e149f7-8533-4d2c-814c-70e4fc8d4841"
content-type: "application/avro"

------------------- value --------------------
{
  "assetId": "287fc583-cc46-4d72-93ba-cad3640f1894"
}
-----------------------------------------------
```

## Anti-Patterns

| Anti-Pattern | Problem | Fix |
|---|---|---|
| Metadata in Avro payload | Mixes platform and domain, breaks schema evolution | Move metadata to Kafka headers |
| Headers without `ce_` prefix | Not CloudEvents-spec-compliant, breaks tooling | Add `ce_` prefix to all CloudEvents attributes |
| `type` as entity-update | e.g. "asset-updated" implies CRUD, not domain event | Describe the actual domain event |
| `source` as API endpoint | e.g. `/api/v1/assets` — source is context, not endpoint | Use business process context URI |
| `source` as environment | e.g. `production`, `staging` — source identifies context, not env | Use business context |
| `time` as infra timestamp | Kafka timestamp instead of business event time | Set business event timestamp |
| Missing idempotency | Consumer processes events multiple times without dedup | Use `ce_id` for deduplication |
| `datacontenttype` with `ce_` prefix | This attribute maps to `content-type` without prefix | Use `content-type` header |
