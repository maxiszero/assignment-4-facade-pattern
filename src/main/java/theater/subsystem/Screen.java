package theater.subsystem;

/** Motorized screen model; projection requires the lowered position. */
public final class Screen {
    private boolean lowered;

    public void lower() {
        lowered = true;
    }

    public void raise() {
        lowered = false;
    }

    public boolean isLowered() {
        return lowered;
    }
}
