---
title: Chip
section: components
cssPrefix: pf-c-chip
---

## Examples
```hbs title=Basic

{{#> chip chip--type="div"}}
  {{#> chip-text chip-text--attribute='id="chip_one"'}}
    Chip
  {{/chip-text}}
  {{#> button button--modifier="pf-m-plain" button--attribute='aria-labelledby="remove_chip_one chip_one" aria-label="Remove" id="remove_chip_one"'}}
    <i class="fas fa-times" aria-hidden="true"></i>
  {{/button}}
{{/chip}}
<br>
<br>
{{#> chip chip--type="div"}}
  {{#> chip-text chip-text--attribute='id="chip_two"'}}
    Really long chip that goes on and on
  {{/chip-text}}
  {{#> button button--modifier="pf-m-plain" button--attribute='aria-labelledby="remove_chip_two chip_two" aria-label="Remove" id="remove_chip_two"'}}
    <i class="fas fa-times" aria-hidden="true"></i>
  {{/button}}
{{/chip}}
<br>
<br>
{{#> chip chip--type="div"}}
  {{#> chip-text chip-text--attribute='id="chip_three"'}}
    Chip
  {{/chip-text}}
  {{#> badge badge--modifier="pf-m-read"}}
    00
  {{/badge}}
  {{#> button button--modifier="pf-m-plain" button--attribute='aria-labelledby="remove_chip_three chip_three" aria-label="Remove" id="remove_chip_three"'}}
    <i class="fas fa-times" aria-hidden="true"></i>
  {{/button}}
{{/chip}}
<br>
<br>
{{#> chip chip--type="div" chip--modifier="pf-m-read-only"}}
  {{#> chip-text}}
    Read-only chip
  {{/chip-text}}
{{/chip}}
<br>
<br>
{{#> chip chip--type="button" chip--modifier="pf-m-overflow"}}
  {{#> chip-text}}
    Overflow chip
  {{/chip-text}}
{{/chip}}
```

## Documentation
### Overview
A Chip is used to display items that have been filtered or selected from a larger group. They comprise of a text element and a button component that is used to remove the chip from selection. When the text overflows it is truncated using ellipses. A chip can be grouped by using the "chip-group" component.


## Accessibility
| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `aria-label="[button label text]"` | `.pf-c-button` |  Provides an accessible name for the button when an icon is used instead of text. Required when an icon is used with no supporting text. |
| `aria-labelledby="[id value of .pf-c-button]"` | `.pf-c-button` | Gives the button an accessible name by referring to the element that provides the position of the button within a list. Required when the button is being removed. |
| `aria-hidden="true"` | `<i>` |  Hides the icon from assistive technologies. |

## Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-chip` | `<div>`, `<button>`, | Initiates the chip component. Use a `<button>` with overflow chips **Required** |
| `.pf-c-chip__text` | `<span>` | Initiates the text inside of the chip. **Required** |
| `.pf-c-button` | `.pf-c-chip <button>` | Initiates the button used to remove the chip. |
| `.pf-c-badge` | `<span>` | Initiates the badge inside the chip. |
| `.pf-m-overflow` | `button.pf-c-chip` | Applies styling of the overflow chip. |
