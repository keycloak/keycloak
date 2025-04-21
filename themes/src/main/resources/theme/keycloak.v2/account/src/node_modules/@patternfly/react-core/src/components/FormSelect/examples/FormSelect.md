---
id: Form select
section: components
cssPrefix: pf-c-form-control
propComponents: ['FormSelect', 'FormSelectOption', 'FormSelectOptionGroup']
ouia: true
---

## Examples

### Basic
```ts file='./FormSelectBasic.tsx'
```

### Validated
```ts file='./FormSelectValidated.tsx'
```

### Disabled
```ts file='./FormSelectDisabled.tsx'
```

### Grouped
```ts file='./FormSelectGrouped.tsx'
```

### Icon sprite variant

**Note:** The dropdown toggle icon is applied as a background image to the form element. By default, the image URLs for these icons are data URIs. However, there may be cases where data URIs are not ideal, such as in an application with a content security policy that disallows data URIs for security reasons. The `isIconSprite` variation changes the icon source to an external SVG file that serves as a sprite for all of the supported icons.

```ts file='./FormSelectIconSpriteVariant.tsx' isBeta
```
