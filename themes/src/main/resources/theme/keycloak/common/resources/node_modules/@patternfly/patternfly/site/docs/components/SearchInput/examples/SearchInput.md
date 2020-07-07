---
title: 'Search input'
beta: true
section: components
cssPrefix: pf-c-search-input
---

## Examples
```hbs title=Basic
{{> search-input search-input--placeholder="Find by name"}}
```

```hbs title=No-match
{{> search-input search-input--placeholder="Find by name" search-input--value="Joh"}}
```

```hbs title=Match-with-result-count
{{> search-input search-input--placeholder="Find by name" search-input--value="John Doe" search-input--count="3"}}
```

```hbs title=Match-with-navigable-options
{{> search-input search-input--placeholder="Find by name" search-input--value="John Doe" search-input--count="1 / 3" search-input--IsNavigable="true" search-input--IsFirstMatch="true"}}
```

### Accessibility
| Attributes | Applied to | Outcome |
| -- | -- | -- |
| `aria-hidden="true"` | `.pf-c-search-input__icon > *` | Hides the search icon from assistive technologies. **Required** |
| `aria-label="Previous"` | `.pf-c-search-input__nav > .pf-c-button` | Provides an accessible label for the previous nav button. **Required** |
| `aria-label="Next"` | `.pf-c-search-input__nav > .pf-c-button` | Provides an accessible label for the next nav button. **Required** |
| `aria-label="[descriptive text]"` | `.pf-c-search-input__text-input` | Provides an accessible label for the search input. **Required** |
| `aria-label="Clear"` | `.pf-c-search-input__clear > .pf-c-button` | Provides an accessible label for the clear button. **Required** |

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-search-input` | `<div>` | Initiates the custom search input component. **Required** |
| `.pf-c-search-input__text` | `<span>` | Initiates the text area. **Required** |
| `.pf-c-search-input__text-input` | `<input>` | Initiates the search input. **Required** |
| `.pf-c-search-input__icon` | `<span>` | Initiates the search icon container. **Required** |
| `.pf-c-search-input__utilities` | `<span>` | Initiates the utilities area beside the search input. |
| `.pf-c-search-input__count` | `<span>` | Initiates the item count container. |
| `.pf-c-search-input__nav` | `<span>` | Initiates the navigable buttons container. |
| `.pf-c-search-input__clear` | `<span>` | Initiates the clear button container. **Required when there is text in the search input** |
