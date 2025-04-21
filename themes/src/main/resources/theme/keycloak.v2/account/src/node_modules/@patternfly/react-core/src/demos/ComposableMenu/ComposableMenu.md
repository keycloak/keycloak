---
id: Composable menu
section: demos
---

import { Link } from '@reach/router';

import CogIcon from '@patternfly/react-icons/dist/esm/icons/cog-icon';
import EllipsisVIcon from '@patternfly/react-icons/dist/esm/icons/ellipsis-v-icon';
import TableIcon from '@patternfly/react-icons/dist/esm/icons/table-icon';
import StorageDomainIcon from '@patternfly/react-icons/dist/esm/icons/storage-domain-icon';
import CodeBranchIcon from '@patternfly/react-icons/dist/esm/icons/code-branch-icon';
import LayerGroupIcon from '@patternfly/react-icons/dist/esm/icons/layer-group-icon';
import CubeIcon from '@patternfly/react-icons/dist/esm/icons/cube-icon';
import BarsIcon from '@patternfly/react-icons/dist/esm/icons/bars-icon';
import ClipboardIcon from '@patternfly/react-icons/dist/esm/icons/clipboard-icon';
import BellIcon from '@patternfly/react-icons/dist/esm/icons/bell-icon';
import ThIcon from '@patternfly/react-icons/dist/esm/icons/th-icon';
import pfIcon from './examples/pf-logo-small.svg';
import SearchIcon from '@patternfly/react-icons/dist/esm/icons/search-icon';
import CaretDownIcon from '@patternfly/react-icons/dist/esm/icons/caret-down-icon';
import avatarImg from './examples/avatarImg.svg';

## Demos

Composable menus currently require consumer keyboard handling and use of our undocumented [popper.js](https://popper.js.org/) wrapper component called Popper. We understand this is inconvientent boilerplate and these examples will be updated to use [Dropdown](/components/dropdown) in a future release.

### Composable simple dropdown

```ts file="./examples/ComposableSimpleDropdown.tsx"
```

### Composable actions menu

```ts file="./examples/ComposableActionsMenu.tsx"
```

### Composable simple select

```ts file="./examples/ComposableSimpleSelect.tsx"
```

### Composable simple checkbox select

```ts isBeta file="./examples/ComposableSimpleCheckboxSelect.tsx"
```

### Composable drilldown menu

```ts isBeta file="./examples/ComposableDrilldownMenu.tsx"
```

### Composable tree view menu

When rendering a menu-like element that does not contain MenuItem components, [Panel](/components/panel) allows more flexible control and customization.

```ts file="./examples/ComposableTreeViewMenu.tsx"
```

### Composable flyout

The flyout will automatically position to the left or top if it would otherwise go outside the window. The menu must be placed in a container outside the main content like Popper, [Popover](/components/popover) or [Tooltip](/components/tooltip) since it may go over the side nav.

```ts isBeta file="./examples/ComposableFlyout.tsx"
```

### Composable application launcher

```ts file="./examples/ComposableApplicationLauncher.tsx"
```

### Composable context selector

```ts file="./examples/ComposableContextSelector.tsx"
```

### Composable options menu variants

```ts file="./examples/ComposableOptionsMenuVariants.tsx"
```

### Composable dropdown variants

```ts file="./examples/ComposableDropdwnVariants.tsx"
```

### Composable date select

```ts file="./examples/ComposableDateSelect.tsx"
```
