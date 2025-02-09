# Translation Guidelines

Thank you for your interest in contributing translations to Keycloak! 
This document provides guidelines for contributing translations and getting started with Weblate.

## Why Translation Matters

Translation enables Keycloak to reach a wider audience by making the platform usable for speakers of various languages. By translating Keycloak, you help improve accessibility and usability for users who may not be proficient in English.

## What to translate

Your contributions can greatly enhance the user experience for non-English speakers.

Keycloak welcomes translations of its user interfaces and error messages via Weblate. 

There is also a [Keycloak Documentation translation project](https://github.com/openstandia/keycloak-documentation-i18n). 

## Getting Started with Weblate

Keycloak uses [Weblate](https://hosted.weblate.org/projects/keycloak/), a web-based translation platform, to manage translations of user interfaces and error messages.

Here's how you can get started:

- Sign Up: Visit Weblate to create an account.
- Configuration: After signing up, configure your Weblate account settings according to your preferences. Please note that the committer email is set to the login email address by default. You can adjust this in your Weblate profile under Account settings.
- Join the Keycloak translation team on Weblate to start contributing translations.

### Guidelines and Conventions

To ensure consistency and quality across translations, please adhere to the following guidelines:

- Use formal or informal language as appropriate for the context.
- For Spanish translations, please use the formal "usted" form instead of the informal "vos or tu" (i.e., avoid tuteo), ensuring a more formal tone.
- Maintain consistency with existing translations.
- Translate text accurately, ensuring that the meaning is preserved.
- Weblate will pushe translations as a pull request to the [Keycloak repository](https://github.com/keycloak/keycloak) at least once a day. A maintainer for that language has the ability to approve or decline that translation.

### Supported translations

Keycloak already supports a lot of translations. 
While were in the transitioning process towards Weblate, the following translations are available in Weblate:

- German
- Dutch
- Japanese
- Catalan
- Spanish

For all other translations, look for the `messages_*.properties` files in the main repository and create a pull request with your translations. 

Please note that while we aspire to support more translations in the future, our current focus is on these translations. We rely on volunteers like you to take the initiative in steering the translation efforts for additional translations. If you're interested in contributing translations for a language not listed above, add a comment to the [localization platform discussion](https://github.com/keycloak/keycloak/discussions/9270).

If you have any questions or need assistance, feel free to reach out to the language maintainers listed below:

* German: [Robin Meese](https://github.com/robson90)
* Dutch: [Jon Koops](https://github.com/jonkoops)
* Japanese: [y-tabata](https://github.com/y-tabata) && [wadahiro](https://github.com/wadahiro)
* Catalan: [jmallach](https://github.com/jmallach) && [Ecron](https://github.com/Ecron)
* Spanish: [herver1971](https://github.com/herver1971) && [anthieni](https://github.com/anthieni)

### Steps to Add a New Language

- Check the discussion, if your language is already proposed [discussion thread](https://github.com/keycloak/keycloak/discussions/9270).
- Each language requires **two volunteers**
- Volunteers should comment on the discussion thread to confirm their participation.
- The Keycloak-Team will then
    - enable the specific language
    - invite the two volunteers
    - comment on discussion thread, that the language has been enabled
- Weblate synchronizes daily, so it may take up to 24 hours after enabling the language before you can start your first translations 

### Translation status

| Language                                                       | Account UI                                                                                    | Admin UI                                                                                    | Theme base/account                                                                                   | Theme base/admin                                                                                   | Theme base/email                                                                                   | Theme base/login                                                                                   | Overall                                                                              |
|----------------------------------------------------------------|-----------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| [German](https://hosted.weblate.org/projects/keycloak/-/de/)   | ![Translation status](https://hosted.weblate.org/widget/keycloak/account-ui/de/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/admin-ui/de/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseaccount/de/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseadmin/de/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseemail/de/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baselogin/de/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/-/de/svg-badge.svg) |
| [Dutch](https://hosted.weblate.org/projects/keycloak/-/nl/)    | ![Translation status](https://hosted.weblate.org/widget/keycloak/account-ui/nl/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/admin-ui/nl/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseaccount/nl/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseadmin/nl/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseemail/nl/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baselogin/nl/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/-/nl/svg-badge.svg) |
| [Japanese](https://hosted.weblate.org/projects/keycloak/-/ja/) | ![Translation status](https://hosted.weblate.org/widget/keycloak/account-ui/ja/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/admin-ui/ja/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseaccount/ja/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseadmin/ja/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseemail/ja/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baselogin/ja/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/-/ja/svg-badge.svg) |
| [Catalan](https://hosted.weblate.org/projects/keycloak/-/ca/)  | ![Translation status](https://hosted.weblate.org/widget/keycloak/account-ui/ca/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/admin-ui/ca/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseaccount/ca/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseadmin/ca/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseemail/ca/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baselogin/ca/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/-/ca/svg-badge.svg) |
| [Spanish](https://hosted.weblate.org/projects/keycloak/-/es/)  | ![Translation status](https://hosted.weblate.org/widget/keycloak/account-ui/es/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/admin-ui/es/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseaccount/es/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseadmin/es/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baseemail/es/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/theme-baselogin/es/svg-badge.svg) | ![Translation status](https://hosted.weblate.org/widget/keycloak/-/es/svg-badge.svg) |
