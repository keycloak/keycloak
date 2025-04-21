---
id: Brand
section: components
cssPrefix: null
propComponents: ['Brand']
---

import pfLogo from './pfLogo.svg';
import logoXl from './pf-c-brand--logo-on-xl.svg';
import logoLg from './pf-c-brand--logo-on-lg.svg';
import logoMd from './pf-c-brand--logo-on-md.svg';
import logoSm from './pf-c-brand--logo-on-sm.svg';
import logo from './pf-c-brand--logo.svg';
import logoBase from './pf-c-brand--logo-base.jpg';

## Examples

### Basic

```ts file="./BrandBasic.tsx"
```

### Responsive

Passing `<source>` elements as children to `Brand` will change the component from an `<img>` to a `<picture>` element. In this form, breakpoint modifiers for width and height may be passed to `Brand`. The `src` and `alt` properties should still be passed to populate the fallback `img` of the brand.

```ts file="./BrandResponsive.tsx"
```
