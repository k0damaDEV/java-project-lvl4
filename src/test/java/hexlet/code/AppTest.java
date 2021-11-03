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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public final class AppTest {
    private static Javalin app;
    private static String baseUrl;
    private static Transaction transaction;
    private static Url url;
    private static MockWebServer mockWebServer;
    private static String mockHtml;

    @BeforeAll
    public static void beforeAll() throws IOException {
        app = App.getApp();
        app.start();
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        url = new Url("https://existensite.com");
        url.save();
        mockHtml = readFileContent("src/test/resources/fixtures/fixture.html");
    }

    @AfterAll
    public static void afterAll() {
        app.stop();
    }

    @BeforeEach
    void beforeEach() {
        mockWebServer = new MockWebServer();
        transaction = DB.beginTransaction();
    }

    @AfterEach
    void afterEach() throws IOException {
        transaction.rollback();
        mockWebServer.shutdown();
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
    void newExistingUrlTest() {
        HttpResponse<String> response = Unirest
                .post(baseUrl + "/urls")
                .field("url", "https://existensite.com")
                .asString();

        String body = response.getBody();

        assertThat(response.getStatus()).isEqualTo(422);
        assertThat(body).contains("Страница уже существует");
    }

    @Test
    void mockParseTest() throws IOException {
        mockWebServer.enqueue(new MockResponse().setBody(mockHtml));
        mockWebServer.start();
        String mockUrl = mockWebServer.url("").toString();
        String editedMockUrl = mockUrl.substring(0, mockUrl.length() - 1);

        HttpResponse postRequest = Unirest
                .post(baseUrl + "/urls")
                .field("url", mockUrl)
                .asEmpty();

        Url url = new QUrl()
                .name.equalTo(editedMockUrl)
                .findOne();

        HttpResponse<String> checksPostRequest = Unirest
                .post(baseUrl + "/urls/" + url.getId() + "/checks")
                .asString();

        HttpResponse<String> getResponse = Unirest
                .get(baseUrl + "/urls/" + url.getId())
                .asString();

        String body = getResponse.getBody();

        assertThat(getResponse.getStatus()).isEqualTo(200);
        assertThat(body).contains("Страница успешно проверена");
        assertThat(body).contains("TestDescription");
        assertThat(body).contains("TestH1");
        assertThat(body).contains("TestTitle");
    }

    private static String readFileContent(String path) throws IOException {
        Path resultPath = Paths.get(path).toAbsolutePath().normalize();
        return Files.readString(resultPath);
    }
}
