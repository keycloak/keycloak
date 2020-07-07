---
title: Accessibility
section: utilities
---

## Examples
```hbs title=Screen-reader-only
Content available only to screen reader, open inspector to investigate
{{#> accessibility accessibility--type="screen-reader"}}
    This content is intended to be announced by assistive technologies, but not visually presented.
{{/accessibility}}
```

```hbs title=Visible
{{#> accessibility accessibility--type="visible"}}
    This class unsets .pf-u-screen-reader and .pf-screen-reader. It will be visible.
{{/accessibility}}
```

```hbs title=Hidden
The text underneath is hidden.
{{#> accessibility accessibility--type="hidden"}}
  This text is hidden.
{{/accessibility}}
```

## Documentation
### Overview
Breakpoint is optional. Breakpoints: base (no breakpoint value), -on-sm, -on-md, -on-lg, -on-xl. **Example .pf-u-screen-reader-on-lg**

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-u-screen-reader{-on-[breakpoint]}` | `*` |  Visually hides element, but leaves accessible to assistive technologies |
| `.pf-u-visible{-on-[breakpoint]}` | `*` |  Unsets `.pf-u-screen-reader` and `.pf-screen-reader` |
