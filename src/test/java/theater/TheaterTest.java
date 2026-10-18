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
}
