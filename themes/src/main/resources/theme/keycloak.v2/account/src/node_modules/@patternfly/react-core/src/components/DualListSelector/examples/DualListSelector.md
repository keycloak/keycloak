---
id: Dual list selector
section: components
cssPrefix: 'pf-c-dual-list-selector'
propComponents:
  [
    'DualListSelector',
    'DualListSelectorPane',
    'DualListSelectorControl',
    'DualListSelectorControlsWrapper',
    'DualListSelectorTree',
    'DualListSelectorTreeItemData',
  ]
beta: true
---

import AngleDoubleLeftIcon from '@patternfly/react-icons/dist/esm/icons/angle-double-left-icon';
import AngleLeftIcon from '@patternfly/react-icons/dist/esm/icons/angle-left-icon';
import AngleDoubleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-double-right-icon';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import PficonSortCommonAscIcon from '@patternfly/react-icons/dist/esm/icons/pficon-sort-common-asc-icon';

## Examples

### Basic

```ts file="./DualListSelectorBasic.tsx"
```

### Basic with tooltips

```ts file="./DualListSelectorBasicTooltips.tsx"
```

### Basic with search

```ts file="./DualListSelectorBasicSearch.tsx"
```

### Using more complex options with actions

```ts file="./DualListSelectorComplexOptionsActions.tsx"
```

### With tree

```ts file="./DualListSelectorTree.tsx"
```

### Composable dual list selector

For more flexibility, a dual list selector can be built using sub components. When doing so, the intended component relationships are arranged as follows:

```noLive
<DualListSelector>
  <DualListSelectorPane>
    <DualListSelectorList>
      <DualListSelectorListItem />
    </DualListSelectorList>
  </DualListSelectorPane>

  <DualListSelectorControlsWrapper>
    <DualListSelectorControl /> /* The standard Dual list selector has 4 controls */
  </DualListSelectorControlsWrapper>

  <DualListSelectorPane isChosen>
    <DualListSelectorList>
      <DualListSelectorListItem />
    </DualListSelectorList>
  </DualListSelectorPane>
</DualListSelector>
```

```ts file="./DualListSelectorComposable.tsx"
```

### Composable dual list selector with drag and drop

This example only allows reordering the contents of the "chosen" pane with drag and drop. To make a pane able to be reordered:

- wrap the `DualListSelectorPane` in a `DragDrop` component
- wrap the `DualListSelectorList` in a `Droppable` component
- wrap the `DualListSelectorListItem` components in a `Draggable` component
- define an `onDrop` callback which reorders the sortable options.
  - The `onDrop` function provides the starting location and destination location for a dragged item. It should return
    true to enable the 'drop' animation in the new location and false to enable the 'drop' animation back to the item's
    old position.
  - define an `onDrag` callback which ensures that the drag event will not cross hairs with the `onOptionSelect` click
    event set on the option. Note: the `ignoreNextOptionSelect` state value is used to prevent selection while dragging.

Note: Keyboard accessibility and screen reader accessibility for the `DragDrop` component are still in development.

```ts file="DualListSelectorComposableDragDrop.tsx"
```

### Composable dual list selector with tree

```ts file="DualListSelectorComposableTree.tsx"
```
