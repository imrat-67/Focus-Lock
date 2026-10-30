# START HERE (read this fully, then follow it exactly)

You are continuing the **Focus Lock** Android project. Previous Claude sessions may have ended suddenly (token limit). Assume nothing; verify with git.
Project root: `/home/imtiaj-hossain-saikat/Documents/project/Focus Lock` · Remote: `git@github.com:imrat-67/Focus-Lock.git`
Use the **Desktop Commander MCP** for all files, shell, installs and git.

## Boot sequence (do in order, keep it cheap)
1. Read `Cloud_Memory/MOTHER_PROMPT.md` (rules, ~1 min).
2. Read `Cloud_Memory/STATUS.md` (task board + the live CHECKPOINT).
3. Read `Cloud_Memory/NEXT.md` (what to do right now).
4. Run `git status -s && git log --oneline -8`. **Git is the source of truth**: if STATUS disagrees with the code, fix STATUS first.
5. Open only what the current task needs: SRS sections by ID (`grep -n "LOCK-" SRS.md`), `docs/ARCHITECTURE.md`, `DECISIONS.md` if a technical choice comes up, `ISSUES.md` if something fails. Never re-read whole files you already know.

## Write-ahead protocol (this is what makes handoffs survive a sudden cutoff)
- BEFORE starting any task or sub-step: update the CHECKPOINT block in `STATUS.md` (task id, step plan, files you will touch).
- AFTER each sub-step: tick it in CHECKPOINT, then `git add -A && git commit -m "wip(Txx): ..."` (small, frequent; push at every finished task or at least hourly).
- AFTER each finished task: mark it DONE in STATUS, delete its checkpoint details, rewrite `NEXT.md` (5–10 lines: next task, exact first command, anything the user must do), commit with `feat(Txx): ...`, push.
- Don't wait for "tokens are low" — you cannot feel it. Update NEXT.md after every task.

## Token discipline
Short replies. Write files in chunks. Use `grep`/`sed -n` ranges instead of reading big files. No unrequested explanations. No unneeded files. Keep Cloud_Memory files tiny; delete finished details instead of archiving them.

## If blocked by something only the user can do
(Firebase console, phone USB authorisation, passwords, GitHub auth) → put it at the top of `NEXT.md` under **USER ACTION NEEDED**, tell the user in 3 lines, and continue with any unblocked task.

## Handoff prompt the user pastes into a new Claude chat
"Use Desktop Commander. Read /home/imtiaj-hossain-saikat/Documents/project/Focus Lock/Cloud_Memory/START_HERE.md and follow it."
