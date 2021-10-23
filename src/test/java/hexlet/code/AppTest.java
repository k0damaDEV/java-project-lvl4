package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;
import io.ebean.DB;
import io.ebean.Transaction;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public final class AppTest {
    private static Javalin app;
    private static String baseUrl;
    private static Transaction transaction;
    private static Url url;

    @BeforeAll
    public static void beforeAll() {
        app = App.getApp();
        app.start();
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        url = new Url("https://yandex.ru");
        url.save();
    }

    @AfterAll
    public static void afterAll() {
        app.stop();
    }

    @BeforeEach
    void beforeEach() {
        transaction = DB.beginTransaction();
    }

    @AfterEach
    void afterEach() {
        transaction.rollback();
    }

    @Test
    void indexTest() {
        HttpResponse response = Unirest
                .get(baseUrl)
                .asEmpty();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void urlsTest() {
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();

        String body = response.getBody();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(body).contains("Сайты");
        assertThat(body).contains("Последняя проверка");
    }

    @Test
    void newUrlTest() {
        HttpResponse postRequest = Unirest
                .post(baseUrl + "/urls")
                .field("url", "https://testsitelog.com")
                .asEmpty();

        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();

        String body = response.getBody();

        Url url = new QUrl()
                        .name.equalTo("https://testsitelog.com")
                        .findOne();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(url).isNotNull();
        assertThat(body).contains("Страница успешно добавлена");
        assertThat(body).contains("https://testsitelog.com");
    }

    @Test
    void newIncorrectUrlTest() {
        HttpResponse<String> response = Unirest
                .post(baseUrl + "/urls")
                .field("url", "gitlab.com")
                .asString();

        String body = response.getBody();

        Url url = new QUrl()
                        .name.equalTo("gitlab.com")
                        .findOne();

        assertThat(response.getStatus()).isEqualTo(422);
        assertThat(body).contains("Некорректный URL");
        assertThat(url).isNull();
    }

    @Test
    void newExistingArticleTest() {
        HttpResponse<String> response = Unirest
                .post(baseUrl + "/urls")
                .field("url", "https://yandex.ru")
                .asString();

        String body = response.getBody();

        assertThat(response.getStatus()).isEqualTo(422);
        assertThat(body).contains("Страница уже существует");
    }
}
