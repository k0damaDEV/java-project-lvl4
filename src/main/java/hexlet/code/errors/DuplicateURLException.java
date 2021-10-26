package hexlet.code.errors;

public class DuplicateURLException extends Exception {
    public DuplicateURLException() {
        super("Страница уже существует");
    }
}
