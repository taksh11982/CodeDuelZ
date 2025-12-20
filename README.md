<h1 align="center"><strong>CodeDuelZ</strong></h1>

CodeDuelZ is our real-time 1v1 competitive programming platform. Two players enter, one emerges victorious. Find an opponent, solve the same problem head-to-head under time pressure, and let an ELO rating system track your skill across platforms. It's like Chess.com but for algorithms.

---
Why We're Building This

Existing platforms have a gap:

    LeetCode is for solo learning

    Codeforces is for massive contests

    No platform unifies ratings across LeetCode, CodeChef, and Codeforces

---

CodeDuelZ fills that gap. Real-time 1v1 battles, unified ELO, and the satisfaction of climbing a leaderboard.
What We're Building

Real-Time 1v1 Battles 🔥
Two players, one problem, simultaneous coding. WebSocket keeps editors synced. When both submit, instant verdict tells you who won and how your ELO shifted.

Unified ELO System 📊
We automatically pull your ratings from LeetCode, Codeforces, and CodeChef. 1200 LeetCode + 1600 Codeforces = one meaningful unified rating. No more fragmentation.

Live Spectator Mode 👥
Friends watch matches in real-time. See code, thought process, and the exact moment of submission. A live chat lets spectators discuss and cheer.

Cross-Platform Profile 🔗
Connect external accounts once. We automatically sync your rating history, recent contests, and problem-solving patterns (strong in graphs, weak in DP).

Global Leaderboards 🏆
See where you stand globally or among friends. Daily, weekly, all-time rankings.

Curated Problems 📚
Curated challenges from easy to hard. Each includes explanations and hidden test cases.

---
Tech Stack

    Frontend: React 18, TypeScript, Vite, Monaco Editor, Tailwind

    Backend: Spring Boot 3, Java 17, Spring Security, JPA

    Real-Time: WebSocket + SockJS

    Database: MySQL / PostgreSQL

    Judge: Docker containers

    Deployment: Vercel (frontend), Railway (backend)

Database Schema 📊

8 core entities:

    User - Authentication & account

    Profile - Extended info, ELO, stats

    Problem - Immutable coding challenges

    Match - 1v1 battle data

    Submission - Code submissions with verdicts

    Leaderboard - Rankings

    Friendship - Social connections

    Admin - Admin roles & permissions

---

How Matches Work

Find Match → Backend queues you with preference & difficulty → Two compatible players found → Match room created → Split-screen editor with WebSocket sync → Both solve in real-time → Submit code → Judge returns verdict in milliseconds → ELO updates instantly → Match ends.
ELO System 📈

Standard Elo formula:

text
Expected Score = 1 / (1 + 10^((opponent_rating - your_rating) / 400))
New Rating = Current Rating + K * (Result - Expected Score)

K=32 for most players, 16 for 2400+. Beat higher-rated opponents, gain more. Lose to lower-rated, lose more.
Judge Verdicts ✅

    AC - Accepted (correct)

    WA - Wrong Answer

    TLE - Time Limit Exceeded

    RTE - Runtime Error

    CE - Compilation Error

---

Status 🚧

Under development. Building to launch.

MIT License. Use it or build on it.
