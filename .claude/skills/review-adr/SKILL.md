---
name: review-adr
description: Reviews an ADR in docs/decisions/ against the project's ADR guidelines. Use it whenever the user asks to review, check or critique an ADR, in Polish or English ("zrób review ADR", "sprawdź ADR", "przejrzyj ADR 0003", "czy ten ADR jest ok", "review the ADR"). Do not use it to write a new ADR; write-adr covers that.
---

# Review an ADR

Read `.claude/adr/guidelines.md` first.

## Steps

1. Review the ADR the user names. If none is named, take the highest number in `docs/decisions/`.
2. Read the ADRs it links or supersedes.
3. Check the form against the guidelines: file name, unique number, `status`, `date`, required sections.
4. Check every criterion from the guidelines. Verify each technical claim in documentation (context7) or in the code. A false claim and an unconfirmed claim are both findings.
5. Check every sentence. Flag a sentence that:
   - adds nothing, so removing it loses no information,
   - needs more context, because the reader must guess what it refers to (an ADR must be explicit),
   - is needlessly complex and can be split or said in simpler words.
6. Report. Do not edit the ADR.

## Report

Write in Polish. Quote the ADR in English.

```markdown
**ADR:** {path}
**Verdict:** {gotowy | do poprawy}

1. **{Section} · {criterion N | adds nothing | needs context | too complex}.** {What is wrong.}
   Fix: {Ready text to paste.}
2. ...
```

Order findings: false claims, missing content, missing context, sentences that add nothing, complex sentences.
With no findings, give the path and the verdict only.
