---
description: Archive the completed active User Story after required human gates.
argument-hint: ""
---

Invoke the story-orchestrator Skill in archive mode.

Requirements:

- require workflow-state.yaml current_stage == COMPLETED and explicit human
  invocation of this command;
- create the delivery summary at the `delivery_summary` path from
  docs/workflow/artifact-paths.yaml;
- set the Story's catalog state to ARCHIVED in docs/catalog/stories.yaml;
- update docs/knowledge/project-state.md; propose (do not auto-apply)
  business-rules and architecture updates for human approval;
- preserve all historical artifacts in place (do not move or delete them);
- do not merge a Pull Request;
- request human approval before remote GitHub writes;
- clear active-story.yaml and set current_stage ARCHIVED only after the
  delivery summary is written; append a history.jsonl event.
