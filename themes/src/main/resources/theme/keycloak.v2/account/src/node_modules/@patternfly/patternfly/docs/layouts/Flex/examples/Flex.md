---
id: Flex
section: layouts
cssPrefix: pf-l-flex
---import './Flex.css'

## Introduction

The flex layout is based on the CSS Flex properties where the layout determines how a flex item will grow or shrink to fit the space available in its container. The system relies on a default spacer value `--pf-l-flex--spacer--base`, whose value is `--pf-global--spacer--md` or `16px` that is applied to flex items. By default, `flex-wrap` is set to `wrap` and `align-items` is set to `baseline`.

### Default spacing

-   Flex items (not last child): `margin-right: 16px`.
-   Nested `.pf-l-flex` containers (not last child): `margin-right: 16px`.
-   `.pf-m-column` direct descendants (not last child): `margin-bottom: 16px`.
-   `.pf-m-column` nested `.pf-l-flex` containers (not last child): `margin-bottom: 16px`.

## Features

-   `.pf-l-flex` is infinitely nestable and can be used to group items within.
-   `.pf-m-spacer-{xs,sm,md,lg,xl,2xl,3xl}` can be applied to parent or direct children and changes the spacer value for only the element to which it is applied. Responsive spacers can be used by appending `{-on-[breakpoint]}` to `.pf-m-spacer-{size}`. Example: `.pf-m-spacer-lg-on-xl`.
-   `.pf-m-space-items-{xs,sm,md,lg,xl,2xl,3xl}` can be applied to `.pf-l-flex` only and changes the spacing of direct children only. Responsive spacers can be used by appending `{-on-[breakpoint]}` to `.pf-m-space-items-{size}`. Example: `.pf-m-space-items-lg-on-xl`.

### Breakpoints

-   [Breakpoints](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes) `-on-sm`, `-on-md`, `-on-lg`, `-on-xl`, and `-on-2xl` are provided.

### Usefulness

-   Use when content dictates layout and elements wrap when necessary.
-   Use when a rigid grid is not necessary/wanted.

### Differences from utility class

-   It contains multiple css declarations and does not use the !important tag.
-   It does not require wrapping elements in columns or rows.
-   It breaks the dependency upon adding utility classes to each child.
-   It can be applied to container elements or components.

## Examples

### Basic

```html
<h3>
  Basic flex -
  <code>.pf-l-flex</code>.
</h3>
<div class="pf-l-flex">
  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
</div>
<br />
<h3>
  Flex nesting -
  <code>.pf-l-flex > .pf-l-flex</code>.
</h3>
<div class="pf-l-flex">
  <div class="pf-l-flex">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>
  <div class="pf-l-flex">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>
</div>
<br />
<h3>Nested flex and items.</h3>
<div class="pf-l-flex">
  <div class="pf-l-flex">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>

  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>

  <div class="pf-l-flex">
    <div class="pf-l-flex__item">Flex item</div>
  </div>
</div>

```

The CSS approach, by keeping specificity low on base class properties and resetting css variable values at higher specificities, allows any spacer property to be overwritten with a single selector (specificity of 10 or greater).

### Usage

| Class              | Applied to       | Outcome                                 |
| ------------------ | ---------------- | --------------------------------------- |
| `.pf-l-flex`       | `*`              | Initiates the flex layout. **Required** |
| `.pf-l-flex__item` | `.pf-l-flex > *` | Initiates a flex item. **Required**     |

### Spacing

```html
<h3>
  Individually spaced items -
  <code>.pf-m-spacer-{xs,sm,md,lg,xl,2xl,3xl}</code>.
</h3>
<div class="pf-l-flex">
  <div class="pf-l-flex__item pf-m-spacer-none">Item - none</div>
  <div class="pf-l-flex__item pf-m-spacer-xs">Item - xs</div>
  <div class="pf-l-flex__item pf-m-spacer-sm">Item - sm</div>
  <div class="pf-l-flex__item pf-m-spacer-md">Item - md</div>
  <div class="pf-l-flex__item pf-m-spacer-lg">Item - lg</div>
  <div class="pf-l-flex__item pf-m-spacer-xl">Item - xl</div>
  <div class="pf-l-flex__item pf-m-spacer-2xl">Item - 2xl</div>
  <div class="pf-l-flex__item pf-m-spacer-3xl">Item - 3xl</div>
</div>
<br />
<h3>
  Flex with modified spacing -
  <code>.pf-m-space-items-xl</code>.
</h3>
<div class="pf-l-flex pf-m-space-items-xl">
  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
</div>
<br />
<h3>
  Flex with modified spacing -
  <code>.pf-m-space-items-none</code>.
</h3>
<div class="pf-l-flex pf-m-space-items-none">
  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
</div>

```

**Applying `.pf-m-spacer-{size}` to direct descendants of `.pf-l-flex` will override css variable value.**

**Applying `.pf-m-space-items-{size}` to `.pf-l-flex` will override css variable values for direct descendants, excluding last child. This spacing can be overridden for direct descendant with `.pf-m-spacer-{size}`.**

### Usage

| Class                                                                 | Applied to                                    | Outcome                                               |
| --------------------------------------------------------------------- | --------------------------------------------- | ----------------------------------------------------- |
| `.pf-m-spacer-{none, xs, sm, md, lg, xl, 2xl}{-on-[breakpoint]}`      | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies a nested flex layout or a flex item spacing. |
| `.pf-m-space-items-{none, xs, sm, md, lg, xl, 2xl}{-on-[breakpoint]}` | `.pf-l-flex`                                  | Modifies the flex layout direct descendant spacing.   |

### Layout modifiers

```html
<h3>
  Default layout
  <code>.pf-l-flex</code>.
</h3>
<div class="pf-l-flex ws-example-flex-border">
  <div class="pf-l-flex__item">Flex item</div>

  <div class="pf-l-flex__item">Flex item</div>

  <div class="pf-l-flex__item">Flex item</div>
</div>
<br />
<h3>
  Inline flex
  <code>.pf-m-inline-flex</code>.
</h3>
<div class="pf-l-flex pf-m-inline-flex ws-example-flex-border">
  <div class="pf-l-flex__item">Flex item</div>

  <div class="pf-l-flex__item">Flex item</div>

  <div class="pf-l-flex__item">Flex item</div>
</div>
<br />
<h3>
  Adjusting width with
  <code>.pf-m-grow</code>. In this example, the first group is set to
  <code>.pf-m-grow</code> and will occupy the remaining available space.
</h3>
<div class="pf-l-flex">
  <div
    class="pf-l-flex pf-m-grow ws-example-flex-border"
    data-label=".pf-m-grow"
  >
    <div class="pf-l-flex__item">Flex item</div>

    <div class="pf-l-flex__item">Flex item</div>

    <div class="pf-l-flex__item">Flex item</div>
  </div>
  <div class="pf-l-flex ws-example-flex-border">
    <div class="pf-l-flex__item">Flex item</div>

    <div class="pf-l-flex__item">Flex item</div>
  </div>
  <div class="pf-l-flex ws-example-flex-border">
    <div class="pf-l-flex__item">Flex item</div>
  </div>
</div>
<br />
<h3>
  Adjusting width with
  <code>.pf-m-flex-1</code>. In this example, all groups are set to
  <code>.pf-m-flex-1</code>. They will share available space equally.
</h3>
<div class="pf-l-flex">
  <div
    class="pf-l-flex pf-m-flex-1 ws-example-flex-border"
    data-label=".pf-m-flex-1"
  >
    <div class="pf-l-flex__item">Flex item</div>

    <div class="pf-l-flex__item">Flex item</div>

    <div class="pf-l-flex__item">Flex item</div>
  </div>
  <div
    class="pf-l-flex pf-m-flex-1 ws-example-flex-border"
    data-label=".pf-m-flex-1"
  >
    <div class="pf-l-flex__item">Flex item</div>

    <div class="pf-l-flex__item">Flex item</div>
  </div>
  <div
    class="pf-l-flex pf-m-flex-1 ws-example-flex-border"
    data-label=".pf-m-flex-1"
  >
    <div class="pf-l-flex__item">Flex item</div>
  </div>
</div>
<br />
<h3>
  Specifying column widths with
  <code>.pf-m-flex-{1,2,3}</code>.
</h3>
<div class="pf-l-flex">
  <div
    class="pf-l-flex pf-m-flex-1 ws-example-flex-border"
    data-label=".pf-m-flex-1"
  >
    <div class="pf-l-flex__item">Flex item</div>
  </div>
  <div
    class="pf-l-flex pf-m-flex-2 ws-example-flex-border"
    data-label=".pf-m-flex-2"
  >
    <div class="pf-l-flex__item">Flex item</div>

    <div class="pf-l-flex__item">Flex item</div>
  </div>
  <div
    class="pf-l-flex pf-m-flex-3 ws-example-flex-border"
    data-label=".pf-m-flex-3"
  >
    <div class="pf-l-flex__item">Flex item</div>

    <div class="pf-l-flex__item">Flex item</div>

    <div class="pf-l-flex__item">Flex item</div>
  </div>
</div>

```

### Usage

| Class                                  | Applied to                                    | Outcome                                                                       |
| -------------------------------------- | --------------------------------------------- | ----------------------------------------------------------------------------- |
| `.pf-m-inline-flex{-on-[breakpoint]}`  | `.pf-l-flex`                                  | Modifies the flex layout display property to inline-flex.                     |
| `.pf-m-grow{-on-[breakpoint]}`         | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies a nested flex layout or flex item flex-grow property to 1.           |
| `.pf-m-shrink{-on-[breakpoint]}`       | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies a nested flex layout or flex item flex-shrink property to 1.         |
| `.pf-m-full-width{-on-[breakpoint]}`   | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies a nested flex layout or flex item flex item to full width of parent. |
| `.pf-m-flex-1{-on-[breakpoint]}`       | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies a nested flex layout or flex item flex shorthand property to 1 0 0.  |
| `.pf-m-flex-2{-on-[breakpoint]}`       | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies a nested flex layout or flex item flex shorthand property to 2 0 0.  |
| `.pf-m-flex-3{-on-[breakpoint]}`       | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies a nested flex layout or flex item flex shorthand property to 3 0 0.  |
| `.pf-m-flex-4{-on-[breakpoint]}`       | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies a nested flex layout or flex item flex shorthand property to 4 0 0.  |
| `.pf-m-flex-default{-on-[breakpoint]}` | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Resets a nested flex layout or flex item flex shorthand property to 0 1 auto. |
| `.pf-m-flex-none{-on-[breakpoint]}`    | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies a nested flex layout or flex item flex shorthand property to none.   |

### Column layout modifiers

```html
<h3>
  Flex column layout. When
  <code>.pf-m-column</code> is applied to
  <code>.pf-l-flex</code>, spacing will be applied to margin-bottom for direct descendants.
</h3>
<div class="pf-l-flex pf-m-column">
  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
</div>
<br />
<h3>
  Stacking
  <code>.pf-l-flex</code> elements.
</h3>
<div class="pf-l-flex pf-m-column">
  <div class="pf-l-flex">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>
  <div class="pf-l-flex">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>
  <div class="pf-l-flex">
    <div class="pf-l-flex__item">Flex item</div>
  </div>
</div>
<br />
<h3>
  Nesting
  <code>.pf-l-flex</code> elements and setting to
  <code>.pf-m-column</code>.
</h3>
<div class="pf-l-flex">
  <div class="pf-l-flex pf-m-column">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>
  <div class="pf-l-flex pf-m-column">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>
</div>

```

### Usage

| Class                            | Applied to   | Outcome                                     |
| -------------------------------- | ------------ | ------------------------------------------- |
| `.pf-m-column{-on-[breakpoint]}` | `.pf-l-flex` | Modifies flex-direction property to column. |

### Responsive layout modifiers

```html
<h3>
  Switching between flex-direction column and row at breakpoints (
  <code>-on-lg</code>).
</h3>
<div class="pf-l-flex pf-m-column pf-m-row-on-lg">
  <div class="pf-l-flex">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>

  <div class="pf-l-flex">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>
</div>
<br />
<h3>
  Switching between flex-direction column and row at breakpoints (
  <code>-on-lg</code>). If content is likely to wrap, modifiers will need to be used to control width. The example below wraps because the flex item expands in response to long paragraph text.
</h3>
<div class="pf-l-flex pf-m-column pf-m-row-on-lg">
  <div class="pf-l-flex">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">
      <b>Because this text is long enough to wrap, this item's width will force the adjacent item to wrap.</b> Lorem ipsum dolor sit amet consectetur adipisicing elit. Est animi modi temporibus, alias qui obcaecati ullam dolor nam, nulla magni iste rem praesentium numquam provident amet ut nesciunt harum accusamus.
    </div>
  </div>

  <div class="pf-l-flex pf-m-column">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>
</div>
<br />
<h3>
  Switching between flex-direction column and row at breakpoints (
  <code>-on-lg</code>). To control the width of the flex item, set
  <code>.pf-m-flex-1</code> on the flex group containing the long paragraph text.
</h3>
<div class="pf-l-flex pf-m-column pf-m-row-on-lg">
  <div class="pf-l-flex pf-m-flex-1">
    <div class="pf-l-flex__item">Flex item</div>
    <div
      class="pf-l-flex__item pf-m-flex-1"
    >Lorem ipsum dolor sit amet consectetur adipisicing elit. Est animi modi temporibus, alias qui obcaecati ullam dolor nam, nulla magni iste rem praesentium numquam provident amet ut nesciunt harum accusamus.</div>
  </div>

  <div class="pf-l-flex pf-m-column">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>
</div>

```

### Usage

| Class                            | Applied to   | Outcome                                     |
| -------------------------------- | ------------ | ------------------------------------------- |
| `.pf-m-column{-on-[breakpoint]}` | `.pf-l-flex` | Modifies flex-direction property to column. |
| `.pf-m-row{-on-[breakpoint]}`    | `.pf-l-flex` | Modifies flex-direction property to row.    |

### Alignment

```html
<h3>
  Aligning right with
  <code>.pf-m-align-right</code>. This solution will always align element right by setting margin-left: auto, including when wrapped.
</h3>
<div class="pf-l-flex ws-example-flex-border">
  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item pf-m-align-right">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
</div>
<br />
<h3>Align right on single item.</h3>
<div class="pf-l-flex ws-example-flex-border">
  <div class="pf-l-flex__item pf-m-align-right">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
</div>
<br />
<h3>Align right on multiple groups.</h3>
<div class="pf-l-flex">
  <div class="pf-l-flex">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>
  <div class="pf-l-flex pf-m-align-right">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>
  <div class="pf-l-flex pf-m-align-right">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>
</div>
<br />
<h3>
  Using
  <code>.pf-m-flex-1</code> to align adjacent content.
</h3>
<div class="pf-l-flex">
  <div class="pf-l-flex pf-m-flex-1">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>
  <div class="pf-l-flex">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>
</div>
<br />
<h3>
  Aligning nested columns with
  <code>.pf-m-align-self-flex-end</code>.
</h3>
<div class="pf-l-flex">
  <div class="pf-l-flex pf-m-column">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>
  <div class="pf-l-flex pf-m-column pf-m-align-self-flex-end">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>
</div>
<br />
<h3>
  Aligning nested columns with
  <code>.pf-m-align-self-center</code>.
</h3>
<div class="pf-l-flex">
  <div class="pf-l-flex pf-m-column">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>
  <div class="pf-l-flex pf-m-column pf-m-align-self-center">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>
</div>
<br />
<h3>
  Aligning nested columns with
  <code>.pf-m-align-self-baseline</code>.
</h3>
<div class="pf-l-flex">
  <div class="pf-l-flex pf-m-column">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>
  <div class="pf-l-flex pf-m-column pf-m-align-self-baseline">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>
</div>
<br />
<h3>
  Aligning nested columns with
  <code>.pf-m-align-self-stretch</code>.
</h3>
<div class="pf-l-flex">
  <div class="pf-l-flex pf-m-column">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>
  <div class="pf-l-flex pf-m-column pf-m-align-self-stretch">
    <div class="pf-l-flex__item">Flex item</div>
    <div class="pf-l-flex__item">Flex item</div>
  </div>
</div>

```

### Usage

| Class                                              | Applied to                                    | Outcome                                     |
| -------------------------------------------------- | --------------------------------------------- | ------------------------------------------- |
| `.pf-m-align-right{-on-[breakpoint]}`              | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies margin-left property to auto.      |
| `.pf-m-align-left{-on-[breakpoint]}`               | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Resets margin-left property 0.              |
| `.pf-m-align-self-flex-start{-on-[breakpoint]}`    | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies align-self property to flex-start. |
| `.pf-m-align-self-flex-end{-on-[breakpoint]}`      | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies align-self property to flex-end.   |
| `.pf-m-align-self-flex-center{-on-[breakpoint]}`   | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies align-self property to center.     |
| `.pf-m-align-self-flex-baseline{-on-[breakpoint]}` | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies align-self property to baseline.   |
| `.pf-m-align-self-flex-stretch{-on-[breakpoint]}`  | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies align-self property to stretch.    |

### Justification

```html
<h3>
  Justify content with
  <code>.pf-m-justify-content-flex-end</code>.
</h3>
<div class="pf-l-flex pf-m-justify-content-flex-end ws-example-flex-border">
  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
</div>
<br />
<h3>
  Justify content with
  <code>.pf-m-justify-content-space-between</code>.
</h3>
<div
  class="pf-l-flex pf-m-justify-content-space-between ws-example-flex-border"
>
  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
</div>
<br />
<h3>
  Justify content with
  <code>.pf-m-justify-content-flex-start</code>.
</h3>
<div class="pf-l-flex pf-m-justify-content-flex-start ws-example-flex-border">
  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
  <div class="pf-l-flex__item">Flex item</div>
</div>

```

### Ordering

Ordering - Ordering can be applied to nested <code>.pf-l-flex</code> and <code>.pf-l-flex\_\_item</code>s. Spacing may need to be managed based on how items are ordered. Because order could apply to an innumerable number of elements, order is set inline as `--pf-l-flex--item--Order{-on-[breakpoint]}: {order}`.

### First last ordering

```html
<div class="pf-l-flex">
  <div
    class="pf-l-flex__item pf-m-spacer-none"
    style="--pf-l-flex--item--Order: 2;"
  >Item A</div>
  <div class="pf-l-flex__item">Item B</div>
  <div
    class="pf-l-flex__item pf-m-spacer-md"
    style="--pf-l-flex--item--Order: -1;"
  >Item C</div>
</div>

```

### Responsive first last ordering

```html
<div class="pf-l-flex">
  <div
    class="pf-l-flex__item pf-m-spacer-none-on-lg"
    style="--pf-l-flex--item--Order-on-lg: 2;"
  >Item A</div>
  <div
    class="pf-l-flex__item pf-m-spacer-none-on-md pf-m-spacer-md-on-lg"
    style="--pf-l-flex--item--Order: -1; --pf-l-flex--item--Order-on-md: 1;"
  >Item B</div>
  <div
    class="pf-l-flex__item pf-m-spacer-md"
    style="--pf-l-flex--item--Order-on-md: -1;"
  >Item C</div>
</div>

```

### Ordering

```html
<div class="pf-l-flex">
  <div class="pf-l-flex pf-m-spacer-none" style="--pf-l-flex--item--Order: 1;">
    <div
      class="pf-l-flex__item pf-m-spacer-none"
      style="--pf-l-flex--item--Order: 3;"
    >Set 1, Item A</div>
    <div
      class="pf-l-flex__item"
      style="--pf-l-flex--item--Order: 1;"
    >Set 1, Item B</div>
    <div class="pf-l-flex__item">Set 1, Item C</div>
    <div class="pf-l-flex__item pf-m-order-2 pf-m-spacer-md">Set 1, Item D</div>
  </div>
  <div class="pf-l-flex pf-m-spacer-md">
    <div
      class="pf-l-flex__item pf-m-spacer-none"
      style="--pf-l-flex--item--Order: 3;"
    >Set 2, Item A</div>
    <div
      class="pf-l-flex__item"
      style="--pf-l-flex--item--Order-on-lg: 1;"
    >Set 2, Item B</div>
    <div class="pf-l-flex__item">Set 2, Item C</div>
    <div
      class="pf-l-flex__item pf-m-spacer-md"
      style="--pf-l-flex--item--Order: 2;"
    >Set 2, Item D</div>
  </div>
</div>

```

### Responsive ordering

```html
<div class="pf-l-flex">
  <div
    class="pf-l-flex pf-m-spacer-none"
    style="--pf-l-flex--item--Order-on-lg: 1;"
  >
    <div
      class="pf-l-flex__item"
      style="--pf-l-flex--item--Order-on-md: 2;"
    >Set 1, Item A</div>
    <div
      class="pf-l-flex__item"
      style="--pf-l-flex--item--Order-on-md: -1;"
    >Set 1, Item B</div>
    <div
      class="pf-l-flex__item"
      style="--pf-l-flex--item--Order-on-xl: 1;"
    >Set 1, Item C</div>
    <div
      class="pf-l-flex__item pf-m-spacer-none-on-xl"
      style="--pf-l-flex--item--Order-on-xl: 2;"
    >Set 1, Item D</div>
  </div>
  <div class="pf-l-flex pf-m-spacer-md-on-lg">
    <div
      class="pf-l-flex__item pf-m-spacer-none"
      style="--pf-l-flex--item--Order: 3;"
    >Set 2, Item A</div>
    <div
      class="pf-l-flex__item"
      style="--pf-l-flex--item--Order: 1;"
    >Set 2, Item B</div>
    <div class="pf-l-flex__item">Set 2, Item C</div>
    <div
      class="pf-l-flex__item pf-m-spacer-md"
      style="--pf-l-flex--item--Order: 2;"
    >Set 2, Item D</div>
  </div>
</div>

```

### List type

```html
<ul class="pf-l-flex">
  <li class="pf-l-flex__item">Flex item</li>
  <li class="pf-l-flex__item">Flex item</li>
  <li class="pf-l-flex__item">Flex item</li>
  <li class="pf-l-flex__item">Flex item</li>
</ul>

```

### Usage

| Class                                                        | Applied to   | Outcome                                                                                             |
| ------------------------------------------------------------ | ------------ | --------------------------------------------------------------------------------------------------- |
| `.pf-m-justify-content-flex-end{-on-[breakpoint]}`           | `.pf-l-flex` | Modifies justification property and descendant spacing.                                             |
| `.pf-m-justify-content-flex-space-between{-on-[breakpoint]}` | `.pf-l-flex` | Modifies justification property and descendant spacing.                                             |
| `.pf-m-justify-content-flex-start{-on-[breakpoint]}`         | `.pf-l-flex` | Modifies justification property and descendant spacing, used primarily to reset spacing to default. |

## Documentation

### Usage

| Class                                                   | Applied to                                    | Outcome                                                                       |
| ------------------------------------------------------- | --------------------------------------------- | ----------------------------------------------------------------------------- |
| `.pf-l-flex`                                            | `*`                                           | Initiates the flex layout. **Required**                                       |
| `.pf-l-flex__item`                                      | `.pf-l-flex > *`                              | Initiates a flex item. **Required**                                           |
| `.pf-m-flex{-on-[breakpoint]}`                          | `.pf-l-flex`                                  | Initializes or resets the flex layout display property to flex.               |
| `.pf-m-inline-flex{-on-[breakpoint]}`                   | `.pf-l-flex`                                  | Modifies the flex layout display property to inline-flex.                     |
| `.pf-m-grow{-on-[breakpoint]}`                          | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies a nested flex layout or flex item flex-grow property to 1.           |
| `.pf-m-shrink{-on-[breakpoint]}`                        | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies a nested flex layout or flex item flex-shrink property to 1.         |
| `.pf-m-full-width{-on-[breakpoint]}`                    | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies a nested flex layout or flex item flex item to full width of parent. |
| `.pf-m-flex-1{-on-[breakpoint]}`                        | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies a nested flex layout or flex item flex shorthand property to 1 0 0.  |
| `.pf-m-flex-2{-on-[breakpoint]}`                        | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies a nested flex layout or flex item flex shorthand property to 2 0 0.  |
| `.pf-m-flex-3{-on-[breakpoint]}`                        | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies a nested flex layout or flex item flex shorthand property to 3 0 0.  |
| `.pf-m-flex-4{-on-[breakpoint]}`                        | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies a nested flex layout or flex item flex shorthand property to 4 0 0.  |
| `.pf-m-flex-default{-on-[breakpoint]}`                  | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Resets a nested flex layout or flex item flex shorthand property to 0 1 auto. |
| `.pf-m-flex-none{-on-[breakpoint]}`                     | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies a nested flex layout or flex item flex shorthand property to none.   |
| `.pf-m-column-reverse{-on-[breakpoint]}`                | `.pf-l-flex`                                  | Modifies the flex layout flex-direction property to column-reverse.           |
| `.pf-m-row-reverse{-on-[breakpoint]}`                   | `.pf-l-flex`                                  | Modifies the flex layout flex-direction property to row-reverse.              |
| `.pf-m-wrap{-on-[breakpoint]}`                          | `.pf-l-flex`                                  | Modifies the flex layout flex-wrap property to wrap.                          |
| `.pf-m-wrap-reverse{-on-[breakpoint]}`                  | `.pf-l-flex`                                  | Modifies the flex layout flex-wrap property to wrap-reverse.                  |
| `.pf-m-nowrap{-on-[breakpoint]}`                        | `.pf-l-flex`                                  | Modifies the flex layout flex-wrap property to nowrap.                        |
| `.pf-m-justify-content-flex-start{-on-[breakpoint]}`    | `.pf-l-flex`                                  | Modifies the flex layout justify-content property to flex-start.              |
| `.pf-m-justify-content-flex-end{-on-[breakpoint]}`      | `.pf-l-flex`                                  | Modifies the flex layout justify-content property to flex-end.                |
| `.pf-m-justify-content-center{-on-[breakpoint]}`        | `.pf-l-flex`                                  | Modifies the flex layout justify-content property to center.                  |
| `.pf-m-justify-content-space-between{-on-[breakpoint]}` | `.pf-l-flex`                                  | Modifies the flex layout justify-content property to space-between.           |
| `.pf-m-justify-content-space-around{-on-[breakpoint]}`  | `.pf-l-flex`                                  | Modifies the flex layout justify-content property to space-around.            |
| `.pf-m-justify-content-space-evenly{-on-[breakpoint]}`  | `.pf-l-flex`                                  | Modifies the flex layout justify-content property to space-evenly.            |
| `.pf-m-align-items-flex-start{-on-[breakpoint]}`        | `.pf-l-flex`                                  | Modifies the flex layout align-items property to flex-start.                  |
| `.pf-m-align-items-flex-end{-on-[breakpoint]}`          | `.pf-l-flex`                                  | Modifies the flex layout align-items property to flex-end.                    |
| `.pf-m-align-items-center{-on-[breakpoint]}`            | `.pf-l-flex`                                  | Modifies the flex layout align-items property to center.                      |
| `.pf-m-align-items-stretch{-on-[breakpoint]}`           | `.pf-l-flex`                                  | Modifies the flex layout align-items property to stretch.                     |
| `.pf-m-align-items-baseline{-on-[breakpoint]}`          | `.pf-l-flex`                                  | Modifies the flex layout align-items property to baseline.                    |
| `.pf-m-align-content-flex-start{-on-[breakpoint]}`      | `.pf-l-flex`                                  | Modifies the flex layout align-content property to flex-start.                |
| `.pf-m-align-content-flex-end{-on-[breakpoint]}`        | `.pf-l-flex`                                  | Modifies the flex layout align-content property to flex-end.                  |
| `.pf-m-align-content-center{-on-[breakpoint]}`          | `.pf-l-flex`                                  | Modifies the flex layout align-content property to center.                    |
| `.pf-m-align-content-stretch{-on-[breakpoint]}`         | `.pf-l-flex`                                  | Modifies the flex layout align-content property to stretch.                   |
| `.pf-m-align-content-space-between{-on-[breakpoint]}`   | `.pf-l-flex`                                  | Modifies the flex layout align-content property to space-between.             |
| `.pf-m-align-content-space-around{-on-[breakpoint]}`    | `.pf-l-flex`                                  | Modifies the flex layout align-content property to space-around.              |
| `.pf-m-align-left{-on-[breakpoint]}`                    | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Resets the flex layout element margin-left property to 0.                     |
| `.pf-m-align-right{-on-[breakpoint]}`                   | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies the flex layout element margin-left property to auto.                |
| `--pf-l-flex--item--Order{-on-[breakpoint]}: {order}`   | `.pf-l-flex > .pf-l-flex`, `.pf-l-flex__item` | Modifies the flex layout element order property.                              |

### Spacer system

| Class                                                                      | Applied to                                    | Outcome                                               |
| -------------------------------------------------------------------------- | --------------------------------------------- | ----------------------------------------------------- |
| `.pf-m-spacer-{none, xs, sm, md, lg, xl, 2xl}{-on-[breakpoint]}`           | `.pf-l-flex`, `.pf-l-flex > .pf-l-flex__item` | Modifies a nested flex layout or a flex item spacing. |
| `.pf-m-item-space-items-{none, xs, sm, md, lg, xl, 2xl}{-on-[breakpoint]}` | `.pf-l-flex`                                  | Modifies the flex layout direct descendant spacing.   |
