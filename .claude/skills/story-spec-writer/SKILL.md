---
name: story-spec-writer
description: Turns a user story or set of requirements (with Acceptance Criteria) into a formal Markdown specification document, stored under docs/specifications. Use this whenever the user asks to "write a spec", "create a specification", "turn this story into a spec", "document these requirements formally", or pastes a user story / set of Acceptance Criteria and wants it formalized. Every requirement in the output spec must be traceable back to a specific Acceptance Criterion ID from the input — this skill never invents requirements that aren't present or directly derivable from what was given.
---

# Story Spec Writer

Converts a user story (or a looser set of requirements) into a clean, traceable Markdown
specification document. The defining constraint of this skill is discipline: the spec must
say only what the input says. It is a faithful restructuring of given requirements into a
formal document, not an opportunity to fill gaps with assumptions.

## Why this matters

Specs are used by other people (and other agents) to build and verify things. If a spec
quietly adds a requirement the stakeholder never asked for, someone will build the wrong
thing, or a reviewer will waste time trying to figure out where an AC came from. Traceability
is what lets a reader jump from any line in the spec back to the exact input requirement that
justifies it. If you can't point to where a requirement came from, it doesn't belong in the spec.

## Workflow

1. **Read the input carefully.** The input may be a user story ("As a ___, I want ___, so
   that ___"), a bug/feature ticket, or a raw list of requirements. Identify or assign
   Acceptance Criteria IDs (e.g. `AC-1`, `AC-2`, ...) if the input doesn't already have them.
   If the input already has IDs (e.g. from Jira, Linear, a ticket), reuse those exact IDs —
   don't renumber them.

2. **Check for ambiguity or gaps before writing.** If a requirement is vague, contradictory,
   or clearly incomplete, do not guess or fill in the blank yourself. Either:
   - Note it explicitly in an "Open Questions" section of the spec, or
   - Ask the user directly if the gap is significant enough to block writing a useful spec.

   The failure mode to avoid: silently deciding what the stakeholder "probably meant" and
   writing that in as if it were given. If you infer something because it's a direct, necessary
   consequence of an explicit AC (not a new capability), it's fine — but call out the inference
   inline so the reader can see the reasoning, e.g. "(derived from AC-2: pagination requires a
   page-size default)".

3. **Structure the spec** using the template below. Every functional requirement bullet must
   end with a traceability tag pointing to the AC(s) it comes from.

4. **Save the output** to `docs/specifications/`, creating the directory if it doesn't exist.
   Use a descriptive kebab-case filename derived from the story title or feature name, e.g.
   `docs/specifications/user-password-reset.md`. If a file with that name already exists and
   this is a revision of the same story, overwrite it; if it's a distinct story that happens to
   share a name, ask the user how to disambiguate.

## Spec template

Use this structure. Omit a section only if there is truly nothing to put in it (e.g. no open
questions) — don't pad sections with filler to look complete.

```markdown
# Specification: <Feature / Story Title>

**Source:** <where this came from — ticket ID, user message, file name>
**Status:** Draft

## Summary
One or two sentences describing what this feature/change is, drawn directly from the story.

## Acceptance Criteria (source)
Reproduce the input Acceptance Criteria verbatim, each with its ID. This is the ground truth
everything else in the document traces back to.

- **AC-1:** <verbatim text>
- **AC-2:** <verbatim text>

## Functional Requirements
Each requirement is a direct restatement or structuring of one or more AC's, tagged with the
ID(s) it traces to.

- The system shall <requirement>. `[AC-1]`
- The system shall <requirement>. `[AC-2, AC-3]`

## Non-Functional Requirements
Only include if the input specifies any (performance, security, accessibility, etc.). Tag the
same way. Omit this section entirely if none were given — do not invent NFRs.

## Out of Scope
Only include if the input explicitly says something is out of scope. Do not speculate about
what's out of scope unless told.

## Open Questions
Anything ambiguous, missing, or contradictory in the source input that blocks a fully precise
spec. Each item should say what's unclear and why it matters.

## Traceability Matrix
A table mapping every AC to the requirement(s) that implement it, so coverage can be checked
at a glance.

| AC ID | Requirement(s) |
|-------|----------------|
| AC-1  | FR listed above referencing AC-1 |
| AC-2  | FR listed above referencing AC-2 |
```

## Rules

- **Never invent requirements.** If the user story doesn't mention error handling, validation
  rules, edge cases, or NFRs, the spec doesn't either — flag it as an open question instead if
  it seems like a meaningful gap.
- **Every functional requirement needs a traceability tag.** A requirement with no `[AC-x]` tag
  is a sign it was invented, not derived — go back and either find its source AC or cut it.
- **Preserve the original AC IDs and wording** in the "Acceptance Criteria (source)" section
  so the spec can always be checked against the original input.
- **Output is Markdown, saved under `docs/specifications/`.** Don't put specs anywhere else
  unless the user explicitly asks for a different location for that particular request.
