## Summary

<!--
Briefly describe what this PR changes.
Focus on intent, not implementation details.
-->

---

## Related issue(s)

<!--
Link the related Jira issue(s).
Use full links or issue keys.
-->

- [SMART-#](https://marsys.atlassian.net/browse/SMART-#)

---

## Architectural impact

<!--
Does this change affect architecture or design decisions?
-->

- [ ] No architectural impact
- [ ] Minor architectural impact (within existing decisions)
- [ ] Significant architectural impact (new or changed decisions)

<!--
ADR references should be included here if applicable.
Please note the ADR references in the following format:
- [#0001 - Sample ADR](docs/adr/0001-sample-adr.md)
-->

If applicable, ADR reference(s):
- None

---

## Design & conventions checklist (filled by the author)

Please confirm the following:

### General
- [ ] This PR has a clear and limited scope
- [ ] Code follows existing naming and structural conventions
- [ ] No unrelated changes are included

### Domain & architecture
- [ ] Domain code contains no infrastructure or framework dependencies
- [ ] Changes respect existing architectural boundaries (DDD / Hexagonal)
- [ ] No new coupling introduced between domain and adapters

### Git & workflow
- [ ] Branch is up to date with `main`
- [ ] Commits are rebased (no merge commits)

---

## Testing

<!--
Explain how this change is tested.
-->

- [ ] Unit tests added or updated
- [ ] Existing tests still pass
- [ ] Changes are testable without external infrastructure
- [ ] Manual testing performed (if applicable)

Details (optional): N/A

---

## Documentation

- [ ] README updated (if applicable)
- [ ] ARCHITECTURE.md updated (if applicable)
- [ ] ADRs added/updated (if applicable)
- [ ] Public APIs or behavior changes are documented

---

## Reviewer checklist

### Correctness & quality
- [ ] The change does what it claims to do
- [ ] Code is readable and maintainable
- [ ] No unnecessary complexity introduced

### Architecture
- [ ] Architectural boundaries are respected
- [ ] Domain model remains consistent and expressive
- [ ] No hidden architectural decisions introduced

### Risk & impact
- [ ] Change is low-risk / well-isolated  
  **or**
- [ ] Risk is understood and acceptable

---

## Notes for reviewer

<!--
Anything the reviewer should pay special attention to?
Trade-offs, open questions, or known limitations.
-->

N/A

---

## Post-merge follow-ups (optional)

<!--
List any follow-up work that is explicitly out of scope for this PR.
Please note them down by adding checkboxes, so that follow-ups and their state can be tracked
-->

- N/A
