package com.example;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Скачивает ленту по HTTP и разбирает её парсером.
 */
public class HttpReader implements NewsReader {

    /**
     * HTTP-клиент для скачивания лент.
     */
    private final HttpClient client = HttpClient.newHttpClient();

    /**
     * Парсер скачанных лент.
     */
    private final FeedParser feedParser = new FeedParser();

    @Override
    public List<FeedItem> read(String url) throws IOException {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() != 200) {
                    throw new IOException("Ответ сервера: " + response.statusCode());
                }
                return feedParser.parse(body);
            }
        } catch (IllegalArgumentException e) {
            throw new IOException("Не удалось прочитать ленту", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Чтение прервано", e);
        }
    }
}
