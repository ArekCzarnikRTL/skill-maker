# kafka-envelope

A Claude Code skill for generating and reviewing Kafka producer/consumer code
that conforms to the CloudEvents-based envelope standard.

## Installation

Copy the `kafka-envelope/` directory to your Claude Code skills location:

- **Personal:** `~/.claude/skills/kafka-envelope/`
- **Project:** `.claude/skills/kafka-envelope/`
- **Plugin:** Follow the Claude Code plugin packaging guide

## Usage

The skill activates automatically when:
- Writing new Kafka producer/consumer code
- Reviewing existing Kafka code
- Kafka client libraries are imported in the project

Or invoke explicitly by referencing "kafka-envelope" in your prompt.

## Modes

- **Generate:** Scaffolds producer/consumer with correct CloudEvents headers
- **Review:** Audits existing code against MUST/SHOULD/MAY rules

## Source

Based on ADR "Kafka Message Struktur" — CloudEvents-compatible envelope
for Kafka messages with headers in binary content mode.

## Examples

Writing new Kafka consumer mit namen OderEvent Consumer der OrderEvents. Das Topic name ist "rlt.oders.events".