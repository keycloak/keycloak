---
title: 'Description list'
section: components
beta: true
cssPrefix: pf-c-description-list
---

## Examples

```hbs title=Default
{{> description-list__example description-list--title="Default DL" description-list--header="Test"}}
```

```hbs title=Default-2-col
{{> description-list__example description-list--title="Default 2 column DL" description-list--modifier="pf-m-2-col"}}
```

```hbs title=Default-3-col-on-lg
{{> description-list__example description-list--title="Default 3 column DL" description-list--modifier="pf-m-3-col-on-lg"}}
```

```hbs title=Horizontal
{{> description-list__example description-list--title="Horizontal DL" description-list--modifier="pf-m-horizontal"}}
```

```hbs title=Horizontal-2-col
{{> description-list__example description-list--title="Horizontal 2 column DL" description-list--modifier="pf-m-horizontal pf-m-2-col"}}
```

```hbs title=Horizontal-3-col-on-lg
{{> description-list__example description-list--title="Horizontal 3 column DL" description-list--modifier="pf-m-horizontal pf-m-3-col-on-lg"}}
```

## Responsive column definitions

```hbs title=Default-responsive-columns
{{> description-list__example description-list--title="Default responsive DL" description-list--modifier="pf-m-2-col-on-lg pf-m-3-col-on-xl"}}
```

```hbs title=Horizontal-responsive-columns
{{> description-list__example description-list--title="Horizontal responsive DL" description-list--modifier="pf-m-horizontal pf-m-2-col-on-lg pf-m-3-col-on-xl"}}
```

## Auto-column-width

```hbs title=Default-auto-columns-width
{{> description-list__example description-list--title="Auto column width DL" description-list--modifier="pf-m-auto-column-widths pf-m-3-col"}}
```

```hbs title=Horizontal-auto-column-width
{{> description-list__example description-list--title="Horizontal ato-fit DL" description-list--modifier="pf-m-horizontal pf-m-auto-column-widths pf-m-2-col-on-lg"}}
```

## Inline grid

```hbs title=Default-inline-grid
{{> description-list__example description-list--title="Default inline grid" description-list--modifier="pf-m-3-col pf-m-inline-grid"}}
```

<!-- ## Auto term with is only supported in FF currently

```hbs title=Horizontal-2-col-auto-term-width
{{> description-list__example description-list--title="Horizontal 2 column DL" description-list--modifier="pf-m-horizontal pf-m-auto-term-widths pf-m-2-col"}}
``` -->

## Documentation

### Accessibility

| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `title` | `.pf-c-description-list` | Provides an accessible title for the description list. **Required** |

### Usage

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-description-list` | `<dl>` | Initiates the description list component. **Required** |
| `.pf-c-description-list__group` | `<div>` | Initiates a description list component group. **Required** |
| `.pf-c-description-list__term` | `<dt>` | Initiates a description list component term. **Required** |
| `.pf-c-description-list__description` | `<dd>` | Initiates a description list component description. **Required** |
| `.pf-c-description-list__text` | `<div>` | Initiates a description list component text element. **Required** |
| `.pf-m-horizontal` | `.pf-c-description-list` | Modifies the description list component term and description pair to a horizontal layout. |
| `.pf-m-auto-column-widths` | `.pf-c-description-list` | Modifies the description list to format automatically. |
| `.pf-m-inline-grid` | `.pf-c-description-list` | Modifies the description list display to inline-grid. |
| `.pf-m-{1,2,3}-col{-on-[md, lg, xl, 2xl]}` | `.pf-c-description-list` | Modifies the description list number of columns. |

<!-- | `.pf-m-order[0-12]{-on-[breakpoint]}` | `.pf-c-description-list__group` | Modifies the order of the flex layout element. |
| `.pf-m-order-first{-on-[breakpoint]}` | `.pf-c-description-list__group` | Modifies the order of the flex layout element to -1. |
| `.pf-m-order-last{-on-[breakpoint]}` | `..pf-c-description-list__group` | Modifies the order of the flex layout element to $limit + 1. | -->
