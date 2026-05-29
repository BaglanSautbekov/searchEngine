package com.baglan.searchEngine.crawler;

import com.baglan.searchEngine.crawler.FetchedPage;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
public class HtmlFetchClient {
    private static final String USER_AGENT = "SimpleJavaSearchBot/1.0";
    private static final int TIMEOUT_MS = 5000;

    public FetchedPage fetch(String url) throws Exception {
        Connection.Response response = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .execute();

        Document document = response.parse();

        return new FetchedPage(
                response.url().toString(),
                response.statusCode(),
                response.contentType(),
                document
        );
    }
}