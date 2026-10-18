package theater;

/** Reports coordination failures while retaining subsystem causes. */
public final class TheaterOperationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public TheaterOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
