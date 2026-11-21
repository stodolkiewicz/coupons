# ADR guidelines

Both the `write-adr` and the `review-adr` skills use this file.
The template defines the structure of an ADR.
The criteria define what makes an ADR good.

## Location and naming

- ADRs live in `docs/decisions/`.
- The file name is `NNNN-title-with-dashes.md`. `NNNN` is the next free number, padded to four digits.
- The title is lowercase words joined with dashes, for example `0003-use-uuid-primary-keys.md`.

## Template

```markdown
---
status: {proposed | accepted | rejected | deprecated | superseded by ADR-NNNN}
date: {YYYY-MM-DD}
---

# {Short title that names the problem and the chosen solution}

## Context and Problem Statement

{The problem. When there are several, list them:}

Problems:

1. **{Name}.** {Problem.}
2. **{Name}.** {Problem.}

## Considered Options

### 1. {Chosen option} (chosen)

{Description.}

Pros:

- {Concrete argument.}

Cons:

- {Concrete argument.}

### 2. {Other option}

{Description.}

Pros:

- {Concrete argument.}

Cons:

- {Concrete argument.}

## More Information

{Only when needed: links to documentation and related ADRs.}
```

## Criteria

1. **One decision per ADR.** A second decision gets its own ADR. Two decisions in one file cannot be superseded separately.
2. **The context is specific to this project.** It names the requirement, the constraint or the code that forces the decision. A context that fits any project explains nothing.
3. **The options are real.** Each option is something a competent engineer would pick in this situation. A strawman option makes the comparison worthless.
4. **Every argument names a mechanism.** "Scalable" or "flexible" alone says nothing. Write what happens: "the atomic `UPDATE` blocks the row, so two parallel requests cannot exceed the limit".
5. **The chosen option comes first.** Its heading ends with "(chosen)". The cons of the other options show why they lost.
6. **Every option lists its cons.** Every decision has a cost. A chosen option without cons hides it.
7. **Every claim is verified.** Library behavior, defaults and versions come from documentation or from the code. An unverified claim stays out of the ADR.
8. **The text is dense.** Each sentence carries one concrete piece of information. Sentences are short and have a verb. Filler, repetition and obvious statements stay out.
9. **The ADR is in English.**
10. **Requirements are stated, not cited.** The ADR never quotes or references `task.md`. A requirement from `task.md` is written in the ADR as a plain English requirement, for example "A user can redeem a given coupon only once."
11. **A superseded ADR points to its successor.** Its status becomes `superseded by ADR-NNNN`. Its content stays unchanged.
