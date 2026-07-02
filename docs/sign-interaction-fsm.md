# Sign interaction — deferred CLAIMED mechanics

> The core state machine (HUNTING → READING → THINKING → WRITING → IDLE),
> gating, threading model and abort rules are implemented and documented in
> the class Javadoc of `SignInteractionGoal` — that is the source of truth.
> This file keeps only the parts **not yet built**: the `CLAIMED` state
> (issue #20) and its expiry (see [ADR-0001](adr/0001-sign-state-data-model.md))
> plus the gamerule tunables ([ADR-0002](adr/0002-tunables-as-gamerules.md),
> issue #19). Terms are defined in [CONTEXT.md](../CONTEXT.md).

## CLAIMED transitions (issue #20)

When multi-GPigT claiming lands, the goal gains these steps:

| Where in the FSM    | Change                                                                     |
|---------------------|----------------------------------------------------------------------------|
| HUNTING → READING   | on arrival, recheck sign is still claimable; if ok → set `CLAIMED` + `CLAIM_TICK` |
| WRITING             | recheck sign still `CLAIMED` (by this hunt) before writing                 |
| target selection    | skip signs another GPigT holds a fresh claim on                            |

## Cleanup rules

- **`stop()` releases an unfinished claim:** if interrupted (panic, damage,
  owner pulls it away) while holding a `CLAIMED` sign it has not answered, reset
  that sign `CLAIMED → PROMPT` so it is instantly re-huntable.
- **Death / unload / crash:** the claim simply expires — any GPigT that finds a
  `CLAIMED` sign older than `gpigtClaimExpiry` reclaims it (see ADR-0001). This
  is why claims need no owner tracking, and why `gpigtClaimExpiry` MUST exceed
  the maximum ponder time.

## Tunables (issue #19)

`HUNT_RADIUS`, `SPIN_DEGREES_PER_TICK` in `SignInteractionGoal` become the
`gpigtHuntRadius` / `gpigtPonderSpin` gamerules; `gpigtClaimExpiry` arrives
with claiming. The mock ponder delay stays a code constant until the real
backend is wired in.
