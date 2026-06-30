# 1. Sign-state data model

Status: Accepted

## Context

A GPigT hunts the nearest sign that holds a player's prompt, responds to it, and
must never re-respond the same sign or treat another GPigT's response as a new
prompt. Multiple GPigTs may operate in the same world. We therefore need each
sign to carry a small piece of mod-specific state — its role in the GPigT
protocol — that:

- belongs to the sign itself (so it travels with the block and dies with it),
- persists across save/load,
- distinguishes a player's prompt from a GPigT's response from an unrelated
  decorative sign,
- can be reset when a player edits an responded to sign to continue the thread.

Players use **ordinary vanilla signs**; requiring a special crafted block is a
non-goal.

## Decision

Attach a four-state enum to the vanilla `SignBlockEntity` using the **Fabric Data
Attachment API**, plus a companion claim timestamp.

- `SignState { NONE, PROMPT, CLAIMED, RESPONSE }` (see CONTEXT.md for the
  canonical meanings). The attachment type uses a default initializer of `NONE`,
  so untouched signs resolve to `NONE` without storing anything (no NBT bloat)
  and reads never return null.
- A **Mixin** on the server-side sign-edit path
  (`ServerGamePacketListenerImpl#updateSignText`) sets a sign to `PROMPT` when
  a player writes non-blank text. This is what moves a sign `NONE → PROMPT`
  and resets `RESPONSE → PROMPT` (and `CLAIMED → PROMPT`) for thread
  continuation.
- When a GPigT reaches a `PROMPT` sign to ponder it, it sets the sign to
  `CLAIMED` and stamps a companion attachment, `CLAIM_TICK` — a `long` holding
  the **world game time** (ticks) at the moment of the claim. Other GPigTs skip
  `CLAIMED` signs, so two GPigTs never responds to the same prompt.
- When a GPigT writes its response it sets the sign to `RESPONSE`. This single
  write is what stops re-responseing and stops other GPigTs from treating the
  response as a prompt — no per-pig bookkeeping required.
- Claim cleanup is **timestamp-based**: any GPigT that finds a `CLAIMED` sign
  whose `CLAIM_TICK` is older than a threshold treats it as `PROMPT` and
  reclaims it. This self-heals the claimant dying, unloading, or a server crash
  mid-ponder in a single rule — no need to track which GPigT holds the claim.

## Consequences

- Multi-GPigT correctness falls out of the shared flag: any GPigT respects
  `RESPONSE` regardless of which GPigT wrote it.
- State is persisted with the block entity and is naturally discarded if the
  sign is broken.
- We take on one Mixin into a vanilla packet handler, because Fabric exposes no
  event for sign edits. The Mixin must target the **player** edit path only, not
  `SignBlockEntity#setText` (which the GPigT also calls), so a GPigT's own write
  is never misread as a player prompt. It must also ignore blank edits, so
  closing the edit GUI on an empty sign does not create a phantom `PROMPT`.
- The enum leaves room for further states (e.g. `ERROR`) without a data
  migration.
- The claim-expiry threshold MUST exceed the maximum ponder time, or a slow but
  healthy GPigT will have its sign reclaimed mid-ponder while it is still
  working. `CLAIM_TICK` uses world game time (ticks), not wall-clock, so it is
  deterministic and survives save/load.

## Alternatives considered

- **Custom sign block / block entity** — rejected. It only attaches to our own
  block, which breaks the "any vanilla sign works" goal and forces players to
  craft special signs, for no benefit.
- **A shared set of responses `BlockPos` in world save data** — rejected. The
  state does not travel with the sign (stale entries when signs are broken and
  replaced) and it cannot observe a player re-editing a sign.
- **Storing the GPigT's last-written text (or its hash) and inferring state by
  comparison** — rejected. It avoids the Mixin, but it cannot represent the
  `NONE` (out-of-scope) state — a decorative sign also has text — so it collapses
  to "every sign is huntable" and reintroduces wasted carrots/tokens on
  decorative signs.
