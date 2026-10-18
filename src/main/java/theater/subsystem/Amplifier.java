package theater.subsystem;

import java.util.Objects;

/** Selects a playing source and applies a validated listening volume. */
public final class Amplifier {
    private DvdPlayer source;
    private int volume;

    public void start(DvdPlayer source, int volume) {
        Objects.requireNonNull(source, "source");
        if (volume < 1 || volume > 100) {
            throw new IllegalArgumentException("Volume must be between 1 and 100");
        }
        if (!source.isPlaying()) {
            throw new IllegalStateException("Audio source must be playing");
        }
        this.source = source;
        this.volume = volume;
    }

    public void stop() {
        source = null;
        volume = 0;
    }

    public boolean isOn() {
        return source != null;
    }

    public boolean isPlayingAudio() {
        return isOn() && source.isPlaying();
    }

    public int getVolume() {
        return volume;
    }
}
