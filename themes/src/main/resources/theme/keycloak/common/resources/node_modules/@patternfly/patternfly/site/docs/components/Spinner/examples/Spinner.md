---
title: Spinner
section: components
cssPrefix: pf-c-spinner
---

## Examples
```hbs title=Basic
{{#> spinner}}Loading...{{/spinner}}
```

```hbs title=Multiple-sizes
{{#> spinner spinner--modifier="pf-m-sm"}}Loading...{{/spinner}}

{{#> spinner spinner--modifier="pf-m-md"}}Loading...{{/spinner}}

{{#> spinner spinner--modifier="pf-m-lg"}}Loading...{{/spinner}}

{{#> spinner spinner--modifier="pf-m-xl"}}Loading...{{/spinner}}
```
    
## Documentation
### Accessibility
| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `role="progressbar"` | `.pf-c-spinner` |  Indicates to assistive technologies that this is an indeterminate progress indicator. |
| `aria-valuetext="Loading..."` | `.pf-c-spinner` |  Describes content that is being loaded, while it is loading. |

Note: If the spinner is showing that loading of a particular region of a page is in process, the author should use `aria-describedby` to point to the status, and set the `aria-busy` attribute to `true` on the region until it is finished loading. 

Note: A live region must be present before changing its status in order for the change to be read. https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/ARIA_Live_Regions

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-spinner` | `<span>` |  Creates a spinner component. The default is an extra large spinner. **Required**|
| `.pf-c-spinner__clipper` | `<span>` |  Creates the spinning line. **Required**|
| `.pf-c-spinner__lead-ball` | `<span>` |  Rounds out the beginning of the spinning line. **Required**|
| `.pf-c-spinner__tail-ball` | `<span>` |  Rounds out the end of the spinning line. **Required**|
| `.pf-m-sm` | `.pf-c-spinner` |  Creates a small spinner. |
| `.pf-m-md` | `.pf-c-spinner` |  Creates a medium spinner. |
| `.pf-m-lg` | `.pf-c-spinner` |  Creates a large spinner. |
| `.pf-m-xl` | `.pf-c-spinner` |  Creates an extra-large spinner. |

