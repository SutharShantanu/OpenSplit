# Contributing to OpenSplit

Thanks for your interest! OpenSplit is a free, open-source Splitwise alternative and
contributions are welcome.

## Getting set up

Follow [docs/SETUP.md](docs/SETUP.md) to configure Firebase and build the project.

## Ground rules

- **Keep it free.** OpenSplit has no paid tier. Don't add features that gate
  functionality behind payment or that require a paid third-party service to work.
- **Material 3 only.** The UI is 100% Jetpack Compose Material 3 — no Material 2
  components (`androidx.compose.material.*`), except the `material-icons-*` artifacts.
- **Respect the layers.** `ui` depends on `domain` interfaces; `data` implements them.
  ViewModels must not reference Firebase directly — go through a repository.
- **Pure logic stays pure.** Split math and debt simplification live in
  `domain/logic/` with no Android/Firebase dependencies, so they can be unit-tested.

## Before you open a PR

1. Run the unit tests:

   ```bash
   ./gradlew :app:testDebugUnitTest
   ```

2. Run lint:

   ```bash
   ./gradlew :app:lintDebug
   ```

3. If you touched split math, balances, or currency formatting, **add or update unit
   tests** under `app/src/test/java/com/opensplit/`.

## Money & correctness

Expense-splitting is a money app; small bugs cause real disputes. When changing
balance or split logic:

- Splits must **sum exactly** to the expense total (push rounding remainder onto the
  last participant).
- Balances must **account for settlements**.
- **Never sum amounts across currencies** — aggregate per-currency.
- Honor per-currency minor units (e.g. JPY has 0 decimal places).

## Commit style

Use clear, conventional-ish messages: `fix: …`, `feat: …`, `refactor: …`,
`docs: …`, `test: …`.
