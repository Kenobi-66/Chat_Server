# Multi-User Chat Server (Java)

A multi-threaded, multi-client chat server built with raw Java sockets, with message persistence backed by SQLite. Built as an Advanced Java (Semester 5) mini-project.

## Features

- **Concurrent multi-client chat** — each client connection is handled on its own thread via a fixed thread pool (`ExecutorService`), so multiple users can chat simultaneously.
- **Thread-safe user management** — connected clients are tracked in a `ConcurrentHashMap<Integer, PrintWriter>`, allowing safe concurrent reads/writes across client threads without external locking.
- **Message persistence (SQLite)** — every message is saved to a local `chat.db` file via a dedicated `DatabaseManager` class, so chat history survives server restarts.
- **Connection pooling (HikariCP)** — database access goes through a pooled `HikariDataSource` rather than opening a new connection per query, avoiding connection-overhead bottlenecks under concurrent load.
- **Chat history on join** — when a client connects, the last 50 messages are replayed to them from the database before live chat begins.

## Architecture

```
chatServer.java      → accepts incoming TCP connections, assigns each a client ID,
                        hands it off to the thread pool
ClientHandler.java    → one instance per connected client (Runnable); reads/broadcasts
                        messages, tracks connected clients via ConcurrentHashMap
DatabaseManager.java  → all SQL access (registering users, saving messages,
                        fetching history), backed by a HikariCP connection pool
chatClient.java       → terminal client — one thread listens for incoming messages,
                        the main thread reads and sends user input
```

**Why these choices (design decisions worth noting):**
- `ConcurrentHashMap` over `HashMap` — multiple client threads read/write the connected-users map concurrently; `ConcurrentHashMap` handles this safely without a global lock, and its iterators are weakly consistent (safe to iterate while another thread mutates it).
- SQLite over MySQL/Postgres — this is a single-process, embedded-file database with no separate server to install/run, which fits a self-contained demo project. The JDBC access pattern is the same as a networked database, so swapping in one later wouldn't require rewriting `DatabaseManager`.
- A dedicated `DatabaseManager` class — keeps all SQL out of `ClientHandler`, so persistence logic is testable and swappable independent of the networking code.

## Requirements

- JDK 17+ (uses `try-with-resources`, `var`, text blocks are not used but modern syntax is)
- Two external libraries (not included in this repo — see `.gitignore`):
  - [sqlite-jdbc](https://github.com/xerial/sqlite-jdbc/releases) (tested with `3.53.4.0`)
  - [HikariCP](https://github.com/brettwooldridge/HikariCP/releases) (tested with `5.x`)

Download both `.jar` files and place them in the project root before compiling.

## Build & Run

**Windows (PowerShell):**

```powershell
# Compile
javac -cp "sqlite-jdbc-3.53.4.0.jar;HikariCP-5.1.0.jar;." *.java

# Run the server
java -cp "sqlite-jdbc-3.53.4.0.jar;HikariCP-5.1.0.jar;." chatServer

# Run a client (in a separate terminal, once per user)
java chatClient
```

**macOS/Linux:**

```bash
# Compile
javac -cp "sqlite-jdbc-3.53.4.0.jar:HikariCP-5.1.0.jar:." *.java

# Run the server
java -cp "sqlite-jdbc-3.53.4.0.jar:HikariCP-5.1.0.jar:." chatServer

# Run a client
java chatClient
```

The server listens on port `5000`. Each client that connects sees the last 50 messages, then joins the live chat. Type `exit` in a client to disconnect.

## Project Status

Currently a terminal-based (TCP socket) chat system with persistence. A browser-based WebSocket front end is planned as a future extension.

## Notes

- `chat.db`, compiled `.class` files, and third-party `.jar` files are intentionally excluded from this repo via `.gitignore` — only source code is tracked.
