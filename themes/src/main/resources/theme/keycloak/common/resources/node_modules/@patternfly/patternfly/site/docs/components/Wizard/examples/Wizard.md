---
title: Wizard
section: components
cssPrefix: pf-c-wizard
wrapperTag: div
---

import './Wizard.css'

## Examples
```hbs title=Basic isFullscreen
{{#> wizard}}
  {{#> wizard-header}}
    {{#> button button--modifier="pf-m-plain pf-c-wizard__close" button--attribute='aria-label="Close"'}}
      <i class="fas fa-times" aria-hidden="true"></i>
    {{/button}}
    {{#> title title--modifier="pf-m-3xl pf-c-wizard__title"}}Wizard title{{/title}}
    {{#> wizard-description}}
      Here is where the description goes
    {{/wizard-description}}
  {{/wizard-header}}
  {{#> wizard-toggle}}
    {{#> wizard-toggle-list}}
        {{#> wizard-toggle-list-item}}
          {{#> wizard-toggle-num}}2{{/wizard-toggle-num}}
          Configuration
          {{> wizard-toggle-separator}}
        {{/wizard-toggle-list-item}}
        {{#> wizard-toggle-list-item}}
          Substep B
        {{/wizard-toggle-list-item}}
      {{/wizard-toggle-list}}
      {{> wizard-toggle-icon}}
    {{/wizard-toggle}}
    {{#> wizard-outer-wrap}}
      {{#> wizard-inner-wrap}}
        {{#> wizard-nav}}
          {{#> wizard-nav-list}}
            {{#> wizard-nav-item}}
              {{#> wizard-nav-link}}
                Information
              {{/wizard-nav-link}}
            {{/wizard-nav-item}}
            {{#> wizard-nav-item}}
              {{#> wizard-nav-link wizard-nav-link--modifier="pf-m-current"}}
                Configuration
              {{/wizard-nav-link}}
              {{#> wizard-nav-list}}
                {{#> wizard-nav-item}}
                  {{#> wizard-nav-link}}
                    Substep A
                  {{/wizard-nav-link}}
                {{/wizard-nav-item}}
                {{#> wizard-nav-item}}
                  {{#> wizard-nav-link wizard-nav-link--modifier="pf-m-current" wizard-nav-link--IsCurrent="true"}}
                    Substep B
                  {{/wizard-nav-link}}
                {{/wizard-nav-item}}
                {{#> wizard-nav-item}}
                  {{#> wizard-nav-link}}
                    Substep C
                  {{/wizard-nav-link}}
                {{/wizard-nav-item}}
              {{/wizard-nav-list}}
            {{/wizard-nav-item}}
            {{#> wizard-nav-item}}
              {{#> wizard-nav-link}}
                Additional
              {{/wizard-nav-link}}
            {{/wizard-nav-item}}
            {{#> wizard-nav-item}}
              {{#> wizard-nav-link wizard-nav-link--IsDisabled="true"}}
                Review
              {{/wizard-nav-link}}
            {{/wizard-nav-item}}
          {{/wizard-nav-list}}
        {{/wizard-nav}}
      {{#> wizard-main}}
        <p>Wizard content goes here</p>
      {{/wizard-main}}
    {{/wizard-inner-wrap}}
    {{#> wizard-footer}}
      {{#> button button--modifier="pf-m-primary" button--IsSubmit="true"}}
        Next
      {{/button}}
      {{#> button button--modifier="pf-m-secondary"}}
        Back
      {{/button}}
      {{#> button button--modifier="pf-m-link"}}
        Cancel
      {{/button}}
    {{/wizard-footer}}
  {{/wizard-outer-wrap}}
{{/wizard}}
```

```hbs title=Nav-expanded-(mobile) isFullscreen
{{#> wizard wizard--IsExpanded="true"}}
  {{#> wizard-header}}
    {{#> button button--modifier="pf-m-plain pf-c-wizard__close" button--attribute='aria-label="Close"'}}
      <i class="fas fa-times" aria-hidden="true"></i>
    {{/button}}
    {{#> title title--modifier="pf-m-3xl pf-c-wizard__title"}}Wizard title{{/title}}
    {{#> wizard-description}}
      Here is where the description goes
    {{/wizard-description}}
  {{/wizard-header}}
  {{#> wizard-toggle}}
    {{#> wizard-toggle-list}}
      {{#> wizard-toggle-list-item}}
        {{#> wizard-toggle-num}}2{{/wizard-toggle-num}}
        Configuration
        {{> wizard-toggle-separator}}
      {{/wizard-toggle-list-item}}
      {{#> wizard-toggle-list-item}}
        Substep B
      {{/wizard-toggle-list-item}}
    {{/wizard-toggle-list}}
    {{> wizard-toggle-icon}}
  {{/wizard-toggle}}
  {{#> wizard-outer-wrap}}
    {{#> wizard-inner-wrap}}
      {{#> wizard-nav}}
        {{#> wizard-nav-list}}
          {{#> wizard-nav-item}}
            {{#> wizard-nav-link}}
              Information
            {{/wizard-nav-link}}
          {{/wizard-nav-item}}
          {{#> wizard-nav-item}}
            {{#> wizard-nav-link wizard-nav-link--modifier="pf-m-current"}}
              Configuration
            {{/wizard-nav-link}}
            {{#> wizard-nav-list}}
              {{#> wizard-nav-item}}
                {{#> wizard-nav-link}}
                  Substep A
                {{/wizard-nav-link}}
              {{/wizard-nav-item}}
              {{#> wizard-nav-item}}
                {{#> wizard-nav-link wizard-nav-link--modifier="pf-m-current" wizard-nav-link--IsCurrent="true"}}
                  Substep B
                {{/wizard-nav-link}}
              {{/wizard-nav-item}}
              {{#> wizard-nav-item}}
                {{#> wizard-nav-link}}
                  Substep C
                {{/wizard-nav-link}}
              {{/wizard-nav-item}}
            {{/wizard-nav-list}}
          {{/wizard-nav-item}}
          {{#> wizard-nav-item}}
            {{#> wizard-nav-link}}
              Additional
            {{/wizard-nav-link}}
          {{/wizard-nav-item}}
          {{#> wizard-nav-item}}
            {{#> wizard-nav-link wizard-nav-link--IsDisabled="true"}}
              Review
            {{/wizard-nav-link}}
          {{/wizard-nav-item}}
        {{/wizard-nav-list}}
      {{/wizard-nav}}
      {{#> wizard-main}}
        <p>Wizard content goes here</p>
      {{/wizard-main}}
    {{/wizard-inner-wrap}}
    {{#> wizard-footer}}
      {{#> button button--modifier="pf-m-primary" button--IsSubmit="true"}}
        Next
      {{/button}}
      {{#> button button--modifier="pf-m-secondary"}}
        Back
      {{/button}}
      {{#> button button--modifier="pf-m-link"}}
        Cancel
      {{/button}}
    {{/wizard-footer}}
  {{/wizard-outer-wrap}}
{{/wizard}}
```

```hbs title=Finished isFullscreen
{{#> wizard wizard--modifier="pf-m-finished"}}
  {{#> wizard-header}}
    {{#> button button--modifier="pf-m-plain pf-c-wizard__close" button--attribute='aria-label="Close"'}}
      <i class="fas fa-times" aria-hidden="true"></i>
    {{/button}}
    {{#> title title--modifier="pf-m-3xl pf-c-wizard__title"}}Wizard title{{/title}}
    {{#> wizard-description}}
      Here is where the description goes
    {{/wizard-description}}
  {{/wizard-header}}
  {{#> wizard-toggle}}
    {{#> wizard-toggle-list}}
      {{#> wizard-toggle-list-item}}
        {{#> wizard-toggle-num}}2{{/wizard-toggle-num}}
        Configuration
        {{> wizard-toggle-separator}}
      {{/wizard-toggle-list-item}}
      {{#> wizard-toggle-list-item}}
        Substep B
      {{/wizard-toggle-list-item}}
    {{/wizard-toggle-list}}
    {{> wizard-toggle-icon}}
  {{/wizard-toggle}}
  {{#> wizard-outer-wrap}}
    {{#> wizard-inner-wrap}}
      {{#> wizard-nav}}
        {{#> wizard-nav-list}}
          {{#> wizard-nav-item}}
            {{#> wizard-nav-link}}
              Information
            {{/wizard-nav-link}}
          {{/wizard-nav-item}}
          {{#> wizard-nav-item}}
            {{#> wizard-nav-link}}
              Configuration
            {{/wizard-nav-link}}
            {{#> wizard-nav-list}}
              {{#> wizard-nav-item}}
                {{#> wizard-nav-link}}
                  Substep A
                {{/wizard-nav-link}}
              {{/wizard-nav-item}}
              {{#> wizard-nav-item}}
                {{#> wizard-nav-link}}
                  Substep B
                {{/wizard-nav-link}}
              {{/wizard-nav-item}}
              {{#> wizard-nav-item}}
                {{#> wizard-nav-link}}
                  Substep C
                {{/wizard-nav-link}}
              {{/wizard-nav-item}}
            {{/wizard-nav-list}}
          {{/wizard-nav-item}}
          {{#> wizard-nav-item}}
            {{#> wizard-nav-link}}
              Additional
            {{/wizard-nav-link}}
          {{/wizard-nav-item}}
          {{#> wizard-nav-item}}
            {{#> wizard-nav-link}}
              Review
            {{/wizard-nav-link}}
          {{/wizard-nav-item}}
        {{/wizard-nav-list}}
      {{/wizard-nav}}
      {{#> wizard-main}}
        {{#> bullseye}}
          {{#> empty-state empty-state--modifier="pf-m-lg"}}
            {{#> empty-state-icon empty-state-icon--type="cogs"}}{{/empty-state-icon}}
            {{#> title titleType="h1" title--modifier="pf-m-lg"}}
              Configuration in progress
            {{/title}}
            {{#> empty-state-body}}
              {{#> progress progress__value="33" progress--modifier="pf-m-singleline" progress__id="progress-singleline-example"}}{{/progress}}
            {{/empty-state-body}}
            {{#> empty-state-body}}
              Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec non pulvinar tortor. Maecenas sit amet pellentesque velit, eu eleifend mauris.
            {{/empty-state-body}}
            {{#> empty-state-secondary}}
              {{#> button button--modifier="pf-m-link"}}
                Cancel
              {{/button}}
            {{/empty-state-secondary}}
          {{/empty-state}}
        {{/bullseye}}
      {{/wizard-main}}
    {{/wizard-inner-wrap}}
    {{#> wizard-footer}}
      {{#> button button--modifier="pf-m-primary" button--IsSubmit="true"}}
        Next
      {{/button}}
      {{#> button button--modifier="pf-m-secondary"}}
        Back
      {{/button}}
      {{#> button button--modifier="pf-m-link"}}
        Cancel
      {{/button}}
    {{/wizard-footer}}
  {{/wizard-outer-wrap}}
{{/wizard}}
```

## Documentation
### Accessibility
| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `aria-expanded="true"` | `.pf-c-wizard__toggle` | Indicates that the steps menu is visible. **Required** |
| `aria-expanded="false"` | `.pf-c-wizard__toggle` | Indicates that the steps menu is hidden. **Required** |
| `aria-label="close"` | `.pf-c-wizard__toggle-icon` | Gives the close button an accessible name. **Required** |
| `aria-hidden="true"` | `.pf-c-wizard__toggle-icon`, `.pf-c-wizard__toggle-divider` | Hides the icon from assistive technologies. **Required** |
| `aria-label="Steps"` | `.pf-c-wizard__nav` | Gives the steps nav element an accessible name. **Required** |
| `disabled` | `button.pf-c-wizard__nav-link` | Indicates that the element is disabled. **Required when a nav item is disabled** |
| `aria-disabled="true"` | `a.pf-c-wizard__nav-link` | Indicates that the element is disabled. **Required for disabled links with `.pf-m-disabled`** |
| `aria-current="page"` | `.pf-c-wizard__nav-link` | Indicates the current page link. Can only occur once on page. **Required for the current link** |
| `tabindex="-1"` | `a.pf-c-wizard__nav-link` | Removes a link from keyboard focus. **Required for disabled links with `.pf-m-disabled`** |

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-wizard` | `<div>` | Initiates the wizard component. **Required** |
| `.pf-c-wizard__header` | `<header>` | Initiates the header. **Required** when the wizard is in a modal. Not recommended to use when the wizard is placed on a page. |
| `.pf-c-wizard__close` | `.pf-c-button.pf-m-plain` | Initiates the close button. **Required** |
| `.pf-c-wizard__title` | `.pf-c-title.pf-m-3xl` | Initiates the title. **Required** |
| `.pf-c-wizard__description` | `<p>` | Initiates the description. |
| `.pf-c-wizard__toggle` | `<button>` | Initiates the mobile steps menu toggle button. **Required** |
| `.pf-c-wizard__toggle-list` | `<ol>` | Initiates the toggle list. **Required** |
| `.pf-c-wizard__toggle-list-item` | `<li>` | Initiates a toggle list item. **Required** |
| `.pf-c-wizard__toggle-num` | `<span>` | Initiates the step number. **Required** |
| `.pf-c-wizard__toggle-separator` | `<i>` | Initiates the separator between steps. |
| `.pf-c-wizard__toggle-icon` | `<span>` | Initiates the toggle icon wrapper. **Required** |
| `.pf-c-wizard__outer-wrap` | `<div>` | Initiates the outer wrapper. **Required** |
| `.pf-c-wizard__inner-wrap` | `<div>` | Initiates the inner wrapper. **Required** |
| `.pf-c-wizard__nav` | `<nav>` | Initiates the steps nav. **Required** |
| `.pf-c-wizard__nav-list` | `<ol>` | Initiates a list of steps. **Required** |
| `.pf-c-wizard__nav-item` | `<li>` | Initiates a step list item. **Required** |
| `.pf-c-wizard__nav-link` | `<a>` | Initiates a step link. **Required** |
| `.pf-c-wizard__main` | `<main>`, `<div>` | Initiates the main container. **Required** Note: use the `<main>` element when when there are no other `<main>` elements on the page.|
| `.pf-c-wizard__main-body` | `<div>` | Initiates the main container body section. **Required** |
| `.pf-c-wizard__footer` | `<footer>` | Initiates the footer. **Required** |
| `.pf-m-expanded` | `.pf-c-wizard__toggle`, `.pf-c-wizard__nav` | Modifies the mobile steps toggle and steps menu for the expanded state. |
| `.pf-m-finished` | `.pf-c-wizard` | Modifies the wizard for the finished state. |
| `.pf-m-current` | `.pf-c-wizard__nav-link` | Modifies a step link for the current state. **Required** |
| `.pf-m-disabled` | `.pf-c-wizard__nav-link` | Modifies a step link for the disabled state. |
| `.pf-m-no-padding` | `.pf-c-wizard__main-body` | Modifies the main container body to remove the padding. |
