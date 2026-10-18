package theater;

import theater.subsystem.Amplifier;
import theater.subsystem.DvdPlayer;
import theater.subsystem.Lights;
import theater.subsystem.Projector;
import theater.subsystem.Screen;

/** Tests use real state and throw AssertionError without requiring -ea. */
public final class TheaterTest {
    private static int passed;

    private TheaterTest() { }

    public static void main(String[] args) {
        run("lights validate brightness without changing the previous setting", () -> {
            Lights lights = new Lights();
            lights.setBrightness(65);
            for (int value : new int[] {-1, 101}) {
                expect(IllegalArgumentException.class, () -> lights.setBrightness(value));
                equal(65, lights.getBrightness());
            }
            lights.setBrightness(0);
            equal(0, lights.getBrightness());
            lights.setBrightness(100);
            equal(100, lights.getBrightness());
        });
        run("player requires a valid movie before playback", () -> {
            DvdPlayer player = new DvdPlayer();
            expect(IllegalStateException.class, player::play);
            for (String title : new String[] {null, "", "  "}) {
                expect(IllegalArgumentException.class, () -> player.loadMovie(title));
            }
            equal(false, player.isPlaying());
            equal("", player.getMovie());
        });
        run("player normalizes titles and prevents replacing a playing movie", () -> {
            DvdPlayer player = new DvdPlayer();
            player.loadMovie("  Interstellar  ");
            player.play();
            equal("Interstellar", player.getMovie());
            expect(IllegalStateException.class, () -> player.loadMovie("Dune"));
            expect(IllegalStateException.class, player::eject);
            equal("Interstellar", player.getMovie());
            player.stop();
            player.eject();
            equal("", player.getMovie());
        });
        run("projector requires both playback and a lowered screen", () -> {
            DvdPlayer player = new DvdPlayer();
            Screen screen = new Screen();
            Projector projector = new Projector();
            expect(IllegalStateException.class, () -> projector.start(player, screen));
            player.loadMovie("Dune");
            player.play();
            expect(IllegalStateException.class, () -> projector.start(player, screen));
            screen.lower();
            projector.start(player, screen);
            equal(true, projector.isProjecting());
            player.stop();
            equal(false, projector.isProjecting());
            projector.stop();
            equal(false, projector.isOn());
        });
        run("amplifier validates volume and uses an active player", () -> {
            DvdPlayer player = new DvdPlayer();
            Amplifier amplifier = new Amplifier();
            expect(IllegalStateException.class, () -> amplifier.start(player, 30));
            player.loadMovie("Dune");
            player.play();
            for (int volume : new int[] {0, -1, 101}) {
                expect(IllegalArgumentException.class, () -> amplifier.start(player, volume));
                equal(false, amplifier.isOn());
            }
            amplifier.start(player, 30);
            equal(true, amplifier.isPlayingAudio());
            equal(30, amplifier.getVolume());
            player.stop();
            equal(false, amplifier.isPlayingAudio());
            amplifier.stop();
            equal(0, amplifier.getVolume());
        });
        run("advanced clients can operate all five subsystems directly", () -> {
            Lights lights = new Lights();
            Screen screen = new Screen();
            DvdPlayer player = new DvdPlayer();
            Amplifier amplifier = new Amplifier();
            Projector projector = new Projector();
            lights.setBrightness(10);
            screen.lower();
            player.loadMovie("Arrival");
            player.play();
            amplifier.start(player, 25);
            projector.start(player, screen);
            equal(true, projector.isProjecting());
            equal(true, amplifier.isPlayingAudio());
            projector.stop();
            amplifier.stop();
            player.stop();
            player.eject();
            screen.raise();
            lights.setBrightness(100);
            equal(false, screen.isLowered());
            equal(100, lights.getBrightness());
        });
        run("one facade call starts all five subsystems", () -> {
            Room room = new Room(new Projector());
            equal("Ready", room.facade.status());
            room.facade.watchMovie("  Interstellar  ");
            equal("Watching: Interstellar", room.facade.status());
            equal(true, room.player.isPlaying());
            equal(true, room.projector.isProjecting());
            equal(true, room.amplifier.isPlayingAudio());
            equal(30, room.amplifier.getVolume());
            equal(20, room.lights.getBrightness());
            equal(true, room.screen.isLowered());
        });
        run("endMovie stops devices and restores the original room brightness", () -> {
            Room room = new Room(new Projector());
            room.lights.setBrightness(65);
            room.facade.watchMovie("Dune");
            room.facade.endMovie();
            room.assertStopped(65);
            equal("Ready", room.facade.status());
        });
        run("ending an idle theater does not alter the room", () -> {
            Room room = new Room(new Projector());
            room.lights.setBrightness(45);
            room.facade.endMovie();
            room.facade.endMovie();
            room.assertStopped(45);
        });
        run("invalid titles cause no subsystem changes", () -> {
            Room room = new Room(new Projector());
            room.lights.setBrightness(75);
            for (String title : new String[] {null, "", " \t\n "}) {
                expect(IllegalArgumentException.class, () -> room.facade.watchMovie(title));
                room.assertStopped(75);
                equal("Ready", room.facade.status());
            }
        });
        run("a second start cannot replace an active movie", () -> {
            Room room = new Room(new Projector());
            room.facade.watchMovie("Arrival");
            expect(IllegalStateException.class, () -> room.facade.watchMovie("Dune"));
            equal("Arrival", room.player.getMovie());
            equal(true, room.projector.isProjecting());
            equal("Watching: Arrival", room.facade.status());
        });
        run("multiple complete sessions reuse the facade safely", () -> {
            Room room = new Room(new Projector());
            for (String title : new String[] {"Arrival", "Dune", "Interstellar"}) {
                room.facade.watchMovie(title);
                equal("Watching: " + title, room.facade.status());
                room.facade.endMovie();
                room.assertStopped(100);
            }
        });
        run("a startup failure rolls back devices and preserves the cause", () -> {
            RuntimeException failure = new IllegalStateException("Projector lamp failure");
            Projector faulty = new Projector() {
                @Override
                public void start(DvdPlayer player, Screen screen) {
                    super.start(player, screen);
                    throw failure;
                }
            };
            Room room = new Room(faulty);
            room.lights.setBrightness(55);
            TheaterOperationException error = expect(TheaterOperationException.class,
                    () -> room.facade.watchMovie("Dune"));
            equal(failure, error.getCause());
            room.assertStopped(55);
            equal("Ready", room.facade.status());
        });
        run("cleanup attempts every step and supports a later retry", () -> {
            Projector faulty = new Projector() {
                private boolean failNextStop = true;

                @Override
                public void stop() {
                    if (failNextStop) {
                        failNextStop = false;
                        throw new IllegalStateException("Projector did not stop");
                    }
                    super.stop();
                }
            };
            Room room = new Room(faulty);
            room.lights.setBrightness(70);
            room.facade.watchMovie("Dune");
            expect(TheaterOperationException.class, room.facade::endMovie);
            equal("Cleanup required", room.facade.status());
            equal(false, room.player.isPlaying());
            equal(false, room.amplifier.isOn());
            equal(false, room.screen.isLowered());
            equal(70, room.lights.getBrightness());
            expect(IllegalStateException.class, () -> room.facade.watchMovie("Arrival"));
            room.facade.endMovie();
            room.assertStopped(70);
            equal("Ready", room.facade.status());
        });
        run("startup failure retains cleanup failures as suppressed information", () -> {
            Projector faulty = new Projector() {
                @Override
                public void start(DvdPlayer player, Screen screen) {
                    throw new IllegalStateException("Startup failed");
                }

                @Override
                public void stop() {
                    throw new IllegalStateException("Cleanup failed");
                }
            };
            Room room = new Room(faulty);
            TheaterOperationException error = expect(TheaterOperationException.class,
                    () -> room.facade.watchMovie("Dune"));
            equal(1, error.getCause().getSuppressed().length);
            equal("Cleanup required", room.facade.status());
            equal(false, room.player.isPlaying());
            equal(100, room.lights.getBrightness());
        });
        run("subsystem collaborators reject null inputs before changing state", () -> {
            Projector projector = new Projector();
            Amplifier amplifier = new Amplifier();
            expect(NullPointerException.class, () -> projector.start(null, new Screen()));
            expect(NullPointerException.class, () -> projector.start(new DvdPlayer(), null));
            expect(NullPointerException.class, () -> amplifier.start(null, 30));
            equal(false, projector.isOn());
            equal(false, amplifier.isOn());
        });
        System.out.println("PASS: " + passed + " tests");
    }

    private static void run(String name, Runnable test) {
        test.run();
        passed++;
        System.out.println("PASS " + name);
    }

    private static void equal(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }

    private static <T extends RuntimeException> T expect(Class<T> type, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException error) {
            if (type.isInstance(error)) {
                return type.cast(error);
            }
            throw error;
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    private static final class Room {
        private final Lights lights = new Lights();
        private final Screen screen = new Screen();
        private final DvdPlayer player = new DvdPlayer();
        private final Amplifier amplifier = new Amplifier();
        private final Projector projector;
        private final HomeTheaterFacade facade;

        private Room(Projector projector) {
            this.projector = projector;
            facade = new HomeTheaterFacade(lights, screen, player, amplifier, projector);
        }

        private void assertStopped(int brightness) {
            equal(false, player.isPlaying());
            equal("", player.getMovie());
            equal(false, projector.isOn());
            equal(false, amplifier.isOn());
            equal(false, screen.isLowered());
            equal(brightness, lights.getBrightness());
        }
    }
}
