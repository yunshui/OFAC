package com.ofac.screening;

import com.ofac.parser.OFACHtmlParser;
import com.ofac.parser.model.OFACQueryResult;

/**
 * CLI entry point: query a name against the OFAC blacklist API and output JSON.
 *
 * Usage:
 *   java -jar ofac-html-parser-1.0.0.jar <name> [options]
 *   java com.ofac.screening.Main <name> [options]
 *
 * Options:
 *   -u, --url URL        API URL (default: http://folcbla-asia.icbc:3012/)
 *   --user NAME          API user (default: cbla)
 *   --pass PASSWORD      API password (default: Oper1234)
 *   --unit CODE          API unit code (default: PEP00110)
 *   -h, --help           Show this help
 *
 * Examples:
 *   java -jar ofac-html-parser-1.0.0.jar "陈平"
 *   java -jar ofac-html-parser-1.0.0.jar "John Doe" -u http://localhost:8080/
 */
public class Main {

    public static void main(String[] args) throws Exception {
        String name = null;
        String apiUrl = "http://folcbla-asia.icbc:3012/";
        String user = "cbla";
        String pass = "Oper1234";
        String unit = "PEP00110";

        // Parse args: first non-option arg is the name
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-u":
                case "--url":
                    apiUrl = args[++i];
                    break;
                case "--user":
                    user = args[++i];
                    break;
                case "--pass":
                    pass = args[++i];
                    break;
                case "--unit":
                    unit = args[++i];
                    break;
                case "-h":
                case "--help":
                    printHelp();
                    return;
                default:
                    if (args[i].startsWith("-")) {
                        System.err.println("Unknown option: " + args[i]);
                        printHelp();
                        return;
                    }
                    name = args[i];
                    break;
            }
        }

        if (name == null || name.isEmpty()) {
            System.err.println("Error: name is required");
            printHelp();
            return;
        }

        // Query API
        ApiConfig config = new ApiConfig(apiUrl, user, pass, unit);
        BlacklistApiClient client = new BlacklistApiClient(config);

        String html = client.query(name);
        if (html == null) {
            System.err.println("Error: API query failed for name: " + name);
            System.exit(1);
            return;
        }

        // Parse and output JSON
        try {
            OFACQueryResult result = OFACHtmlParser.parse(html);
            System.out.println(OFACHtmlParser.toJson(result));
        } catch (Exception e) {
            System.err.println("Error parsing response: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printHelp() {
        System.out.println("OFAC Blacklist Query Tool");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -jar ofac-html-parser-1.0.0.jar <name> [options]");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  name                 Name to search (required)");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -u, --url URL        API URL (default: http://folcbla-asia.icbc:3012/)");
        System.out.println("  --user NAME          API user (default: cbla)");
        System.out.println("  --pass PASSWORD      API password (default: Oper1234)");
        System.out.println("  --unit CODE          API unit code (default: PEP00110)");
        System.out.println("  -h, --help           Show this help");
        System.out.println();
        System.out.println("Excel screening tool:");
        System.out.println("  java com.ofac.screening.Main1 -i input.xlsx -o output.xlsx");
    }
}
