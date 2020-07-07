---
title: Avatar
section: components
cssPrefix: pf-c-avatar
---

## Examples
```hbs title=Basic
{{#> avatar avatar--attribute='src="/assets/images/img_avatar.svg" alt="Avatar image"'}}
{{/avatar}}
```

## Documentation
### Overview
The avatar component provides a default SVG icon. If an image is used it should be 36px by 36px.

### Accessibility
| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `alt` | `.pf-c-avatar` | The alt attribute specifies an alternate text for an image, if the image cannot be displayed. **Required** |

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-avatar` | `<img>` |  Initiates an avatar image. **Required** |