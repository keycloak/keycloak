---
id: Avatar
section: components
cssPrefix: pf-c-avatar
---import './Avatar.css'

## Examples

### Basic

```html
<img
  class="pf-c-avatar"
  src="/assets/images/img_avatar-light.svg"
  alt="Avatar image"
/>

```

### Bordered - light

```html
<img
  class="pf-c-avatar pf-m-light"
  src="/assets/images/img_avatar-light.svg"
  alt="Avatar image light"
/>

```

### Bordered - dark

```html
<img
  class="pf-c-avatar pf-m-dark"
  src="/assets/images/img_avatar-dark.svg"
  alt="Avatar image dark"
/>

```

### Small

```html
<img
  class="pf-c-avatar pf-m-sm"
  src="/assets/images/img_avatar-light.svg"
  alt="Avatar image small"
/>

```

### Medium

```html
<img
  class="pf-c-avatar pf-m-md"
  src="/assets/images/img_avatar-light.svg"
  alt="Avatar image medium"
/>

```

### Large

```html
<img
  class="pf-c-avatar pf-m-lg"
  src="/assets/images/img_avatar-light.svg"
  alt="Avatar image large"
/>

```

### Extra large

```html
<img
  class="pf-c-avatar pf-m-xl"
  src="/assets/images/img_avatar-light.svg"
  alt="Avatar image extra large"
/>

```

## Documentation

### Overview

The avatar component provides a default SVG icon. If an image is used it should be 36px by 36px.

### Accessibility

| Attribute | Applied to     | Outcome                                                                                                    |
| --------- | -------------- | ---------------------------------------------------------------------------------------------------------- |
| `alt`     | `.pf-c-avatar` | The alt attribute specifies an alternate text for an image, if the image cannot be displayed. **Required** |

### Usage

| Class                        | Applied to     | Outcome                                                                                                                                                                             |
| ---------------------------- | -------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-c-avatar`               | `<img>`        | Initiates an avatar image. **Required**                                                                                                                                             |
| `.pf-m-light`                | `.pf-c-avatar` | Modifies an avatar for use against a light background.                                                                                                                              |
| `.pf-m-dark`                 | `.pf-c-avatar` | Modifies an avatar for use against a dark background.                                                                                                                               |
| `.pf-m-sm{-on-[breakpoint]}` | `.pf-c-avatar` | Modifies an avatar to be small on an optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).                                      |
| `.pf-m-md{-on-[breakpoint]}` | `.pf-c-avatar` | Modifies an avatar to be medium on an optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes). **Note:** This is the default size. |
| `.pf-m-lg{-on-[breakpoint]}` | `.pf-c-avatar` | Modifies an avatar to be large on an optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).                                      |
| `.pf-m-xl{-on-[breakpoint]}` | `.pf-c-avatar` | Modifies an avatar to be extra large on an optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).                                |
