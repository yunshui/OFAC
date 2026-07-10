package com.ofac.screening;

import java.time.LocalDate;

/**
 * CLI entry point for the OFAC screening tool.
 *
 * Usage:
 *   java -jar ofac-xlsx-1.0.0.jar \
 *        -i input.xlsx -o output.xlsx -u http://folcbla-asia.icbc:3012/ \
 *        [--user cbla] [--pass Oper1234] [--unit PEP00110] [-n 10]
 */
public class Main {

    private static final LocalDate EXPIRY_DATE = LocalDate.of(2026, 10, 1);

    public static void main(String[] args) throws Exception {
        if (!LocalDate.now().isBefore(EXPIRY_DATE)) {
            System.err.println("Error: 超过使用期限，请联系提供方。");
            System.exit(1);
            return;
        }

        String inputFile = null;
        String outputFile = null;
        String apiUrl = "http://folcbla-asia.icbc:3012/";
        String user = "cbla";
        String pass = "Oper1234";
        String unit = "PEP00110";
        Integer maxRows = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-i":
                case "--input":
                    inputFile = args[++i];
                    break;
                case "-o":
                case "--output":
                    outputFile = args[++i];
                    break;
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
                case "-n":
                case "--num":
                    maxRows = Integer.parseInt(args[++i]);
                    break;
                case "-h":
                case "--help":
                    printHelp();
                    return;
                default:
                    System.err.println("Unknown option: " + args[i]);
                    printHelp();
                    return;
            }
        }

        if (inputFile == null || outputFile == null) {
            System.err.println("Error: -i and -o are required");
            printHelp();
            return;
        }

        ApiConfig config = new ApiConfig(apiUrl, user, pass, unit);
        BlacklistApiClient client = new BlacklistApiClient(config);
        ScreeningEngine engine = new ScreeningEngine(client);

        engine.run(inputFile, outputFile, maxRows);
    }

    private static void printHelp() {
        System.out.println("OFAC Blacklist Screening Tool");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -jar ofac-xlsx-1.0.0.jar [options]");
        System.out.println();
        System.out.println("Required options:");
        System.out.println("  -i, --input PATH     Input Excel file (.xlsx)");
        System.out.println("  -o, --output PATH    Output Excel file (.xlsx)");
        System.out.println();
        System.out.println("Optional options:");
        System.out.println("  -u, --url URL        OFAC blacklist API URL (default: http://folcbla-asia.icbc:3012/)");
        System.out.println("  --user NAME          API user (default: cbla)");
        System.out.println("  --pass PASSWORD      API password (default: Oper1234)");
        System.out.println("  --unit CODE          API unit code (default: PEP00110)");
        System.out.println("  -n, --num N          Process only first N rows");
        System.out.println("  -h, --help           Show this help");
    }
}
