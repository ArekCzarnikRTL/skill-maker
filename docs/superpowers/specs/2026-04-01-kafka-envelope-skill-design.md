# Kafka Envelope Skill — Design Spec

## Zusammenfassung

Ein Claude Code Skill, der Entwicklern beim Erstellen und Reviewen von Kafka Producer/Consumer-Code hilft. Der Skill stellt sicher, dass die CloudEvents-kompatible Envelope-Struktur (ADR "Kafka Message Struktur") korrekt in Kafka-Message-Headern umgesetzt wird.

## Anforderungen

- **Sprachagnostisch:** Erkennt Sprache/Framework aus dem Projektkontext
- **Zwei Modi:** Code-Generierung (Scaffolding) und Code-Review (Audit)
- **Automatische + explizite Aktivierung:** Trigger bei Kafka-Library-Imports oder auf Anfrage
- **Distribution:** Eigenständiges Skill-Paket/Plugin

## Dateistruktur

```
kafka-envelope/
  SKILL.md              # Trigger, Workflows (Generate + Review), Checklist-Template
  envelope-spec.md      # Vollständige Attribut-Referenz, Regeln, Beispiele, Anti-Patterns
```

### SKILL.md (< 400 Wörter)

**Frontmatter:**
```yaml
---
name: kafka-envelope
description: >
  Use when writing or reviewing Kafka producer/consumer code,
  when Kafka client libraries are imported, or when event/message
  schemas are being designed for Kafka topics.
---
```

**Inhalt:**
- Kurzes Overview (1-2 Saetze): Was der Skill tut
- Trigger-Bedingungen (automatisch + explizit)
- Flowchart fuer Modus-Entscheidung (Generate vs Review)
- Generate-Workflow (Schritte 1-4)
- Review-Workflow (Schritte 1-3)
- Checklist-Template (inline)

### envelope-spec.md (~ 200 Zeilen)

Wird nur geladen wenn der Skill aktiv wird. Enthaelt:

1. Attribut-Tabelle
2. MUST/SHOULD/MAY-Regeln
3. Vollstaendiges Message-Beispiel
4. Anti-Patterns

## Modus 1: Generate

### Trigger

- Entwickler will neuen Kafka Producer oder Consumer implementieren
- Kafka-Library-Imports erkannt (Spring Kafka, KafkaJS, confluent-kafka, etc.)

### Workflow

1. **Erkennen:** Producer, Consumer oder beides
2. **Sprache/Framework ableiten:** Aus Projektkontext (pom.xml, package.json, go.mod, requirements.txt, etc.)
3. **Scaffolding generieren:**
   - **Producer:** Klasse mit Envelope-Header-Setzung (`ce_`-Prefix), Tracing-Propagation (`traceparent`), Avro-Serialisierung, Message-Key-Handling
   - **Consumer:** Klasse mit Envelope-Header-Auslesen, Tracing-Context-Extraktion, Deserialisierung, Idempotenz-Hinweis (basierend auf `ce_id`)
4. **Compliance-Checklist ausgeben:** Nach der Code-Generierung

### Checklist-Output

```
Envelope Compliance Check:
  [x] id            — UUID generiert
  [x] source        — aus Kontext abgeleitet
  [x] specversion   — 1.0
  [x] type          — Event-Typ definiert
  [x] time          — RFC 3339 Timestamp
  [x] traceparent   — W3C Trace Context propagiert
  [ ] correlationid (SHOULD) — pruefen ob fachlicher Kontext existiert
  [ ] causationid   (SHOULD) — pruefen ob ausloesendes Event bekannt
  [ ] messagekey    (optional) — pruefen ob Partitionierung benoetigt
  [ ] subject       (optional) — pruefen ob Filterung gewuenscht
```

## Modus 2: Review

### Trigger

- Entwickler bittet um Review von bestehendem Kafka-Code
- Bestehender Producer/Consumer-Code wird bearbeitet

### Workflow

1. **Identifizieren:** Bestehenden Producer/Consumer-Code finden
2. **Pruefen:** Gegen MUST/SHOULD/MAY-Regeln aus dem ADR
3. **Audit-Ergebnis ausgeben:** Strukturiert mit Schweregraden

### Audit-Output

```
Kafka Envelope Audit: <Klassenname>

MUST (Verstoesse blockieren):
  [x] id           — gesetzt via UUID
  [!] source       — FEHLT, muss gesetzt werden
  [x] specversion  — 1.0
  [x] type         — "domain.entity.event.v1"
  [!] time         — nicht gesetzt, Pflichtattribut
  [x] traceparent  — propagiert

SHOULD (Empfehlungen):
  [ ] correlationid — nicht gesetzt, fachlicher Kontext vorhanden
  [x] causationid   — korrekt weitergeleitet

MAY:
  [ ] subject       — nicht gesetzt
  [ ] messagekey    — fehlt, Partitionierung pruefen

Weitere Findings:
  [!] Header ohne ce_-Prefix: "type" -> sollte "ce_type" sein
  [!] Metadaten im Avro-Payload: "traceId" -> gehoert in Header
```

**Schweregrade:**
- `[!]` MUST-Verstoss — muss behoben werden
- `[ ]` SHOULD/MAY nicht erfuellt — Empfehlung
- `[x]` Konform

## Envelope-Spec: Attribut-Referenz

### Pflicht-Attribute (MUST)

| Attribut | Kafka-Header | Typ | Beschreibung | Beispiel |
|---|---|---|---|---|
| id | `ce_id` | String (UUID) | Eindeutige Event-ID. source + id muss langfristig eindeutig sein. | `44e149f7-8533-4d2c-814c-70e4fc8d4841` |
| source | `ce_source` | URI | Kontext des Events (kein API-Endpoint). | `https://netrtl.com/assets/asset-management` |
| specversion | `ce_specversion` | String | CloudEvents-Spec-Version. | `1.0` |
| type | `ce_type` | String | Fachliches Ereignis (nicht Entity-Update). Versionierung empfohlen. | `assets.material.asset-created.v1` |
| time | `ce_time` | Timestamp (RFC 3339) | Fachlicher Zeitpunkt des Ereignisses. | `2025-04-12T23:20:50.52Z` |
| traceparent | `ce_traceparent` | String (W3C) | Trace Context. Muss propagiert oder erstellt werden. | `00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-00` |

### Empfohlene Attribute (SHOULD)

| Attribut | Kafka-Header | Typ | Beschreibung | Beispiel |
|---|---|---|---|---|
| correlationid | `ce_correlationid` | String (UUID) | Fachliche Korrelation ueber System-/Topic-Grenzen. | `2b9e7524-bbd9-469e-a510-95c10795d561` |
| causationid | `ce_causationid` | String | ID des ausloesenden Events. Darf nicht veraendert werden. | Siehe `id` |

### Optionale Attribute (MAY)

| Attribut | Kafka-Header | Typ | Beschreibung | Beispiel |
|---|---|---|---|---|
| datacontenttype | `content-type` | String (RFC 2046) | MediaType des Payloads. | `application/avro` |
| subject | `ce_subject` | String | Fachliche Identitaet + Gruppierung fuer Filterung. | `small-material/287fc583-...` |
| messagekey | Kafka Message Key | String | Partitionierungsschluessel, abgeleitet aus subject. | `c57f740a-e871-...` |

### Vollstaendiges Message-Beispiel

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

### MUST-Regeln (verbindlich)

1. Jede Kafka Message muss einen Event-Envelope in den Message Headern enthalten
2. Pflicht-Attribute: id, source, specversion, type, time, traceparent
3. Kafka-Header muessen mit `ce_`-Prefix versehen sein (CloudEvents Kafka Protocol Binding). Ausnahme: `datacontenttype` wird als `content-type` Header ohne Prefix transportiert (gemaess Spec)
4. Header-Werte muessen als UTF-8 Strings kodiert sein
5. Message Value darf keine technischen Metadaten enthalten (Tracing, Korrelation, Governance)
6. `type` muss ein fachliches Ereignis beschreiben (created, assigned, released) — keine Entity-Updates oder Snapshots
7. Kafka Messages duerfen nicht implizit als Event interpretiert werden wenn sie Entity-Streams/Snapshots sind
8. `messagekey` soll aus `subject` abgeleitet werden fuer Reihenfolge-Garantie pro Entitaet
9. `traceparent` muss propagiert werden wenn Tracing-Kontext existiert oder erstellt werden kann

### SHOULD-Regeln (empfohlen)

1. `correlationid` setzen und ueber System-/Service-/Topic-Grenzen propagieren
2. `causationid` setzen wenn ausloesendes Event identifizierbar
3. Event-Typen versionieren (z.B. `.v1`, `.v2`)

### MAY-Regeln (optional)

1. Bestehende Entity-Streams duerfen weiterbetrieben werden, muessen Envelope-Standard einhalten
2. Snapshots muessen explizit erkennbar sein (ueber type oder Topic-Naming)
3. Zusaetzliche Metadaten ueber CloudEvents-Extensions, wenn fachlich begruendet und dokumentiert
4. Existierende Envelopes im Message Value schrittweise migrieren

## Anti-Patterns

| Anti-Pattern | Problem | Korrektur |
|---|---|---|
| Metadaten im Avro-Payload | Vermischt Plattform und Domaene, erschwert Schema-Evolution | Metadaten in Kafka-Header verschieben |
| Header ohne `ce_`-Prefix | Nicht CloudEvents-Spec-konform, bricht Tooling-Kompatibilitaet | `ce_`-Prefix fuer alle CloudEvents-Attribute |
| `type` als Entity-Update | z.B. "asset-updated" statt "asset-created" — impliziert CRUD statt Domain-Event | Fachliches Ereignis beschreiben |
| `source` als API-Endpoint | z.B. `/api/v1/assets` — source ist Kontext, nicht Endpoint | URI des Geschaeftsprozess-Kontexts verwenden |
| `source` als Environment | z.B. `production`, `staging` — source identifiziert Kontext, nicht Umgebung | Fachlichen Kontext verwenden |
| `time` als Infrastruktur-Timestamp | Kafka-Timestamp statt fachlichem Zeitpunkt | Fachlichen Zeitpunkt des Ereignisses setzen |
| Fehlende Idempotenz | Consumer verarbeitet Events mehrfach ohne Deduplizierung | `ce_id` fuer Deduplizierung nutzen |

## Nicht im Scope

- Schema-Registry-Konfiguration
- Kafka-Cluster-Setup / Infrastruktur
- Topic-Naming-Konventionen (separater Skill)
- Avro-Schema-Design (separater Skill)
