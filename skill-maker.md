# Skill Maker — ADR zu Claude Code Skill

Universeller Prompt zur Erzeugung eines Claude Code Skills aus einem Architecture Decision Record (ADR).

---

## Prompt

```
Ich möchte aus dem folgenden ADR einen Claude Code Skill erzeugen.

### Input

- **ADR-Datei:** <Pfad zur ADR Markdown-Datei>
- **Skill-Name:** <gewünschter Skill-Name mit Hyphens, z.B. kafka-envelope>
- **Zielgruppe:** <wer soll den Skill nutzen, z.B. "Entwickler die Kafka Producer/Consumer implementieren">

### Anforderungen

1. **Prüfe den ADR** — Lies die ADR-Datei vollständig und identifiziere:
   - Verbindliche Regeln (MUST/MUSS)
   - Empfehlungen (SHOULD/SOLL)
   - Optionale Aspekte (MAY/KANN)
   - Beispiele und Anti-Patterns

2. **Recherchiere Best Practices** — Nutze context7 um die relevanten
   Spezifikationen/Standards zu prüfen, auf die sich das ADR stützt.
   Ergänze fehlende technische Details (z.B. Protokoll-Bindings, Prefixe, Encoding).

3. **Brainstorming (superpowers:brainstorming)** — Führe mich durch den
   Design-Prozess mit folgenden Klärungsfragen (eine pro Nachricht):

   a) **Trigger:** Wann soll der Skill aktiviert werden?
      (automatisch bei Code-Erkennung / explizit / beides)
   b) **Modi:** Welche Modi braucht der Skill?
      (z.B. Generate = neuen Code erzeugen, Review = bestehenden Code prüfen)
   c) **Sprache:** Sprachspezifisch oder sprachagnostisch?
   d) **Code-Tiefe:** Nur Regeln liefern / Scaffolding generieren /
      vollständiges Code-Template?
   e) **Distribution:** Persönlich / Projekt / Plugin-Paket?

4. **Design erstellen** — Präsentiere das Design sektionsweise:
   - Skill-Struktur (SKILL.md + Referenz-Datei)
   - Frontmatter (name, description mit "Use when..." Triggern)
   - Workflow pro Modus
   - Output-Formate (Checklist, Audit-Report)
   - Referenz-Datei Struktur (Attribut-Tabellen, Regeln, Beispiele, Anti-Patterns)

5. **Spec schreiben** — Speichere unter
   `docs/superpowers/specs/YYYY-MM-DD-<skill-name>-design.md`

6. **Implementierungsplan (superpowers:writing-plans)** — Erstelle Plan mit
   TDD-Zyklus:
   - Task: Referenz-Datei erstellen (Regeln, Tabellen, Beispiele aus ADR)
   - Task: SKILL.md erstellen (< 400 Wörter, Workflows, Checklisten)
   - Task: RED — Baseline OHNE Skill testen (Generate + Review)
   - Task: GREEN — MIT Skill testen, Baseline-Gaps verifizieren
   - Task: REFACTOR — Loopholes schliessen
   - Task: Packaging (README)

7. **Ausführung (superpowers:subagent-driven-development)** — Implementiere
   den Plan mit Subagents und Spec-Reviews nach jedem Task.

8. **Abschluss (superpowers:finishing-a-development-branch)** — Push/Merge.

### Skill-Struktur Schema

Das erzeugte Skill-Paket folgt immer dieser Struktur:

```
<skill-name>/
  SKILL.md              # Hauptdatei (< 400 Wörter)
  <reference>.md        # Referenz-Datei (Regeln, Tabellen, Beispiele)
  README.md             # Installationsanleitung
```

### SKILL.md Schema

```yaml
---
name: <skill-name>
description: >
  Use when <triggering conditions — KEINE Workflow-Zusammenfassung>.
---
```

Inhalt:
- Overview (1-2 Sätze)
- Modus-Entscheidung (Flowchart wenn > 1 Modus)
- Pro Modus: nummerierter Workflow + Output-Template
- Referenz via @<reference>.md

### Referenz-Datei Schema

1. **Attribut-/Regel-Tabellen** — Gruppiert nach Pflicht-Level (MUST/SHOULD/MAY)
2. **Regeln** — Wörtlich aus dem ADR, ergänzt um technische Details aus Specs
3. **Vollständiges Beispiel** — Ein konkretes, korrektes Beispiel
4. **Anti-Patterns** — Tabelle: Anti-Pattern | Problem | Korrektur

### Output-Formate

**Compliance Checklist (nach Code-Generierung):**
```
Compliance Check:
  [x] <attribut> — <status>
  [ ] <attribut> (<level>) — <handlungsempfehlung>
```

**Audit Report (nach Code-Review):**
```
Audit: <Klassenname>

MUST (Verstösse blockieren):
  [x] <attribut> — <status>
  [!] <attribut> — <problem>, <fix>

SHOULD (Empfehlungen):
  [ ] <attribut> — <empfehlung>

MAY:
  [ ] <attribut> — <vorschlag>

Findings:
  [!] <spezifisches Problem> — <fix>
```

Schweregrade: `[!]` = Verstoss, `[ ]` = Gap, `[x]` = konform.

### TDD-Zyklus für Skills

| Phase | Aktion |
|---|---|
| RED | Baseline OHNE Skill testen — dokumentiere was Claude falsch macht |
| GREEN | MIT Skill testen — verifiziere dass alle Baseline-Gaps behoben sind |
| REFACTOR | Loopholes schliessen — neue Rationalisierungen adressieren |

### Qualitätskriterien

- [ ] SKILL.md unter 400 Wörter
- [ ] Description beginnt mit "Use when..." (keine Workflow-Zusammenfassung)
- [ ] Alle ADR-Regeln in der Referenz-Datei abgebildet
- [ ] Technische Details aus Originalspezifikation ergänzt
- [ ] Anti-Patterns dokumentiert
- [ ] Baseline-Test zeigt messbare Gaps ohne Skill
- [ ] GREEN-Test schliesst alle Baseline-Gaps
- [ ] Compliance/Audit Output-Formate funktionieren
```

---

## Verwendung

1. Kopiere den Prompt-Block oben
2. Ersetze die `<Platzhalter>` mit deinen Werten
3. Starte eine neue Claude Code Session und füge den Prompt ein
4. Claude führt dich durch den gesamten Prozess
