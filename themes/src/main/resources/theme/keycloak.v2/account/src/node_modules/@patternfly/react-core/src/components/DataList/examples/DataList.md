---
id: Data list
section: components
cssPrefix: pf-c-data-list
propComponents:
  [
    'DataList',
    'SelectableRowObject',
    'DataListAction',
    'DataListCell',
    'DataListCheck',
    'DataListItem',
    'DataListItemCells',
    'DataListItemRow',
    'DataListToggle',
    'DataListContent',
    'DataListDragButton',
    'DataListControl',
  ]
---

import CodeBranchIcon from '@patternfly/react-icons/dist/esm/icons/code-branch-icon';
import AngleDownIcon from '@patternfly/react-icons/dist/esm/icons/angle-down-icon';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import { css } from '@patternfly/react-styles';

## Examples

### Basic

```ts file="./DataListBasic.tsx"
```

### Compact

```ts file="./DataListCompact.tsx"
```

### Checkboxes, actions and additional cells

```ts file="./DataListCheckboxes.tsx"
```

### Actions: single and multiple

```ts file="./DataListActions.tsx"
```

### Expandable

```ts file="./DataListExpandable.tsx"
```

### Width modifiers

```ts file="./DataListWidthModifiers.tsx"
```

### Selectable rows

```ts file="./DataListSelectableRows.tsx"
```

### Controlling text

```ts file="./DataListControllingText.tsx"
```

### Draggable

Draggable data lists used to have their own HTML5-based API for drag and drop, which wasn't able to fulfill requirements such as custom styling on items being dragged. So we wrote generic `DragDrop`, `Draggable`, and `Droppable` components for this purpose. Use those new components instead of the deprecated (and buggy!) HTML5-based API.

Note: Keyboard accessibility and screen reader accessibility for the `DragDrop` component are still in development.

```ts isBeta file="./DataListDraggable.tsx"
```

### Small grid breakpoint

```ts file="./DataListSmGridBreakpoint.tsx"
```
