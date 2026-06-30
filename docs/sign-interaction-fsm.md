# SignInteractionGoal — state machine

> Reference for the GPigT sign-answering goal. **Temporary home** — fold this
> into the class-level doc comment on `SignInteractionGoal` when that class is
> written, then delete this file so there is a single source of truth.
>
> Terms (`NONE`/`QUESTION`/`CLAIMED`/`ANSWER`, Hunt, Ponder) are defined in
> [CONTEXT.md](../CONTEXT.md). Data model: [ADR-0001](adr/0001-sign-state-data-model.md).
> Tunables: [ADR-0002](adr/0002-tunables-as-gamerules.md).

## Core invariant

**Every transition and every world/entity access runs on the server thread.**
The only off-thread work is the ponder (LLM) call body; its result is handed
back through a `volatile` field that the server-thread tick polls. Nothing
touches the world, the entity, or the sign off-thread.

The FSM lives entirely in the goal. The entity holds only a `huntRequested`
trigger flag, set by `mobInteract`.

## Gating

- **Trigger:** player right-clicks the GPigT with a carrot or golden carrot →
  `mobInteract` consumes one item and sets `huntRequested = true`. (Carrot /
  golden carrot do not breed or tempt; potato & beetroot still breed.)
- **`canUse()`:** `huntRequested` AND a `QUESTION` sign exists within
  `gpigtHuntRadius` → cache the nearest as `targetSignPos`.
- **`canContinueToUse()`:** `state != IDLE`.
- **Flags:** `setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK))` so it cooperates with
  the inherited pig goals (panic, tempt, wander).

## Transitions

| From → To          | Condition / trigger        | Action                                                                                          | Thread        |
|--------------------|----------------------------|-------------------------------------------------------------------------------------------------|---------------|
| (start) → HUNTING  | goal starts                | `state=HUNTING`, clear `huntRequested`                                                           | server        |
| HUNTING → READING  | within ~1.5 blocks         | recheck sign still `QUESTION`; if ok → set `CLAIMED` + `CLAIM_TICK`, look at sign                | server        |
| HUNTING → IDLE     | recheck fails / sign gone  | abort                                                                                            | server        |
| READING → THINKING | text read                  | `SignIO.readQuestion`; log "<name> is pondering the infinite cosmos" once; launch async ponder   | server        |
| (ponder body)      | —                          | mock: `delayedExecutor(DURATION)` → `"??????"`; set `volatile pendingResponse`                   | **async**     |
| THINKING (tick)    | pondering                  | rotate yaw by `gpigtPonderSpin`°, keep look at sign, stop nav, poll `pendingResponse`            | server        |
| THINKING → WRITING | `pendingResponse != null`  | —                                                                                               | server        |
| WRITING → IDLE     | ok                         | recheck sign exists & still `CLAIMED`; `SignIO.writeAnswer` (front→back, answer→front, `ANSWER`) | server        |
| WRITING → IDLE     | recheck fails              | abort, no write                                                                                  | server        |
| IDLE               | —                          | `canContinueToUse` false → `stop()`                                                              | server        |

## Cleanup rules

- **`stop()` releases an unfinished claim:** if interrupted (panic, damage,
  owner pulls it away) while holding a `CLAIMED` sign it has not answered, reset
  that sign `CLAIMED → QUESTION` so it is instantly re-huntable.
- **Death / unload / crash:** the claim simply expires — any GPigT that finds a
  `CLAIMED` sign older than `gpigtClaimExpiry` reclaims it (see ADR-0001). This
  is why claims need no owner tracking, and why `gpigtClaimExpiry` MUST exceed
  the maximum ponder time.

## Ponder / LLM harness

The mock uses the **real** async structure: a `CompletableFuture` (own small
executor, not the shared `ForkJoinPool`) returns `"??????"` after `DURATION`.
Wiring OpenRouter later only replaces the delayed sleep with the HTTP call — the
future + `volatile` hand-off stay. `DURATION` is a plain editable constant and
goes away at integration.
