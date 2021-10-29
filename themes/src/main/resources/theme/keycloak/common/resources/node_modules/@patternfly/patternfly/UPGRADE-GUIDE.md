---
id: Upgrade guide
section: developer-resources
---

Hey, Flyers! We've been busy for the past 12 weeks working on significant changes to PatternFly's HTML and CSS. We made our components mobile-first and changed colors and the default font. This upgrade guide details **what** was broken and **how** to fix it. To learn **why** a change was made, check out the linked pull requests.

## Global
### Colors
To meet accessibility requirements, various colors have been updated to increase contrast in more usage scenarios. Some color variables have been remapped to better suit new component designs such as alerts, and we’ve also added new `-50` color tints to serve as colored backgrounds where appropriate. As a follow-up to last year's color changes, we've updated Blue and Cyan palettes' `-600` and `-700` values as well.

### Mobile first CSS
We've updated some components' CSS to be mobile first by using `@min-width` media queries. The components that are now mobile-first are background image, data list, toolbar, form, login, page, toolbar, and wizard [(#2816)](https://github.com/patternfly/patternfly/pull/2816).
  - The only component that cannot be mobile-first is the table, specifically with the `.pf-m-grid[-on-{breakpoint}]` modifiers. At the specified grid breakpoint, native/default properties are modified to achieve a grid-based layout. Overwriting these changes would require adding back native properties to the table. For that reason, the table will remain the exception to a mobile-first approach.

### Vertical navigation hidden breakpoint
We've changed the hidden breakpoint for the vertical nav to be `$pf-global--breakpoint--xl` (1200px) rather than `$pf-global--breakpoint--md` (768px) [(#2962)](https://github.com/patternfly/patternfly/pull/2962). The overall page chrome and some individual components' horizontal spacing becomes more compact at this breakpoint (true of the old breakpoint and the new). Applications' custom elements that aligned with the old breakpoint may not align now since the chrome spacing changes at 1200px instead of 768px. You may need to make updates to match the chrome's spacing at the new breakpoint.

### Removed "shield" CSS
We've made shield styles optional by default [(#2872)](https://github.com/patternfly/patternfly/pull/2872). The "shield" styles were intended to help resolve styling issues when using PF3 and PF4 together, and when having opted out of the global reset CSS, but they have proven to be problematic and not necessary for most users. With this change, we encourage applications to adopt PatternFly's reset CSS, if they have specifically opted out of it previously. The shield styles can be re-enabled if needed, either by setting `$pf-global--enable-shield: true;` and compiling PatternFly's SCSS, or by manually importing `base/patternfly-shield-inheritable.css` and `base/patternfly-shield-non-inheritable`. See [(#2872)](https://github.com/patternfly/patternfly/pull/2872) for more details.

### Default font
We've updated the default font from `Overpass` to `RedHatText` and `RedHatDisplay` [(#2955)](https://github.com/patternfly/patternfly/pull/2955). To continue to use `Overpass`, add the class `pf-m-overpass-font` on an element that wraps your application (for example, `<body>`).

You don’t have to do anything further to use this font. However, with the change from  `Overpass`  to  `RedHatText` and `RedHatDisplay`, we encourage you to review your application’s typography styles to ensure they are correct.

### Directory structure
We've cleaned up our root directory a little in [(#2960)](https://github.com/patternfly/patternfly/pull/2960). If you're compiling or importing more internal parts of PatternFly, you'll likely need to update your imports:
- `patternfly-common.css` to `base/patternfly-common.css`
- `patternfly-fa-icons.css` to `base/patternfly-fa-icons.css`
- `patternfly-fonts.css to` to `base/patternfly-fonts.css`
- `patternfly-globals.css` to `base/patternfly-globals.css`
- `patternfly-icons.css` to `base/patternfly-icons.css`
- `patternfly-pf-icons.css` to `base/patternfly-pf-icons.css`
- `patternfly-shield-inheritable.css` to `base/patternfly-shield-inheritable.css`
- `patternfly-shield-noninheritable.css` to `base/patternfly-shield-noninheritable.css`
- `patternfly-themes.css` to `base/patternfly-themes.css`
- `patternfly-variables.css` to `base/patternfly-variables.css`
- Removed `patternfly-imports.scss`

### Code changes

Updated CSS:
- `--pf-global--gutter` has changed to `--pf-global--spacer--md`
- `--pf-global--gutter--md` has changed to `--pf-global--spacer--lg`
- `--pf-global--success-color--100` has changed to `--pf-global--palette--green-500`
- `--pf-global--success-color--200` has changed to `--pf-global--palette--green-700`
- `--pf-global--info-color--100` has changed to `--pf-global--palette--blue-300`
- `--pf-global--BackgroundColor--200` has changed to `--pf-global--palette--black-200`
- `--pf-global--palette--green-50` has changed to `#f3faf2`
- `--pf-global--palette--green-500` has changed to `#3e8635`
- `--pf-global--palette--black-200` has changed to `#f0f0f0`
- `--pf-global--palette--black-600` has changed to `#6a6e73`
- `--pf-global--palette--blue-50` has changed to `#e7f1fa`
- `--pf-global--palette--blue-600` has changed to `#002952 `
- `--pf-global--palette--blue-700` has changed to `#001223 `
- `--pf-global--palette--cyan-600` has changed to `#002323`
- `--pf-global--palette--cyan-700` has changed to `#000f0f `
- Updated global shadows and added xl shadow [(#2854)](https://github.com/patternfly/patternfly/pull/2854)
- Added new cyan, gold, green, red colors [(#2906)](https://github.com/patternfly/patternfly/pull/2906)

Removed classes:
- `.pf-m-redhat-font`
  - `RedHatText` is now the default font. Apply `.pf-m-overpass-font` to use `Overpass` instead.

Removed variables:
- `--pf-global--BackgroundColor--150` and `--pf-global--BackgroundColor--300`
  - These can be updated to use `--pf-global--BackgroundColor--200` instead, but you should consult your visual designer first.
- All bootstrap variables [(#2871)](https://github.com/patternfly/patternfly/pull/2871)
  - These are no longer included with PatternFly by default. If you still need these, you can import `sass-utilities/bs-variables.scss` manually.
- `--pf-global--link--FontWeight`
- `--pf-global--FontFamily--redhatfont--sans-serif`
- `--pf-global--FontFamily--redhatfont--heading--sans-serif`
- `--pf-global--FontFamily--redhatfont--monospace`
- `--pf-global--FontWeight--redhatfont--bold`
- `--pf-global--golden-ratio`

## Components

**CSS variables:**
Our components have seen many CSS variables changed, removed, and renamed. Removed and renamed variables may require changes to your CSS and HTML.

**Visual changes:**
We applied some design updates to many of our components, which introduce visual breaking changes. Some are simple updates to existing styles (such as spacing or color), while others are an overhaul of the design (such as Tabs and Label).

**Renamed and removed components:**
- Data toolbar (`.pf-c-data-toolbar`) has been renamed to toolbar (`.pf-c-toolbar`).
- The toolbar layout (`.pf-l-toolbar`) has been removed.
- Expandable (`.pf-c-expandable`) has been renamed to expandable section (`.pf-c-expandable-section`).

### About modal box
- Removed the class `.pf-m-hover`, which modified the close button. [(#2975)](https://github.com/patternfly/patternfly/pull/2975).

Removed classes:
- `.pf-m-hover` from `.pf-c-about-modal-box__close > .pf-c-button`
  - The `:hover` selector still has styles applied to it.

### Accordion
- Updated spacing, removed box shadow and no-shadow variation [(#2760)](https://github.com/patternfly/patternfly/pull/2760)
- Removed the hover, active, and focus variations [(#2975)](https://github.com/patternfly/patternfly/pull/2975)
- Cleaned up vars [(#3016)](https://github.com/patternfly/patternfly/pull/3016)
- Added toggle icon wrapper [(#2927)](https://github.com/patternfly/patternfly/pull/2927)

Updated markup:
- The toggle icon should be wrapped in a `<span>` element, and the class `.pf-c-accordion__toggle-icon` should be moved from the icon to the `<span>`.

Updated CSS:
- `--pf-c-accordion__toggle--PaddingRight` from `--pf-global--spacer--xl` to `--pf-global--spacer--md`
- `--pf-c-accordion__toggle--PaddingLeft` from `--pf-global--spacer--xl` to `--pf-global--spacer--md`
- `--pf-c-accordion__expanded-content-body--PaddingRight` from `--pf-global--spacer--xl` to `--pf-global--spacer--md`
- `--pf-c-accordion__expanded-content-body--PaddingLeft` from `--pf-global--spacer--xl` to `--pf-global--spacer--md`
- Removed the default box shadow and outer top/bottom padding.

Removed classes:
- `.pf-m-no-box-shadow` from `.pf-c-accordion`
- `.pf-m-hover` from `.pf-c-accordion__toggle`. The toggle still has `:hover` styles.
- `.pf-m-active` from `.pf-c-accordion__toggle`. The toggle still has `:active` styles.
- `.pf-m-focus` from `.pf-c-accordion__toggle`. The toggle still has `:focus` styles.

Renamed variables:
- `--pf-c-accordion__toggle--BorderLeftColor` to `--pf-c-accordion__toggle--before--BackgroundColor`
- `--pf-c-accordion__toggle--m-expanded--BorderWidth` to `--pf-c-accordion__toggle--before--Width`
- `--pf-c-accordion__toggle--m-expanded--BorderLeftColor` to `--pf-c-accordion__toggle--m-expanded--before--BackgroundColor`
- `--pf-c-accordion__toggle-text--hover--Color` to `--pf-c-accordion__toggle--hover__toggle-text--Color`
- `--pf-c-accordion__toggle-text--active--Color` to `--pf-c-accordion__toggle--active__toggle-text--Color`
- `--pf-c-accordion__toggle-text--active--FontWeight` to `--pf-c-accordion__toggle--active__toggle-text--FontWeight`
- `--pf-c-accordion__toggle-text--focus--Color` to `--pf-c-accordion__toggle--focus__toggle-text--Color`
- `--pf-c-accordion__toggle-text--focus--FontWeight` to `--pf-c-accordion__toggle--focus__toggle-text--FontWeight`
- `--pf-c-accordion__toggle-text--expanded--Color` to `--pf-c-accordion__toggle--m-expanded__toggle-text--Color`
- `--pf-c-accordion__toggle-text--expanded--FontWeight` to `--pf-c-accordion__toggle--m-expanded__toggle-text--FontWeight`
- `--pf-c-accordion__expanded-content--BorderLeftColor` to `--pf-c-accordion__expanded-content-body--before--BackgroundColor`
- `--pf-c-accordion__expanded-content--m-expanded--BorderWidth` to `--pf-c-accordion__expanded-content-body--before--Width`
- `--pf-c-accordion__expanded-content--m-expanded--BorderLeftColor` to `--pf-c-accordion__expanded-content--m-expanded__expanded-content-body--before--BackgroundColor`
- `--pf-c-accordion__toggle--m-expanded__toggle-icon--Transform` to `--pf-c-accordion__toggle--m-expanded__toggle-icon--Rotate`

Removed variables:
- `--pf-c-accordion--BorderWidth`
- `--pf-c-accordion--BoxShadow`
- `--pf-c-accordion--PaddingTop`
- `--pf-c-accordion--PaddingBottom`
- `--pf-c-accordion__toggle-text--hover--FontWeight`
- `--pf-c-accordion__toggle-icon--LineHeight`

### Alert
- Update styling for better accessibility [(#2921)](https://github.com/patternfly/patternfly/pull/2921)

Updated markup:
- The only action that should go in `.pf-c-alert__action` is the close button. All other actions should be in a new element with the class `.pf-c-alert__action-group` appended to `.pf-c-alert`.

Renamed variables:
- `--pf-c-alert--grid-template-columns` to `--pf-c-alert--GridTemplateColumns`
- `--pf-c-alert__action--Transform` to `--pf-c-alert__action--TranslateY`

Removed variables:
- `--pf-c-alert--grid-template-rows`
- `--pf-c-alert__icon--Padding`
- `--pf-c-alert__icon--BackgroundColor`
- `--pf-c-alert__title--FontSize`
- `--pf-c-alert__title--PaddingTop`
- `--pf-c-alert__title--PaddingRight`
- `--pf-c-alert__title--PaddingBottom`
- `--pf-c-alert__title--PaddingLeft`
- `--pf-c-alert__description--PaddingRight`
- `--pf-c-alert__description--PaddingBottom`
- `--pf-c-alert__description--PaddingLeft`
- `--pf-c-alert__description--MarginTop`
- `--pf-c-alert__action--PaddingTop`
- `--pf-c-alert__action--PaddingRight`
- `--pf-c-alert--m-success__icon--BackgroundColor`
- `--pf-c-alert--m-danger__icon--BackgroundColor`
- `--pf-c-alert--m-warning__icon--BackgroundColor`
- `--pf-c-alert--m-warning__icon--FontSize`
- `--pf-c-alert--m-info__icon--BackgroundColor`
- `--pf-c-alert--m-inline--BorderColor`
- `--pf-c-alert--m-inline--BorderStyle`
- `--pf-c-alert--m-inline--BorderTopWidth`
- `--pf-c-alert--m-inline--BorderRightWidth`
- `--pf-c-alert--m-inline--BorderBottomWidth`
- `--pf-c-alert--m-inline--BorderLeftWidth`
- `--pf-c-alert--m-inline--before--Width`
- `--pf-c-alert--m-inline--before--Top`
- `--pf-c-alert--m-inline--before--Bottom`
- `--pf-c-alert--m-inline--before--BackgroundColor`
- `--pf-c-alert--m-inline__icon--FontSize`
- `--pf-c-alert--m-inline__icon--Color`
- `--pf-c-alert--m-inline__icon--BackgroundColor`
- `--pf-c-alert--m-inline__icon--PaddingTop`
- `--pf-c-alert--m-inline__icon--PaddingRight`
- `--pf-c-alert--m-inline__icon--PaddingBottom`
- `--pf-c-alert--m-inline__icon--PaddingLeft`
- `--pf-c-alert--m-inline--m-warning__icon--FontSize`
- `--pf-c-alert--m-inline--m-success__icon--Color`
- `--pf-c-alert--m-inline--m-success--before--BackgroundColor`
- `--pf-c-alert--m-inline--m-danger__icon--Color`
- `--pf-c-alert--m-inline--m-danger--before--BackgroundColor`
- `--pf-c-alert--m-inline--m-warning__icon--Color`
- `--pf-c-alert--m-inline--m-warning--before--BackgroundColor`
- `--pf-c-alert--m-inline--m-info__icon--Color`
- `--pf-c-alert--m-inline--m-info--before--BackgroundColor`

### Application launcher
- Applied external link icon class to icon wrapper [(#2904)](https://github.com/patternfly/patternfly/pull/2904)
- Removed separator in favor of divider component [(#2944)](https://github.com/patternfly/patternfly/pull/2944)
- Removed hover, active, focus, and disabled modifiers [(#2975)](https://github.com/patternfly/patternfly/pull/2975)
- Cleaned up variables [(#3012)](https://github.com/patternfly/patternfly/pull/3012)

Updated markup:
- Moves the class `.pf-c-app-launcher__menu-item-external-icon` from the external link icon itself to a `<span>` that wraps the external link icon. Any instances of the external link icon should be wrapped in a `<span>`, and the `.pf-c-app-launcher__menu-item-external-icon` class should be moved from the icon to the `<span>`.

Removed classes:
- `.pf-c-app-launcher__separator` - element was removed. Use the divider component instead.
- `.pf-m-hover` from `.pf-c-app-launcher__toggle` and `.pf-c-app-launcher__menu`
- `.pf-m-focus` from `.pf-c-app-launcher__toggle` and `.pf-c-app-launcher__menu`
- `.pf-m-disabled` from `.pf-c-app-launcher__toggle`
  - The `:hover`, `:active`, `:focus`, and `:disabled` selectors still have styles applied to them

Renamed variables:
- `--pf-c-app-launcher--m-top__menu--Transform` to `--pf-c-app-launcher--m-top__menu--TranslateY`
- `--pf-c-app-launcher__menu-item-external-icon--Transform` to `--pf-c-app-launcher__menu-item-external-icon--TranslateY`

Removed variables:
- `--pf-c-app-launcher__group--PaddingTop`
- `--pf-c-app-launcher__group--first-child--PaddingTop`
- `--pf-c-app-launcher__menu--BorderWidth`
- `--pf-c-app-launcher__separator--Height`
- `--pf-c-app-launcher__separator--BackgroundColor`
- `--pf-c-app-launcher__separator--MarginTop`
- `--pf-c-app-launcher__separator--MarginBottom`
- `--pf-c-app-launcher__separator--Height`

### Backdrop
- Removed blur [(#3009)](https://github.com/patternfly/patternfly/pull/3009)

Renamed variables:
- `--pf-c-backdrop--Color` to `--pf-c-backdrop--BackgroundColor`

Removed variables:
- `--pf-c-backdrop--BackdropFilter`

### Breadcrumb
- Enabled long strings to break to multiple lines, made item link and divider icon display inline, moved divider icon to come before the item link text [(#2916)](https://github.com/patternfly/patternfly/pull/2916)
- Removed hover, active, focus, and disabled modifiers [(#2975)](https://github.com/patternfly/patternfly/pull/2975)


Updated markup:
- In the markup, move any reference to the divider element `.pf-c-breadcrumb__item-divider` so that is comes before the breadcrumb link element `.pf-c-breadcrumb__link` instead of after it.

Updated CSS:
- The text will now break if there is a long string in an item link that is wider than the viewport.
- The divider will always appear inline with the item text instead of wrapping to a new line in some cases.

Removed classes:
- `.pf-m-hover` from `.pf-c-breadcrumb__link`
  - The `:hover` selector still has styles applied to it.

Renamed variables:
- `--pf-c-breadcrumb__item-divider--MarginLeft` to `--pf-c-breadcrumb__item-divider--MarginRight`

Removed variables:
- `--pf-c-breadcrumb__item--FontWeight`
- `--pf-c-breadcrumb__link--FontWeight`

### Button
- Removed hover, active, focus, and disabled modifiers [(#2975)](https://github.com/patternfly/patternfly/pull/2975)
- Cleaned up vars [(#3028)](https://github.com/patternfly/patternfly/pull/3028)
- Updated control variation disabled state [(#3049)](https://github.com/patternfly/patternfly/pull/3049)
- Added modifiers to position icon in button [(#2828)](https://github.com/patternfly/patternfly/pull/2828)
- Made shield styles optional by default [(#2872)](https://github.com/patternfly/patternfly/pull/2872)

Updated markup:
- Added the modifiers `.pf-m-start` and `.pf-m-end` to position `pf-c-button__icon` in the button component depending on whether it comes before or after text. If you are using a button with an icon, use one of these modifiers to style the icon.

Updated CSS:
- Link and plain buttons now define `background-color: transparent` for their normal, hover, focus, and active states.
- The control button now sets a white background and default text color for its normal, hover, focus, and active states.


Removed classes:
- Removed `.pf-c-button__text` (entire element was removed)
- Removed `.pf-m-hover` from `.pf-c-button`
- Removed `.pf-m-focus` from `.pf-c-button`
  - The `:hover` and `:focus` selectors still have styles applied to them.

Renamed variables:
- `--pf-c-button--BorderColor` to `--pf-c-button--after--BorderColor`
- `--pf-c-button--BorderWidth` to `--pf-c-button--after--BorderWidth`
- `--pf-c-button--hover--BorderWidth` to `--pf-c-button--hover--after--BorderWidth`
- `--pf-c-button--focus--BorderWidth` to `--pf-c-button--focus--after--BorderWidth`
- `--pf-c-button--active--BorderWidth` to `--pf-c-button--active--after--BorderWidth`
- `--pf-c-button--disabled--BorderColor` to `--pf-c-button--disabled--after--BorderColor`
- `--pf-c-button--m-secondary--BorderColor` to `--pf-c-button--m-secondary--after--BorderColor`
- `--pf-c-button--m-secondary--hover--BorderColor` to `--pf-c-button--m-secondary--hover--after--BorderColor`
- `--pf-c-button--m-secondary--focus--BorderColor` to `--pf-c-button--m-secondary--focus--after--BorderColor`
- `--pf-c-button--m-secondary--active--BorderColor` to `--pf-c-button--m-secondary--active--after--BorderColor`
- `--pf-c-button--m-tertiary--BorderColor` to `--pf-c-button--m-tertiary--after--BorderColor`
- `--pf-c-button--m-tertiary--hover--BorderColor` to `--pf-c-button--m-tertiary--hover--after--BorderColor`
- `--pf-c-button--m-tertiary--focus--BorderColor` to `--pf-c-button--m-tertiary--focus--after--BorderColor`
- `--pf-c-button--m-tertiary--active--BorderColor` to `--pf-c-button--m-tertiary--active--after--BorderColor`

Removed variables:
- `--pf-c-button__icon--MarginRight`
- `--pf-c-button__text--icon--MarginLeft`
- `--pf-c-button--m-control--disabled--after--BorderTopColor`
- `--pf-c-button--m-control--disabled--after--BorderRightColor`
- `--pf-c-button--m-control--disabled--after--BorderBottomColor`
- `--pf-c-button--m-control--disabled--after--BorderLeftColor`

### Card
- Renamed vars, updated docs [(#2759)](https://github.com/patternfly/patternfly/pull/2759)
- Moved `__header` to `__title`, `__head` to `__header` [(#3035)](https://github.com/patternfly/patternfly/pull/3035)

Updated markup:
- `.pf-c-card__header` has been renamed to `.pf-c-card__title`.
- `.pf-c-title` and the title size modifiers should be removed from `.pf-c-card__title`.
- `.pf-c-card__head` has been renamed to `.pf-c-card__header`.
- `.pf-c-card__head-main` has been renamed to `.pf-c-card__header-main`.

Renamed variables:
- `--pf-c-card--m-compact__header--not-last-child--PaddingBottom` to `--pf-c-card--m-compact__header--not--last-child--PaddingBottom`
- `--pf-c-card__header--not-last-child--PaddingBottom` to `--pf-c-card__header--not--last-child--PaddingBottom`
- `--pf-c-card__header--not-last-child--PaddingBottom` to `--pf-c-card__header--not--last-child--PaddingBottom`
- `--pf-c-card--m-compact__header--not--last-child--PaddingBottom` to `--pf-c-card--m-compact__title--not--last-child--PaddingBottom`
- `--pf-c-card__header--FontFamily` to `--pf-c-card__title--FontFamily`
- `--pf-c-card__header--FontWeight` to `--pf-c-card__title--FontWeight`
- `--pf-c-card__header--not--last-child--PaddingBottom` to `--pf-c-card__title--not--last-child--PaddingBottom`
- `--pf-c-card__header--not--last-child--PaddingBottom` to `--pf-c-card__title--not--last-child--PaddingBottom`

### Chip
- Refactor styles [(#2941)](https://github.com/patternfly/patternfly/pull/2941).
- Removed hover, active, focus, and disabled modifiers [(#2975)](https://github.com/patternfly/patternfly/pull/2975).
- Adjusted chip top/bottom padding and the chip group margin in select [(#3079)](https://github.com/patternfly/patternfly/pull/3079).

Updated markup:
- The "x" icon in the close button changed from `fa-times-circle` to `fa-times`.

Updated CSS:
- `--pf-c-chip--BorderColor` changed from `--pf-global--secondary-color--100` to `--pf-global--BorderColor--300`

Removed classes:
- `.pf-m-read-only`
  - Just omit the close button instead.
- `.pf-m-hover` from `.pf-c-chip.pf-m-overflow`
- `.pf-m-active` from `.pf-c-chip.pf-m-overflow`
- `.pf-m-focus` from `.pf-c-chip.pf-m-overflow`
  - The `:hover`, `:active` and `:focus` selectors still have styles applied to them.

Renamed variables:
- `--pf-c-chip--BorderColor` to `--pf-c-chip--before--BorderColor`
- `--pf-c-chip--BorderWidth` to `--pf-c-chip--before--BorderWidth`
- `--pf-c-chip--BorderRadius` to `--pf-c-chip--before--BorderRadius`
- `--pf-c-chip--m-overflow--c-button--hover--BorderColor` to `--pf-c-chip--m-overflow--hover--before--BorderColor`
- `--pf-c-chip--m-overflow--c-button--active--BorderColor` to `--pf-c-chip--m-overflow--active--before--BorderColor`
- `--pf-c-chip--m-overflow--c-button--focus--BorderColor` to `--pf-c-chip--m-overflow--focus--before--BorderColor`
- `--pf-c-chip--c-badge--MarginLeft` to `--pf-c-chip__c-badge--MarginLeft`

Removed variables:
- `--pf-c-chip--m-read-only--PaddingTop`
- `--pf-c-chip--m-read-only--PaddingRight`
- `--pf-c-chip--m-read-only--PaddingBottom`
- `--pf-c-chip--m-overflow--BackgroundColor`
- `--pf-c-chip--m-overflow--PaddingLeft`
- `--pf-c-chip--m-overflow--BorderWidth`
- `--pf-c-chip--m-overflow--c-button--BorderRadius`
- `--pf-c-chip--m-overflow--c-button--BorderWidth`
- `--pf-c-chip--m-overflow--c-button--PaddingLeft`
- `--pf-c-chip--m-overflow--c-button--PaddingRight`
- `--pf-c-chip--m-overflow--c-button--hover--BorderWidth`
- `--pf-c-chip--m-overflow--c-button--hover--BorderColor`
- `--pf-c-chip--m-overflow--c-button--active--BorderWidth`
- `--pf-c-chip--m-overflow--c-button--active--BorderColor`
- `--pf-c-chip--m-overflow--c-button--focus--BorderWidth`
- `--pf-c-chip--m-overflow--c-button--focus--BorderColor`
- `--pf-c-select__toggle-wrapper--c-chip--c-button--PaddingTop`
- `--pf-c-select__toggle-wrapper--c-chip--c-button--PaddingBottom`

### Chip group
- Clarified chip group types, overflow, and categories [(#2961)](https://github.com/patternfly/patternfly/pull/2961)
- No longer nests [(#3058)](https://github.com/patternfly/patternfly/pull/3058)

Updated markup:
- The outer `.pf-c-chip-group` element should now always be a `<div>`. All instances of `.pf-c-chip-group` should be updated to be a `<div>`.
- `.pf-c-chip-group__label` is now a `<span>` instead of a heading element.
- `.pf-c-chip-group__label` should have an `id` value that will be used as the value for `aria-labelledby` on the `ul.pf-c-chip-group__list` element.
- Because screen readers read the text in `.pf-c-chip-group__label` via `aria-labelledby` on the `pf-c-chip-group__list` element, `.pf-c-chip-group__label` should now also have `aria-hidden="true"`.
- The list of chips inside of a chip group should be wrapped in a `<ul>` with the class `pf-c-chip-group__list`.
- `.pf-c-chip-group__list` should have a (redundant) `role="list"` assigned. This is needed for screen readers to announce the list properly.
- Chips inside of `.pf-c-chip-group__list` should each be wrapped with a `li.pf-c-chip-group__list-item` element.
- Chips are now a `<div>` by default now. When used in a chip-group, `div.pf-c-chip` is a child of `ul.pf-c-chip-group__list > li.pf-c-chip-group__list-item`

Plain chip group structure (no label):

```pug
div.chip-group
  ul.chip-group-list[aria-label="foo"][role="list"]
    li.chip-group-list-item
       div.chip
```

Chip group structure with label:

```pug
div.chip-group
  span.chip-group-label[aria-hidden="true"][id="foo"]
  ul.chip-group-list[aria-labelledby="foo"][role="list"]
    li.chip-0-list-item
       div.chip
    li.chip-group-list-item
       div.chip
```

Updated CSS:
- Changed max-width for `--pf-c-chip-group__label--MaxWidth` to 160px

Renamed classes:
- `.pf-m-toolbar` to `.pf-m-category`

Renamed variables:
- `--pf-c-chip-group--m-toolbar-PaddingTop` to `--pf-c-chip-group--m-category--PaddingTop`
- `--pf-c-chip-group--m-toolbar-PaddingRight` to `--pf-c-chip-group--m-category--PaddingRight`
- `--pf-c-chip-group--m-toolbar-PaddingBottom` to `--pf-c-chip-group--m-category--PaddingBottom`
- `--pf-c-chip-group--m-toolbar-PaddingLeft` to `--pf-c-chip-group--m-category--PaddingLeft`
- `--pf-c-chip-group--m-toolbar-BorderRadius` to `--pf-c-chip-group--m-category--BorderRadius`
- `--pf-c-chip-group--m-toolbar--BackgroundColor` to `--pf-c-chip-group--m-category--BackgroundColor`
- `--pf-c-chip-group--MarginBottom` to `--pf-c-chip-group__list--MarginBottom`
- `--pf-c-chip-group__label--PaddingRight` to `--pf-c-chip-group__label--MarginRight`
- `--pf-c-chip-group__label--Maxwidth` to `--pf-c-chip-group__label--Maxwidth`
- `--pf-c-chip-group--c-chip--MarginRight` to `--pf-c-chip-group__list-item--MarginRight`
- `--pf-c-chip-group--c-chip--MarginBottom` to `--pf-c-chip-group__list-item--MarginBottom`

Removed variables:
- `--pf-c-chip--m-overflow--hover--before--BorderColor`
- `--pf-c-chip--m-overflow--active--before--BorderColor`
- `--pf-c-chip--m-overflow--focus--before--BorderColor`
- `--pf-c-chip--m-overflow--hover--before--BorderColor`
- `--pf-c-chip--m-overflow--active--before--BorderColor`
- `--pf-c-chip--m-overflow--focus--before--BorderColor`
- `--pf-c-chip-group--MarginRight`
- `--pf-c-chip-group--MarginBottom`
- `--pf-c-chip-group__li--m-toolbar--MarginRight`
- `--pf-c-chip-group__label--PaddingTop`
- `--pf-c-chip-group__label--PaddingBottom`
- `--pf-c-chip-group__label--PaddingLeft`

### Clipboard copy
- Replaced expandable box-shadow with border [(#2856)](https://github.com/patternfly/patternfly/pull/2856)
- Replaced copy button with button component [(#2922)](https://github.com/patternfly/patternfly/pull/2922)

Updated markup:
- Removes the elements with classes `.pf-c-clipboard-copy__group-toggle` and `.pf-c-clipboard-copy__group-copy`. All instances of these elements should be removed from your application and replaced with the button control component `.pf-c-button.pf-m-control`.

Updated CSS:
- The expandable content's box-shadow has been replaced with a border, and the transparent border around the expandable content box has been removed.

Renamed variables:
- `--pf-c-clipboard-copy__group-toggle-icon--Transition` to `--pf-c-clipboard-copy__toggle-icon--Transition`
- `--pf-c-clipboard-copy--m-expanded__group-toggle-icon--Transform` to `--pf-c-clipboard-copy--m-expanded__toggle-icon--Transform`
- `--pf-c-clipboard-copy--m-expanded__toggle-icon--Transform` to `--pf-c-clipboard-copy--m-expanded__toggle-icon--Rotate`

Removed variables:
- `--pf-c-clipboard-copy__expandable-content--BorderWidth`
- `--pf-c-clipboard-copy__expandable-content--BoxShadow`
- `--pf-c-clipboard-copy__group-toggle--PaddingRight`
- `--pf-c-clipboard-copy__group-toggle--PaddingLeft`
- `--pf-c-clipboard-copy__group-toggle--BorderWidth`
- `--pf-c-clipboard-copy__group-toggle--BorderTopColor`
- `--pf-c-clipboard-copy__group-toggle--BorderRightColor`
- `--pf-c-clipboard-copy__group-toggle--BorderBottomColor`
- `--pf-c-clipboard-copy__group-toggle--BorderLeftColor`
- `--pf-c-clipboard-copy__group-toggle--hover--BorderBottomColor`
- `--pf-c-clipboard-copy__group-toggle--active--BorderBottomWidth`
- `--pf-c-clipboard-copy__group-toggle--active--BorderBottomColor`
- `--pf-c-clipboard-copy__group-toggle--focus--BorderBottomWidth`
- `--pf-c-clipboard-copy__group-toggle--focus--BorderBottomColor`
- `--pf-c-clipboard-copy__group-toggle--m-expanded--BorderBottomWidth`
- `--pf-c-clipboard-copy__group-toggle--m-expanded--BorderBottomColor`
- `--pf-c-clipboard-copy__group-toggle--OutlineOffset`
- `--pf-c-clipboard-copy__group-copy--PaddingRight`
- `--pf-c-clipboard-copy__group-copy--PaddingLeft`
- `--pf-c-clipboard-copy__group-copy--BorderWidth`
- `--pf-c-clipboard-copy__group-copy--BorderTopColor`
- `--pf-c-clipboard-copy__group-copy--BorderRightColor`
- `--pf-c-clipboard-copy__group-copy--BorderBottomColor`
- `--pf-c-clipboard-copy__group-copy--BorderLeftColor`
- `--pf-c-clipboard-copy__group-copy--hover--BorderBottomColor`
- `--pf-c-clipboard-copy__group-copy--active--BorderBottomWidth`
- `--pf-c-clipboard-copy__group-copy--active--BorderBottomColor`
- `--pf-c-clipboard-copy__group-copy--focus--BorderBottomWidth`
- `--pf-c-clipboard-copy__group-copy--focus--BorderBottomColor`

### Content
- Updated margin to NOT be on first-child or last-child [(#2930)](https://github.com/patternfly/patternfly/pull/2930)

Updated CSS:
- The margin-bottom has been removed for `<h1> - <h6>` if the element is `:last-child`.
- The margin-top has been removed from  `<ol>` and `<ul>`.

Removed variables:
- `--pf-c-content--blockquote--FontWeight`
- `--pf-c-content--small--FontWeight`
- `--pf-c-content--ol--MarginTop`
- `--pf-c-content--ul--MarginTop`

### Context selector
- Removed hover, active, focus, and disabled modifiers [(#2975)](https://github.com/patternfly/patternfly/pull/2975)

Updated markup:
- Added `span.pf-c-context-selector__toggle-icon` wrapper to toggle icon [(#2927)](https://github.com/patternfly/patternfly/pull/2927)

Removed classes:
- `.pf-m-hover` from `.pf-c-context-selector__toggle` and `.pf-c-context-selector__menu-list-item`
- `.pf-m-focus` from `.pf-c-context-selector__menu-list-item`
- `.pf-m-disabled` from `.pf-c-context-selector__menu-list-item`
  - The `:hover`, `:active`, `:focus`, and `:disabled` selectors still have styles applied to them.

Renamed variables:
- `--pf-c-context-selector__menu-input` to `--pf-c-context-selector__menu-search`
- `--pf-c-select__menu-input` to `--pf-c-select__menu-search`
- `--pf-c-context-selector__menu-input--PaddingTop` to `--pf-c-context-selector__menu-search--PaddingTop`
- `--pf-c-context-selector__menu-input--PaddingRight` to `--pf-c-context-selector__menu-search--PaddingRight`
- `--pf-c-context-selector__menu-input--PaddingBottom` to `--pf-c-context-selector__menu-search--PaddingBottom`
- `--pf-c-context-selector__menu-input--PaddingLeft` to `--pf-c-context-selector__menu-search--PaddingLeft`
- `--pf-c-context-selector__menu-input--BottomBorderColor` to `--pf-c-context-selector__menu-search--BorderBottomColor`
- `--pf-c-context-selector__menu-input--BottomBorderWidth` to `--pf-c-context-selector__menu-search--BorderBottomWidth`

Removed variables:
- `--pf-c-context-selector__menu--BorderWidth`

### Data list
- Update compact font-size [(#2777)](https://github.com/patternfly/patternfly/pull/2777)
- Changed expanded content shadow to a border [(#2885)](https://github.com/patternfly/patternfly/pull/2885)
- Removed flex wrap for action button [(#2923)](https://github.com/patternfly/patternfly/pull/2923)
- Fix shadows on selected rows [(#3068)](https://github.com/patternfly/patternfly/pull/3068)

Updated markup:
- The icon in the button in `.pf-c-data-list__toggle` should be wrapped with a new `<div>` with class `.pf-c-data-list__toggle-icon`.
- When the list of buttons get too long you should use the overflow menu to hide buttons. Alternatively, you can use `.pf-m-hidden{-on-[breakpoint]}` and `.pf-m-hidden{-on-[breakpoint]}` on `.pf-c-data-list__item-action`.

Updated CSS:
- Changes compact data-list font-size to `--pf-global--FontSize--sm`.

Removed variables:
- `--pf-c-data-list__expandable-content--BoxShadow`
- `--pf-c-data-list__item--before--Height`
- `--pf-c-data-list__item-item--before--Height`
- `--pf-c-data-list__item-action--not-last-child--MarginBottom`
- `--pf-c-data-list--BackgroundColor`
- `--pf-c-data-list--BorderBottomColor`
- `--pf-c-data-list--BorderBottomWidth`
- `--pf-c-data-list__expandable-content--BorderRightWidth`
- `--pf-c-data-list__expandable-content--BorderBottomWidth`
- `--pf-c-data-list__expandable-content--BorderLeftWidth`
- `--pf-c-data-list__expandable-content--BorderRightColor`
- `--pf-c-data-list__expandable-content--BorderBottomColor`
- `--pf-c-data-list__expandable-content--BorderLeftColor`

Renamed variables:
- `--pf-c-data-list__item-content--PaddingBottom` to `--pf-c-data-list__item-content--md--PaddingBottom`
- `--pf-c-data-list__item-action--not-last-child--lg--MarginBottom` to `--pf-c-data-list__item-action--not-last-child--MarginBottom`
- `--pf-c-data-list__expandable-content--md--MaxHeight` to `--pf-c-data-list__expandable-content--MaxHeight`
- `--pf-c-data-list__item-content--PaddingBottom` to `--pf-c-data-list__item-content--md--PaddingBottom`
- `--pf-c-data-list__item--m-expanded__expandable-content--BorderTopWidth` to `--pf-c-data-list__expandable-content--BorderTopWidth`
- `--pf-c-data-list__item-item--BorderTopColor` to `--pf-c-data-list__item--item--BorderBottomColor`
- `--pf-c-data-list__item-item--BorderTopWidth` to `--pf-c-data-list__item--item--BorderBottomWidth`
- `--pf-c-data-list__item--hover--item--BorderTopColor` to `--pf-c-data-list__item--m-selectable--hover__item--BorderTopColor`
- `--pf-c-data-list__item--hover--item--BorderTopWidth` to `--pf-c-data-list__item--m-selectable--hover__item--BorderTopWidth`
- `--pf-c-data-list__item-item--sm--BorderTopWidth` to `--pf-c-data-list__item--item--sm--BorderBottomWidth`
- `--pf-c-data-list__item-item--sm--BorderTopColor` to `--pf-c-data-list__item--item--sm--BorderBottomColor`
- `--pf-c-data-list__item-item--before--Top` to `--pf-c-data-list__item--item--before--Top`
- `--pf-c-data-list__cell-cell--PaddingTop` to `--pf-c-data-list__cell--cell--PaddingTop`
- `--pf-c-data-list__cell-cell--md--PaddingTop` to `--pf-c-data-list__cell--cell--md--PaddingTop`
- `--pf-c-data-list__cell-cell--MarginRight` to `--pf-c-data-list__cell--MarginRight`
- `--pf-c-data-list__item--m-expanded__toggle--c-button-icon--Transform` to `--pf-c-data-list__item--m-expanded__toggle-icon--Transform`
- `--pf-c-data-list--m-compact__cell-cell--MarginRight` to `--pf-c-data-list--m-compact__cell--cell--MarginRight`
- `--pf-c-data-list__item--m-expanded__toggle-icon--Transform` to `--pf-c-data-list__item--m-expanded__toggle-icon--Rotate`
- `--pf-c-data-list__item--item--BorderBottomColor` to `--pf-c-data-list__item--BorderBottomColor`
- `--pf-c-data-list__item--item--BorderBottomWidth` to `--pf-c-data-list__item--BorderBottomWidth`
- `--pf-c-data-list__item--m-selectable--hover__item--BorderTopColor` to - `--pf-c-data-list__item--m-selectable--hover--item--BorderTopColor`
- `--pf-c-data-list__item--m-selectable--hover__item--BorderTopWidth` to - `--pf-c-data-list__item--m-selectable--hover--item--BorderTopWidth`
- `--pf-c-data-list__item--item--sm--BorderBottomWidth` to - `--pf-c-data-list__item--sm--BorderBottomWidth`
- `--pf-c-data-list__item--item--sm--BorderBottomColor` to - `--pf-c-data-list__item--sm--BorderBottomColor`
- `--pf-c-data-list__item--item--before--Top` to `--pf-c-data-list__item--before--sm--Top`

### Drawer
- Removed `.pf-m-border`, enabled `.pf-m-no-border` on inline/static [(#2887)](https://github.com/patternfly/patternfly/pull/2887)

Updated CSS:
- When the panel overlays the content, the panel displays a shadow to indicate it is placed over the content. And when the panel is beside the content, a border is used to separate the panel from the content. To disable either the border or shadow, use `.pf-m-no-border` on `.pf-c-drawer__panel`.

Removed classes:
- `.pf-m-border` from `.pf-c-drawer__panel`

### Dropdown
- Removed separator in favor of divider component [(#2944)](https://github.com/patternfly/patternfly/pull/2944)
- Removed hover, active, focus, and disabled modifiers [(#2975)](https://github.com/patternfly/patternfly/pull/2975)
- Cleaned up vars [(#3020)](https://github.com/patternfly/patternfly/pull/3020)

Removed classes:
- `.pf-c-dropdown__separator` - element was removed. Use the divider component instead.
- `.pf-m-hover` on `.pf-c-dropdown__toggle` and `.pf-c-dropdown__menu-item`
- `.pf-m-focus` on `.pf-c-dropdown__toggle` and `.pf-c-dropdown__menu-item`
  - The `:hover` and `:focus` selectors still have styles applied to them.

Renamed variables:
- `--pf-c-dropdown__c-divider--MarginTop` to `--pf-c-dropdown--c-divider--MarginTop`
- `--pf-c-dropdown__c-divider--MarginBottom` to `--pf-c-dropdown--c-divider--MarginBottom`
- `--pf-c-dropdown__toggle--BorderWidth` to `--pf-c-dropdown__toggle--before--BorderWidth`
- `--pf-c-dropdown__toggle--BorderTopColor` to `--pf-c-dropdown__toggle--before--BorderTopColor`
- `--pf-c-dropdown__toggle--BorderRightColor` to `--pf-c-dropdown__toggle--before--BorderRightColor`
- `--pf-c-dropdown__toggle--BorderBottomColor` to `--pf-c-dropdown__toggle--before--BorderBottomColor`
- `--pf-c-dropdown__toggle--BorderLeftColor` to `--pf-c-dropdown__toggle--before--BorderLeftColor`
- `--pf-c-dropdown__toggle--hover--BorderBottomColor` to `--pf-c-dropdown__toggle--hover--before--BorderBottomColor`
- `--pf-c-dropdown__toggle--active--BorderBottomWidth` to `--pf-c-dropdown__toggle--active--before--BorderBottomWidth`
- `--pf-c-dropdown__toggle--active--BorderBottomColor` to `--pf-c-dropdown__toggle--active--before--BorderBottomColor`
- `--pf-c-dropdown__toggle--focus--BorderBottomWidth` to `--pf-c-dropdown__toggle--focus--before--BorderBottomWidth`
- `--pf-c-dropdown__toggle--focus--BorderBottomColor` to `--pf-c-dropdown__toggle--focus--before--BorderBottomColor`
- `--pf-c-dropdown__toggle--expanded--BorderBottomWidth` to `--pf-c-dropdown--m-expanded__toggle--before--BorderBottomWidth`
- `--pf-c-dropdown__toggle--expanded--BorderBottomColor` to `--pf-c-dropdown--m-expanded__toggle--before--BorderBottomColor`
- `--pf-c-dropdown__toggle--m-split-button__toggle-check__input--Transform` to `--pf-c-dropdown__toggle--m-split-button__toggle-check__input--TranslateY`
- `--pf-c-dropdown--m-top--m-expanded__toggle-icon--Transform` to `--pf-c-dropdown--m-top--m-expanded__toggle-icon--Rotate`
- `--pf-c-dropdown--m-top__menu--Transform` to `--pf-c-dropdown--m-top__menu--TranslateY`

Removed variables:
- `--pf-c-dropdown__menu--BorderWidth`
- `--pf-c-dropdown__separator--Height`
- `--pf-c-dropdown__separator--BackgroundColor`
- `--pf-c-dropdown__separator--MarginTop`
- `--pf-c-dropdown__separator--MarginBottom`
- `--pf-c-dropdown__toggle--m-plain--BorderColor`
- `--pf-c-dropdown__group--PaddingTop`
- `--pf-c-dropdown__group--first-child--PaddingTop`

### Empty state
- Center content, fix vars, adjust secondary margin [(#2866)](https://github.com/patternfly/patternfly/pull/2866).

  - Adds a `.pf-m-full-height` variation to `.pf-c-empty-state` for use when the empty state component should be centered vertically in a container, and the empty state does not already consume the height of its container.
  - `--pf-c-empty-state__secondary--MarginRight` used to be used to create space between the list of secondary buttons. This variable has been removed, and the margin between secondary actions is now calculated from `--pf-c-empty-state__secondary--child--MarginLeft` and `--pf-c-empty-state__secondary--child--MarginRight` between adjacent buttons.
- Split single padding var to top/right/bottom/left [(#3092)](https://github.com/patternfly/patternfly/pull/3092).
  - `--pf-c-empty-state--Padding` has been replaced with `--pf-c-empty-state--PaddingTop`, `--pf-c-empty-state--PaddingRight`, `--pf-c-empty-state--PaddingBottom`, and `--pf-c-empty-state--PaddingLeft`.

Updated markup:
- All of the content inside of the empty state component should be wrapped in a new element `div.pf-c-empty-state__content`. This element is centered horizontally and vertically inside of `.pf-c-empty-state`.
- The bullseye layout is no longer required to center the empty state, since the empty state contents are now centered.

Updated CSS:
- The empty state component is now always full width. The `max-width` used for the size variations (`.pf-m-sm` and `.pf-m-lg`) applies to `.pf-c-empty-state__content` instead of `.pf-c-empty-state`.

Removed variables:
- `--pf-c-empty-state__secondary--MarginRight`
- `--pf-c-empty-state--Padding`

Renamed variables
- `--pf-c-empty-state__secondary__c-button--MarginRight` to `--pf-c-empty-state__secondary--child--MarginRight`
- `--pf-c-empty-state__secondary__c-button--MarginBottom` to `--pf-c-empty-state__secondary--child--MarginBottom`
- `--pf-c-empty-state--c-button--MarginTop` to `--pf-c-empty-state__primary--MarginTop`
- `--pf-c-empty-state--c-button__secondary--MarginTop` to `--pf-c-empty-state__primary--secondary--MarginTop`

### Expandable section (previously Expandable)
- Removed hover, active, focus, and disabled modifiers [(#2975)](https://github.com/patternfly/patternfly/pull/2975).
- Added wrapper with classname to all icons [(#2927)](https://github.com/patternfly/patternfly/pull/2927).

Updated markup:
- Added `span.pf-c-expandable-section__toggle-icon` wrapper to toggle icon.
- Toggle text is now wrapped in a `<span>` with class `.pf-c-expandable-section__toggle-text`.

Removed classes:
- `.pf-m-hover` from `.pf-c-expandable-section__toggle`
- `.pf-m-active` from `.pf-c-expandable-section__toggle`
- `.pf-m-focus` from `.pf-c-expandable-section__toggle`
  - The `:hover`, `:active` and `:focus` selectors still have styles applied to them.

Renamed variables:
- `--pf-c-expandable-section--m-expanded__toggle-icon--Transform` to `--pf-c-expandable-section--m-expanded__toggle-icon--Rotate`

Removed variables:
- `--pf-c-expandable-section__toggle--FontWeight`
- `--pf-c-expandable-section__toggle-icon--MarginRight`

### File upload
- Removed hover, active, focus, and disabled modifiers [(#2975)](https://github.com/patternfly/patternfly/pull/2975)
- Updated control variation disabled state [(#3049)](https://github.com/patternfly/patternfly/pull/3049)

Removed classes:
- `.pf-m-disabled` from `.pf-c-file-upload__file-select .pf-c-button.pf-m-control`
  - The `:disabled` selector still has styles applied to it.

Removed variables:
- `--pf-c-file-upload__file-select__c-button--m-control--disabled--BackgroundColor`
- `--pf-c-file-upload__file-select__c-button--m-control--disabled--after--BorderTopColor`
- `--pf-c-file-upload__file-select__c-button--m-control--disabled--after--BorderRightColor`
- `--pf-c-file-upload__file-select__c-button--m-control--disabled--after--BorderBottomColor`
- `--pf-c-file-upload__file-select__c-button--m-control--disabled--after--BorderLeftColor`
- `--pf-c-file-upload__file-select__c-button--m-control--disabled--after--BorderWidth`

### Form
- Refactored label/control sections, added label help [(#2766)](https://github.com/patternfly/patternfly/pull/2766)
- Removed hover, active, focus, and disabled modifiers [(#2975)](https://github.com/patternfly/patternfly/pull/2975)

Updated markup:
- There are 2 new elements used for the overall form layout. These elements are the main sections that make up a `.pf-c-form__group`.
  - `.pf-c-form__group-label` - defines the label section of the form. Anything that is above the form control element(s) in a stacked form, and to the left of form control element(s) in a horizontal form. Primarily this will hold the `.pf-c-form__label`. Also as a change introduced in this PR, it will hold the field level help element `.pf-c-form__label-help`. This should be the first child of `.pf-c-form__group`.
    - All `.pf-c-label` and any other elements that should be grouped with the label should now be wrapped in `.pf-c-form__group-label`
  - `.pf-c-form__group-control` - defines the control section of the form. This is where form control elements are placed. It will hold the form elements and form element helper text. This should be the last child of `.pf-c-form__group`.
    - All form control elements (`.pf-c-form-control`, `.pf-c-check`, `.pf-c-radio`, etc ) should be wrapped in `.pf-c-form__group-control`.
  - The following modifiers have moved from one element to another. Any reference to them should be moved from the old element to the new element
    - `.pf-m-no-padding-top` has moved from `.pf-c-form__label` to `.pf-c-form__group-label`
    - `.pf-m-inline` has been moved from `.pf-c-form__group` to `.pf-c-form__group-control`

Removed classes:
- `.pf-m-border`

Removed variables:
- `--pf-c-form__label--Color`
- `--pf-c-form__label--FontWeight`
- `--pf-c-form__group--MarginLeft`

Renamed variables:
- `--pf-c-form--m-inline--MarginRight` to `--pf-c-form__group-control--m-inline--child--MarginRight`
- `--pf-c-form--m-error--Color` to `--pf-c-form__helper-text--m-error--Color`
- `--pf-c-form--m-success--Color` to `--pf-c-form__helper-text--m-success--Color`
- `--pf-c-form--m-horizontal--md__group--GridTemplateColumns` has been renamed to `--pf-c-form--m-horizontal__group-label--md--GridColumnWidth` (to modify the label column width) and  `--pf-c-form--m-horizontal__group-control--md--GridColumnWidth` (to modify the form control column width)

### Form control
- Enabled success/invalid states for readonly controls [(#2753)](https://github.com/patternfly/patternfly/pull/2753).

Removed classes:
- `.pf-m-hover`
- `.pf-m-focus`
  - The `:hover` and `:focus` selectors still have styles applied to them.

Renamed variables:
- `--pf-c-form-control--readonly--focus--BackgroundColor` to `--pf-c-form-control--readonly--BackgroundColor`

Removed variables:
- `--pf-c-form-control--success--check--Background`
- `--pf-c-form-control--success--Background`
- `--pf-c-form-control__select--arrow--Background`
- `--pf-c-form-control__select--Background`
- `--pf-c-form-control__select--invalid--Background`
- `--pf-c-form-control__select--success--Background`

### Input group
- Removed legacy button styles [(#2935)](https://github.com/patternfly/patternfly/pull/2935).

Updated markup:
- We recommend using the `.pf-m-control` variation of the button component instead.

Removed variables:
- `--pf-c-input-group--BorderRadius`
- `--pf-c-input-group--c-button--BorderRadius`

### Label
- Refactored content and icons [(#2943)](https://github.com/patternfly/patternfly/pull/2943).

Updated markup:
- The text and optional icon inside of the label (except for the optional close button) should be wrapped in `span.pf-c-label__content` (or `a.pf-c-label__content` if the label is a link).
- You can add an optional `.pf-c-button.pf-m-plain` element as the last child of the label as an optional close button.

Removed classes:
- `.pf-m-compact` from `.pf-c-label`

### Login
- Moved helper text icon from login to form component [(#2928)](https://github.com/patternfly/patternfly/pull/2928).
- Changed components to be mobile first [(#2816)](https://github.com/patternfly/patternfly/pull/2816).

Renamed variables:
- `--pf-c-login__header--sm--PaddingLeft` to `--pf-c-login__header--PaddingLeft`
- `--pf-c-login__header--sm--PaddingRight` to `--pf-c-login__header--PaddingRight`
- `--pf-c-login__main--xl--MarginBottom` to `--pf-c-login__main--MarginBottom`
- `--pf-c-login__footer--sm--PaddingLeft` to `--pf-c-login__footer--PaddingLeft`
- `--pf-c-login__footer--sm--PaddingRight` to `--pf-c-login__footer--PaddingRight`
- `--pf-c-login__main-body--c-form__helper-text-icon--FontSize` to `--pf-c-form__helper-text-icon--FontSize`
- `--pf-c-login__main-body--c-form__helper-text-icon--MarginRight` to `--pf-c-form__helper-text-icon--MarginRight`

### Modal box
- Reduced spacing [(#2761)](https://github.com/patternfly/patternfly/pull/2761)
- Changes box shadow from lg to xl [(#2859)](https://github.com/patternfly/patternfly/pull/2859)
- Left-aligned footer actions by default [(#2881)](https://github.com/patternfly/patternfly/pull/2881)
- Added title, moved padding from container to elements [(#2969)](https://github.com/patternfly/patternfly/pull/2969)
- Added `__header` element [(#3084)](https://github.com/patternfly/patternfly/pull/3084)

Updated markup:
- Any instance of `.pf-c-title` in the modal that serves as the modal title should be replaced with `.pf-c-modal-box__title`.
- All instances of `.pf-c-modal-box__title` and `.pf-c-modal-box__description` should be wrapped in `.pf-c-modal-box__header`.

Updated CSS:
- The top/bottom/left/right padding changed from `--pf-global--spacer--xl` to `--pf-global--spacer--lg`.
- The space between the title and description changed from `--pf-global--spacer--sm` to `--pf-global--spacer--xs`.
- The space between the description and body changed from `--pf-global--spacer--lg` to `--pf-global--spacer--md`.
- The space between the title and body changed from `--pf-global--spacer--lg` to `--pf-global--spacer--md`.
- The space above the footer changed from `--pf-global--spacer--xl` to `--pf-global--spacer--lg`.
- The `.pf-c-form__helper-text-icon` styles have moved from the login component stylesheet to the form component stylesheet.
- Actions in the footer are now left aligned by default.
- `--pf-c-modal-box--PaddingTop` is now achieved with `--pf-c-modal-box__[modal element]--PaddingTop`. However it's important to note that this variable changes depending on the markup present in the modal, so this variable serves multiple purposes. And it will need to be modified for each child of the modal component that touches the top edge of the modal.
- `--pf-c-modal-box--PaddingRight` is now achieved with `--pf-c-modal-box__[modal element]--PaddingRight`. However it will need to be modified for each child of the modal component that touches the right edge of the modal.
- `--pf-c-modal-box--PaddingBottom` is now achieved with `--pf-c-modal-box__[modal element]--last-child--PaddingBottom`. However it will need to be modified for each child of the modal component that touches the bottom edge of the modal.
- `--pf-c-modal-box--PaddingLeft` is now achieved with `--pf-c-modal-box__[modal element]--PaddingLeft`. However it will need to be modified for each child of the modal component that touches the left edge of the modal.

Removed classes:
- `.pf-m-align-left` from `.pf-c-modal-box__footer`

Removed variables:
- `--pf-c-modal-box--BorderColor`
- `--pf-c-modal-box--BorderSize`
- `--pf-c-modal-box__footer__c-button--first-of-type--MarginLeft`
- `--pf-c-modal-box__description--PaddingRight`
- `--pf-c-modal-box__description--PaddingLeft`
- `--pf-c-modal-box__description--last-child--PaddingBottom`
- `--pf-c-modal-box__description--body--PaddingTop`

Renamed variables
- `--pf-c-modal-box--body--MinHeight` to `--pf-c-modal-box__body--MinHeight`
- `--pf-c-modal-box__c-title--description--MarginTop` to `--pf-c-modal-box__title--description--PaddingTop`
- `--pf-c-modal-box__description--body--MarginTop` to `--pf-c-modal-box__description--body--PaddingTop`
- `--pf-c-modal-box--c-title--body--MarginTop` to `--pf-c-modal-box__title--body--PaddingTop`
- `--pf-c-modal-box__footer--MarginTop` to `--pf-c-modal-box__footer--PaddingTop`
- `--pf-c-modal-box__title--PaddingTop` to `--pf-c-modal-box__header--PaddingTop`
- `--pf-c-modal-box__title--PaddingRight` to `--pf-c-modal-box__header--PaddingRight`
- `--pf-c-modal-box__title--PaddingLeft` to `--pf-c-modal-box__header--PaddingLeft`
- `--pf-c-modal-box__title--last-child--PaddingBottom` to `--pf-c-modal-box__header--last-child--PaddingBottom`
- `--pf-c-modal-box__title--body--PaddingTop` to `--pf-c-modal-box__header--body--PaddingTop`

### Nav
- Refactored vertical nav CSS structure [(#2884)](https://github.com/patternfly/patternfly/pull/2884)
- Updated nav scroll buttons to be inline [(#2942)](https://github.com/patternfly/patternfly/pull/2942)
- Removed hover, active, focus, and disabled modifiers [(#2975)](https://github.com/patternfly/patternfly/pull/2975)
- Updated nav to dark theme [(#2978)](https://github.com/patternfly/patternfly/pull/2978)
- Made `pf-c-nav__list` a flex parent [(#3006)](https://github.com/patternfly/patternfly/pull/3006)
- Removed `__simple-list`, added support for `-m-horizontal/-m-tertiary` at root [(#3062)](https://github.com/patternfly/patternfly/pull/3062)

Updated markup:
- The icon in `.pf-c-nav__toggle` should be wrapped in a new element with class `.pf-c-nav__toggle-icon`
- Instead of applying `.pf-m-start` and `.pf-m-end` to `.pf-c-nav` when there is overflow on a particular end of the nav, if there is any overflow on any end, apply the class `.pf-m-scrollable` to `.pf-c-nav`.
- For the horizontal masthead and horizontal tertiary nav variations, the `.pf-c-nav` component now needs the `.pf-m-horizontal` modifier class.
- `.pf-c-nav__simple-list`, `.pf-c-nav__horizontal-list`, and `.pf-c-nav__tertiary-list` should be updated to be `.pf-c-nav__list` instead.

Removed classes:
- `.pf-m-start` from `.pf-c-nav`
- `.pf-m-end` from `.pf-c-nav`
- `.pf-m-hover` from `.pf-c-nav__link`
- `.pf-m-active` from `.pf-c-nav__link`
- `.pf-m-focus` from `.pf-c-nav__link`
  - The `:hover`, `:active` and `:focus` selectors still have styles applied to them.
- `.pf-m-dark` from `.pf-c-nav`

Renamed variables:
- `--pf-c-nav__c-nested-component--Property` to `--pf-c-nav--c-nested-component--Property`
- `--pf-c-nav__toggle--Transition` to `--pf-c-nav__toggle-icon--Transition`
- `--pf-c-nav__item--m-expanded__toggle-icon--Transform` to `--pf-c-nav__item--m-expanded__toggle-icon--Rotate`
- All vars with `--m-dark` to `--m-light`
- All vars with `--pf-c-nav__horizontal-list__link` to `--pf-c-nav--m-horizontal__link`
- All vars with `--pf-c-nav__tertiary-list__link` to `--pf-c-nav--m-tertiary__link`
- All vars with `--pf-c-nav--subnav__simple-list__link` to `--pf-c-nav__subnav__link`

Removed variables:
- `--pf-c-nav__scroll-button--Display`
- `--pf-c-nav__scroll-button--Visibility`
- `--pf-c-nav__scroll-button--ZIndex`
- `--pf-c-nav__scroll-button--Height`
- `--pf-c-nav__scroll-button--lg--Height`
- `--pf-c-nav__scroll-button--PaddingRight`
- `--pf-c-nav__scroll-button--PaddingLeft`
- `--pf-c-nav__scroll-button--nth-of-type-1--BoxShadow`
- `--pf-c-nav__scroll-button--nth-of-type-2--BoxShadow`
- `--pf-c-nav__scroll-button--m-dark--nth-of-type-1--BoxShadow`
- `--pf-c-nav__scroll-button--m-dark--nth-of-type-2--BoxShadow`
- `--pf-c-page__header-nav--lg--MarginRight`
- `--pf-c-page__header-nav--lg--MarginLeft`
- `--pf-c-page__header-nav--c-nav__scroll-button--nth-of-type-1--Left`
- `--pf-c-page__header-nav--c-nav__scroll-button--nth-of-type-1--md--Left`
- `--pf-c-page__header-nav--c-nav__scroll-button--nth-of-type-1--lg--Left`
- `--pf-c-page__header-nav--c-nav__scroll-button--lg--BackgroundColor`
- `--pf-c-page__header-nav--c-nav__scroll-button--lg--Top`
- `--pf-c-page__main-nav--c-nav__scroll-button--nth-of-type-1--Left`
- `--pf-c-page__main-nav--c-nav__scroll-button--nth-of-type-1--md--Left`
- `--pf-c-page__main-nav--c-nav__scroll-button--nth-of-type-2--Right`
- `--pf-c-page__main-nav--c-nav__scroll-button--nth-of-type-2--md--Right`
- `--pf-c-nav__horizontal-list__link--FontWeight`
- `--pf-c-nav__tertiary-list__link--FontWeight`
- `--pf-c-nav__tertiary-list__scroll-button--before--BorderColor`
- `--pf-c-nav__tertiary-list__scroll-button--disabled--before--BorderColor`
- `--pf-c-nav__toggle-icon--Transform`
- `--pf-c-nav__scroll-button--lg--Height`
- `--pf-c-nav__scroll-button--Height`
- `--pf-c-nav__horizontal-list__link--lg--PaddingBottom`
- `--pf-c-nav__horizontal-list__link--lg--PaddingTop`
- `--pf-c-nav__item--m-expanded__toggle--Transform`

### Notification drawer
- Split out transform variables [(#3037)](https://github.com/patternfly/patternfly/pull/3037)

Renamed variables:
- `--pf-c-notification-drawer__group--m-expanded__group-toggle-icon--Transform` to `--pf-c-notification-drawer__group--m-expanded__group-toggle-icon--Rotate`

### Options menu
- Added color declaration to menu items [(#2938)](https://github.com/patternfly/patternfly/pull/2938)
- Removed separator in favor of divider component [(#2944)](https://github.com/patternfly/patternfly/pull/2944)
- Removed hover, active, focus, and disabled modifiers [(#2975)](https://github.com/patternfly/patternfly/pull/2975)
- Cleaned up vars [(#3018)](https://github.com/patternfly/patternfly/pull/3018)
- Added wrapper with classname to all icons[(#2927)](https://github.com/patternfly/patternfly/pull/2927)

Updated markup:
- The class `.pf-c-options-menu__toggle-icon` should be removed from the menu item icon and moved to a new element that wraps the icon - `div.pf-c-options-menu__toggle-icon`.
- The class `.pf-c-options-menu__menu-item-icon` should be removed from the menu item icon and moved to a new element that wraps the icon - `div.pf-c-options-menu__menu-item-icon`.
- The icon in `.pf-c-options-menu__toggle-button` should be wrapped in a new element `span.pf-c-options-menu__toggle-button-icon`.

Updated CSS:
- Added a `color` declaration for `.pf-c-options-menu__menu-item` items so that the color will always match the default text color (`--pf-global--Color--100)`. Since this wasn't defined previously, if applications were using `<a>` elements as menu items, the item color would have been the default blue link color.

Removed classes:
- `.pf-c-options-menu__separator` - element was removed. Use the divider component instead.
  - The element was removed. Use the divider component instead.
- `.pf-m-hover` from `.pf-c-options-menu__toggle`
- `.pf-m-focus` from `.pf-c-options-menu__toggle`
  - The `:hover`, `:active` and `:focus` selectors still have styles applied to them.

Renamed variables:
- `--pf-c-options-menu__c-divider--MarginTop` to `--pf-c-options-menu--c-divider--MarginTop`
- `--pf-c-options-menu__c-divider--MarginBottom` to `--pf-c-options-menu--c-divider--MarginBottom`
- `--pf-c-options-menu__toggle--Background` to `--pf-c-options-menu__toggle--BackgroundColor`
- `--pf-c-options-menu__toggle-button--Background` to `--pf-c-options-menu__toggle-button--BackgroundColor`
- `--pf-c-options-menu__menu-item--Background` to `--pf-c-options-menu__menu-item--BackgroundColor`
- `--pf-c-options-menu--m-top--m-expanded__toggle-icon--Transform` to `--pf-c-options-menu--m-top--m-expanded__toggle-icon--Rotate`
- `--pf-c-options-menu--m-top__menu--Transform` to `--pf-c-options-menu--m-top__menu--TranslateY`

Removed variables:
- `--pf-c-options-menu__menu--BorderWidth`
- `--pf-c-options-menu__separator--Height`
- `--pf-c-options-menu__separator--BackgroundColor`
- `--pf-c-options-menu__separator--MarginTop`
- `--pf-c-options-menu__separator--MarginBottom`

### Page
- Reduced spacing between tertiary nav, breadcrumbs, main section [(#2837)](https://github.com/patternfly/patternfly/pull/2837).
- Updated page and page header background colors [(#2883)](https://github.com/patternfly/patternfly/pull/2883)
- Cleaned up vars [(#3066)](https://github.com/patternfly/patternfly/pull/3066)
- Removed pf-m-mobile/icon/user, added hidden/visible [(#3091)](https://github.com/patternfly/patternfly/pull/3091)
- Moved masthead selected button mod to header-tools-item [(#3109)](https://github.com/patternfly/patternfly/pull/3109)

Updated markup:
- Adds `.pf-c-page__header-tools-item`, as a child of `.pf-c-page__header-tools-group` to wrap header tools items. All items should now be wrapped in `.pf-c-page__header-tools-item`.
  - `.pf-c-page__header-tools-group` is required as a parent of `.pf-c-page__header-tools-item`.
- Any instance of `.pf-m-selected` on a `.pf-c-button` in the masthead should be moved to its containing `.pf-c-page__header-tools-item` element.
- Removed the class `.pf-m-no-padding-mobile` from `.pf-c-page__main-section`.
  - We added new classes that add/remove the padding on any breakpoint. Instead of `.pf-m-no-padding-mobile`, use `.pf-m-no-padding{-on-[breakpoint]}` to remove padding from the main page section at an optional breakpoint, and `.pf-m-padding{-on-[breakpoint]}` add padding back in at a specified breakpoint.

Updated CSS:
- Changes `--pf-c-page--BackgroundColor` from `--pf-global--BackgroundColor--dark-100` to `--pf-global--BackgroundColor--light-300`.
- Adds a `background-color` of `--pf-global--BackgroundColor--dark-100` to `.pf-c-page__header`.
- `--pf-c-page__main-section--PaddingTop` changed from `--pf-global--spacer--md` (on mobile) and `--pf-global--spacer--lg` (on desktop) to `--pf-global--spacer--md` (defined as `--pf-c-page__main-breadcrumb--main-section--PaddingTop`) when a `.pf-c-page__main-section` follows `.pf-c-page__main-breadcrumb`.
- Adds `.pf-m-hidden[-on-{breakpoint}]` and `.pf-m-visible[-on-{breakpoint}]` that can be applied to `pf-c-page__header-tools-[item/group]`. These classes replace `.pf-m-[icon/mobile/user]` for hiding/showing header tools items and groups.

Removed classes:
- `.pf-m-icon` from `.pf-c-page__header-tools *`
- `.pf-m-mobile` from `.pf-c-page__header-tools *`
- `.pf-m-user` from `.pf-c-page__header-tools *`

Removed variables
- `--pf-c-page__header-tools--MarginTop: var(--pf-global--spacer--sm)`
- `--pf-c-page__header-tools--MarginBottom: var(--pf-global--spacer--sm)`
- `--pf-c-page__main-nav--PaddingBottom`
- `--pf-c-page__main-breadcrumb--md--PaddingTop`
- `--pf-c-page__main-nav--main-breadcrumb--PaddingTop`
- `--pf-c-page__header-sidebar-toggle__c-button--xl--MarginLeft`
- `--pf-c-page__main-section--m-no-padding-mobile--md--PaddingTop`
- `--pf-c-page__main-section--m-no-padding-mobile--md--PaddingRight`
- `--pf-c-page__main-section--m-no-padding-mobile--md--PaddingBottom`
- `--pf-c-page__main-section--m-no-padding-mobile--md--PaddingLeft`

Renamed variables:
- `--pf-c-page__sidebar--Transform` to two variables: `--pf-c-page__sidebar--TranslateX` and `--pf-c-page__sidebar--TranslateZ`
- `--pf-c-page__sidebar--m-expanded--Transform` to `--pf-c-page__sidebar--m-expanded--TranslateX`
- `--pf-c-page__sidebar--xl--Transform` to `--pf-c-page__sidebar--xl--TranslateX`

### Pagination
- Removed options menu per page text styling [(#3047)](https://github.com/patternfly/patternfly/pull/3047)
- Updated bottom pagination design [(#3050)](https://github.com/patternfly/patternfly/pull/3050)

Updated markup:
- The buttons used in `.pf-c-pagination__nav` are now individually wrapped with `div.pf-c-pagination__nav-control`, and this new element has modifiers `.pf-m-first`, `.pf-m-prev`, `.pf-m-next`, and `.pf-m-last` to indicate the first, previous, next, and last buttons. For example, the "first" button should now be wrapped in `<div class="pf-c-pagination__nav-control pf-m-first">// first button</div>`, the "next" wrapped in `<div class="pf-c-pagination__nav-control pf-m-next">// next button</div>`, and so on.
- Since the pagination now spans the width of it's parent container on mobile, it should not be used in a toolbar, and the bottom pagination should be placed directly after the element the pagination is for (data list, table, etc).

Updated CSS:
- On mobile viewports, the bottom pagination now spans the width of the parent container and is "sticky" to the bottom of the viewport and will remain at the bottom of the viewport as content above it scrolls. This behavior can be disabled by applying `.pf-m-static` to the `.pf-c-pagination` component.

Removed classes:
- `.pf-c-pagination__menu-text` - element was removed

Renamed classes:
- `.pf-m-footer` to `.pf-m-bottom`

Renamed variables:
- `--pf-c-pagination__nav--c-button--PaddingLeft` to `--pf-c-pagination__nav-control--c-button--PaddingLeft`
- `--pf-c-pagination__nav--c-button--PaddingRight` to `--pf-c-pagination__nav-control--c-button--PaddingRight`
- `--pf-c-pagination__nav--c-button--FontSize` to `--pf-c-pagination__nav-control--c-button--FontSize`
- `--pf-c-pagination--m-compact__nav--c-button--MarginLeft` to `--pf-c-pagination--m-compact__nav-control--nav-control--MarginLeft`
- All variables with `--m-footer` in the name should change to `--m-bottom`

Removed variables:
- `--pf-c-pagination__menu-text--PaddingLeft`
- `--pf-c-pagination__menu-text--FontSize`
- `--pf-c-pagination__menu-text--Color`

### Popover
- Reduced spacing [(#2762)](https://github.com/patternfly/patternfly/pull/2762)

Updated markup:
- The title component size changes from `.pf-m-xl` to `.pf-m-md`. You will need to change the title component variation used in the popover component from `.pf-m-xl` to `.pf-m-md`

Updated CSS:
- Top/right/bottom/left padding changed from `--pf-global--spacer--xl` to `--pf-global--spacer--md`
- Increased right padding of element displayed to the left of the close button to make more room for the close button. That padding is defined as `--pf-c-popover--c-button--sibling--PaddingRight` and it changed from `--pf-global--spacer--xl` to `--pf-global--spacer--2xl`
- Popover now has a `font-size` of `--pf-global--FontSize--sm`
- Space below title defined as `--pf-c-popover--c-title--MarginBottom` changed from `--pf-global--spacer--md` to `--pf-global--spacer--sm`
- Space above footer defined as `--pf-c-popover__footer--MarginTop` changed from `--pf-global--spacer--lg` to `--pf-global--spacer--md`

Renamed variables:
- `--pf-c-popover__arrow--m-top--Transform` split into `--pf-c-popover__arrow--m-top--TranslateX` and `--pf-c-popover__arrow--m-top--TranslateY` and `--pf-c-popover__arrow--m-top--Rotate`
- `--pf-c-popover__arrow--m-right--Transform` split into `--pf-c-popover__arrow--m-right--TranslateX` and `--pf-c-popover__arrow--m-right--TranslateY` and `--pf-c-popover__arrow--m-right--Rotate`
- `--pf-c-popover__arrow--m-bottom--Transform` split into `--pf-c-popover__arrow--m-bottom--TranslateX` and `--pf-c-popover__arrow--m-bottom--TranslateY` and `--pf-c-popover__arrow--m-bottom--Rotate`
- `--pf-c-popover__arrow--m-left--Transform` split into `--pf-c-popover__arrow--m-left--TranslateX` and `--pf-c-popover__arrow--m-left--TranslateY` and `--pf-c-popover__arrow--m-left--Rotate`

### Radio
- Fixed radio left margin overflow, error in selector [(#2799)](https://github.com/patternfly/patternfly/pull/2799).
- Fixed issue where radio is cut off when comes after label [(#2912)](https://github.com/patternfly/patternfly/pull/2912).

Updated CSS:
- Adds a `1px` margin to the radio input on the edge that touches the `.pf-c-radio` component container.

### Select
- Removed separator in favor of divider component [(#2944)](https://github.com/patternfly/patternfly/pull/2944)
- Removed hover, active, focus, and disabled modifiers [(#2975)](https://github.com/patternfly/patternfly/pull/2975)
- Cleaned up vars [(#3019)](https://github.com/patternfly/patternfly/pull/3019)

Updated markup:
- Added `span.pf-c-select__menu-item-icon` wrapper to menu item icon [(#2927)](https://github.com/patternfly/patternfly/pull/2927)
- Added `span.pf-c-select__toggle-arrow` wrapper to toggle icon [(#2927)](https://github.com/patternfly/patternfly/pull/2927)

Removed classes:
- `.pf-c-select__separator` - element was removed. Use the divider component instead.
- `.pf-m-hover` from `.pf-c-select__toggle` and `.pf-c-select__menu-item`
- `.pf-m-focus` from `.pf-c-select__toggle-typeahead`
  - The `:hover` and `:focus` selectors still have styles applied to them.

Renamed variables:
- `--pf-c-select__menu-input--PaddingTop` to `--pf-c-select__menu-search--PaddingTop`
- `--pf-c-select__menu-input--PaddingRight` to `--pf-c-select__menu-search--PaddingRight`
- `--pf-c-select__menu-input--PaddingBottom` to `--pf-c-select__menu-search--PaddingBottom`
- `--pf-c-select__menu-input--PaddingLeft` to `--pf-c-select__menu-search--PaddingLeft`
- `--pf-c-select__toggle--BorderWidth` to `--pf-c-select__toggle--before--BorderWidth`
- `--pf-c-select__toggle--BorderTopColor` to `--pf-c-select__toggle--before--BorderTopColor`
- `--pf-c-select__toggle--BorderRightColor` to `--pf-c-select__toggle--before--BorderRightColor`
-  `--pf-c-select__toggle--BorderBottomColor` to `--pf-c-select__toggle--before--BorderBottomColor`
- `--pf-c-select__toggle--BorderLeftColor` to `--pf-c-select__toggle--before--BorderLeftColor`
- `--pf-c-select__toggle--hover--BorderBottomColor` to `--pf-c-select__toggle--hover--before--BorderBottomColor`
- `--pf-c-select__toggle--active--BorderBottomColor` to `--pf-c-select__toggle--active--before--BorderBottomColor`
- `--pf-c-select__toggle--expanded--BorderBottomColor` to `--pf-c-select__toggle--m-expanded--before--BorderBottomColor`
- `--pf-c-select__toggle--active--BorderBottomWidth` to `--pf-c-select__toggle--active--before--BorderBottomWidth`
- `--pf-c-select__toggle--expanded--BorderBottomWidth` to `--pf-c-select__toggle--m-expanded--before--BorderBottomWidth`
- `--pf-c-select__toggle-typeahead-form--MinWidth` to `--pf-c-select__toggle-typeahead--MinWidth`
- `--pf-c-select__toggle-typeahead--active--PaddingBottom` to `--pf-c-select__toggle-typeahead--focus--PaddingBottom`
- `--pf-c-select__toggle-button--PaddingLeft` to `--pf-c-select__toggle-clear--toggle-button--PaddingLeft`
- `--pf-c-select__toggle-arrow--m-top--m-expanded__toggle-arrow--Transform` to `--pf-c-select__toggle-arrow--m-top--m-expanded__toggle-arrow--Rotate`
- `--pf-c-select__menu--m-top--Transform` to `--pf-c-select__menu--m-top--TranslateY`
- `--pf-c-select__menu-item-icon--Transform` to `--pf-c-select__menu-item-icon--TranslateY`

Removed variables:
- `--pf-c-select__menu--BorderWidth`
- `--pf-c-select__separator--Height`
- `--pf-c-select__separator--BackgroundColor`
- `--pf-c-select__separator--MarginTop`
- `--pf-c-select__separator--MarginBottom`
- `--pf-c-select__toggle--m-plain--BorderColor`
- `--pf-c-select__toggle--m-plain--Color`
- `--pf-c-select__toggle--m-plain--hover--Color`
- `--pf-c-select__toggle-wrapper--m-typeahead--PaddingTop`

### Simple list
- Removed hover, active, focus, and disabled modifiers [(#2975)](https://github.com/patternfly/patternfly/pull/2975)

Removed classes:
- `.pf-m-hover` from `.pf-c-simple-list__item-link`
- `.pf-m-active` from `.pf-c-simple-list__item-link`
- `.pf-m-focus` from `.pf-c-simple-list__item-link`
  - The `:hover`, `:active` and `:focus` selectors still have styles applied to them.

### Skip to content
- Removed hover, active, focus, and disabled modifiers [(#2975)](https://github.com/patternfly/patternfly/pull/2975)

Removed classes:
- `.pf-m-focus` from `.pf-c-skip-to-content`
  - The `:focus` selector still has styles applied to it.

### Switch
- Cleaned up vars [(#3026)](https://github.com/patternfly/patternfly/pull/3026)

Renamed variables:
- `--pf-c-switch--Width` to `--pf-c-switch__toggle--Width`
- `--pf-c-switch__input--checked__toggle--before--Transform` to `--pf-c-switch__input--checked__toggle--before--TranslateX`

Removed variables:
- `--pf-c-switch__toggle-icon--PaddingLeft`
- `--pf-c-switch__toggle-icon--Top`
- `--pf-c-switch__toggle-icon--Transform`
- `--pf-c-switch__label--FontSize`
- `--pf-c-switch__label--FontWeight`
- `--pf-c-switch__label--LineHeight`

### Tabs
- Updated to new tab design [(#2757)](https://github.com/patternfly/patternfly/pull/2757)
- Updated class name for tabs button to be tabs link [(#2919)](https://github.com/patternfly/patternfly/pull/2919)
- Fixed tab focus [(#3095)](https://github.com/patternfly/patternfly/pull/3095)

Updated CSS:
- The CSS for tabs was refactored. See the [PR changeset](https://github.com/patternfly/patternfly/pull/2757/files) for a full list of the changes.

### Table
- Reduced spacing [(#2775)](https://github.com/patternfly/patternfly/pull/2775)
- Replaced expanded content box shadow with border [(#2861)](https://github.com/patternfly/patternfly/pull/2861)
- Fixed td wrapping by adding to overflow-wrap [(#2868)](https://github.com/patternfly/patternfly/pull/2868)
- Fixed borders, row height, and focus [(#2965)](https://github.com/patternfly/patternfly/pull/2965)
- Updated min-width vars, removed unused [(#3074)](https://github.com/patternfly/patternfly/pull/3074)
- Moved borders to ::after [(#3113)](https://github.com/patternfly/patternfly/pull/3113)

Updated markup:
- The class `.pf-c-table__toggle-icon` should be removed from the toggle icon and added to a new element that wraps the icon - `div.pf-c-table__toggle-icon`
- Sortable table header cell buttons `(.pf-c-table__sort > .pf-c-button)`
  - The `.pf-c-button` element should be replaced with `button.pf-c-table__button`, with an inner wrapper `div.pf-c-table__button-content`.
  - In `div.pf-c-table__button-content`, the text beside `.pf-c-table__sort-indicator` should now be wrapped in `span.pf-c-table__text`.
- Compound expansion table body buttons (`.pf-c-table__compound-expansion-toggle > .pf-c-button`)
  - The `.pf-c-button` element should be replaced with `button.pf-c-table__button`, with an inner container of `span.pf-c-table__text`.

Updated CSS:
- Changed compact table's header spacing from `--pf-global--spacer--md` top and bottom padding to `calc(var(--pf-global--spacer--sm) + var(--pf-global--spacer--xs))` top padding and `var(--pf-global--spacer--sm)` bottom padding.
- Changed compact table's responsive/grid spacers. All values changed from the default table's responsive spacers:
  - `<tr>` top/bottom padding to `--pf-global--spacer--sm`
  - `<td>` top/bottom padding to `var--pf-global--spacer--xs`
  - `.pf-c-table__action` top/bottom margin set to `calc(var(--pf-global--spacer--xs) * -1);` to better align in the grid layout.
  - The expandable row toggle button's (`.pf-c-table__toggle .pf-c-button`) bottom margin set to`calc(#{pf-size-prem(6px)} * -1);` to reduce gap between toggle and expandable row content.
- Expandable row borders are now defined using `::after` instead of `::before`.
- Changed `td { word-break: break-word; }` to `tr > * { overflow-wrap: break-word; }`.

Removed classes:
- `.pf-m-height-auto` from `<tr>`

Renamed variables:
- `--pf-c-table--FontWeight` to `--pf-c-table--cell--FontWeight`
- All variables that start with `--pf-c-table-cell--` to `--pf-c-table--cell--`
- All variables that start with `--pf-c-table--m-compact-cell--` to `--pf-c-table--m-compact--cell--`
- `--pf-c-table__expandable-row--before--BackgroundColor` to `--pf-c-table__expandable-row--before--BorderColor`
- `--pf-c-table__expandable-row--before--Width` to `--pf-c-table__expandable-row--before--BorderWidth`
- `--pf-c-table__sort-indicator--hover--Color` to `--pf-c-table__sort__button--hover__sort-indicator--Color`
- `--pf-c-table__toggle--m-expanded__icon--Transform` to `--pf-c-table__toggle--m-expanded__icon--Rotate`
- `--pf-c-table__toggle--c-button__toggle-icon--Transform` to `--pf-c-table__toggle--c-button__toggle-icon--Rotate`
- `--pf-c-table__toggle--c-button--m-expanded__toggle-icon--Transform` to `--pf-c-table__toggle--c-button--m-expanded__toggle-icon--Rotate`
- `--pf-c-table-tbody--responsive—BorderWidth` to `--pf-c-table--tbody--responsive--border-width--base`
- `--pf-c-table--tbody--after—BorderWidth` to `--pf-c-table--tbody--after--border-width—base`
- `--pf-c-table-tr--responsive—BorderWidth` to `--pf-c-table-tr--responsive--border-width—base`
- `--pf-c-table-tr--responsive--last-child—BorderWidth` to `--pf-c-table-tr--responsive--last-child—BorderBottomWidth`
- `--pf-c-table-tbody--responsive—BorderWidth` to `--pf-c-table--tbody--responsive--border-width--base`
- `--pf-c-table-tr--responsive—BorderWidth` to `--pf-c-table-tr--responsive--border-width--base`
- `--pf-c-table--BorderWidth` to `--pf-c-table--border-width--base`
- `--pf-c-table__expandable-row--before--Top` to `--pf-c-table__expandable-row--after--Top`
- `--pf-c-table__expandable-row--before--Bottom` to `--pf-c-table__expandable-row--after--Bottom`
- `--pf-c-table__expandable-row--before--BorderWidth` to `--pf-c-table__expandable-row--after--border-width--base`
- `--pf-c-table__expandable-row--before--BorderLeftWidth` to `--pf-c-table__expandable-row--after--BorderLeftWidth`
- `--pf-c-table__expandable-row--before--BorderColor` to `--pf-c-table__expandable-row--after--BorderColor`
- `--pf-c-table__compound-expansion-toggle__button--before--BorderWidth` to `--pf-c-table__compound-expansion-toggle__button--before--border-width--base`
- `--pf-c-table__compound-expansion-toggle__button--after--BorderWidth` to `--pf-c-table__compound-expansion-toggle__button--after--border-width--base`
- `--pf-c-table-cell-th--responsive--PaddingTop` to `--pf-c-table--cell--first-child--responsive--PaddingTop`

Removed variables:
- `--pf-c-table__expandable-row--before--BorderRightWidth`
- `--pf-c-table__expandable-row--m-expanded--BoxShadow`
- `--pf-c-table__expandable-row--before--ZIndex`
- `--pf-c-table-cell--FontWeight`
- `--pf-c-table__sort--c-button--Color`
- `--pf-c-table__sort-indicator--LineHeight`
- `--pf-c-table__compound-expansion-toggle--BorderTop--BorderWidth`
- `--pf-c-table__compound-expansion-toggle--BorderTop--BorderColor`
- `--pf-c-table__compound-expansion-toggle--BorderRight--BorderWidth`
- `--pf-c-table__compound-expansion-toggle--BorderLeft--BorderWidth`
- `--pf-c-table__compound-expansion-toggle--BorderRight--BorderColor`
- `--pf-c-table__compound-expansion-toggle--BorderLeft--BorderColor`
- `--pf-c-table__compound-expansion-toggle--BorderBottom--BorderWidth`
- `--pf-c-table__compound-expansion-toggle--BorderBottom--BorderColor`
- `--pf-c-table__sort--sorted--Color`
- `--pf-c-table--thead--cell--Width`

### Toolbar (previously Data toolbar)
- Removed separator in favor of divider component [(#2944)](https://github.com/patternfly/patternfly/pull/2944)

Removed classes:
- `.pf-c-data-toolbar__item.pf-m-separator` - element has been removed. Use the divider component instead.

Removed variables:
- `--pf-c-data-toolbar__item--m-separator--spacer`
- `--pf-c-data-toolbar__item--m-separator--BackgroundColor`
- `--pf-c-data-toolbar__item--m-separator--Width`
- `--pf-c-data-toolbar__item--m-separator--Height`

### Tooltip
- Reduced spacing [(#2763)](https://github.com/patternfly/patternfly/pull/2763)
- Adds medium box shadow to the tooltip component [(#2855)](https://github.com/patternfly/patternfly/pull/2855)

Renamed variables:
- `--pf-c-tooltip__arrow--m-top--Transform` split into  `--pf-c-tooltip__arrow--m-top--TranslateX` and `--pf-c-tooltip__arrow--m-top--TranslateY` and `--pf-c-tooltip__arrow--m-top--Rotate`
- `--pf-c-tooltip__arrow--m-right--Transform` split into `--pf-c-tooltip__arrow--m-right--TranslateX` and `--pf-c-tooltip__arrow--m-right--TranslateY` and `--pf-c-tooltip__arrow--m-right--Rotate`
- `--pf-c-tooltip__arrow--m-bottom--Transform` split into `--pf-c-tooltip__arrow--m-bottom--TranslateX` and `--pf-c-tooltip__arrow--m-bottom--TranslateY` and `--pf-c-tooltip__arrow--m-bottom--Rotate`
- `--pf-c-tooltip__arrow--m-left--Transform` split into `--pf-c-tooltip__arrow--m-left--TranslateX` and `--pf-c-tooltip__arrow--m-left--TranslateY` and `--pf-c-tooltip__arrow--m-left--Rotate`

Updated CSS:
- Top and bottom padding changed from `--pf-global--spacer--md` to `--pf-global--spacer--sm`
- Left and right padding changed from `--pf-global--spacer--lg` to `--pf-global--spacer--md`

### Wizard
- Updates to shadows and borders [(#2860)](https://github.com/patternfly/patternfly/pull/2860)
- Move padding modifier to body [(#2924)](https://github.com/patternfly/patternfly/pull/2924)
- Made desktop nav 250px, remove compact modifier [(#2936)](https://github.com/patternfly/patternfly/pull/2936)
- Reworked wizard so it can be used in modal [(#2964)](https://github.com/patternfly/patternfly/pull/2964)
- Removed hover, active, focus, and disabled modifiers [(#2975)](https://github.com/patternfly/patternfly/pull/2975)
- Cleaned up vars [(#3013)](https://github.com/patternfly/patternfly/pull/3013)
- Added wrapper with classname to all icons[(#2927)](https://github.com/patternfly/patternfly/pull/2927)

Updated markup:
- The class `.pf-c-wizard__toggle-icon` on the toggle icon should be removed from the icon and added to a new element that wraps the icon - `span.pf-c-wizard__toggle-icon`.
- When using the wizard in a modal, simply omit all of the element children of the modal component, and place the `.pf-c-wizard` component as the direct and only child of `.pf-c-modal-box`.
- `.pf-m-no-padding` has been moved from `.pf-c-wizard__main` to `.pf-c-wizard__main-body`. The underlying CSS didn't change, just where the class goes.

Updated CSS:
- Removes box shadow from mobile nav toggle when expanded, replaces with border.
- Replaces desktop nav box shadow with border.
- `--pf-c-wizard__nav--lg--Width` (the desktop nav width) changed from 300px to 250px.
- Removed the "modal" functionality/layout from the wizard and makes it a normal container that fills the space of the element it is placed inside of.
- The wizard now consumes the height of its parent container with `height: 100%`, except when the wizard is placed in a modal. Then its height is 762px, defined as `--pf-c-modal-box--c-wizard--FlexBasis`.

Removed classes:
- `.pf-m-compact-nav` from `.pf-c-wizard`
- `.pf-m-full-width` from `.pf-c-wizard`
- `.pf-m-full-height` from `.pf-c-wizard`
- `.pf-m-in-page` from `.pf-c-wizard`
- `.pf-m-hover` from `.pf-c-wizard__nav-link`
- `.pf-m-focus` from `.pf-c-wizard__nav-link`
  - The `:hover` and `:focus` selectors still have styles applied to them.

Renamed variables:
- `--pf-c-wizard__close--lg--Right` to `--pf-c-wizard__close--xl--Right`
- `--pf-c-wizard__main-body--lg--PaddingTop` to `--pf-c-wizard__main-body--xl--PaddingTop`
- `--pf-c-wizard__main-body--lg--PaddingRight` to `--pf-c-wizard__main-body--xl--PaddingRight`
- `--pf-c-wizard__main-body--lg--PaddingBottom` to `--pf-c-wizard__main-body--xl--PaddingBottom`
- `--pf-c-wizard__main-body--lg--PaddingLeft` to `--pf-c-wizard__main-body--xl--PaddingLeft`
- `--pf-c-wizard__footer--lg--PaddingTop` to `--pf-c-wizard__footer--xl--PaddingTop`
- `--pf-c-wizard__footer--lg--PaddingRight` to `--pf-c-wizard__footer--xl--PaddingRight`
- `--pf-c-wizard__footer--lg--PaddingBottom` to `--pf-c-wizard__footer--xl--PaddingBottom`
- `--pf-c-wizard__footer--lg--PaddingLeft` to `--pf-c-wizard__footer--xl--PaddingLeft`
- `--pf-c-wizard__nav-link--before--Transform` to `--pf-c-wizard__nav-link--before--TranslateX`
- `--pf-c-wizard__toggle--m-expanded__toggle-icon--Transform` to `--pf-c-wizard__toggle--m-expanded__toggle-icon--Rotate`

Removed variables:
- `--pf-c-wizard__nav--lg--BoxShadow`
- `--pf-c-wizard--m-in-page__nav--lg--BoxShadow`
- `--pf-c-wizard--m-in-page__nav--lg--BorderRightWidth`
- `--pf-c-wizard--m-in-page__nav--lg--BorderRightColor`
- `--pf-c-wizard--m-compact-nav__nav--lg--Width`
- `--pf-c-wizard--m-in-page__nav--lg--Width`
- `--pf-c-wizard--BoxShadow`
- `--pf-c-wizard--Width`
- `--pf-c-wizard--lg--Width`
- `--pf-c-wizard--lg--MaxWidth`
- `--pf-c-wizard--lg--Height`
- `--pf-c-wizard--lg--MaxHeight`
- `--pf-c-wizard--m-full-width--lg--MaxWidth`
- `--pf-c-wizard--m-full-height--lg--Height`
- `--pf-c-wizard--m-in-page--BoxShadow`
- `--pf-c-wizard--m-in-page--Height`
- `--pf-c-wizard--m-in-page--Width`
- `--pf-c-wizard--m-in-page--lg--MaxWidth`
- `--pf-c-wizard--m-in-page--lg--MaxHeight`
- `--pf-c-wizard--m-in-page__nav-list--md--PaddingLeft`
- `--pf-c-wizard--m-in-page__nav-list--xl--PaddingLeft`
- `--pf-c-wizard--m-in-page__main-body--md--PaddingTop`
- `--pf-c-wizard--m-in-page__main-body--md--PaddingRight`
- `--pf-c-wizard--m-in-page__main-body--md--PaddingBottom`
- `--pf-c-wizard--m-in-page__main-body--md--PaddingLeft`
- `--pf-c-wizard--m-in-page__main-body--xl--PaddingRight`
- `--pf-c-wizard--m-in-page__main-body--xl--PaddingLeft`
- `--pf-c-wizard--m-in-page__footer--md--PaddingTop`
- `--pf-c-wizard--m-in-page__footer--md--PaddingRight`
- `--pf-c-wizard--m-in-page__footer--md--PaddingBottom`
- `--pf-c-wizard--m-in-page__footer--md--PaddingLeft`
- `--pf-c-wizard--m-in-page__footer--xl--PaddingRight`
- `--pf-c-wizard--m-in-page__footer--xl--PaddingLeft`
- `--pf-c-wizard__toggle-icon--MarginTop`

## Layouts
- Update gutters in patternfly layouts (gallery, grid, level, split, stack) to have a single instead of responsive gutter, so the gutter is always 16px instead of being 16px on mobile and 24px on desktop. [(#2962)](https://github.com/patternfly/patternfly/pull/2962)
