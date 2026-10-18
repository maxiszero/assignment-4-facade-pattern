package theater.subsystem;

import java.util.Objects;

/** Projects an active player onto a lowered screen; independent of any facade. */
public class Projector {
    private DvdPlayer source;
    private Screen screen;

    public void start(DvdPlayer source, Screen screen) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(screen, "screen");
        if (!source.isPlaying()) {
            throw new IllegalStateException("Video source must be playing");
        }
        if (!screen.isLowered()) {
            throw new IllegalStateException("Lower the screen before starting projection");
        }
        this.source = source;
        this.screen = screen;
    }

    public void stop() {
        source = null;
        screen = null;
    }

    public boolean isOn() {
        return source != null;
    }

    public boolean isProjecting() {
        return isOn() && source.isPlaying() && screen.isLowered();
    }
}
