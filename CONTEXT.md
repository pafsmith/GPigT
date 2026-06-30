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
- **QUESTION** — a sign a player has written a question on that has not yet
  been answered. This is what a GPigT hunts.
- **CLAIMED** — a QUESTION sign a GPigT has reached and is currently pondering.
  The claim is recorded with a **claim timestamp** (the world time at which the
  GPigT claimed it). Other GPigTs skip a CLAIMED sign, so two GPigTs never
  answer the same sign — unless the claim is **stale**: if its timestamp is
  older than a set threshold (the claiming GPigT died, unloaded, or the server
  crashed mid-ponder), another GPigT may reclaim it.
- **ANSWER** — a sign a GPigT has written its response onto. Ignored by every
  GPigT, including ones other than the author.

**Re-question:** writing new question text onto a sign always returns it to
QUESTION, so a player can continue the thread on the same sign.
