package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;
import io.ebean.DB;
import io.ebean.Transaction;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public final class AppTest {
    private static Javalin app;
    private static String baseUrl;
    private static Transaction transaction;
    private static Url url;
    private static MockWebServer mockWebServer;

    @BeforeAll
    public static void beforeAll() throws IOException {
        app = App.getApp();
        app.start();
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        url = new Url("https://yandex.ru");
        url.save();
        mockWebServer = new MockWebServer();
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        mockWebServer.start();
    }

    @AfterAll
    public static void afterAll() throws IOException {
        app.stop();
        mockWebServer.shutdown();
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

        HttpResponse<String> secondResponse = Unirest
                        .get(baseUrl + "/urls/" + url.getId())
                        .asString();
        String singleDisplayBody = secondResponse.getBody();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(url).isNotNull();
        assertThat(body).contains("Страница успешно добавлена");
        assertThat(body).contains("https://testsitelog.com");
        assertThat(singleDisplayBody).contains("https://testsitelog.com");
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

    @Test
    void mockParseTest() {
        String mockUrl = mockWebServer.url("/").toString();
        String editedMockUrl = mockUrl.substring(0, mockUrl.length() - 1);

        HttpResponse postRequest = Unirest
                .post(baseUrl + "/urls")
                .field("url", mockUrl)
                .asEmpty();

        HttpResponse<String> requestCheckTest = Unirest
                .post(baseUrl + "/urls/" + url.getId() + "/checks")
                .asString();

        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();

        Url url = new QUrl()
                .name.equalTo(editedMockUrl)
                .findOne();

        String body = response.getBody();

        assertThat(requestCheckTest.getStatus()).isEqualTo(302); // redirected
        assertThat(body).contains("Страница успешно проверена");
        assertThat(body).contains(editedMockUrl);
        assertThat(url).isNotNull();
    }
}
