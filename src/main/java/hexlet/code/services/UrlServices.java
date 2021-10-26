package hexlet.code.services;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;
import hexlet.code.errors.DuplicateURLException;
import hexlet.code.errors.InvalidURLException;
import hexlet.code.errors.NonExistentURLException;
import io.ebean.DuplicateKeyException;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class UrlServices {
    public static Url getUrlById(final int id) {
        return new QUrl()
                .id.equalTo(id)
                .findOne();
    }

    public static List<Url> getUrlList(int page) {
        final int maxRows = 10;
        final int offset = (page - 1) * maxRows;

        return new QUrl()
                .setFirstRow(offset)
                .setMaxRows(maxRows)
                .orderBy()
                .id.asc()
                .findList();
    }

    public static Map<String, String> urlAnalyzer(String url) throws NonExistentURLException {
        String h1;
        String description;
        String statusCode;
        String title;

        try {
            HttpResponse<String> response = Unirest
                    .get(url)
                    .asString();

            String body = response.getBody();
            Document doc = Jsoup.parse(body);
            Element h1Element = doc.selectFirst("h1");
            Element descriptionElement = doc.selectFirst("meta[name=description]");

            h1 = h1Element == null ? "" : h1Element.text();
            description = descriptionElement == null ? "" : descriptionElement.attr("content");
            statusCode = String.valueOf(response.getStatus());
            title = doc.title();
        } catch (UnirestException e) {
            throw new NonExistentURLException();
        }

        return Map.of(
                "statusCode", statusCode,
                "title", title,
                "h1", h1,
                "description", description
        );
    }

    public static void createUrl(String url) throws DuplicateURLException {
        try {
            Url newUrl = new Url(url);
            newUrl.save();
        } catch (DuplicateKeyException e) {
            throw new DuplicateURLException();
        }
    }

    public static String formParamUrlBuilder(String formUrl) throws InvalidURLException {
        try {
            URL url = new URL(formUrl);
            return url.getPort() == -1
                    ? String.format("%s://%s", url.getProtocol(), url.getHost())
                    : String.format("%s://%s:%d", url.getProtocol(), url.getHost(), url.getPort());
        } catch (MalformedURLException e) {
            throw new InvalidURLException();
        }
    }
}
