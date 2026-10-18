package theater;

/** Normal client: all theater operations go exclusively through the facade. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Usage: java -cp build/classes theater.Main [movie-title]");
            System.exit(2);
        }
        HomeTheaterFacade theater = new HomeTheaterFacade();
        String title = args.length == 0 ? "Interstellar" : args[0];
        try {
            System.out.println("Initial status: " + theater.status());
            theater.watchMovie(title);
            System.out.println("After watchMovie: " + theater.status());

            try {
                theater.watchMovie("Arrival");
            } catch (IllegalStateException expected) {
                System.out.println("Repeated start rejected: " + expected.getMessage());
            }

            theater.endMovie();
            System.out.println("After endMovie: " + theater.status());
            theater.endMovie();
            System.out.println("Repeated endMovie is safe: " + theater.status());

            theater.watchMovie("Dune");
            System.out.println("Next session: " + theater.status());
            theater.endMovie();
            System.out.println("Final status: " + theater.status());
        } catch (IllegalArgumentException error) {
            System.err.println("Invalid request: " + error.getMessage());
            System.exit(2);
        } catch (TheaterOperationException error) {
            System.err.println("Theater operation failed: " + error.getMessage());
            System.exit(1);
        }
    }
}
