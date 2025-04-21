---
id: Dropdown
section: components
cssPrefix: pf-c-dropdown
propComponents:
  ['Dropdown', 'DropdownGroup', 'DropdownItem', 'DropdownToggle', 'DropdownToggleCheckbox', 'DropdownToggleAction']
ouia: true
---

import ThIcon from '@patternfly/react-icons/dist/esm/icons/th-icon';
import CaretDownIcon from '@patternfly/react-icons/dist/esm/icons/caret-down-icon';
import CogIcon from '@patternfly/react-icons/dist/esm/icons/cog-icon';
import BellIcon from '@patternfly/react-icons/dist/esm/icons/bell-icon';
import CubesIcon from '@patternfly/react-icons/dist/esm/icons/cubes-icon';
import AngleLeftIcon from '@patternfly/react-icons/dist/esm/icons/angle-left-icon';
import { Link } from '@reach/router';
import avatarImg from '../../Avatar/examples/avatarImg.svg';

## Examples

### Basic

```ts file='./DropdownBasic.tsx'
```

### With initial selection

```ts file="./DropdownInitialSelection.tsx"
```

### With groups

```ts file="./DropdownGroups.tsx"
```

### Disabled

```ts file="./DropdownDisabled.tsx"
```

### Primary toggle

```ts file="./DropdownPrimaryToggle.tsx"
```

### Secondary toggle

```ts file="./DropdownSecondaryToggle.tsx"
```

### Plain with text toggle

```ts file="./DropdownPlainTextToggle.tsx"
```

### Position right

```ts file="./DropdownPositionRight.tsx"
```

### Alignments on different breakpoints

```ts file="./DropdownAlignmentOnBreakpoints.tsx"
```

### Direction up

```ts file="./DropdownDirectionUp.tsx"
```

### With kebab

```ts file="./DropdownKebab.tsx"
```

### With badge

```ts file="./DropdownBadge.tsx"
```

### Icon only

```ts file="./DropdownIconOnly.tsx"
```

### Split button

```ts file="./DropdownSplitButton.tsx"
```

### Split button (with text)

```ts file="./DropdownSplitButtonText.tsx"
```

### Split button (indeterminate state)

```ts file="./DropdownSplitButtonIndeterminate.tsx"
```

### Split button (disabled)

```ts file="./DropdownSplitButtonDisabled.tsx"
```

### Split button action

```ts file="./DropdownSplitButtonAction.tsx"
```

### Split button primary action

```ts file="./DropdownSplitButtonActionPrimary.tsx"
```

### Basic panel

```ts file="./DropdownBasicPanel.tsx"
```

### Router link

```ts file="./DropdownRouterLink.tsx"
```

### Dropdown with image and text

```ts file="./DropdownImageAndText.tsx"
```

### Appending document body vs parent

Avoid passing in `document.body` when passing a value to the `menuAppendTo` prop on the Dropdown component, as it can cause accessibility issues. These issues can include, but are not limited to, being unable to enter the contents of the Dropdown options via assistive technologies (like keyboards or screen readers).

Instead append to `"parent"` to achieve the same result without sacrificing accessibility.

In this example, while, after making a selection, both variants retain focus on their respective Dropdown component, the options for the `document.body` variant cannot be navigated to via Voice Over.

```ts file="./DropdownAppendBodyVsParent.tsx"
```

### Dropdown with descriptions

```ts file="./DropdownDescriptions.tsx"
```
