# Baseline Test Results (RED Phase)

## Generate Mode — Without Skill

Prompt: "Create a Kafka producer in Java with Spring Kafka for asset-created event on assets.material.events. Use CloudEvents envelope."

### Findings

| Aspect | Expected (per ADR) | Actual (baseline) | Gap? |
|---|---|---|---|
| ce_ prefix | All attributes prefixed | Used ce_ prefix | No |
| id | ce_id with UUID | Set correctly | No |
| source | URI like `https://netrtl.com/assets/asset-management` | `/assets/service` (not ADR format) | YES |
| specversion | ce_specversion = 1.0 | Set correctly | No |
| type | `assets.material.asset-created.v1` format | `com.example.assets.asset.created` (reverse-DNS, no version) | YES |
| time | ce_time RFC 3339 | Set correctly | No |
| traceparent | ce_traceparent MUST, propagated | Set but treated as optional extension | YES |
| datacontenttype | `content-type` (NO ce_ prefix) | `ce_datacontenttype` (WRONG prefix) | YES |
| correlationid | SHOULD be mentioned | Not mentioned at all | YES |
| causationid | SHOULD be mentioned | Not mentioned at all | YES |
| messagekey | Derived from subject | Used assetId as key (acceptable) | Minor |
| Compliance checklist | MUST/SHOULD/MAY format | Generic table, not ADR-structured | YES |
| Idempotency hint | Via ce_id for consumers | Not mentioned | YES |

### Key Gaps

1. **datacontenttype prefix** — Used `ce_datacontenttype` instead of `content-type` (CloudEvents Kafka Binding exception)
2. **type format** — Reverse-DNS instead of ADR dot-notation with version (e.g. `.v1`)
3. **source format** — Generic path instead of business process URI
4. **traceparent** — Treated as optional, not MUST
5. **No SHOULD attributes** — correlationid and causationid completely absent
6. **No structured checklist** — No MUST/SHOULD/MAY compliance output

## Review Mode — Without Skill

Prompt: "Review BadProducer.java for CloudEvents compliance" (BadProducer has: no ce_ prefix, missing id/specversion/time/traceparent, source=production, type=asset-updated)

### Findings

| Aspect | Expected | Actual (baseline) | Gap? |
|---|---|---|---|
| Detect missing ce_ prefix | Yes | Yes, caught | No |
| Detect missing MUST attributes | All 4 (id, specversion, time, traceparent) | Caught all 4 | No |
| Detect source anti-pattern | source=production is environment | Caught correctly | No |
| Detect type semantic error | "asset-updated" vs "created" | Caught correctly | No |
| datacontenttype fix | Should suggest `content-type` (no prefix) | Suggested `ce_datacontenttype` (WRONG) | YES |
| time classification | MUST per ADR | Called it "RECOMMENDED/OPTIONAL" | YES |
| Structured audit output | MUST/SHOULD/MAY sections with [x]/[!]/[ ] | Free-text issues list | YES |
| Severity classification | [!] for MUST violations | "Spec violation" vs "recommendation" (informal) | YES |
| correlationid mention | SHOULD be checked | Not mentioned | YES |
| causationid mention | SHOULD be checked | Not mentioned | YES |

### Key Gaps

1. **datacontenttype** — Suggested wrong fix (ce_ prefix instead of content-type)
2. **time obligation level** — Downgraded from MUST to OPTIONAL/RECOMMENDED
3. **No structured audit format** — Free-text instead of MUST/SHOULD/MAY sections
4. **No SHOULD/MAY checks** — Only checked obvious violations, missed correlationid/causationid
5. **No severity indicators** — No [!]/[ ]/[x] system

## Conclusion

Both modes show significant gaps without the skill:
- The `content-type` exception (no ce_ prefix for datacontenttype) is consistently missed
- ADR-specific conventions (type format, source URI format) are not applied
- SHOULD attributes (correlationid, causationid) are ignored
- No structured compliance/audit output format
- time is misclassified as optional

The skill must address all these gaps explicitly.
