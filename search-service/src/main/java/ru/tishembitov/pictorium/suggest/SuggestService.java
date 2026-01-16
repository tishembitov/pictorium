package ru.tishembitov.pictorium.suggest;

public interface SuggestService {

    SuggestResponse suggest(String query, int limit);
}