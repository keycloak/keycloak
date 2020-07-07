---
title: Navigation
section: components
cssPrefix: pf-c-nav
---

import './Nav.css'

## Examples

```hbs title=Default
{{#> nav nav--attribute='aria-label="Global"'}}
  {{#> nav-list}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#"}}
        Link 1
      {{/nav-link}}
    {{/nav-item}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#" nav-link--current="true"}}
        Current link
      {{/nav-link}}
    {{/nav-item}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#"}}
        Link 3
      {{/nav-link}}
    {{/nav-item}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#"}}
        Link 4
      {{/nav-link}}
    {{/nav-item}}
  {{/nav-list}}
{{/nav}}
```

```hbs title=Grouped-nav
{{#> nav nav--attribute='aria-label="Global"'}}
  {{#> nav-section nav-section--attribute='aria-labelledby="grouped-title1"'}}
    {{#> nav-section-title nav-section-title--attribute='id="grouped-title1"'}}
      Section title 1
    {{/nav-section-title}}
    {{#> nav-list}}
      {{#> nav-item}}
        {{#> nav-link nav-link--href="#"}}
          Link 1
        {{/nav-link}}
      {{/nav-item}}
      {{#> nav-item}}
        {{#> nav-link nav-link--href="#"}}
          Link 2
        {{/nav-link}}
      {{/nav-item}}
      {{#> nav-item}}
        {{#> nav-link nav-link--href="#"}}
          Link 3
        {{/nav-link}}
      {{/nav-item}}
    {{/nav-list}}
  {{/nav-section}}
  {{#> nav-section nav-section--attribute='aria-labelledby="grouped-title2"'}}
    {{#> nav-section-title nav-section-title--attribute='id="grouped-title2"'}}
      Section title 2
    {{/nav-section-title}}
    {{#> nav-list}}
      {{#> nav-item}}
        {{#> nav-link nav-link--href="#"}}
          Link 1
        {{/nav-link}}
      {{/nav-item}}
      {{#> nav-item}}
        {{#> nav-link nav-link--href="#" nav-link--current="true"}}
          Current link
        {{/nav-link}}
      {{/nav-item}}
      {{#> nav-item}}
        {{#> nav-link nav-link--href="#"}}
          Link 3
        {{/nav-link}}
      {{/nav-item}}
    {{/nav-list}}
  {{/nav-section}}
{{/nav}}
```

```hbs title=Expanded
{{#> nav nav--attribute='aria-label="Global"'}}
  {{#> nav-list}}
    {{#> nav-item nav-item--expandable="true" nav-item--expanded="true" nav-item--current="true"}}
      {{#> nav-link nav-link--href="#" nav-link--attribute='id="expandable-example1"'}}
        Link 1 (current and expanded example)
      {{/nav-link}}
      {{#> nav-subnav nav-subnav--attribute='aria-labelledby="expandable-example1"'}}
        {{#> nav-list}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Current link
            {{/nav-link}}
          {{/nav-item}}
          {{> divider divider--type="li"}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Subnav link 2
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#" nav-link--current="true"}}
              Subnav link 3
            {{/nav-link}}
          {{/nav-item}}
        {{/nav-list}}
      {{/nav-subnav}}
    {{/nav-item}}
    {{#> nav-item nav-item--expandable="true" nav-item--expanded="true"}}
      {{#> nav-link nav-link--href="#" nav-link--attribute='id="expandable-example2"'}}
        Link 2 (expanded, but not current example)
      {{/nav-link}}
      {{#> nav-subnav nav-subnav--attribute='aria-labelledby="expandable-example2"'}}
        {{#> nav-list}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Subnav link 1
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Subnav link 2
            {{/nav-link}}
          {{/nav-item}}
        {{/nav-list}}
      {{/nav-subnav}}
    {{/nav-item}}
    {{#> nav-item nav-item--expandable="true"}}
      {{#> nav-link nav-link--href="#" nav-link--attribute='id="expandable-example3"'}}
        Link 3
      {{/nav-link}}
      {{#> nav-subnav nav-subnav--attribute='aria-labelledby="expandable-example3"'}}
        {{#> nav-list}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Subnav link 1
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Subnav link 2
            {{/nav-link}}
          {{/nav-item}}
        {{/nav-list}}
      {{/nav-subnav}}
    {{/nav-item}}
  {{/nav-list}}
{{/nav}}
```

```hbs title=Expanded-with-subnav-titles
{{#> nav nav--attribute='aria-label="Global"'}}
  {{#> nav-list}}
    {{#> nav-item nav-item--expandable="true" nav-item--expanded="true" nav-item--current="true"}}
      {{#> nav-link nav-link--href="#"}}
        Link 1
      {{/nav-link}}
      {{#> nav-subnav nav-subnav--attribute='aria-labelledby="subnav-title1"'}}
        {{#> nav-subnav-title nav-subnav-title--attribute='id="subnav-title1"'}}
          Current and expanded example sub-navigation
        {{/nav-subnav-title}}
        {{#> nav-list}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Current link
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#" nav-link--current="true"}}
              Subnav link 2
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Subnav link 3
            {{/nav-link}}
          {{/nav-item}}
        {{/nav-list}}
      {{/nav-subnav}}
    {{/nav-item}}
    {{#> nav-item nav-item--expandable="true" nav-item--expanded="true"}}
      {{#> nav-link nav-link--href="#"}}
        Link 2
      {{/nav-link}}
      {{#> nav-subnav nav-subnav--attribute='aria-labelledby="subnav-title2"'}}
        {{#> nav-subnav-title nav-subnav-title--attribute='id="subnav-title2"'}}
          Expanded, but not current example sub-navigation
        {{/nav-subnav-title}}
        {{#> nav-list}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Subnav link 1
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Subnav link 2
            {{/nav-link}}
          {{/nav-item}}
        {{/nav-list}}
      {{/nav-subnav}}
    {{/nav-item}}
  {{/nav-list}}
{{/nav}}
```

```hbs title=Mixed
{{#> nav nav--attribute='aria-label="Global"'}}
  {{#> nav-list}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#"}}
        Link 1 (not expandable)
      {{/nav-link}}
    {{/nav-item}}
    {{#> nav-item nav-item--expandable="true" nav-item--expanded="true"}}
      {{#> nav-link nav-link--href="#" nav-link--attribute='id="nav-mixed-link2"'}}
        Link 2 (expanded, but not current example)
      {{/nav-link}}
      {{#> nav-subnav nav-subnav--attribute='aria-labelledby="nav-mixed-link2"'}}
        {{#> nav-list}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Subnav link 1
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Subnav link 2
            {{/nav-link}}
          {{/nav-item}}
        {{/nav-list}}
      {{/nav-subnav}}
    {{/nav-item}}
    {{#> nav-item nav-item--expandable="true" nav-item--current="true"}}
      {{#> nav-link nav-link--href="#" nav-link--attribute='id="nav-mixed-link4"'}}
        Link 3 (current, but not expanded example)
      {{/nav-link}}
      {{#> nav-subnav nav-subnav--attribute='aria-labelledby="nav-mixed-link4"'}}
        {{#> nav-list}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Subnav link 1
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#" nav-link--current="true"}}
              Subnav link 2
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Subnav link 3
            {{/nav-link}}
          {{/nav-item}}
        {{/nav-list}}
      {{/nav-subnav}}
    {{/nav-item}}
  {{/nav-list}}
{{/nav}}
```

```hbs title=Horizontal
{{#> nav nav--IsHorizontal="true" nav--attribute='aria-label="Global"'}}
  {{#> nav-list}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#" nav-link--current="true"}}
        Item 1
      {{/nav-link}}
    {{/nav-item}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#"}}
        Item 2
      {{/nav-link}}
    {{/nav-item}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#"}}
        Item 3
      {{/nav-link}}
    {{/nav-item}}
  {{/nav-list}}
{{/nav}}
```

```hbs title=Horizontal-overflow
{{#> nav nav--IsHorizontal="true" nav--IsScrollable="true" nav--attribute='aria-label="Global"'}}
  {{#> nav-list}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#"}}
        Horizontal nav item 1
      {{/nav-link}}
    {{/nav-item}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#"}}
        Horizontal nav item 2
      {{/nav-link}}
    {{/nav-item}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#"}}
        Horizontal nav item 3
      {{/nav-link}}
    {{/nav-item}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#"}}
        Horizontal nav item 4
      {{/nav-link}}
    {{/nav-item}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#" nav-link--current="true"}}
        Horizontal nav item 5
      {{/nav-link}}
    {{/nav-item}}
  {{/nav-list}}
{{/nav}}
```

```hbs title=Tertiary
{{#> nav nav--IsHorizontal="true" nav--IsTertiary="true" nav--attribute='aria-label="Local"'}}
  {{#> nav-list}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#" nav-link--current="true"}}
        Item 1
      {{/nav-link}}
    {{/nav-item}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#"}}
        Item 2
      {{/nav-link}}
    {{/nav-item}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#"}}
        Item 3
      {{/nav-link}}
    {{/nav-item}}
  {{/nav-list}}
{{/nav}}
```

```hbs title=Tertiary-overflow
{{#> nav nav--IsHorizontal="true" nav--IsTertiary="true" nav--IsScrollable="true" nav--attribute='aria-label="Local"'}}
  {{#> nav-list}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#" nav-link--current="true"}}
        Tertiary nav item 1
      {{/nav-link}}
    {{/nav-item}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#"}}
        Tertiary nav item 2
      {{/nav-link}}
    {{/nav-item}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#"}}
        Tertiary nav item 3
      {{/nav-link}}
    {{/nav-item}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#"}}
        Tertiary nav item 4
      {{/nav-link}}
    {{/nav-item}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#"}}
        Tertiary nav item 5
      {{/nav-link}}
    {{/nav-item}}
  {{/nav-list}}
{{/nav}}
```

```hbs title=Default-in-light-mode
{{#> nav nav--attribute='aria-label="Global"' nav--modifier="pf-m-light"}}
  {{#> nav-list}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#"}}
        Current link
      {{/nav-link}}
    {{/nav-item}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#" nav-link--current="true"}}
        Link 2
      {{/nav-link}}
    {{/nav-item}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#"}}
        Link 3
      {{/nav-link}}
    {{/nav-item}}
    {{#> nav-item}}
      {{#> nav-link nav-link--href="#"}}
        Link 4
      {{/nav-link}}
    {{/nav-item}}
  {{/nav-list}}
{{/nav}}
```

```hbs title=Expanded-in-light-mode
{{#> nav nav--attribute='aria-label="Global"' nav--modifier="pf-m-light"}}
  {{#> nav-list}}
    {{#> nav-item nav-item--expandable="true" nav-item--expanded="true" nav-item--current="true"}}
      {{#> nav-link nav-link--href="#" nav-link--attribute='id="expandable-example1"'}}
        Link 1 (current and expanded example)
      {{/nav-link}}
      {{#> nav-subnav nav-subnav--attribute='aria-labelledby="expandable-example1"'}}
        {{#> nav-list}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Current link
            {{/nav-link}}
          {{/nav-item}}
          {{> divider divider--type="li"}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Subnav link 2
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#" nav-link--current="true"}}
              Subnav link 3
            {{/nav-link}}
          {{/nav-item}}
        {{/nav-list}}
      {{/nav-subnav}}
    {{/nav-item}}
    {{#> nav-item nav-item--expandable="true" nav-item--expanded="true"}}
      {{#> nav-link nav-link--href="#" nav-link--attribute='id="expandable-example2"'}}
        Link 2 (expanded, but not current example)
      {{/nav-link}}
      {{#> nav-subnav nav-subnav--attribute='aria-labelledby="expandable-example2"'}}
        {{#> nav-list}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Subnav link 1
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Subnav link 2
            {{/nav-link}}
          {{/nav-item}}
        {{/nav-list}}
      {{/nav-subnav}}
    {{/nav-item}}
    {{#> nav-item nav-item--expandable="true"}}
      {{#> nav-link nav-link--href="#" nav-link--attribute='id="expandable-example3"'}}
        Link 3
      {{/nav-link}}
      {{#> nav-subnav nav-subnav--attribute='aria-labelledby="expandable-example3"'}}
        {{#> nav-list}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Subnav link 1
            {{/nav-link}}
          {{/nav-item}}
          {{#> nav-item newcontent}}
            {{#> nav-link nav-link--href="#"}}
              Subnav link 2
            {{/nav-link}}
          {{/nav-item}}
        {{/nav-list}}
      {{/nav-subnav}}
    {{/nav-item}}
  {{/nav-list}}
{{/nav}}
```

## Documentation

### Overview

The navigation system relies on several different sub-components:

* `.pf-c-nav__list` - default navigation list. It is the basis for both default and expandable, vertical navigation.

### Accessibility

| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `aria-label="[landmark description]"` | `.pf-c-nav` |  Describes `<nav>` landmark. |
| `aria-labelledby="[id value of link describing subnav]"` | `.pf-c-nav__subnav` |  Gives the subnav `<section>` landmark an accessible name by referring to the element that provides the subnav `<section>` landmark title. |
| `aria-expanded="false"` | `.pf-c-nav__link` |  Indicates that subnav section is hidden. |
| `aria-expanded="true"` | `.pf-c-nav__link` |  Indicates that subnav section is visible. |
| `hidden` | `.pf-c-nav__subnav` |  Indicates that the subnav section is hidden so that it isn't visible in the UI and isn't accessed by assistive technologies. |
| `disabled` | `.pf-c-nav__scroll-button` | Indicates that a scroll button is disabled, when at the first or last item of a list. **Required when disabled** |
| `aria-current="page"` | `.pf-c-nav__link` |  Indicates the current page link. Can only occur once on page. |

### Usage

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-nav` | `<nav>` | Initiates a primary nav element. |
| `.pf-c-nav__subnav` | `<section>` | Initiates a subnav section. |
| `.pf-c-nav__list` | `<ul>` | Initiates nav list. |
| `.pf-c-nav__item` | `<li>` | Initiates nav list item. |
| `.pf-c-nav__link` | `<a>` | Initiates nav list link. |
| `.pf-c-nav__section` | `<section>` | Initiates a nav section element. |
| `.pf-c-nav__section-title` | `<h1>`, `<h2>`, `<h3>`, `<h4>`, `<h5>`, `<h6>` | Initiates a nav section title. |
| `.pf-c-nav__toggle` | `<span>` | Initiates the nav toggle wrapper. |
| `.pf-c-nav__toggle-icon` | `<span>` | Initiates a nav toggle icon wrapper. |
| `.pf-c-nav__scroll-button` | `<button>` | Initiates a nav scroll button. **Required for horizontal navs** |
| `.pf-m-horizontal` | `.pf-c-nav` | Modifies nav for the horizontal variation. |
| `.pf-m-tertiary` | `.pf-c-nav` | Modifies nav for the tertiary variation. |
| `.pf-m-light` | `.pf-c-nav` | Modifies nav for the light variation. **Note: only for use with vertical navs, and requires `.pf-m-light` on the page component's sidebar element (`.pf-c-page__sidebar`)**. |
| `.pf-m-scrollable` | `.pf-c-nav` | Modifies nav for the scrollable state. |
| `.pf-m-expandable` | `.pf-c-nav__item` | Modifies for the expandable state. |
| `.pf-m-expanded` | `.pf-c-nav__item` | Modifies for the expanded state. |
| `.pf-m-current` | `.pf-c-nav__link` | Modifies for the current state. |
