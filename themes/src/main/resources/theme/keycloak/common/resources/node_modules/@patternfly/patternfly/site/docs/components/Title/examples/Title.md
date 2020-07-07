---
title: Title
section: components
cssPrefix: pf-c-title
---

## Examples
```hbs title=Size-modifiers
{{#> title titleType="h1" title--modifier="pf-m-4xl"}}
    4xl title
{{/title}}
{{#> title titleType="h1" title--modifier="pf-m-3xl"}}
    3xl title
{{/title}}
{{#> title titleType="h1" title--modifier="pf-m-2xl"}}
    2xl title
{{/title}}
{{#> title titleType="h1" title--modifier="pf-m-xl"}}
    xl title
{{/title}}
{{#> title titleType="h1" title--modifier="pf-m-lg"}}
    lg title
{{/title}}
{{#> title titleType="h1" title--modifier="pf-m-md"}}
    md title
{{/title}}
```

## Documentation
### Overview
The title component styles font-size, font-weight, and line-height to titles.

The content component defines margin on headers. To regain the same spacing use, spacer utility classes:

| Title | Margin top | Margin bottom |
| -- | -- | -- |
| 4xl | `.pf-u-mt-lg` | `.pf-u-mb-sm` |
| 3xl | `.pf-u-mt-lg` | `.pf-u-mb-sm` |
| 2xl | `.pf-u-mt-lg` | `.pf-u-mb-sm` |
| xl | `.pf-u-mt-lg` | `.pf-u-mb-sm` |
| lg | `.pf-u-mt-lg` | `.pf-u-mb-sm` |
| md | `.pf-u-mt-lg` | `.pf-u-mb-sm` |

### Usage
| Class | Applied | Outcome |
| -- | -- | -- |
| `.pf-c-title` | `*` |  Initiates a title. Always use it with a modifier class. |
| `.pf-m-4xl` | `.pf-c-title` | Modifies for 4xl size |
| `.pf-m-3xl` | `.pf-c-title` | Modifies for 3xl size |
| `.pf-m-2xl` | `.pf-c-title` | Modifies for 2xl size |
| `.pf-m-xl` | `.pf-c-title` | Modifies for xl size |
| `.pf-m-lg` | `.pf-c-title` | Modifies for lg size |
| `.pf-m-md` | `.pf-c-title` | Modifies for md size |
