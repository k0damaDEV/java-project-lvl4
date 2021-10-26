package hexlet.code.errors;

public class NonExistentURLException extends Exception {
    public NonExistentURLException() {
        super("Страница не существует");
    }
}
