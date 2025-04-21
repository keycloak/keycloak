---
id: Brand
section: components
---## Examples

### Basic

```html
<img class="pf-c-brand" src="/assets/images/pf_logo.svg" alt="PatternFly logo" />

```

### Responsive

```html
<picture
  class="pf-c-brand pf-m-picture"
  style="--pf-c-brand--Width: 40px; --pf-c-brand--Width-on-sm: 60px; --pf-c-brand--Width-on-md: 220px;"
>
  <source
    media="(min-width: 1200px)"
    srcset="/assets/images/pf-c-brand__logo-on-xl.svg"
  />
  <source
    media="(min-width: 992px)"
    srcset="/assets/images/pf-c-brand__logo-on-lg.svg"
  />
  <source
    media="(min-width: 768px)"
    srcset="/assets/images/pf-c-brand__logo-on-md.svg"
  />
  <source
    media="(min-width: 576px)"
    srcset="/assets/images/pf-c-brand__logo-on-sm.svg"
  />
  <source srcset="/assets/images/pf-c-brand__logo.svg" />
  <img
    src="/assets/images/pf-c-brand__logo-base.jpg"
    alt="Fallback patternFly default logo"
  />
</picture>

```

## Documentation

### Overview

Simple brand component.

### Accessibility

| Attribute | Applied to    | Outcome                                                                                                    |
| --------- | ------------- | ---------------------------------------------------------------------------------------------------------- |
| `alt`     | `.pf-c-brand` | The alt attribute specifies an alternate text for an image, if the image cannot be displayed. **Required** |

### Usage

| Class                                              | Applied to         | Outcome                                                                                                                                             |
| -------------------------------------------------- | ------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-c-brand`                                      | `<img>, <picture>` | Initiates a brand image. **Required**                                                                                                               |
| `.pf-m-picture`                                    | `.pf-c-brand`      | Modifies a brand image to a picture.                                                                                                                |
| `--pf-c-brand--Width{-on-[breakpoint]}: {width}`   | `.pf-c-brand`      | Modifies the width value of a picture on optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).  |
| `--pf-c-brand--Height{-on-[breakpoint]}: {height}` | `.pf-c-brand`      | Modifies the height value of a picture on optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes). |
