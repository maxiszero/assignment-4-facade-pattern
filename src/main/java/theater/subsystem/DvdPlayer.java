package theater.subsystem;

/** Owns movie loading and the playback lifecycle. */
public final class DvdPlayer {
    private String movie = "";
    private boolean playing;

    public static String requireMovieTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Movie title must not be blank");
        }
        return title.strip();
    }

    public void loadMovie(String title) {
        String normalized = requireMovieTitle(title);
        if (playing) {
            throw new IllegalStateException("Stop playback before loading another movie");
        }
        movie = normalized;
    }

    public void play() {
        if (movie.isEmpty()) {
            throw new IllegalStateException("Load a movie before starting playback");
        }
        playing = true;
    }

    public void stop() {
        playing = false;
    }

    public void eject() {
        if (playing) {
            throw new IllegalStateException("Stop playback before ejecting the movie");
        }
        movie = "";
    }

    public boolean isPlaying() {
        return playing;
    }

    public String getMovie() {
        return movie;
    }
}
