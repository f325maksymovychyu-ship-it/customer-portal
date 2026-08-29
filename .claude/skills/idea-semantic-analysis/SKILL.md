---
name: idea-semantic-analysis
description: >
  Uses IntelliJ IDEA MCP semantic tools to inspect project modules, symbols,
  dependencies, call paths, and repository structure. Use for impact analysis
  and architecture exploration before relying on text search.
---

# IDEA Semantic Analysis

## Preferred Tools

- project modules;
- project dependencies;
- search symbol;
- symbol information;
- call analysis;
- directory tree.

## Workflow

1. Identify the relevant module.
2. Search symbols semantically.
3. Inspect symbol metadata.
4. Analyze callers and callees.
5. Produce an evidence-based summary.
6. Use regex/text search only as a fallback.

## Constraints

- Read-only.
- Do not refactor.
- Distinguish semantic findings from text-search findings.