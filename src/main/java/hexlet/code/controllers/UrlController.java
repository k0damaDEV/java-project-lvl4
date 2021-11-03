package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import hexlet.code.domain.query.QUrlCheck;
import io.ebean.DuplicateKeyException;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;
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

public class UrlController {
    public static Handler showUrl = ctx -> {
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .urlChecks.fetch()
                .orderBy()
                .urlChecks.createdAt.desc()
                .findOne();

        if (url == null) {
            throw new NotFoundResponse();
        }

        ctx.attribute("url", url);
        ctx.render("urls/show.html");
    };

    public static Handler showUrls = ctx -> {
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
        final int maxRows = 10;
        final int offset = (page - 1) * maxRows;

        List<Url> urlList = new QUrl()
                .setFirstRow(offset)
                .setMaxRows(maxRows)
                .orderBy()
                    .id.asc()
                .findList();

        Map<Integer, UrlCheck> checks = new QUrlCheck()
                .url.id.asMapKey()
                .orderBy()
                .createdAt.desc()
                .findMap();

        ctx.attribute("page", page);
        ctx.attribute("checks", checks);
        ctx.attribute("urls", urlList);
        ctx.render("urls/index.html");
    };

    public static Handler newUrl = ctx -> {
        String url = ctx.formParam("url");
        Url urlToSave;

        try {
            URL newUrl = new URL(url);
            String finalUrl = newUrl.getPort() == -1
                    ? String.format("%s://%s", newUrl.getProtocol(), newUrl.getHost())
                    : String.format("%s://%s:%d", newUrl.getProtocol(), newUrl.getHost(), newUrl.getPort());
            urlToSave = new Url(finalUrl);
            urlToSave.save();
        } catch (MalformedURLException | DuplicateKeyException e) {
            String msg = e instanceof DuplicateKeyException ? "Страница уже существует" : "Некорректный URL";
            ctx.status(422);
            ctx.sessionAttribute("flash-type", "danger");
            ctx.sessionAttribute("flash", msg);
            ctx.render("index.html");
            return;
        }

        ctx.sessionAttribute("flash-type", "success");
        ctx.sessionAttribute("flash", "Страница успешно добавлена");
        ctx.redirect("/urls");
    };

    public static Handler runCheck = ctx -> {
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        try {
            HttpResponse<String> response = Unirest
                    .get(url.getName())
                    .asString();

            String body = response.getBody();
            Document doc = Jsoup.parse(body);
            Element h1Element = doc.selectFirst("h1");
            Element descriptionElement = doc.selectFirst("meta[name=description]");

            UrlCheck urlCheck = new UrlCheck();
            urlCheck.setUrl(url);
            urlCheck.setStatusCode(response.getStatus());
            urlCheck.setTitle(doc.title());
            urlCheck.setH1(h1Element == null ? "" : h1Element.text());
            urlCheck.setDescription(descriptionElement == null ? "" : descriptionElement.attr("content"));
            urlCheck.save();
        } catch (UnirestException e) {
            ctx.sessionAttribute("flash-type", "danger");
            ctx.sessionAttribute("flash", "Страница не существует");
            ctx.redirect("/urls/" + id);
            return;
        }

        ctx.sessionAttribute("flash-type", "success");
        ctx.sessionAttribute("flash", "Страница успешно проверена");
        ctx.redirect("/urls/" + id);
    };
}
