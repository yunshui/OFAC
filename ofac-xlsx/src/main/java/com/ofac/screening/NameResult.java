package com.ofac.screening;

import com.ofac.parser.model.OFACQueryResult;

/**
 * Holds the screening result for a single queried name.
 */
public class NameResult {

    private final String queryName;
    private final OFACQueryResult queryResult;
    private final String error;

    public NameResult(String queryName, OFACQueryResult queryResult, String error) {
        this.queryName = queryName;
        this.queryResult = queryResult;
        this.error = error;
    }

    public NameResult(String queryName, OFACQueryResult queryResult) {
        this(queryName, queryResult, null);
    }

    public NameResult(String queryName, String error) {
        this(queryName, null, error);
    }

    public String getQueryName() { return queryName; }
    public OFACQueryResult getQueryResult() { return queryResult; }
    public String getError() { return error; }

    public boolean hasError() { return error != null && !error.isEmpty(); }
    public boolean isHitDetected() {
        return queryResult != null && queryResult.isHitDetected();
    }
}
