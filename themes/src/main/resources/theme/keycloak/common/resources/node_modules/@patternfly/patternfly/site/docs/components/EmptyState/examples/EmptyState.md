---
title: Empty state
section: components
cssPrefix: pf-c-empty-state
---

## Examples

```hbs title=Basic
{{#> empty-state}}
  {{#> empty-state-icon}}{{/empty-state-icon}}
  {{#> title titleType="h1" title--modifier="pf-m-lg"}}
    Empty state
  {{/title}}
  {{#> empty-state-body}}
    This represents an the empty state pattern in PatternFly 4. Hopefully it's simple enough to use but flexible enough to meet a variety of needs.
  {{/empty-state-body}}
  {{#> button button--modifier="pf-m-primary"}}
    Primary action
  {{/button}}
  {{#> empty-state-secondary}}
    {{#> button button--modifier="pf-m-link"}}
      Multiple
    {{/button}}
    {{#> button button--modifier="pf-m-link"}}
      Action buttons
    {{/button}}
    {{#> button button--modifier="pf-m-link"}}
      Can
    {{/button}}
    {{#> button button--modifier="pf-m-link"}}
      Go here
    {{/button}}
    {{#> button button--modifier="pf-m-link"}}
      In the secondary
    {{/button}}
    {{#> button button--modifier="pf-m-link"}}
      Action area
    {{/button}}
  {{/empty-state-secondary}}
{{/empty-state}}
```

```hbs title=Small
{{#> empty-state empty-state--modifier="pf-m-sm"}}
  {{#> empty-state-icon}}{{/empty-state-icon}}
  {{#> title titleType="h1" title--modifier="pf-m-lg"}}
    Empty state
  {{/title}}
  {{#> empty-state-body}}
    This represents an the empty state pattern in PatternFly 4. Hopefully it's simple enough to use but flexible enough to meet a variety of needs.
  {{/empty-state-body}}
  {{#> button button--modifier="pf-m-primary"}}
    Primary action
  {{/button}}
  {{#> empty-state-secondary}}
    {{#> button button--modifier="pf-m-link"}}
      Multiple
    {{/button}}
    {{#> button button--modifier="pf-m-link"}}
      Action buttons
    {{/button}}
    {{#> button button--modifier="pf-m-link"}}
      Can
    {{/button}}
    {{#> button button--modifier="pf-m-link"}}
      Go here
    {{/button}}
    {{#> button button--modifier="pf-m-link"}}
      In the secondary
    {{/button}}
    {{#> button button--modifier="pf-m-link"}}
      Action area
    {{/button}}
  {{/empty-state-secondary}}
{{/empty-state}}
```

```hbs title=Large
{{#> empty-state empty-state--modifier="pf-m-lg"}}
  {{#> empty-state-icon}}{{/empty-state-icon}}
  {{#> title titleType="h1" title--modifier="pf-m-lg"}}
    Empty state
  {{/title}}
  {{#> empty-state-body}}
    This represents an the empty state pattern in PatternFly 4. Hopefully it's simple enough to use but flexible enough to meet a variety of needs.
  {{/empty-state-body}}
  {{#> button button--modifier="pf-m-primary"}}
    Primary action
  {{/button}}
  {{#> empty-state-secondary}}
    {{#> button button--modifier="pf-m-link"}}
      Multiple
    {{/button}}
    {{#> button button--modifier="pf-m-link"}}
      Action buttons
    {{/button}}
    {{#> button button--modifier="pf-m-link"}}
      Can
    {{/button}}
    {{#> button button--modifier="pf-m-link"}}
      Go here
    {{/button}}
    {{#> button button--modifier="pf-m-link"}}
      In the secondary
    {{/button}}
    {{#> button button--modifier="pf-m-link"}}
      Action area
    {{/button}}
  {{/empty-state-secondary}}
{{/empty-state}}
```

```hbs title=Extra-large
{{#> empty-state empty-state--modifier="pf-m-xl"}}
  {{#> empty-state-icon}}{{/empty-state-icon}}
  {{#> title titleType="h1" title--modifier="pf-m-4xl"}}
    Empty state
  {{/title}}
  {{#> empty-state-body}}
    This represents an the empty state pattern in PatternFly 4. Hopefully it's simple enough to use but flexible enough to meet a variety of needs.
  {{/empty-state-body}}
  {{#> button button--modifier="pf-m-primary"}}
    Primary action
  {{/button}}
{{/empty-state}}
```

```hbs title=With-primary-element
{{#> empty-state}}
  {{#> empty-state-icon}}{{/empty-state-icon}}
  {{#> title titleType="h1" title--modifier="pf-m-lg"}}
    Empty State
  {{/title}}
  {{#> empty-state-body}}
    This represents an the empty state pattern in PatternFly 4. Hopefully it's simple enough to use but flexible enough to meet a variety of needs.
  {{/empty-state-body}}
  {{#> empty-state-primary}}
    {{#> button button--modifier="pf-m-link"}}
      Action buttons
    {{/button}}
  {{/empty-state-primary}}
{{/empty-state}}
```

## Documentation
### Accessibility
| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `aria-hidden="true"` | `.pf-c-empty-state__icon` |  Hides icon for assistive technologies. **Required** |

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-empty-state` | `<div>` |  Initiates an empty state component. The empty state centers its content (`.pf-c-empty-state__content`) vertically and horizontally. **Required** |
| `.pf-c-empty-state__content` | `<div>` |  Creates the content container. **Required** |
| `.pf-c-empty-state__icon` | `<i>`, `<div>` |  Creates the empty state icon or icon container when used as a `<div>`. |
| `.pf-c-title` | `<h1>, <h2>, <h3>, <h4>, <h5>, <h6>` |  Creates the empty state title. **Required** |
| `.pf-c-empty-state__body` | `<div>` |  Creates the empty state body content. You can have more than one `.pf-c-empty-state__body` elements. |
| `.pf-c-button.pf-m-primary` | `<button>` |  Creates the primary action button. |
| `.pf-c-empty-state__primary` | `<div>` |  Container for primary actions. Can be used in lieu of using `.pf-c-button.pf-m-primary`. |
| `.pf-c-empty-state__secondary` | `<div>` |  Container secondary actions. |
| `.pf-m-sm` | `.pf-c-empty-state` | Modifies the empty state for a small max-width. |
| `.pf-m-lg` | `.pf-c-empty-state` | Modifies the empty state for a large max-width. |
| `.pf-m-xl` | `.pf-c-empty-state` | Modifies the empty state for a x-large max-width. |
| `.pf-m-full-height` | `.pf-c-empty-state` | Modifies the empty state to be `height: 100%`. If you need the empty state content to be centered vertically, you can use this modifier to make the empty state fill the height of its container, and center `.pf-c-empty-state__content`. **Note:** this modifier requires the parent of `.pf-c-empty-state` have an implicit or explicit `height` defined.  |
