---
id: Application launcher
section: components
cssPrefix: pf-c-app-launcher
propComponents: ['ApplicationLauncher', 'ApplicationLauncherItem', 'ApplicationLauncherContent']
ouia: true
---

import HelpIcon from '@patternfly/react-icons/dist/esm/icons/help-icon';
import { Link } from '@reach/router';
import pfLogoSm from './pf-logo-small.svg';

Note: Application launcher is built on Dropdown, for extended API go to [Dropdown](/documentation/react/components/dropdown) documentation.
To add a tooltip, use the `tooltip` prop and optionally add more tooltip props by using `tooltipProps`. For more tooltip information go to [Tooltip](/documentation/react/components/tooltip).

## Examples

### Basic

```ts file="./ApplicationLauncherBasic.tsx"
```

### Router link

```ts file="./ApplicationLauncherRouterLink.tsx"
```

### Disabled

```ts file="./ApplicationLauncherDisabled.tsx"
```

### Aligned right

```ts file="./ApplicationLauncherAlignRight.tsx"
```

### Aligned top

```ts file="./ApplicationLauncherAlignTop.tsx"
```

### With tooltip

```ts file="./ApplicationLauncherTooltip.tsx"
```

### With sections and icons

```ts file="./ApplicationLauncherSectionsAndIcons.tsx"
```

### With favorites and search

```ts file="./ApplicationLauncherFavoritesAndSearch.tsx"
```

### With custom icon

```ts file="./ApplicationLauncherCustomIcon.tsx"
```

### Basic with menu appended to document body

```ts file="./ApplicationLauncherDocumentBody.tsx"
```
