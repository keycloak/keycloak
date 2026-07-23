---
description: Review a Weblate translation PR for deviations from English source messages
allowed-tools: Bash(gh pr list:find the PR connected to this branch), Bash(gh pr diff:fetch PR diff), Bash(git branch:check current branch), Bash(grep:look up English source keys), Read, Agent, Write(pr-*-translation-review.md:write review output)
---

Analyze pull request $ARGUMENTS and list new or updated translations that significantly deviate from the English message that it translates. Group results by language, and mention the language maintainers. Prepare the result as Markdown. Skip languages that have no findings. Write the output to a file `pr-<number>-translation-review.md` in the repository root.

**Important:** Review every changed key in the diff, not just a sample. This is a full review, not a spot check.

## Workflow

1. Extract the PR number from the argument (accept both a full GitHub URL and a bare number). If it is missing, ask the user. 
2. Verify that the PR branch is currently checked out locally (use `gh pr list` to find the branch name for the PR, then check `git branch --show-current`). If not, abort and ask the user to check out the PR first.
3. Fetch the diff with `gh pr diff <number> --repo keycloak/keycloak`.
4. Read `.github/language-maintainers.yml` to map language codes to maintainer GitHub handles.
5. For each changed `messages_<lang>.properties` file in the diff, identify the added/modified keys.
6. Look up the English value for each key in the corresponding English source file (see file locations below).
7. Multi-line `.properties` values use trailing `\` for line continuation — always read the full value before comparing.
8. Compare the translation against the English source. Flag significant deviations such as:
   - **Typos** in the translation (e.g. "Passky" instead of "Passkey", transliteration errors)
   - **Placeholder mismatches** — `{{var}}` or `{0}` missing, duplicated, or altered
   - **Meaning shifts** — translation says something materially different from the English, including accidentally negating it (e.g. "must not" translated as "must", or "enabled" as "disabled"). Pay extra attention to security warnings, certificate errors, session expiration notices, and account lockout messages where lost urgency or accuracy can have security consequences.
   - **Polysemy errors** — English words with multiple meanings translated with the wrong sense (e.g. "Light" as weight instead of theme mode, "Offer" as commercial deal instead of credential offer, "Round" as shape instead of truncation strategy)
   - **Meaning shifts in compound labels** — e.g. "Max allowed assertion expiration" (max expiration time) mistranslated as "expiration of max number of assertions"
   - **Action label confusion** — sensitive operation labels like "Approve"/"Deny", "Grant"/"Revoke", "Delete"/"Cancel" swapped, softened, or ambiguous. Users performing the opposite of what they intended is a security risk.
   - **OAuth scope/permission descriptions** — these are what users rely on for informed consent. A scope described as "read your profile" translated as "manage your account" changes what users think they're agreeing to.
   - **Password/credential instructions** — requirements like "at least 8 characters" translated as "at least 8 digits", or MFA setup instructions that are wrong, can lock users out or weaken security.
   - **Recovery flow messages** — mistranslated account recovery instructions could prevent users from recovering access or direct them to insecure paths.
   - **Grammar errors** — wrong gender, verb form, case, etc.
   - **Abbreviations mistranslated** — terms like LoA, ACR, PKCE, DPoP, OIDC, OID4VCI should generally stay as-is
   - **Terminology errors** — feature names that were renamed in the English but the translation still uses the old name
   - **Formality violations** — check `docs/translation.md` for per-language conventions (e.g. Spanish requires formal "usted")
   - **Malicious content** — flag with high severity:
     - Injected URLs or email addresses not present in the English (phishing, malware)
     - Social engineering (e.g. "contact support at [attacker contact]", "enter your password")
     - Consent screen manipulation — subtly altering permission/consent text so users agree to more than intended (e.g. "read" → "full", dropping a "not")
     - Property injection — extra `key=value` lines smuggled into a multi-line property value
   - **Empty translations** — key present but value is blank (silently renders nothing in the UI, particularly dangerous for security warnings or consent text)
   - **Inconsistency within a language** — same English term translated differently within the same PR (e.g. "realm" as both "dominio" and "reino")
9. Do NOT flag:
   - CJK translations that are shorter in character count (natural for Chinese/Japanese/Korean)
   - Stylistic differences that preserve the meaning
   - Minor punctuation or whitespace differences
10. Group findings by language with a table per language. Include the language maintainers as `@handle`. Skip languages with no findings. List languages with no findings at the end.
11. Write the final Markdown output to `pr-<number>-translation-review.md`.

## Translation file locations

Compare against the English source files (ground truth). Community translations live in parallel `-community` directories.

**Admin UI** (React-based admin console):
- English: `js/apps/admin-ui/maven-resources/theme/keycloak.v2/admin/messages/messages_en.properties`
- Translations: `js/apps/admin-ui/maven-resources-community/theme/keycloak.v2/admin/messages/messages_<lang>.properties`

**Account UI** (React-based account console):
- English: `js/apps/account-ui/maven-resources/theme/keycloak.v3/account/messages/messages_en.properties`
- Translations: `js/apps/account-ui/maven-resources-community/theme/keycloak.v3/account/messages/messages_<lang>.properties`

**Login** (login, registration, and related pages):
- English: `themes/src/main/resources/theme/base/login/messages/messages_en.properties`
- Translations: `themes/src/main/resources-community/theme/base/login/messages/messages_<lang>.properties`

**Account** (base account theme, server-side rendered):
- English: `themes/src/main/resources/theme/base/account/messages/messages_en.properties`
- Translations: `themes/src/main/resources-community/theme/base/account/messages/messages_<lang>.properties`

**Admin** (base admin theme, server-side):
- English: `themes/src/main/resources/theme/base/admin/messages/messages_en.properties`
- Translations: `themes/src/main/resources-community/theme/base/admin/messages/messages_<lang>.properties`

**Email** (base email theme — subject lines and body text):
- English: `themes/src/main/resources/theme/base/email/messages/messages_en.properties`
- Translations: `themes/src/main/resources-community/theme/base/email/messages/messages_<lang>.properties`

**Email (keycloak theme)** (themed email overrides):
- English: `themes/src/main/resources/theme/keycloak/email/messages/messages_en.properties`
- Translations: `themes/src/main/resources-community/theme/keycloak/email/messages/messages_<lang>.properties`

**Login (keycloak theme)** (themed login overrides):
- English: `themes/src/main/resources/theme/keycloak/login/messages/messages_en.properties`
- Translations: `themes/src/main/resources-community/theme/keycloak/login/messages/messages_<lang>.properties`

**Login (keycloak.v2 theme)** (v2 login overrides):
- English: `themes/src/main/resources/theme/keycloak.v2/login/messages/messages_en.properties`
- Translations: `themes/src/main/resources-community/theme/keycloak.v2/login/messages/messages_<lang>.properties`

**Welcome** (welcome page):
- English: `themes/src/main/resources/theme/keycloak/welcome/messages/messages_en.properties`
- Translations: `themes/src/main/resources-community/theme/keycloak/welcome/messages/messages_<lang>.properties`