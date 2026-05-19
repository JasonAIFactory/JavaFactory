# 03 - Load Balancing: Explain It in Simple English

> Goal: you can explain every idea here OUT LOUD, in simple clear English,
> to a US coworker or interviewer. Read each script aloud 3 times.
> Simple words. Short sentences. No jargon without explaining it.

---

## The one-line definition (memorize this)

> "A load balancer sits in front of a pool of identical servers and
> spreads incoming traffic across the **healthy** ones. It gives us more
> capacity, high availability, and zero-downtime deploys."

---

## The restaurant analogy (your 60-second story)

Say this without notes:

> "Think of a popular restaurant. With one dining room, the line goes out
> the door. So we build several identical rooms and put a **host at the
> door**. The host sends each party to a free room - that is the **load
> balancer** spreading traffic. If a room has a problem, the host stops
> seating people there - that is a **health check** ejecting a bad server.
> Sometimes a party must return to the same room - that is a **sticky
> session**. And if we ever close a room, we let the people inside finish
> first - that is **connection draining**. The danger: if there is only
> **one host** and the host walks away, nobody gets seated - so we run
> **several load balancers**."

---

## Why we use it (say all four)

> "Without a load balancer, four things break:
> 1. **Capacity ceiling.** One server has a hard limit. Past it, requests
>    are rejected.
> 2. **Single point of failure.** If that one server dies, everything is
>    down.
> 3. **No horizontal scale.** Buying a bigger box has a limit; to add more
>    boxes you need something in front to split traffic.
> 4. **No zero-downtime deploy.** With one box, restarting it is an
>    outage."

---

## L4 vs L7 (you WILL be asked this)

> "An **L4** load balancer works at the transport layer. It only sees IP
> and port. It just forwards the connection. It is very fast but dumb -
> it cannot read the URL.
>
> An **L7** load balancer works at the application layer. It reads the
> HTTP request, so it can route by path or header, do cookie-based
> stickiness, terminate TLS, and retry. It costs a little more CPU per
> request.
>
> Big systems use both: a fast L4 layer spreads raw traffic across many
> L7 proxies, and the L7 proxies do the smart per-request routing."

---

## The algorithms and their trade-offs

> "**Round robin** sends one request to each server in turn. It is fine
> when servers are identical. But if one server is slower, round robin
> still sends it an equal count, so its queue and tail latency explode.
>
> **Least connections** sends the next request to the server with the
> fewest in-flight requests, so it naturally steers away from a slow or
> overloaded server.
>
> **Consistent hashing** maps a key to a server so the same key always
> lands on the same server - good for stickiness and cache locality - and
> when a server is added or removed, only that server's keys move, not
> all of them.
>
> **Power of two choices** picks two servers at random and sends to the
> less busy one. It is almost as good as perfect balancing but needs no
> global state, so real proxies love it."

---

## Health checks (the heart of an LB)

> "**Active** health check: the load balancer calls `/health` on each
> server every few seconds. If it fails, the server is **ejected** from
> rotation and added back when it recovers.
>
> **Passive** health check: if real requests to a server fail several
> times in a row, eject it - because `/health` can say OK while the
> server is actually broken.
>
> **Slow start**: when a server comes back, ramp traffic up slowly. If
> you flood a cold server with a cold cache, it falls over again.
>
> The trade-off is the interval: too long and traffic leaks to a dead
> server; too short and the health checks themselves become load."

---

## Sticky sessions (easy but costly)

> "If a server keeps the login session in its own memory, the next
> request that lands on a different server says 'who are you?' and the
> user is logged out. **Sticky sessions** route the same session to the
> same server to fix that.
>
> But stickiness has a cost: load gets uneven because some servers get
> hot users; if that server dies, those sessions are gone anyway; and
> deploys and autoscaling get harder because you are pinning users.
>
> The real answer is to make servers **stateless** - put the session in a
> shared store like Redis, or in a signed token like a JWT - so any
> server can serve any request. Stickiness is the fallback when you
> cannot do that."

---

## How does the load balancer itself scale? (great senior question)

> "Once backends scale out behind the LB, the **LB becomes the new single
> point of failure and bottleneck**. We fix that with: multiple load
> balancers with client failover; DNS round robin or **anycast** so
> clients can reach any LB; and often an L4 layer in front that spreads
> raw traffic across many L7 proxies - that is how Google's Maglev works.
>
> And for deploys we use **connection draining**: to remove a backend, we
> first stop sending it new requests, wait for its in-flight requests to
> finish, and only then remove it. That is how you deploy with zero
> dropped requests."

---

## The 30-second version (for a quick interview answer)

> "A load balancer spreads traffic across a pool of identical servers so
> we get capacity and availability. L4 routes by IP and port and is fast;
> L7 reads HTTP so it can route by path or cookie. It health-checks
> backends and ejects the dead ones. Round robin is the default, but
> least-connections is better when servers are uneven. Sticky sessions
> are a fallback - stateless servers with a shared session store are
> better. And the LB itself must be made highly available, usually with
> multiple LBs behind anycast, plus connection draining for zero-downtime
> deploys."

---

## Words you must be able to say correctly

| Term | Say it like | One-line meaning |
| --- | --- | --- |
| backend pool | "the pool of identical servers behind the LB" | the servers doing the work |
| eject / drain | "eject a dead node", "drain before removing" | remove from rotation safely |
| sticky / affinity | "session affinity" | same key → same server |
| anycast | "one IP, nearest LB answers" | many LBs share an IP |
| consistent hashing | "only the removed node's keys move" | minimal reshuffle on change |
| outlier detection | "auto-eject a node with a bad error rate" | Envoy's passive ejection |
| zero-downtime deploy | "drain, then replace, no dropped requests" | safe rollout |
