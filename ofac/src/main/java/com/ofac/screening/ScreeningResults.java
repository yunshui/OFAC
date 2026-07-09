package com.ofac.screening;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects NameResult entries across all screened names.
 */
public class ScreeningResults {

    private final List<NameResult> results = new ArrayList<>();

    public void add(NameResult result) {
        results.add(result);
    }

    public List<NameResult> getAll() {
        return results;
    }

    public int size() {
        return results.size();
    }
}
