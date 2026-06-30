# 2. Tunable gameplay values are custom gamerules

Status: Accepted

## Context

Several GPigT behaviours need to be adjustable per world, in-game, without
recompiling, and must be **server-authoritative** (correct in multiplayer — the
value cannot depend on which client is looking). The first three:

- how far a GPigT searches for a sign,
- how long a `CLAIMED` sign may sit before another GPigT can reclaim it,
- how fast a GPigT spins while pondering.

The mock ponder *duration* is different in kind: it is a temporary testing value
that disappears once the real LLM backend (OpenRouter) is connected, so it does
not need a player-facing knob.

## Decision

Expose persistent, server-authoritative tunables as **custom gamerules** via
Fabric's `GameRuleRegistry`. Current registry:

| Gamerule           | Type            | Default | Bounds / notes                                    |
|--------------------|-----------------|---------|---------------------------------------------------|
| `gpigtHuntRadius`  | int (blocks)    | 32      | clamp 1–128; bounds the sign search               |
| `gpigtClaimExpiry` | int (seconds)   | 30      | converted to ticks; MUST exceed max ponder time   |
| `gpigtPonderSpin`  | int (deg/tick)  | 30      | clamp 0–360; 0 = no spin                          |

Temporary / dev-only values (the mock ponder duration) stay as plain code
constants, kept easily editable, and are removed when the real backend lands.

## Consequences

- Players and server admins tune behaviour with `/gamerule`, per world, persisted
  in the save, and correct in multiplayer.
- Gamerules are **int or boolean only** — no floats. Durations are expressed as
  integer seconds and converted to ticks internally.
- Gamerule input is unrestricted, so every value is **clamped at read time**
  (e.g. negative radius, spin outside 0–360) rather than trusted.
- The `gpigtClaimExpiry` vs ponder-time coupling (see ADR-0001) means the default
  must be revisited once real LLM latency — which is variable — replaces the mock.

## Alternatives considered

- **Config file (JSON/TOML)** — rejected. Editable, but not in-game, and in
  multiplayer needs server file access plus a reload step.
- **In-game options GUI (Cloth Config + Mod Menu)** — rejected/deferred. It is a
  heavier dependency and is client-side, which is semantically wrong for
  server-authoritative gameplay values.
