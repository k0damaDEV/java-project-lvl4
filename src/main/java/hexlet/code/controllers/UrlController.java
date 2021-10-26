package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import hexlet.code.domain.query.QUrlCheck;
import hexlet.code.services.UrlServices;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;
import java.util.List;
import java.util.Map;

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

        List<Url> urlList = UrlServices.getUrlList(page);

        ctx.attribute("page", page);
        ctx.attribute("urls", urlList);
        ctx.render("urls/index.html");
    };

    public static Handler newUrl = ctx -> {
        String url = ctx.formParam("url");
        String finalUrl;

        try {
            finalUrl = UrlServices.formParamUrlBuilder(url);
            UrlServices.createUrl(finalUrl);
        } catch (Exception e) {
            ctx.status(422);
            ctx.sessionAttribute("flash-type", "danger");
            ctx.sessionAttribute("flash", e.getMessage());
            ctx.render("index.html");
            return;
        }

        ctx.sessionAttribute("flash-type", "success");
        ctx.sessionAttribute("flash", "Страница успешно добавлена");
        ctx.redirect("urls");
    };

    public static Handler checkStart = ctx -> {
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);
        Url url = UrlServices.getUrlById(id);
        Map<String, String> analyzedUrl;

        try {
            analyzedUrl = UrlServices.urlAnalyzer(url.getName());
        } catch (Exception e) {
            ctx.sessionAttribute("flash-type", "danger");
            ctx.sessionAttribute("flash", e.getMessage());
            ctx.redirect("/urls/" + id);
            return;
        }

        UrlCheck urlCheck = new UrlCheck(
                url,
                Integer.parseInt(analyzedUrl.get("statusCode")),
                analyzedUrl.get("title"),
                analyzedUrl.get("h1"),
                analyzedUrl.get("description")
        );

        urlCheck.save();

        url.setLastStatusCode(Integer.parseInt(analyzedUrl.get("statusCode")));
        url.setLastCheckDate(urlCheck.getCreatedAt());
        url.save();

        ctx.sessionAttribute("flash-type", "success");
        ctx.sessionAttribute("flash", "Страница успешно проверена");
        ctx.redirect("/urls/" + id);
    };
}
