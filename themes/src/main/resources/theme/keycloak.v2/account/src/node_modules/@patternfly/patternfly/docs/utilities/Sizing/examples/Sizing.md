---
id: Sizing
section: utilities
---import './Sizing.css'

## Examples

### Width base and percentage units

```html
<div class="ws-example-u-sizing-item pf-u-w-0">.pf-u-w-0</div>
<div class="ws-example-u-sizing-item pf-u-w-25">.pf-u-w-25</div>
<div class="ws-example-u-sizing-item pf-u-w-50">.pf-u-w-50</div>
<div class="ws-example-u-sizing-item pf-u-w-75">.pf-u-w-75</div>
<div class="ws-example-u-sizing-item pf-u-w-100">.pf-u-w-100</div>
<div class="ws-example-u-sizing-item pf-u-w-initial">.pf-u-w-initial (auto)</div>
<div class="ws-example-u-sizing-item pf-u-w-inherit">.pf-u-w-inherit</div>

```

### Usage

[Breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes) is optional. Breakpoints: base (no breakpoint value), `-on-sm`, `-on-md`, `-on-lg`, and `-on-xl`. **Example .pf-u-w-initial-on-lg**

| Class                               | Applied to | Outcome                         |
| ----------------------------------- | ---------- | ------------------------------- |
| `.pf-u-w-initial{-on-[breakpoint]}` | `*`        | Sets width: initial (auto)      |
| `.pf-u-w-inherit{-on-[breakpoint]}` | `*`        | Sets width: inherit             |
| `.pf-u-w-0{-on-[breakpoint]}`       | `*`        | Sets width: 0%                  |
| `.pf-u-w-25{-on-[breakpoint]}`      | `*`        | Sets width: 25%                 |
| `.pf-u-w-33{-on-[breakpoint]}`      | `*`        | Sets width: calc(100% / 3)      |
| `.pf-u-w-50{-on-[breakpoint]}`      | `*`        | Sets width: 50%                 |
| `.pf-u-w-66{-on-[breakpoint]}`      | `*`        | Sets width: calc(100% / 3 \* 2) |
| `.pf-u-w-75{-on-[breakpoint]}`      | `*`        | Sets width: 75%                 |
| `.pf-u-w-100{-on-[breakpoint]}`     | `*`        | Sets width: 100%                |

### Width viewport units

```html isFullscreen
<div class="ws-example-u-sizing-item pf-u-w-25vw">.pf-u-w-25vw</div>
<div class="ws-example-u-sizing-item pf-u-w-33vw">.pf-u-w-33vw</div>
<div class="ws-example-u-sizing-item pf-u-w-50vw">.pf-u-w-50vw</div>
<div class="ws-example-u-sizing-item pf-u-w-66vw">.pf-u-w-66vw</div>
<div class="ws-example-u-sizing-item pf-u-w-75vw">.pf-u-w-75vw</div>
<div class="ws-example-u-sizing-item pf-u-w-100vw">.pf-u-w-100vw</div>

```

### Usage

[Breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes) is optional. Breakpoints: base (no breakpoint value), -on-sm, -on-md, -on-lg, -on-xl. **Example .pf-u-w-25vw-on-lg**

| Class                             | Applied to | Outcome                          |
| --------------------------------- | ---------- | -------------------------------- |
| `.pf-u-w-25vw{-on-[breakpoint]}`  | `*`        | Sets width: 25vw                 |
| `.pf-u-w-33vw{-on-[breakpoint]}`  | `*`        | Sets width: calc(100vw / 3)      |
| `.pf-u-w-50vw{-on-[breakpoint]}`  | `*`        | Sets width: 50vw                 |
| `.pf-u-w-66vw{-on-[breakpoint]}`  | `*`        | Sets width: calc(100vw / 3 \* 2) |
| `.pf-u-w-75vw{-on-[breakpoint]}`  | `*`        | Sets width: 75vw                 |
| `.pf-u-w-100vw{-on-[breakpoint]}` | `*`        | Sets width: 100vw                |

### Height base and percentage units

```html
<div
  class="ws-example-u-sizing-item pf-u-h-0 pf-u-display-inline-block"
>.pf-u-h-0</div>
<div
  class="ws-example-u-sizing-item pf-u-h-25 pf-u-display-inline-block"
>.pf-u-h-25</div>
<div
  class="ws-example-u-sizing-item pf-u-h-50 pf-u-display-inline-block"
>.pf-u-h-50</div>
<div
  class="ws-example-u-sizing-item pf-u-h-75 pf-u-display-inline-block"
>.pf-u-h-75</div>
<div
  class="ws-example-u-sizing-item pf-u-h-100 pf-u-display-inline-block"
>.pf-u-h-100</div>
<div
  class="ws-example-u-sizing-item pf-u-h-initial pf-u-display-inline-block"
>.pf-u-h-initial (auto)</div>
<div
  class="ws-example-u-sizing-item pf-u-h-inherit pf-u-display-inline-block"
>.pf-u-h-inherit</div>

```

### Usage

[Breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes) is optional. Breakpoints: base (no breakpoint value), -on-sm, -on-md, -on-lg, -on-xl. **Example .pf-u-h-initial-on-lg**

| Class                               | Applied to | Outcome                          |
| ----------------------------------- | ---------- | -------------------------------- |
| `.pf-u-h-initial{-on-[breakpoint]}` | `*`        | Sets height: initial (auto)      |
| `.pf-u-h-inherit{-on-[breakpoint]}` | `*`        | Sets height: inherit             |
| `.pf-u-h-0{-on-[breakpoint]}`       | `*`        | Sets height: 0%                  |
| `.pf-u-h-25{-on-[breakpoint]}`      | `*`        | Sets height: 25%                 |
| `.pf-u-h-33{-on-[breakpoint]}`      | `*`        | Sets height: calc(100% / 3)      |
| `.pf-u-h-50{-on-[breakpoint]}`      | `*`        | Sets height: 50%                 |
| `.pf-u-h-66{-on-[breakpoint]}`      | `*`        | Sets height: calc(100% / 3 \* 2) |
| `.pf-u-h-75{-on-[breakpoint]}`      | `*`        | Sets height: 75%                 |
| `.pf-u-h-100{-on-[breakpoint]}`     | `*`        | Sets height: 100%                |

### Height viewport units

```html isFullscreen
<div
  class="ws-example-u-sizing-item pf-u-h-25vh pf-u-display-inline-block"
>.pf-u-h-25vh</div>
<div
  class="ws-example-u-sizing-item pf-u-h-33vh pf-u-display-inline-block"
>.pf-u-h-33vh</div>
<div
  class="ws-example-u-sizing-item pf-u-h-50vh pf-u-display-inline-block"
>.pf-u-h-50vh</div>
<div
  class="ws-example-u-sizing-item pf-u-h-66vh pf-u-display-inline-block"
>.pf-u-h-66vh</div>
<div
  class="ws-example-u-sizing-item pf-u-h-75vh pf-u-display-inline-block"
>.pf-u-h-75vh</div>
<div
  class="ws-example-u-sizing-item pf-u-h-100vh pf-u-display-inline-block"
>.pf-u-h-100vh</div>

```

### Usage

[Breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes) is optional. Breakpoints: base (no breakpoint value), -on-sm, -on-md, -on-lg, -on-xl. **Example .pf-u-h-25vh-on-lg**

| Class                             | Applied to | Outcome                           |
| --------------------------------- | ---------- | --------------------------------- |
| `.pf-u-h-25vh{-on-[breakpoint]}`  | `*`        | Sets height: 25vh                 |
| `.pf-u-h-33vh{-on-[breakpoint]}`  | `*`        | Sets height: calc(100vh / 3)      |
| `.pf-u-h-50vh{-on-[breakpoint]}`  | `*`        | Sets height: 50vh                 |
| `.pf-u-h-66vh{-on-[breakpoint]}`  | `*`        | Sets height: calc(100vh / 3 \* 2) |
| `.pf-u-h-75vh{-on-[breakpoint]}`  | `*`        | Sets height: 75vh                 |
| `.pf-u-h-100vh{-on-[breakpoint]}` | `*`        | Sets height: 100vh                |

### Min width

```html
<div
  class="pf-u-min-width"
  style="--pf-u-min-width--MinWidth: 50ch;"
>Min-width 50ch example</div>

```

### Max width

```html
<div
  class="pf-u-max-width"
  style="--pf-u-max-width--MaxWidth: 50ch;"
>Max-width 50ch example</div>

```

### Min and max width

```html
<div
  class="pf-u-min-width pf-u-max-width"
  style="--pf-u-min-width--MinWidth: 30ch; --pf-u-max-width--MaxWidth: 50ch;"
>Min-width 30ch, max-width 50ch example</div>

```

### Responsive width control

```html
<div
  class="pf-u-min-width pf-u-max-width"
  style="--pf-u-min-width--MinWidth: 20ch; --pf-u-max-width--MaxWidth: 30ch; --pf-u-max-width--MaxWidth-on-md: 50ch; --pf-u-max-width--MaxWidth-on-xl: 70ch;"
>Min-width 20ch, max-width 30ch, max-width-on-md 50ch, max-width-on-xl 70ch example</div>

```

### Usage

| Class             | Applied to | Outcome                                                              |
| ----------------- | ---------- | -------------------------------------------------------------------- |
| `.pf-u-min-width` | `*`        | Sets min-width: `var(--pf-u-min-width--MinWidth{-on-[breakpoint]})`. |
| `.pf-u-max-width` | `*`        | Sets min-width: `var(--pf-u-max-width--MaxWidth{-on-[breakpoint]})`. |

[Breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes) is optional. Breakpoints: base (no breakpoint value), -on-sm, -on-md, -on-lg, -on-xl. **Example --pf-u-min-width--MinWidth-on-lg**

| Custom property                                         | Applied to        | Outcome                                 |
| ------------------------------------------------------- | ----------------- | --------------------------------------- |
| `--pf-u-min-width--MinWidth{-on-[breakpoint]}: {width}` | `.pf-u-min-width` | Modifies the min width custom property. |
| `--pf-u-max-width--MaxWidth{-on-[breakpoint]}: {width}` | `.pf-u-max-width` | Modifies the max width custom property. |

### Min height

```html
<div
  class="pf-u-min-height"
  style="--pf-u-min-height--MinHeight: 50ch;"
>Min-height 50ch example</div>

```

### Max height

```html
<div
  class="pf-u-max-height"
  style="--pf-u-max-height--MaxHeight: 50ch;"
>Max-height 50ch example</div>

```

### Min and max height

```html
<div
  class="pf-u-min-height pf-u-max-height"
  style="--pf-u-min-height--MinHeight: 30ch; --pf-u-max-height--MaxHeight: 50ch;"
>Min-height 30ch, max-height 50ch example</div>

```

### Responsive height control

```html
<div
  class="pf-u-min-height pf-u-max-height"
  style="--pf-u-min-height--MinHeight: 20ch; --pf-u-max-height--MaxHeight: 30ch; --pf-u-max-height--MaxHeight-on-md: 50ch; --pf-u-max-height--MaxHeight-on-xl: 70ch;"
>Min-height 20ch, max-height 30ch, max-height-on-md 50ch, max-height-on-xl 70ch example</div>

```

### Usage

| Class              | Applied to | Outcome                                                                 |
| ------------------ | ---------- | ----------------------------------------------------------------------- |
| `.pf-u-min-height` | `*`        | Sets min-height: `var(--pf-u-min-height--MinHeight{-on-[breakpoint]})`. |
| `.pf-u-max-height` | `*`        | Sets max-height: `var(--pf-u-max-height--MaxHeight{-on-[breakpoint]})`. |

[Breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes) is optional. Breakpoints: base (no breakpoint value), -on-sm, -on-md, -on-lg, -on-xl. **Example --pf-u-min-height--MinHeight-on-lg**

| Custom property                                            | Applied to         | Outcome                                  |
| ---------------------------------------------------------- | ------------------ | ---------------------------------------- |
| `--pf-u-min-height--MinHeight{-on-[breakpoint]}: {height}` | `.pf-u-min-height` | Modifies the min height custom property. |
| `--pf-u-max-height--MaxHeight{-on-[breakpoint]}: {height}` | `.pf-u-max-height` | Modifies the max height custom property. |
