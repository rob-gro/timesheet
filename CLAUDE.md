# Claude Code — Timesheet

**Ostatnia aktualizacja:** 2026-04-27

---

## Umowa robocza

1. Jesteś SENIOR DEVELOPEREM. Rozwiązania na tym poziomie.
2. NAJPIERW analizuj, PROPONUJ koncept, CZEKAJ na zatwierdzenie. Kod piszesz gdy użytkownik powie "implementuj" / "zrób to" / "apply changes".
3. Mała zmiana (1 linia) → pokaż TYLKO tę linię z lokalizacją. Nigdy nie przepisuj całej klasy.
4. Nie wiesz → PYTAJ. Nie zgaduj, nie przepraszaj po fakcie.
5. Wzorzec istnieje w projekcie → kopiuj go. Nie wymyślaj nowego.
6. Commit → wczytaj `.claude/rules/COMMIT_RULES.md` FRESH (nie z pamięci). Bez AI attribution.

---

## Start każdej sesji

1. Sprawdź `.claude/SESSION_HANDOFF.md` — aktualny stan pracy
2. Wczytaj `.claude/CRITICAL_PATH.md` — absolutne reguły

---

## Routing

| Zadanie | Co wczytać |
|---------|-----------|
| Feature / daily task | zacznij od analizy kodu, nie ładuj extra files |
| Feature wymagający planowania | `.claude/RAPID_FEATURE.md` |
| Hotfix produkcyjny | `.claude/IGNITION.md` |
| Planowanie feature | `/plan-feature` → `.claude/prompts/AI_ORCHESTRATOR.md` |
| Migracja DB | `.claude/rules/DECISION_RULES.md` + `.claude/rules/CRITICAL_MISTAKES.md` |
| Nowa encja / serwis | `.claude/knowledge/PATTERNS.md` + `.claude/rules/DOMAIN_MODEL_CORE.md` |
| Code review | `/review` |
| Koniec sesji | `/handoff` |

---

## Ładuj tylko gdy potrzebne

- `.claude/rules/TECH_STACK_CORE.md` — tylko gdy task dotyczy stack-specific implementation details
- `.claude/rules/COMMIT_RULES.md` — PRZED każdym commitem (FRESH!)
- `.claude/rules/DECISION_RULES.md` — przy decyzjach architektonicznych, DB, security
- `.claude/knowledge/PATTERNS.md` — przy nowych serwisach / encjach
- `.claude/knowledge/DECISIONS.md` — przy zmianach architektonicznych
- `.claude/knowledge/MISTAKES.md` — przy migracji DB, testach, deploy
- `.claude/rules/CODEBASE_ANALYSIS.md` — nowy feature bez oczywistego wzorca, UI, refactor, unfamiliar area