---
name: write-adr
description: Writes an Architecture Decision Record (ADR) into docs/decisions/. Use this skill whenever the user asks to write, create, add or record an ADR, or to document an architectural or technical decision, in Polish or English ("napisz ADR", "zrób ADR", "udokumentuj decyzję", "zapisz decyzję", "write an ADR for ..."). Use it also when the user wants a decision made in the conversation, such as UUID keys or JPA over JDBC, captured in the repo. Do not use it to review an existing ADR; the review-adr skill covers that.
---

# Write an ADR

Read `.claude/adr/guidelines.md` first.
It holds the location, the naming rule, the template and the quality criteria.
The `review-adr` skill checks ADRs against the same criteria, so an ADR written here should pass that review.

## Steps

### 1. Collect the decision

Find these facts:

- the decision itself,
- the problem that forced it,
- the options that were considered,
- the reasons the chosen option won and the others lost.

Take them from the conversation first. Decisions are often discussed before the user asks for the ADR.
Ask the user for any fact that is missing. The ADR records the user's reasoning, so do not invent reasons or options.
Ask all missing questions in one message.

### 2. Check existing ADRs

List `docs/decisions/`.
Use the highest existing number plus one. Start with `0001` when the directory is empty or missing.
Read any ADR that covers the same topic.
When the new decision replaces an old one, set the old ADR's status to `superseded by ADR-NNNN` and leave its content unchanged.
When an old ADR already records the same decision, tell the user and stop.

### 3. Verify the technical claims

Each argument in the ADR is a claim about a library, a database or the code.
Verify each claim in documentation (for example through context7) or in the code before writing it.
Leave out a claim that you cannot verify. An ADR with a false argument misleads every later reader.

### 4. Write the file

Fill the template from the guidelines.
Use status `accepted` when the user has already made the decision. Use `proposed` when the decision is still open.
Use today's date.

### 5. Check the draft against the criteria

Go through each criterion in the guidelines and fix every violation.
The most common violations are a generic context, an argument without a mechanism and a chosen option without cons.

Then check every sentence and fix a sentence that:

- adds nothing, so removing it loses no information,
- needs more context, because the reader must guess what it refers to (an ADR must be explicit),
- is needlessly complex and can be split or said in simpler words.

### 6. Report

Tell the user the file path and the chosen option in one or two sentences.
