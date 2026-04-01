Kontext & Problemstellung

In unserer Kafka-Landschaft werden Nachrichten derzeit ohne einheitlich definiertes Schema für Metadaten publiziert. Metadaten wie Event-Typ, Quelle, Korrelation oder Tracing werden daher inkonsistent und in individuellen Lösungen abgebildet – teilweise im Payload, teilweise in Headern oder implizit über Konventionen. Diese Inhomogenität erschwert es, Tooling und Plattform-Funktionalitäten (z. B. Monitoring, Tracing, Replay, DLQ-Analyse) eindeutig zu konfigurieren und Metadaten zuverlässig zu interpretieren oder darzustellen.

Es braucht eine verbindliche Message-Struktur (Envelope + Payload), die domänenübergreifend konsistent ist und Wachstum kontrolliert.

Da durch CloudEvents schon ein Protokoll existiert, das sich dem Problem des Event Envelopes und Event Metadaten widmet, möchten wir uns darauf stützen.
Ziele

    Einheitlicher Envelope für Metadaten (id, type, source, time, tracing, schema/version) orientiert an CloudEvents

    Saubere Trennung: Metadaten vs fachlicher Payload

    Observability (Trace/Span, correlationId)

    Fehlerhandling & Replay (idempotent, deterministisch)

    Kurzfristig: Unterstützung von Entity-Streams als Übergangslösung

    Langfristig: Reduktion von Entity-Streams zugunsten expliziter Domain-Events

CloudEvents

CloudEvents ist eine technologie-agnostische Spezifikation zur Beschreibung von Ereignisdaten in gängigen Formaten, um die Interoperabilität zwischen Diensten, Plattformen und Systemen zu gewährleisten.

Die genaue Zusammenstellung der Attribute in einem CloudEvent ist hier zu finden.
Events bei RTL

Wir definieren - kompatibel zur CloudEvents-Spezifikation - die folgenden Attribute als Event-Envelope:

Name


Beschreibung


Beispiele


Motivation


Pflicht

id


Identifiziert das Event, bzw. die Nachricht. Producer müssen sicherstellen, dass source + id langfristig eindeutig ist.

Identifiziert nicht die Entität.


44e149f7-8533-4d2c-814c-70e4fc8d4841

12


Konformität zur CloudEvents Spezifikation; Voraussetzung für Idempotenz, Deduplication, Replay und Fehleranalyse.


Check Mark

source


Identifiziert den Kontext indem ein Event auftritt. Um die Eindeutigkeit von source + id zu gewährleisten, bietet es sich an, den Geschäftsprozess als Bestandteil der source zu sehen.

Der Geschäftsprozess kann ggf. bereits aus dem mehrstufigen fachlichen Kontext abgeleitet werden, der im Topic-Namen verankert ist, siehe .

Der Kontext definiert explizit keine API-Endpunkte.


https://netrtl.com/assets/asset-management

https://netrtl.com/assets/asset-assignments

https://netrtl.com/planning/linear/right-assignment

https://netrtl.com/planning/linear/run-allocation


Konformität zur CloudEvents Spezifikation. Fördert Ownership, Debugging, Auditing und Governance.


Check Mark

specversion


Die Version der CloudEvents-Spezifikation, die dem Event zugrundeliegt.


1.0


Konformität zur CloudEvents Spezifikation. Ermöglicht saubere Interpretation der Attribute.


Check Mark

type


Beschreibt die Art des Events und den Zusammenhang mit dem Vorfall, der das Event hervorgerufen hat.

Kann in Zukunft auch für die Version des Events verwendet werden.


assets.material.asset-created.v1

assets.material.created

planning.linear.rights-assigned

planning.linear.run-allocated


Konformität zur CloudEvents Spezifikation. Erkennen des Eventtyps vor Deserialisierung.


Check Mark

datacontenttype


Der MediaType des Contents gemäß RFC 2046.


application/avro

application/json


Konformität zur CloudEvents Spezifikation.

Derzeit gibt es unterschiedliche MediaTypes bei RTL.


Cross Mark

subject


Beschreibt die fachliche Identität hinter dem Event.

Zusätzlich zu dieser Identität können weitere Gruppierungsebenen eingeführt werden, die Downstream-Systemen ermöglichen anhand dieser zu filtern.


small-material/287fc583-cc46-4d72-93ba-cad3640f1894

adv-material/ariel-spot-1.mp4

content-material/titanic-mov-de.mp4

287fc583-cc46-4d72-93ba-cad3640f1894


Konformität zur CloudEvents Spezifikation. Möglichkeit der Filterung nach spezifizierten Themen im Consumer ohne die Notwendigkeit der Deserialisierung.


Cross Mark

time


Der Zeitstempel des Vorfalls, den das Event hervorgerufen hat gemäß RFC 3339.


2025-04-12T23:20:50.52Z

1996-12-19T16:39:57-08:00


Konformität zur CloudEvents Spezifikation.

Infrastrukturkomponenten kennen mehrere Zeitstempel rundum das Senden und Empfangen von Events. Dieser Zeitstempel wird durch das produzierende System gesetzt und bestimmt den fachlichen Zeitpunkt des Ereignisses unabhängig von Infrastruktur.


Check Mark

Event Data


Der fachliche Inhalt des Events (Payload).

Wird üblicherweise durch eine Avro Schema gestützt.





Konformität zur CloudEvents Spezifikation.


Cross Mark

Distributed Tracing (extension)

traceparent


Beinhaltet eine Version, TraceId, SpanId und Trace-Optionen gemäß W3C Trace Context.

Besteht noch kein Trace Context wird einer erstellt.


00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-00


Konformität zur CloudEvents Spezifikation und W3C.


Check Mark



Extension Context Attributes

messagekey


Extension für den Key der Message; definiert eine Beziehung zwischen mehreren Events. Er kann aus dem subject abgeleitet werden. Standardmäßig wird dieser zusätzlich verwendet um Events zu partitionieren.

Der messagekey sollte möglichst kurz, aber aussagekräftig genug sein.

Für Partitionierung abseits des Standardfalls gilt .


c57f740a-e871-4996-9f3d-592a3f7bfed3


Der messagekey ist nicht Teil der CloudEvents Spezifikation, ist jedoch kompatibel, weil die Spezifikation sogenannte Extension Context Attributes definiert.

Wir haben uns trotzdem dazu entschieden ihn als Attribut aufzunehmen, weil wir uns auf Kafka als Infrastrukturkomponente stützen möchten, diese einen messagekey kennt und dessen Benutzung sich im Haus etabliert hat.


Cross Mark

correlationid


Extension zur fachlichen Nachverfolgung zusammengehöriger Nachrichten über System-, Service- und Topic-Grenzen hinweg


2b9e7524-bbd9-469e-a510-95c10795d561


Ermöglicht uns fachlich zusammenhängende Prozessschritte zu identifizieren.


Cross Mark

causationid


Extension für die Id des auslösenden Events.

Falls diese angegeben wurde, darf sie nicht verändert werden und muss weitergeleitet werden.


Siehe id.


Ermöglicht die Ursachenermittlung bei Prozessschritten.


Cross Mark
Lösungsalternativen
Option 1: Envelope als Header, nicht im Avro-Schema
Beispiele
Asset wurde erstellt

Header
id: ca39745a-2b82-4097-9cdf-c40814492c1c
source: https://netrtl.com/assets/asset-management
specversion: 1.0
type: assets.material.asset-created.v1
datacontenttype: application/avro
subject: small-material/287fc583-cc46-4d72-93ba-cad3640f1894
time: 2026-01-05T14:26:50.524Z
traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
correlationid: 287fc583-cc46-4d72-93ba-cad3640f1894
causationid: 44e149f7-8533-4d2c-814c-70e4fc8d4841

Key (messagekey)
287fc583-cc46-4d72-93ba-cad3640f1894

Value
{
"assetId": "287fc583-cc46-4d72-93ba-cad3640f1894"
}
Vorteile

    Standardnähe & Interop
    CloudEvents-Begriffe (id, type, source, time, subject) sind am Markt etabliert. Das erleichtert spätere Integrationen (Gateways/Bridges/Tooling) und senkt Mapping-Aufwand.

    Saubere Schema-Evolution mit Schema Registry
    Der Avro-Value bleibt rein fachlich. Änderungen am Envelope (z. B. zusätzliche Metadaten) erzwingen keine Schema-Änderung am Value.

    Bessere Observability ohne Deserialisierung
    Korrelation/Tracing/Typ/Quelle sind aus Headern sofort verfügbar (Logging, Monitoring, DLQ-Analyse, Routing), auch wenn das Value-Schema unbekannt ist.

    Leitplanken gegen Metadaten-Wildwuchs
    Extensions sind möglich, aber das Kernset ist klar; das minimiert das Risiko, dass der Envelop unkontrolliert wächst.

    Trennung von Plattform und Domäne
    Platform-Metadaten bleiben außerhalb des Domänenvertrag, was DDD/Bounded Contexts unterstützt.

    Unterstützung von Snapshots und Entity-Streams 
    Alle derzeit vorhandenen Snapshot Topics und Entity-Streams können auf diesen Standard umstellen, ohne ihren Payload zu ändern, da die semantische Veränderung zunächst im Envelope stattfindet.

Nachteile

    Header-Tooling & Sichtbarkeit
    Nicht jedes Tool oder jede UI zeigt Kafka-Header gut. Debugging/Inspection kann je nach Stack unbequemer sein.

    Implementierungsdisziplin unabdingbar
    Ohne zentrale Library/Interceptor droht der Drift (fehlende Pflichtheader, unterschiedliche Namen/Encodings). 

    Header unterliegen keinem Schema
    Durch ein fehlendes Schema, muss besonders viel Augenmerk auf die Kompatibilität der Datentypen gelegt werden.

    Semantik muss verstanden werden
    Teams müssen CloudEvents-Semantik sauber leben (z. B. type ist nicht Topic, source ist nicht Environment).
Entscheidung

Es wird Option 1 – Envelope als Header, nicht im Avro-Schema gewählt.

Der Event-Envelope orientiert sich an der CloudEvents-Spezifikation und wird in den Kafka Message Headern transportiert.
Die fachliche Payload wird weiterhin als Avro-serialisiertes Event im Message Value übertragen.

Begründung

Option 1 bietet die beste Balance zwischen Standardnähe, Evolvierbarkeit und pragmatischer Einsetzbarkeit in der bestehenden Kafka-Landschaft.
Insbesondere ermöglicht sie:

    eine saubere Trennung zwischen fachlichem Domainvertrag (Avro Value) und technischen bzw. semantischen Metadaten (Envelope),

    eine kontrollierte Schema-Evolution mit Avro und Schema Registry, da Änderungen am Envelope keine Value-Schema-Änderungen erzwingen,

    eine inkrementelle Migration bestehender Snapshots und Entity-Streams, da der Payload unverändert bleiben kann und die semantische Verbesserung zunächst im Envelope erfolgt,

    verbesserte Observability und Governance, da Korrelation, Tracing und Event-Semantik ohne Deserialisierung verfügbar sind.


Die folgenden Leitplanken sind verbindlich und gelten für alle neu entwickelten Kafka Producer und Consumer.
MUST

    Jede Kafka Message muss einen Event-Envelope in den Message Headern enthalten.

    Der Envelope muss mindestens die folgenden Attribute enthalten:

        id

        source

        specversion

        type

        time

        traceparent

    Der Message Value darf keine technischen Metadaten (z. B. Tracing, Korrelation, Governance) enthalten.

    type muss ein fachliches Ereignis beschreiben (z. B. …created, …assigned, …released) und darf keine Entitäts-Updates oder Snapshots implizieren.

    Kafka Messages dürfen nicht implizit als Event interpretiert werden, wenn sie faktisch Entity-Streams oder Snapshots darstellen.

    Falls eine fachliche Identität existiert, soll der messagekey aus dem subject abgeleitet werden, um Reihenfolge pro Entität zu garantieren.

    traceparent muss propagiert werden, sofern ein technischer Tracing-Kontext existiert oder erstellt werden kann.

SHOULD

    correlationid soll gesetzt und über System-, Service- und Topic-Grenzen hinweg propagiert werden, sofern ein fachlicher Prozesskontext existiert.

    causationid soll gesetzt werden, sofern ein auslösendes Event identifiziert werden kann.

    Event-Typen sollten versioniert werden (z. B. …v1, …v2).

MAY

    Bestehende Entity-Streams dürfen weiterhin betrieben werden, müssen jedoch den definierten Envelope-Standard einhalten.

    Snapshots dürfen veröffentlicht werden, müssen jedoch explizit als solche erkennbar sein (z. B. über type oder Topic-Naming).

    Zusätzliche Metadaten dürfen über CloudEvents-Extensions ergänzt werden, sofern sie fachlich begründet und dokumentiert sind.

    Existierende Event-Envelopes im Message Value sollen Schritt für Schritt migriert werden (bspw. mit Topic-Versionierung).

Nicht-Ziel / Klarstellung

Dieses ADR erzwingt keine sofortige Abschaffung bestehender Snapshots oder Entity-Streams.
Es schafft jedoch eine verbindliche Struktur und Semantik, die eine schrittweise Migration hin zu expliziten Domain Events ermöglicht und langfristig fördert.