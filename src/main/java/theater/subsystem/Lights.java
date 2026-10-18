package theater.subsystem;

/** Room lighting with a validated brightness level. */
public final class Lights {
    private int brightness = 100;

    public void setBrightness(int brightness) {
        if (brightness < 0 || brightness > 100) {
            throw new IllegalArgumentException("Brightness must be between 0 and 100");
        }
        this.brightness = brightness;
    }

    public int getBrightness() {
        return brightness;
    }
}
