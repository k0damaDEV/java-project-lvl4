package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
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
        ctx.attribute("url", url);
        ctx.render("urls/show.html");
    };

    public static Handler showUrls = ctx -> {
        List<Url> urlList = new QUrl()
                .setMaxRows(10)
                .findList();
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
}
