---
title: Pagination
section: components
cssPrefix: pf-c-pagination
---

## Examples
```hbs title=Top isFullscreen
{{#> pagination}}
  {{> pagination-total-items-content}}
  {{> pagination-options-menu options-menu id="pagination-options-menu-top-example" options-menu--IsText="true"}}
  {{> pagination-nav-content}}
{{/pagination}}
```

```hbs title=Top-expanded isFullscreen
{{#> pagination}}
  {{> pagination-total-items-content}}
  {{> pagination-options-menu options-menu--IsExpanded="true" id="pagination-options-menu-top-expanded-example" options-menu--IsText="true"}}
  {{> pagination-nav-content}}
{{/pagination}}
```

```hbs title=Bottom isFullscreen
{{#> pagination pagination--modifier="pf-m-bottom"}}
  {{> pagination-options-menu id="pagination-options-menu-bottom-example" options-menu--IsText="true"}}
  {{> pagination-nav-content}}
{{/pagination}}
```

```hbs title=Top-disabled isFullscreen
{{#> pagination}}
  {{> pagination-total-items-content}}
  {{> pagination-options-menu id="pagination-options-menu-top-disabled-example" options-menu--IsText="true" options-menu-toggle--IsDisabled="true"}}
  {{> pagination-nav-content pagination-nav-content--IsDisabled="true"}}
{{/pagination}}
```

```hbs title=Compact isFullscreen
{{#> pagination pagination--IsCompact="true"}}
  {{> pagination-total-items-content}}
  {{> pagination-options-menu options-menu id="pagination-options-menu-compact-example" options-menu--IsText="true"}}
  {{> pagination-nav-content}}
{{/pagination}}
```

## Documentation
Note: `<button>` or `<a>` elements can be used in `.pf-c-pagination__nav-page-select`.

### Accessibility
| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `aria-label`  | `.pf-c-pagination__nav` |  Provides an accessible name for pagination navigation element. **Required** |

## Pagination nav input

| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `type="number"` | `.pf-c-pagination` > `.pf-c-form-control` | Defines a field as a number. **Required** |
| `value` | `.pf-c-pagination__nav-page-select` > `.pf-c-form-control` | Provides initial integer value. **Required** |
| `min` | `.pf-c-pagination__nav-page-select` > `.pf-c-form-control` | Provides minimum integer value. **Required** |
| `max` | `.pf-c-pagination__nav-page-select` > `.pf-c-form-control` | Provides max integer value. **Required** |

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-pagination` | `<div>` |  Initiates pagination. |
| `.pf-c-pagination__current` | `<div>` |  Initiates element to display currently displayed items for use in responsive view. Only needed for default pagination, not `.pf-m-bottom`. |
| `.pf-c-pagination__total-items` | `<div>` | Initiates element to replace the options menu on mobile. |
| `.pf-c-pagination__nav` | `<nav>` |  Initiates pagination nav. |
| `.pf-c-pagination__nav-control` | `<div>` |  Initiates pagination nav control. |
| `.pf-c-pagination__nav-page-select` | `<div>` |  Initiates pagination nav page select. |
| `.pf-m-bottom` | `.pf-c-pagination` | Modifies for bottom pagination component styles. |
| `.pf-m-compact` | `.pf-c-pagination` | Modifies for compact pagination component styles. |
| `.pf-m-static` | `.pf-c-pagination.pf-m-bottom` | Modifies bottom pagination to not be positioned sticky on mobile. |
| `.pf-m-first` | `.pf-c-pagination__nav-control` | Indicates the control is for the first page button. |
| `.pf-m-prev` | `.pf-c-pagination__nav-control` | Indicates the control is for the previous page button. |
| `.pf-m-next` | `.pf-c-pagination__nav-control` | Indicates the control is for the next page button. |
| `.pf-m-last` | `.pf-c-pagination__nav-control` | Indicates the control is for the last page button. |
