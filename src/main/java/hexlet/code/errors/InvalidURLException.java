package hexlet.code.errors;

public class InvalidURLException extends Exception {
    public InvalidURLException() {
        super("Некорректный URL");
    }
}
