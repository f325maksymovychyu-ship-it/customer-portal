# Archive Flow

Consolidate knowledge from a finished delivery and clean up workflow state.
Explicit, infrequent, human-invoked only. Never inferred.

## Preconditions

Archive only when ALL hold:

- exactly one active Story;
- `workflow-state.yaml.current_stage == COMPLETED` (a human recorded PR merge /
  delivery completion via `/so:approve` at the `COMPLETED` gate);
- `reconciliation` and `pr_summary` artifacts exist, current, `APPROVED`;
- no Critical/Major findings remain open;
- no blocking Open Decisions remain;
- artifact inventory is complete (every mandatory `stage-map.yaml` output
  produced and current);
- no unclassified Story files remain in the working tree.

Do not archive merely because an implementation exists.

## Steps

1. Confirm `current_stage == COMPLETED` and explicit human invocation.
2. Build the artifact inventory for the Story from `artifact-paths.yaml`
   (path, type, version, status).
3. Create the **delivery summary** at the `delivery_summary` registry path
   (`docs/evidence/{story_id}-delivery-summary.md`), owned by this Skill:
   Story id; final catalog state; source Issue + PR reference (or null);
   final branch; activation / completion / archive timestamps (runtime);
   full artifact inventory; final Acceptance Criteria result; final
   verification / security / reconciliation verdicts; known limitations;
   deferred work; history reference. No secrets, no full sensitive logs.
4. Set the Story's catalog `state` to `ARCHIVED` in `docs/catalog/stories.yaml`
   (atomically). Set each Story artifact's front-matter `status` to `ARCHIVED`
   where appropriate. **Do not move any file.** Paths and `supersedes` links
   are preserved.
5. Update `docs/knowledge/project-state.md` (registry key `project_state`) with
   the capabilities this Story delivered.
6. **Propose** (do not apply) updates to `docs/product/business-rules.md` and
   the `docs/architecture/*` documents implied by the delivery. Present them as
   a diff / bullet list for human review.
7. Apply the knowledge-doc updates from step 6 **only after explicit human
   approval** in this session. If not approved, leave them as proposals in the
   delivery summary.
8. GitHub sync (if a source is configured and permitted): show the proposed
   Issue state change; request explicit approval; perform only the approved
   read/label/close operation; record the result. Never merge a Pull Request.
9. Clear active state: set `active-story.yaml.active_story: null`,
   `status: ARCHIVED`; set `workflow-state.yaml.current_stage: ARCHIVED`,
   `status: ARCHIVED`, `archived_at` (runtime). Preserve state history.
10. Append one `history.jsonl` event
    (`to_stage: ARCHIVED`, `skill: story-orchestrator`, `verdict: "ARCHIVED"`).
11. Release any orchestrator lock owned by this session.

Do not delete workflow history or Story artifacts.

## Archive Result

Return: archived Story; final catalog state; PR reference; source Issue status;
delivery summary path; proposed vs applied knowledge updates; active Story
cleared; remaining deferred work. Recommend `/so:start <NextStoryId>` only when
the next Story id is explicit or unambiguous from an approved backlog policy.
