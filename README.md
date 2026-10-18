# Assignment 4 - Facade Pattern

**Olzhabekov Ali - SE-2515**

Repository: [maxiszero/assignment-4-facade-pattern](https://github.com/maxiszero/assignment-4-facade-pattern).

Java 17 Home Theater Facade for Software Design Patterns, Astana IT University.
`HomeTheaterFacade` turns the setup and shutdown of five collaborating subsystems
into `watchMovie(title)` and `endMovie()`. `status()` reports the session state.
This is a stateful console model; it does not control physical hardware or play
video files. No third-party libraries are needed.

## Run

Use a full JDK 17 or newer with `java` and `javac` on PATH. Open a terminal in this
project directory.

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1 test
powershell -ExecutionPolicy Bypass -File .\build.ps1 demo
```

Linux, macOS or Git Bash:

```sh
sh build.sh test
sh build.sh demo
```

Both launchers compile with `--release 17 -encoding UTF-8 -Xlint:all -Werror`.
`compile` only compiles. Expected test summary: **PASS: 16 tests**. Assertions are
explicit, so the tests do not require `-ea` or a downloaded test framework.

To choose the first movie after compiling:

```sh
java -cp build/classes theater.Main "The Lord of the Rings"
```

The demo prints `Ready`, `Watching: Interstellar`, a rejected second start, a safe
repeated shutdown, a second session for `Dune`, and the final `Ready` state.
Blank movie titles and excess arguments exit with code 2. Unexpected theater
coordination failures exit with code 1.

## Simplified client

```java
HomeTheaterFacade theater = new HomeTheaterFacade();
theater.watchMovie("Interstellar");
System.out.println(theater.status());
theater.endMovie();
```

`Main` never creates or calls a subsystem. The facade's public constructor and
methods expose no device types. A package-private constructor injects collaborators
for tests without expanding the normal client API.

## Collaborating subsystems

| Class | Responsibility and interaction |
| --- | --- |
| `Lights` | Validates brightness from 0 to 100; the previous level is restored after a session. |
| `Screen` | Maintains its raised/lowered position; projection requires a lowered screen. |
| `DvdPlayer` | Validates and loads a title; requires a loaded movie before playback and a stopped player before ejecting. |
| `Amplifier` | Requires an actively playing `DvdPlayer`; validates volume and models audio availability. |
| `Projector` | Requires a playing `DvdPlayer` and a lowered `Screen`; models projection availability. |

All subsystem classes are public and unaware of the facade. Advanced clients can
instantiate them directly. The tests include a full direct-use scenario proving
that the facade is optional rather than an access restriction.

## Coordination and failure handling

Starting a movie validates its title and checks the session state. The facade then
saves the room brightness, loads the movie, dims lights, lowers the screen, starts
playback, connects audio at volume 30, and starts projection. Movie brightness is 20.

Shutdown stops projection, audio and playback, ejects the movie, raises the screen,
and restores the saved brightness. Ending an idle session is a no-op. Starting a
second movie during a session is rejected.

Startup failures trigger the same cleanup routine. Cleanup attempts every step
even if one device fails. The original cause and additional cleanup failures are
retained in exceptions. An incomplete cleanup leaves `Cleanup required` status;
new sessions are blocked until a later successful `endMovie()`.

These are compensating operations in an in-memory model, not an atomic hardware
transaction. One facade instance represents one room and is intended for
single-threaded use. The facade owns a standard movie setup; advanced device
settings remain available through direct subsystem use.

## Project structure

```text
src/main/java/theater/            Facade, client and operation exception
src/main/java/theater/subsystem/  Five independent subsystem classes
src/test/java/theater/            TheaterTest with 16 behavioral checks
docs/                            PDF report, UML source and defense notes
.github/workflows/               Java 17 checks on Windows and Linux
build.ps1, build.sh               Build, test and demo commands
```

For IntelliJ IDEA, open the folder, choose JDK 17+, mark `src/main/java` as Sources
Root and `src/test/java` as Test Sources Root, then run `theater.Main` or
`theater.TheaterTest`. Use the project root as the working directory.

The report is `docs/Assignment_4_Facade_Pattern_Report.pdf`. The diagram is
`docs/facade-uml.svg`, with editable PlantUML source in `docs/facade.puml`.
Russian defense preparation is in `docs/DEFENSE_RU.md`.

## Verification

The tests cover subsystem preconditions, direct subsystem use, complete sessions,
invalid input, repeat calls, original brightness restoration, startup failure,
continued cleanup after a failure, retry, and retained error causes.

Captured results and the Java 17 workflow link are in `docs/verification.txt`.
All 16 tests and the demo passed on Java 17 in both Windows and Linux in the
[verified workflow run](https://github.com/maxiszero/assignment-4-facade-pattern/actions/runs/39141879643).

## References

- Supplied Assignment 4 Facade Pattern Instructions.
- [Facade explanation from the assigned reading](https://refactoring.guru/design-patterns/facade).
