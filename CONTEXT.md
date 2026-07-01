# GPigT — Shared Language

Glossary only. No implementation details. Defines the words we use so the
code, the docs, and the conversation all mean the same thing.

## GPigT
A pig-derived creature that answers questions players write on signs. Each
GPigT acts independently; one GPigT's work never spills into another's.

## Hunt
A GPigT seeking out the nearest **Pending** sign in order to answer it.
Triggered by feeding the GPigT a carrot or golden carrot held in hand.

## Ponder (Pondering)
The phase in which a GPigT is composing its answer to a question. While
pondering, a GPigT visibly spins in place.

## Sign state
A sign is always in exactly one of four states with respect to GPigTs. These
are the canonical names — code and docs use the same words.

- **NONE** (default) — a sign no GPigT will touch: freshly placed, blank,
  purely decorative, or a pre-existing sign a player has never questioned on.
- **PROMPT** — a sign a player has written a prompt on that has not yet
  been responded to. This is what a GPigT hunts.
- **CLAIMED** — a QUESTION sign a GPigT has reached and is currently pondering.
  The claim is recorded with a **claim timestamp** (the world time at which the
  GPigT claimed it). Other GPigTs skip a CLAIMED sign, so two GPigTs never
  answer the same sign — unless the claim is **stale**: if its timestamp is
  older than a set threshold (the claiming GPigT died, unloaded, or the server
  crashed mid-ponder), another GPigT may reclaim it.
- **RESPONSE** — a sign a GPigT has written its response onto. Ignored by every
  GPigT, including ones other than the author.

**Re-question:** writing new question text onto a sign always returns it to
QUESTION, so a player can continue the thread on the same sign.

## Spawn Storm

The event that announces HOGZILLA's arrival: 30 lightning strikes scattered
within a 30-block radius over 2 seconds. Occurs once, immediately on spawn.

## HOGZILLA (HOGZ)

A colossal pig-derived boss entity (`MobCategory.MONSTER`). HOGZILLA never
attacks directly — its **Aura** is the sole source of all damage and
disruption. The threat is environmental, not melee.

## Stalk (Stalking)

HOGZILLA's sole objective: relentlessly pursue the nearest player. HOGZILLA
never abandons the Stalk. It moves at approximately one block per five seconds
in a direct 3D vector toward the target — no pathfinding, no navigation mesh.
HOGZILLA always moves this way; on flat ground it appears to walk, in the air
it floats, but the movement mode is identical. The walking animation plays
regardless of altitude. Any block in the path is destroyed instantly via
**Unstoppable**.

## Aura

The field of periodic hazards HOGZILLA radiates at all times while Stalking.
The Aura has three components: **Bolts**, **Salvo**, and **Porkzillary Forces**.
All cadences and radii are hardcoded (not exposed as gamerules).

- **Bolts** — 5–10 lightning strikes within a 20-block radius, every ~3 seconds.
- **Salvo** — 8–12 lit TNT fired radially in all directions, every ~8 seconds.
- **Porkzillary Forces** — 4–6 piglets spawned within 8 blocks, every ~15 seconds.

## Unstoppable

The property by which HOGZILLA immediately destroys any block it attempts to
move into — including obsidian, ancient debris, and bedrock. No player-built
structure stops HOGZILLA except **brick walls** (a reference to the Three
Little Pigs: the only material that holds against the wolf). Applies on all
axes during Airborne movement.
