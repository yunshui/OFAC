package com.ofac.screening;

import com.ofac.parser.OFACHtmlParser;
import com.ofac.parser.model.OFACQueryResult;

import java.util.List;

/**
 * Orchestrator for the OFAC screening pipeline:
 * 1. Read names from Excel
 * 2. Query blacklist API for each name
 * 3. Parse HTML results using OFACHtmlParser
 * 4. Collect results
 * 5. Write output Excel
 */
public class ScreeningEngine {

    private final BlacklistApiClient apiClient;
    private final ScreeningResultWriter resultWriter;

    public ScreeningEngine(BlacklistApiClient apiClient) {
        this.apiClient = apiClient;
        this.resultWriter = new ScreeningResultWriter();
    }

    /**
     * Run the full screening pipeline.
     *
     * @param inputFile  input Excel file path
     * @param outputFile output Excel file path
     * @param maxRows    max rows to process (null or <= 0 = all)
     */
    public void run(String inputFile, String outputFile, Integer maxRows)
            throws Exception {

        System.out.println("Reading names from: " + inputFile);
        List<String> names = ExcelNameReader.readNames(inputFile);

        if (maxRows != null && maxRows > 0 && maxRows < names.size()) {
            names = names.subList(0, maxRows);
            System.out.println("Limited to " + maxRows + " names");
        }

        System.out.println("Total names to screen: " + names.size());
        ScreeningResults results = new ScreeningResults();

        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            System.out.println("Processing [" + (i + 1) + "/" + names.size() + "]: " + name);

            // 1. Query API
            String html = apiClient.query(name);
            if (html == null) {
                results.add(new NameResult(name, "API query failed"));
                continue;
            }

            // 2. Parse HTML
            try {
                OFACQueryResult queryResult = OFACHtmlParser.parse(html);
                results.add(new NameResult(name, queryResult));
            } catch (Exception e) {
                System.err.println("  [Parse Error] " + e.getMessage());
                results.add(new NameResult(name, "Parse error: " + e.getMessage()));
            }

            // 3. Rate limiting delay
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("Writing results to: " + outputFile);
        resultWriter.write(results, outputFile);
        System.out.println("Done! " + results.size() + " names processed.");
    }
}
