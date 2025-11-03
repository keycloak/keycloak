# Translation Guidelines

Thank you for your interest in contributing translations to Keycloak!
This document provides guidelines for contributing translations and getting started with Weblate.

## Why Translation Matters

Translation enables Keycloak to reach a wider audience by making the platform usable for speakers of various languages.
By translating Keycloak, you help to improve accessibility and usability for users who may not be proficient in English.

## What to translate

Keycloak welcomes translations of its user interfaces and error messages via two distinct ways:

* GitHub pull requests to `messages_*.properties` files,

OR

* Web based translation via [Weblate](https://hosted.weblate.org/projects/keycloak/).

There is also a [Keycloak Documentation translation project](https://github.com/openstandia/keycloak-documentation-i18n) to translate some parts of the documentation to Japanese.

## Guidelines and Conventions

To ensure consistency and quality across translations, please adhere to the following guidelines:

- Use formal or informal language as appropriate for the context.
- Maintain consistency with existing translations.
- Translate text accurately, ensuring that the meaning is preserved.

### Handling of single quotes (`'`)

Whenever messages are formatted in the backend, Keycloak uses Java's MessageFormat to replace placeholders. This uses
single quotes (`'`) as an escape mechanism.

Therefore, use typographic quotes like `‘` and `’` where possible.

To avoid problems, automatic checks ensure the following:

* In all "base" themes, where message keys are evaluated in the backend, no single quote (`'`) must be used standalone as it would not print when used in Java's MessageFormat.
  For legacy messages, a double single quote (`''`) can be used to print a single quote.

* In all UI themes, where message keys are evaluated in the frontend, a single quote (`'`) must only be used standalone, and a double single (`''`) must not be used.

### Spanish translations

- Use the formal "usted" form instead of the informal "vos or tu" (i.e., avoid tuteo), ensuring a more formal tone.

## Using GitHub pull requests to update translations

Translations via GitHub pull requests are possible for all languages.

While for those languages on Weblate the preferred way is to use Weblate, contributions are still possible via GitHub.
If you are not familiar with GitHub, and want your language to be added to Weblate, see "Steps to add a new language to
Weblate"

**Tasks of the translator:**

1. Optional: Create an issue or discussion as outlined in the [contributing docs](../CONTRIBUTING.md).
2. Update the `messages_*.properties` files for one or more languages.
3. Commit the changes with sign-off as outlined in the [contributing docs](../CONTRIBUTING.md). If you created an issue in the first step, reference it in the commit.
4. Create a pull request. If you created an issue in the first step, link it to the issue as described in [contributing docs](../CONTRIBUTING.md).
5. Find a reviewer that is a native speaker in that language.

**Task of the reviewer:**

1. As a native speaker, review the changes in the pull request. Once you are satisfied with the changes, leave a comment in the pull request that you approve the changes.

**Tasks of the maintainer:**

1. Once the review from the native speaker is in, check that the changes to the `messages_*.properties` files are syntactically correct.
2. If there are any translation pull requests created by Weblate, assure to merge them first to avoid conflicts in Weblate which are difficult to resolve.
3. If the changes are ok, approve the PR and merge it. If there is an existing issue, reference that in the squash-message, otherwise reference the issue of the pull request.

## Using Weblate to to update translations

Keycloak uses [Weblate](https://hosted.weblate.org/projects/keycloak/), a web-based translation platform, to manage translations of user interfaces and error messages.

It allows for notifications when the original string changes, and keeps track of missing translations. It also allows contributors without knowledge of Git to contribute to the translations.

The following translations are available in Weblate. If you have any questions or need assistance, feel free to reach out to the language maintainers listed below:

* German: [Robin Meese](https://github.com/robson90) && [Alexander Schwartz](https://github.com/ahus1)
* Dutch: [janher](https://github.com/janher) && [Erik Jan de wit](https://github.com/edewit)
* Japanese: [y-tabata](https://github.com/y-tabata) && [wadahiro](https://github.com/wadahiro) && [tnorimat](https://github.com/tnorimat) && [k-tamura](https://github.com/k-tamura)
* Catalan: [jmallach](https://github.com/jmallach) && [Ecron](https://github.com/Ecron)
* Spanish: [herver1971](https://github.com/herver1971) && [anthieni](https://github.com/anthieni)
* Slovenian: [mathmul](https://github.com/mathmul) && [SaraPristovnik](https://github.com/SaraPristovnik)
* Italian: [GioviQ](https://github.com/GioviQ) && [EdoardoTona](https://github.com/EdoardoTona)
* Romanian: [edwint88](https://github.com/edwint88) && [liviuroman](https://github.com/liviuroman)
* Portuguese (Brazil): [rafaelrddc](https://github.com/rafaelrddc) && [felipebz](https://github.com/felipebz) && [julianorodrigox](https://github.com/julianorodrigox)
* French: [Dodouce](https://github.com/Dodouce) && [GitSpoon](https://github.com/GitSpoon)
* Russian: [petrov9](https://github.com/petrov9) && [pasternake](https://github.com/pasternake)
* Traditional Chinese: [allen0099](https://github.com/allen0099) && [benwater12](https://github.com/benwater12)
* Greek: [infl00p](https://github.com/infl00p) && [knaiskes](https://github.com/knaiskes)
* Turkish: [spctr](https://github.com/spctr) && [ariferol](https://github.com/ariferol)
* Czech: [pionl](https://github.com/pionl) && [pschiffe](https://github.com/pschiffe)

To add a new language, see the section "Steps to Add a new language to Weblate" below.

**Tasks of the translator:**

1. Sign Up: Visit Weblate to create an account.
2. Configuration: After signing up, configure your Weblate account settings according to your preferences. 
   Please note that the committer email is set to the login email address by default. You can adjust this in your Weblate profile under Account settings.
3. Navigate to [Keycloak on Weblate](https://hosted.weblate.org/projects/keycloak/), confirm the contribution agreement, and start contributing translations.
4. For all untranslated and not-yet-approved keys you can directly add or update the translation.
5. For all approved keys you can suggest an alternative translation.

**Tasks of the maintainer:**

Weblate will create automated pull requests based on new or updated translations within 24 hours.
The goal is to merge those pull requests within 2-3 working days.
Translations are reviewed in Weblate by language maintainers for their correctness, still maintainers do some minimal checks as outlined below.

1. Check that the changes to the `messages_*.properties` files are syntactically correct.
2. Do a spot-check with Google Translate to avoid malicious community translations.
3. If the changes are ok, approve the PR and merge it. For pull requests created by Weblate there is no referenced GitHub issue, therefore reference the ID of the pull request in the squash-message.
4. Once the PR is merged, notify the respective language maintainers via a comment in the pull request that there are changes for their languages.

**Tasks of the language maintainers:**

**Goal:** Before a minor or major release that is due every three months, all translations should be in the state approved and all suggested translation should be reviewed.
A language that misses that goal will be removed from Weblate, and translations will then only be possible via GitHub pull requests afterwards.

1. Review all translations from the community (`state:=translated`) for your language, review them and set them to **Approved**.
2. Review all translations that have community suggestions (`has:suggestion`) for your language, review them and approve or reject the suggestions.
3. When notified that translation changes have been merged in a PR, review the translations that originated either from the community of from a co-maintainer of the language in that pull request, and issue any necessary updates via Weblate.

> [!TIP]
> We recommend language maintainers to set up notifications in Weblate to receive email notifications of outstanding tasks.

All language maintainers are set up in Weblate as reviewers for their language according to Weblate's "Dedicated Reviewers" process.

## Steps to add a new language to Weblate

We rely on volunteers like you to take the initiative in steering the translation efforts for additional translations.
If you're interested in contributing translations for a language not listed above, join
the [localization platform discussion](https://github.com/keycloak/keycloak/discussions/9270):

- Check the discussion, if your language is already proposed [discussion thread](https://github.com/keycloak/keycloak/discussions/9270).
- Each language requires **two volunteers**
- Volunteers should comment on the discussion thread to confirm their participation.
- The Keycloak-Team will then
    - enable the specific language
    - invite the two volunteers
    - comment on discussion thread, that the language has been enabled
- Weblate synchronizes daily, so it may take up to 24 hours after enabling the language before you can start your first translations

## Weblate Translation status

| Language                                                                       | Account UI                                                                                         | Admin UI                                                                                         | Theme base/account                                                                                        | Theme base/admin                                                                                        | Theme base/email                                                                                        | Theme base/login                                                                                        | Overall                                                                                   |
|--------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------|
| [German](https://hosted.weblate.org/projects/keycloak/-/de/)                   | ![Translation status](https://hosted.weblate.org/widget/keycloak/account-ui/de/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/admin-ui/de/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseaccount/de/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseadmin/de/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseemail/de/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baselogin/de/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/-/de/svg-badge.svg)      |
| [Dutch](https://hosted.weblate.org/projects/keycloak/-/nl/)                    | ![Translation status](https://hosted.weblate.org/widget/keycloak/account-ui/nl/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/admin-ui/nl/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseaccount/nl/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseadmin/nl/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseemail/nl/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baselogin/nl/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/-/nl/svg-badge.svg)      |
| [Japanese](https://hosted.weblate.org/projects/keycloak/-/ja/)                 | ![Translation status](https://hosted.weblate.org/widget/keycloak/account-ui/ja/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/admin-ui/ja/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseaccount/ja/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseadmin/ja/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseemail/ja/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baselogin/ja/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/-/ja/svg-badge.svg)      |
| [Catalan](https://hosted.weblate.org/projects/keycloak/-/ca/)                  | ![Translation status](https://hosted.weblate.org/widget/keycloak/account-ui/ca/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/admin-ui/ca/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseaccount/ca/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseadmin/ca/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseemail/ca/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baselogin/ca/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/-/ca/svg-badge.svg)      |
| [Spanish](https://hosted.weblate.org/projects/keycloak/-/es/)                  | ![Translation status](https://hosted.weblate.org/widget/keycloak/account-ui/es/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/admin-ui/es/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseaccount/es/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseadmin/es/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseemail/es/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baselogin/es/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/-/es/svg-badge.svg)      |
| [Slovenian](https://hosted.weblate.org/projects/keycloak/-/sl/)                | ![Translation status](https://hosted.weblate.org/widget/keycloak/account-ui/sl/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/admin-ui/sl/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseaccount/sl/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseadmin/sl/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseemail/sl/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baselogin/sl/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/-/sl/svg-badge.svg)      |
| [Italian](https://hosted.weblate.org/projects/keycloak/-/it/)                  | ![Translation status](https://hosted.weblate.org/widget/keycloak/account-ui/it/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/admin-ui/it/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseaccount/it/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseadmin/it/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseemail/it/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baselogin/it/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/-/it/svg-badge.svg)      |
| [Romanian](https://hosted.weblate.org/projects/keycloak/-/ro/)                 | ![Translation status](https://hosted.weblate.org/widget/keycloak/account-ui/ro/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/admin-ui/ro/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseaccount/ro/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseadmin/ro/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseemail/ro/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baselogin/ro/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/-/ro/svg-badge.svg)      |
| [Portuguese (Brazil)](https://hosted.weblate.org/projects/keycloak/-/pt_BR/)   | ![Translation status](https://hosted.weblate.org/widget/keycloak/account-ui/pt_BR/svg-badge.svg)   | ![Translation status](https://hosted.weblate.org/widget/keycloak/admin-ui/pt_BR/svg-badge.svg)   | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseaccount/pt_BR/svg-badge.svg)   | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseadmin/pt_BR/svg-badge.svg)   | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseemail/pt_BR/svg-badge.svg)   | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baselogin/pt_BR/svg-badge.svg)   | ![Translation status](https://hosted.weblate.org/widget/keycloak/-/pt_BR/svg-badge.svg)   |
| [French](https://hosted.weblate.org/projects/keycloak/-/fr/)                   | ![Translation status](https://hosted.weblate.org/widget/keycloak/account-ui/fr/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/admin-ui/fr/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseaccount/fr/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseadmin/fr/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseemail/fr/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baselogin/fr/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/-/fr/svg-badge.svg)      |
| [Russian](https://hosted.weblate.org/projects/keycloak/-/ru/)                  | ![Translation status](https://hosted.weblate.org/widget/keycloak/account-ui/ru/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/admin-ui/ru/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseaccount/ru/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseadmin/ru/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseemail/ru/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baselogin/ru/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/-/ru/svg-badge.svg)      |
| [Traditional Chinese](https://hosted.weblate.org/projects/keycloak/-/zh_hant/) | ![Translation status](https://hosted.weblate.org/widget/keycloak/account-ui/zh_hant/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/admin-ui/zh_hant/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseaccount/zh_hant/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseadmin/zh_hant/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseemail/zh_hant/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baselogin/zh_hant/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/-/zh_hant/svg-badge.svg) |
| [Greek](https://hosted.weblate.org/projects/keycloak/-/el/)                    | ![Translation status](https://hosted.weblate.org/widget/keycloak/account-ui/el/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/admin-ui/el/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseaccount/el/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseadmin/el/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseemail/el/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baselogin/el/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/-/el/svg-badge.svg)      |
| [Turkish](https://hosted.weblate.org/projects/keycloak/-/tr/)                  | ![Translation status](https://hosted.weblate.org/widget/keycloak/account-ui/tr/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/admin-ui/tr/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseaccount/tr/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseadmin/tr/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseemail/tr/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baselogin/tr/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/-/tr/svg-badge.svg)      |
| [Czech](https://hosted.weblate.org/projects/keycloak/-/cz/)                    | ![Translation status](https://hosted.weblate.org/widget/keycloak/account-ui/cz/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/admin-ui/cz/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseaccount/cz/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseadmin/cz/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseemail/cz/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baselogin/cz/svg-badge.svg)      | ![Translation status](https://hosted.weblate.org/widget/keycloak/-/cz/svg-badge.svg)      |