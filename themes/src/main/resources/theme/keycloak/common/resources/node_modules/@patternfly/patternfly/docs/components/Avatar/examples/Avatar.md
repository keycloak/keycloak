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

## Documentation

### Overview

The avatar component provides a default SVG icon. If an image is used it should be 36px by 36px.

### Accessibility

| Attribute | Applied to     | Outcome                                                                                                    |
| --------- | -------------- | ---------------------------------------------------------------------------------------------------------- |
| `alt`     | `.pf-c-avatar` | The alt attribute specifies an alternate text for an image, if the image cannot be displayed. **Required** |

### Usage

| Class          | Applied to     | Outcome                                                |
| -------------- | -------------- | ------------------------------------------------------ |
| `.pf-c-avatar` | `<img>`        | Initiates an avatar image. **Required**                |
| `.pf-m-light`  | `.pf-c-avatar` | Modifies an avatar for use against a light background. |
| `.pf-m-dark`   | `.pf-c-avatar` | Modifies an avatar for use against a dark background.  |
