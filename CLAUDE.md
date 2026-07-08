# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

OFAC blacklist screening tool. Reads names from an Excel file, queries an internal OFAC blacklist API, uses an LLM (Qwen3.5) to parse HTML results into structured fields, classifies matches (Exact/Partial/Not Match), and outputs a formatted Excel report.

## Commands

```bash
# Run screening with required input/output file paths
python request_script.py -i test.xlsx -o output_blacklist_result.xlsx

# Process only first N rows
python request_script.py -i test.xlsx -o output_blacklist_result.xlsx -n 10

# Install dependencies
pip install requests pandas openpyxl beautifulsoup4
```

## Code Architecture

The project is a single script (`request_script.py`) with this pipeline:

1. **process_excel()** — Main orchestrator. Reads an Excel file, iterates rows, calls the screening pipeline per name, and writes results.
2. **query_blacklist_api(name)** — POSTs a name to an internal OFAC blacklist API (`http://folcbla-asia.icbc:3012/`), returns raw HTML response.
3. **extract_fields_with_llm(html)** — Cleans HTML via `clean_html_with_bs4()`, then sends it to a local LLM (Qwen3.5 at `http://123.192.49.9:8086/`) with a prompt to extract structured hit data as JSON. Returns a list of hit dicts.
4. **clean_html_with_bs4(html)** — Strips script/style tags, Word/FrontPage namespace attributes (`mso-`, `v:`, `o:`, `w:`, `u*:`), and empty tags.
5. **classify_match(query_name, hit_names_list)** — Tokenizes names into word sets. Returns `"Exact Match"` (all query words present in a hit name), `"Partial Match"` (some overlap), or `"Not Match"`.
6. **normalize_name(name)** — Lowercases, strips punctuation, splits into word set for matching.

## Configuration (hardcoded at top of script)

| Variable | Purpose |
|---|---|
| `QUERY_URL` | OFAC API endpoint |
| `QUERY_HEADERS` | HTTP headers for API call |
| `QUERY_CREDENTIALS` | Auth (user, pass, UNIT) |
| `LLM_URL` / `LLM_MODEL` | LLM API for HTML parsing |
| `INPUT_FILE` / `OUTPUT_FILE` | Default file paths |
| `QUERY_NAME_COLUMN` | Excel column to read names from |
| `NEW_COLUMNS` | Output columns appended to results |

## Output Format

The output Excel contains all original columns plus: `If Hitted`, `Hit#`, `Exact Match`, `Partial Match`, `Not Match`, and detail fields (`ID`, `ORIGIN`, `DESIGNATION`, etc.). Multi-value fields are newline-separated within cells with `wrap_text` alignment.

## Key Constraints

- LLM prompt must return pure JSON (no markdown fences). The script strips ` ```json ` and ` ``` ` markers as a safety measure.
- The script assumes a local network environment (internal ICBC URLs, local LLM).
- A 0.5s delay is applied between queries to avoid rate limiting.
- Match classification is via word-set inclusion, not fuzzy/string-similarity.
- The LLM response timeout is set to 600s (10 minutes) — LLM calls can be slow.
