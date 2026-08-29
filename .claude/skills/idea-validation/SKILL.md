---
name: idea-validation
description: >
  Uses IntelliJ IDEA MCP build, diagnostics, lint, and run-configuration tools
  to generate deterministic validation evidence.
---

# IDEA Validation

## Workflow

1. Identify applicable run configurations.
2. Build the project.
3. Collect file problems.
4. Run lint/inspection where configured.
5. Run approved tests.
6. Report actual evidence.

## Output

Return PASS or FAIL with:

- tools used;
- exit status;
- diagnostics;
- failed checks.

Do not fix problems unless invoked by an implementation workflow.