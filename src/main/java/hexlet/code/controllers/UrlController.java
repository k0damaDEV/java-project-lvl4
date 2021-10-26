package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import hexlet.code.domain.query.QUrlCheck;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UrlController {
    public static Handler showUrl = ctx -> {
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        if (url == null) {
            throw new NotFoundResponse();
        }

        List<UrlCheck> urlCheckList = new QUrlCheck()
                .url.id.equalTo(id)
                .setMaxRows(15)
                .orderBy().id.desc()
                        .findList();

        ctx.attribute("urlCheckList", urlCheckList);
        ctx.attribute("url", url);
        ctx.render("urls/show.html");
    };

    public static Handler showUrls = ctx -> {
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
        int urlsPerPage = 10;
        int offset = (page - 1) * urlsPerPage;

        List<Url> urlList = new QUrl()
                .setFirstRow(offset)
                .setMaxRows(urlsPerPage)
                .orderBy()
                .id.asc()
                .findList();

        ctx.attribute("page", page);
        ctx.attribute("urls", urlList);
        ctx.render("urls/index.html");
    };

    public static Handler newUrl = ctx -> {
        String url = ctx.formParam("url");
        StringBuilder finalUrl = new StringBuilder();
        try {
            URL url1 = new URL(url);
            finalUrl.append(url1.getProtocol())
                            .append("://")
                            .append(url1.getHost());
            if (url1.getPort() != -1) {
                finalUrl.append(":")
                        .append(url1.getPort());
            }
        } catch (MalformedURLException e) {
            ctx.status(422);
            ctx.sessionAttribute("flash-type", "danger");
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.render("index.html");
            return;
        }

        Url dublicateUrl = new QUrl()
                .name.equalTo(finalUrl.toString())
                .findOne();

        if (!Objects.equals(dublicateUrl, null)) {
            ctx.status(422);
            ctx.sessionAttribute("flash-type", "danger");
            ctx.sessionAttribute("flash", "Страница уже существует");
            ctx.render("index.html");
            return;
        }
        Url urlToAdd = new Url(finalUrl.toString());
        urlToAdd.save();
        ctx.status(200);
        ctx.sessionAttribute("flash-type", "success");
        ctx.sessionAttribute("flash", "Страница успешно добавлена");
        ctx.redirect("urls");
    };

    public static Handler checkStart = ctx -> {
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        List<UrlCheck> urlCheckList = new QUrlCheck()
                .url.id.equalTo(id)
                .findList();

        Map<String, String> analyzedUrl = urlAnalyzer(url.getName());

        if (analyzedUrl == null) {
            ctx.attribute("urlCheckList", urlCheckList);
            ctx.attribute("url", url);
            ctx.sessionAttribute("flash-type", "danger");
            ctx.sessionAttribute("flash", "Страница не существует");
            ctx.render("urls/show.html");
            return;
        }

        UrlCheck urlCheck = new UrlCheck();
        urlCheck.setStatusCode(Integer.parseInt(analyzedUrl.get("statusCode")));
        urlCheck.save();

        new QUrlCheck()
                .id.equalTo(urlCheck.getId())
                .asUpdate()
                .set("url_id", id)
                .update();

        ctx.sessionAttribute("flash-type", "success");
        ctx.sessionAttribute("flash", "Страница успешно проверена");
        ctx.redirect("/urls/" + id);
    };

    public static Map<String, String> urlAnalyzer(String url) {
        Map<String, String> result = new HashMap<>();
        System.out.println(url);
        try {
            HttpResponse<String> response = Unirest
                    .get(url)
                    .asString();
            result.put("statusCode", String.valueOf(response.getStatus()));
        } catch (UnirestException e) {
            return null;
        }
        return result;
    }
}
