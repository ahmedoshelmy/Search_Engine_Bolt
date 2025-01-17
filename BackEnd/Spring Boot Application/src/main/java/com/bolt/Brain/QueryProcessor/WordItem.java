package com.bolt.Brain.QueryProcessor;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class WordItem extends BooleanItem {

    public WordItem(ProcessQueryUnit processQueryUnit, String content) {
        super(processQueryUnit, content);
    }
    @Override
    public void executeOne(List<BooleanItem> items, int index) throws IOException {
        List<String> w = processQueryUnit.process(content);
        results = QueryProcessor.getWordResult(w.get(0));
    }
}
