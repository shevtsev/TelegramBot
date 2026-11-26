package com.example.feed;

import com.example.storage.Feed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Ищет новости по ключевым словам в выбранных лентах.
 */
public class NewsSearch {

    /**
     * Читает новости из лент.
     */
    private final NewsReader reader;

    /**
     * Создаёт поиск с заданным источником новостей.
     */
    public NewsSearch(NewsReader reader) {
        this.reader = reader;
    }

    /**
     * Ищет хотя бы одно из слов в заголовке или описании без учёта регистра.
     * Слова разделяются пробелами. Пустой запрос возвращает пустой список.
     * Результаты сохраняют порядок переданных лент и записей в них.
     */
    public List<SearchResult> search(List<Feed> feeds, String query) throws IOException {
        if (query.isBlank()) {
            return List.of();
        }

        Pattern keywords = keywordPattern(query);
        List<SearchResult> results = new ArrayList<>();
        for (Feed feed : feeds) {
            for (FeedItem item : reader.read(feed.url())) {
                if (keywords.matcher(item.getTitle()).find()
                        || keywords.matcher(item.getDescription()).find()) {
                    results.add(new SearchResult(feed, item));
                }
            }
        }
        return results;
    }

    /**
     * Собирает шаблон для поиска целых слов с учётом букв разных алфавитов.
     */
    private Pattern keywordPattern(String query) {
        List<String> words = new ArrayList<>();
        for (String word : query.strip().split("\\p{javaWhitespace}+")) {
            words.add(Pattern.quote(word));
        }
        String wordCharacter = "[\\p{L}\\p{M}\\p{N}_]";
        String expression = "(?<!" + wordCharacter + ")(?:" + String.join("|", words)
                + ")(?!" + wordCharacter + ")";
        return Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }
}
