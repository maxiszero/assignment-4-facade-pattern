package theater;

import java.util.List;
import java.util.Objects;
import theater.subsystem.Amplifier;
import theater.subsystem.DvdPlayer;
import theater.subsystem.Lights;
import theater.subsystem.Projector;
import theater.subsystem.Screen;

/** Coordinates a movie session while hiding device setup from normal clients. */
public final class HomeTheaterFacade {
    private static final int MOVIE_BRIGHTNESS = 20;
    private static final int MOVIE_VOLUME = 30;

    private enum State { READY, WATCHING, CLEANUP_REQUIRED }

    private final Lights lights;
    private final Screen screen;
    private final DvdPlayer player;
    private final Amplifier amplifier;
    private final Projector projector;
    private State state = State.READY;
    private int previousBrightness;
    private String currentMovie = "";

    public HomeTheaterFacade() {
        this(new Lights(), new Screen(), new DvdPlayer(), new Amplifier(), new Projector());
    }

    // Package-private injection supports tests without exposing devices in the public API.
    HomeTheaterFacade(Lights lights, Screen screen, DvdPlayer player,
                     Amplifier amplifier, Projector projector) {
        this.lights = Objects.requireNonNull(lights, "lights");
        this.screen = Objects.requireNonNull(screen, "screen");
        this.player = Objects.requireNonNull(player, "player");
        this.amplifier = Objects.requireNonNull(amplifier, "amplifier");
        this.projector = Objects.requireNonNull(projector, "projector");
    }

    public void watchMovie(String title) {
        String movie = DvdPlayer.requireMovieTitle(title);
        if (state != State.READY) {
            throw new IllegalStateException("End the current session before starting another movie");
        }
        previousBrightness = lights.getBrightness();
        state = State.CLEANUP_REQUIRED;
        try {
            player.loadMovie(movie);
            lights.setBrightness(MOVIE_BRIGHTNESS);
            screen.lower();
            player.play();
            amplifier.start(player, MOVIE_VOLUME);
            projector.start(player, screen);
            currentMovie = movie;
            state = State.WATCHING;
        } catch (RuntimeException failure) {
            try {
                restoreRoom();
            } catch (RuntimeException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw new TheaterOperationException("Could not start movie: " + movie, failure);
        }
    }

    public void endMovie() {
        if (state == State.READY) {
            return;
        }
        state = State.CLEANUP_REQUIRED;
        restoreRoom();
    }

    public String status() {
        return switch (state) {
            case READY -> "Ready";
            case WATCHING -> "Watching: " + currentMovie;
            case CLEANUP_REQUIRED -> "Cleanup required";
        };
    }

    private void restoreRoom() {
        RuntimeException firstFailure = null;
        List<Runnable> steps = List.of(projector::stop, amplifier::stop,
                player::stop, player::eject, screen::raise,
                () -> lights.setBrightness(previousBrightness));
        for (Runnable step : steps) {
            try {
                step.run();
            } catch (RuntimeException failure) {
                if (firstFailure == null) {
                    firstFailure = failure;
                } else {
                    firstFailure.addSuppressed(failure);
                }
            }
        }
        if (firstFailure != null) {
            throw new TheaterOperationException("Cleanup incomplete; call endMovie again", firstFailure);
        }
        currentMovie = "";
        state = State.READY;
    }
}
