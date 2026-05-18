# 02 - Caching: Explain It in Simple English

> Goal: explain every idea here OUT LOUD, in simple clear English, to a
> US coworker or interviewer. Read each script aloud 3 times.
> Simple words. Short sentences. No jargon without explaining it.

---

## The one-line definition (memorize this)

> "A cache is a small, fast copy of data you put in front of a slow
> source, so you don't do the same expensive work twice. You trade a
> little correctness for a lot of speed."

---

## The sticky-note analogy (your 60-second story)

Say this without notes:

> "Imagine people keep asking me for someone's phone extension. Looking
> it up in the thick phone book every time is slow. So I write the
> common ones on a **sticky note** on my monitor - that is the
> **cache**. Next time I just read the note - a **cache hit**. If it's
> not on the note, I open the phone book - a **cache miss** - and then
> add it to the note. The note has limited space, so when it's full I
> throw away the one I use least - that is **eviction**. And if someone
> changes their number, my note now **lies** - that is **stale data** -
> so I either cross it out (**invalidation**) or write an expiry on it
> (**TTL**)."

---

## Why we use it (say all four)

> "Without a cache, four things break:
> 1. **Slow.** Every request runs the heavy query, so latency is always
>    the full query time.
> 2. **The database does the same work N times.** If 10,000 users a
>    second view one hot product, the db reads the same row 10,000 times
>    a second. That's read amplification.
> 3. **Cost.** Scaling reads only by adding database replicas gets
>    expensive fast. One cache node can replace many db nodes.
> 4. **Spikes kill the database.** Without a cache absorbing the shock,
>    a traffic spike takes the db down first."

Short version: **"A cache means: don't do the same work twice."**

---

## The core terms (define each in one sentence)

| Term | Say it like this |
| --- | --- |
| **Cache hit / miss** | "Hit means the data was in the cache. Miss means we had to go to the slow source." |
| **Hit ratio** | "Hits divided by total reads. It measures whether the cache is actually worth it." |
| **Cache-aside** | "The app checks the cache; on a miss it reads the db and fills the cache. On a write it invalidates. The default pattern." |
| **Write-through** | "Write to the cache and the db together, so the cache is always fresh - but writes are slower." |
| **TTL** | "A time-to-live expiry. It bounds how stale the data can get." |
| **Eviction (LRU/LFU)** | "When the cache is full, drop the least recently used (or least frequently used) entry." |
| **Invalidation** | "On a data change, remove or refresh the cached copy. This is the genuinely hard part." |
| **Stampede (dogpile)** | "A hot key expires and a flood of misses all hit the db at the same instant." |
| **Single-flight** | "On a miss, only the first caller loads the value; everyone else waits and shares that one result." |
| **Penetration** | "Requests for a key that doesn't exist always miss. Fix: cache the 'not found' too." |
| **Avalanche** | "Many keys share the same TTL and expire together. Fix: add random jitter to the TTL." |

---

## Invalidation: why it's the hard part (the interview favorite)

> "There's a saying: the two hardest things in computer science are cache
> invalidation and naming things. Here's the trap. With cache-aside, a
> reader can have a miss and read the OLD value from the db. Before it
> stores that value, a writer updates the db and invalidates the cache.
> Then the reader resumes and writes its OLD value into the cache. Now
> the cache is wrong until the TTL. The pragmatic fix used everywhere is
> a TTL as a safety net - the system becomes eventually consistent.
> Stronger options are write-through, rechecking after the commit, or
> versioned keys."

---

## Scale-up vs scale-out / where the cache lives

> "**Local cache** lives inside the app process - nanosecond reads, but
> each instance has its own copy, so it can be stale across instances.
> **Distributed cache** like Redis is shared by all instances - about a
> millisecond, consistent across instances, but it's new infrastructure.
> **Multi-tier** puts a small local L1 in front of a shared L2 for the
> hottest keys - fastest, but the L1 can be briefly stale. **CDN edge**
> caches public static responses near the user - the cheapest, because
> the traffic never even reaches your app.
>
> To scale a distributed cache, you shard keys across nodes, and use
> consistent hashing so adding a node doesn't reshuffle everything."

---

## Trade-offs (always mention at least two)

> "A cache is not free. We gain low latency, less db load, and spike
> absorption. We pay with: **possible staleness**, **invalidation
> complexity**, **a new failure point and ops cost**, and **memory
> cost**. If the data is write-heavy, needs strong consistency, or is
> rarely reused, a cache is a net loss. Cache data that is read-heavy
> and changes rarely."

---

## Big tech, in one breath each

> - "**Redis** - the de facto standard distributed cache. Rich data
>   structures, TTL, atomic operations.
> - **Memcached** - a simpler pure key-value cache. Facebook runs it at
>   huge scale and uses 'leases' to fight stampedes and stale writes.
> - **Netflix EVCache** - a multi-region replicated tier on top of
>   Memcached.
> - **CDN edge (CloudFront / Cloudflare)** - caches public content near
>   the user using Cache-Control and ETag headers."

---

## How to read the code (say this while pointing at it)

- `R1` - "The naive cache. It works, but I left two flaws in on purpose:
  it grows forever and it can serve stale data forever."
- `R2` - "Fixes both: a TTL bounds staleness, and LRU plus a max size
  bounds memory."
- `R3` - "Fixes the stampede: single-flight collapses many concurrent
  misses on one key into a single db call. I prove it with 200 threads."
- `evolution/S0..S5` - "The same system growing: no cache, local cache,
  distributed cache, the invalidation race, the three load failure
  modes, and finally a multi-tier cache."

---

## 30-second answer (rehearse this exact text)

> "A cache is a fast copy in front of a slow source so we don't repeat
> expensive work. I usually start with cache-aside: check the cache, on
> a miss read the db and fill it, on a write invalidate it. I set a TTL
> to bound staleness and LRU to bound memory. The hard part is
> invalidation - races can re-poison the cache, so I rely on a TTL
> safety net for eventual consistency. For hot keys I add single-flight
> to prevent a stampede. I start with an in-process cache like Caffeine,
> move to Redis when I scale out to many instances, and put public
> read-heavy content on a CDN."

> Practice rule: record yourself saying the 30-second answer. If you use
> a Korean filler word or pause more than 2 seconds, do it again.
