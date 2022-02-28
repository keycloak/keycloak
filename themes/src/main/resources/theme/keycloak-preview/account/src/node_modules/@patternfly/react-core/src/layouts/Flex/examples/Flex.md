---
title: 'Flex'
cssPrefix: 'pf-l-flex'
section: 'layouts'
propComponents: ['Flex']
typescript: true
---

import { Flex, FlexItem, FlexModifiers, FlexBreakpoints } from '@patternfly/react-core';
import './flex.css';

## Examples
### Flex Basics
```js title=Basic
import React from 'react';
import { Flex, FlexItem } from '@patternfly/react-core';

BasicFlexExample = () => (
  <Flex>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
  </Flex>
);
```

```js title=Nesting
import React from 'react';
import { Flex, FlexItem } from '@patternfly/react-core';

NestingFlexExample = () => (
  <Flex>
    <Flex>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
    <Flex>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
  </Flex>
);
```

```js title=Nested-with-items
import React from 'react';
import { Flex, FlexItem } from '@patternfly/react-core';

NestedItemsFlexExample = () => (
  <Flex>
    <Flex>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
    <Flex>
      <FlexItem>Flex item</FlexItem>
    </Flex>
  </Flex>
);
```

### Flex Spacing
```js title=Individually-spaced
import React from 'react';
import { Flex, FlexItem, FlexModifiers } from '@patternfly/react-core';

FlexSpacingExample = () => (
  <Flex>
    <FlexItem breakpointMods={[{modifier: FlexModifiers["spacer-none"]}]}>Item - none</FlexItem>
    <FlexItem breakpointMods={[{modifier: FlexModifiers["spacer-xs"]}]}>Item - xs</FlexItem>
    <FlexItem breakpointMods={[{modifier: FlexModifiers["spacer-sm"]}]}>Item -sm</FlexItem>
    <FlexItem breakpointMods={[{modifier: FlexModifiers["spacer-md"]}]}>Item - md</FlexItem>
    <FlexItem breakpointMods={[{modifier: FlexModifiers["spacer-lg"]}]}>Item - lg</FlexItem>
    <FlexItem breakpointMods={[{modifier: FlexModifiers["spacer-xl"]}]}>Item - xl</FlexItem>
    <FlexItem breakpointMods={[{modifier: FlexModifiers["spacer-2xl"]}]}>Item - 2xl</FlexItem>
    <FlexItem breakpointMods={[{modifier: FlexModifiers["spacer-3xl"]}]}>Item - 3xl</FlexItem>
  </Flex>
);
```

```js title=Spacing-xl
import React from 'react';
import { Flex, FlexItem, FlexModifiers } from '@patternfly/react-core';

FlexSpacingXlExample = () => (
  <Flex breakpointMods={[{modifier: FlexModifiers["space-items-xl"]}]}>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
  </Flex>
);
```

```js title=Spacing-none
import React from 'react';
import { Flex, FlexItem, FlexModifiers } from '@patternfly/react-core';

FlexSpacingNoneExample = () => (
  <Flex breakpointMods={[{modifier: FlexModifiers["space-items-none"]}]}>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
  </Flex>
);
```

### Flex layout modifiers

```js title=Default-layout
import React from 'react';
import { Flex, FlexItem } from '@patternfly/react-core';

FlexLayoutModifiersExample = () => (
  <Flex className="example-border">
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
  </Flex>
);
```

```js title=Inline
import React from 'react';
import { Flex, FlexItem, FlexModifiers } from '@patternfly/react-core';

FlexInlineExample = () => (
  <Flex className="example-border" breakpointMods={[{modifier: FlexModifiers["inline-flex"]}]}>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
  </Flex>
);
```

```js title=Using-canGrow
import React from 'react';
import { Flex, FlexItem, FlexModifiers } from '@patternfly/react-core';

FlexUsingCanGrowExample = () => (
  <Flex>
    <Flex breakpointMods={[{modifier: FlexModifiers.grow}]}>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
    <Flex>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
    <Flex>
      <FlexItem>Flex item</FlexItem>
    </Flex>
  </Flex>
);
```

```js title=Adjusting-width
import React from 'react';
import { Flex, FlexItem, FlexModifiers } from '@patternfly/react-core';

FlexAdjustingWidthExample = () => (
  <Flex>
    <Flex breakpointMods={[{modifier: FlexModifiers["flex-1"]}]}>
      <FlexItem>Flex item</FlexItem>
    </Flex>
    <Flex breakpointMods={[{modifier: FlexModifiers["flex-1"]}]}>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
    <Flex breakpointMods={[{modifier: FlexModifiers["flex-1"]}]}>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
  </Flex>
);
```

```js title=Specifying-column-widths
import React from 'react';
import { Flex, FlexItem, FlexModifiers } from '@patternfly/react-core';

FlexSpecifyingColumnWidthsExample = () => (
  <Flex>
    <Flex breakpointMods={[{modifier: FlexModifiers["flex-1"]}]}>
      <FlexItem>Flex item</FlexItem>
    </Flex>
    <Flex breakpointMods={[{modifier: FlexModifiers["flex-2"]}]}>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
    <Flex breakpointMods={[{modifier: FlexModifiers["flex-3"]}]}>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
  </Flex>
);
```

### Column layout modifiers

```js title=Column-layout
import React from 'react';
import { Flex, FlexItem, FlexModifiers } from '@patternfly/react-core';

FlexColumnLayoutExample = () => (
  <Flex breakpointMods={[{modifier: FlexModifiers.column}]}>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
  </Flex>
);
```

```js title=Stacking-elements
import React from 'react';
import { Flex, FlexItem, FlexModifiers } from '@patternfly/react-core';

FlexStackingElementsExample = () => (
  <Flex breakpointMods={[{modifier: FlexModifiers.column}]}>
    <Flex>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
    <Flex>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
    <Flex>
      <FlexItem>Flex item</FlexItem>
    </Flex>
  </Flex>
);
```

```js title=Nesting-elements-in-columns
import React from 'react';
import { Flex, FlexItem, FlexModifiers } from '@patternfly/react-core';

FlexNestingElementsInColumnsExample = () => (
  <Flex>
    <Flex breakpointMods={[{modifier: FlexModifiers["column"]}]}>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
    <Flex breakpointMods={[{modifier: FlexModifiers["column"]}]}>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
  </Flex>
);
```

### Responsive layout modifiers

```js title=Switching-between-direction-column-and-row
import React from 'react';
import { Flex, FlexItem, FlexModifiers, FlexBreakpoints } from '@patternfly/react-core';

FlexSwitchingBetweenDirectionColumnAndRowExample = () => (
  <Flex breakpointMods={[{modifier: FlexModifiers["column"]}, {modifier: FlexModifiers["row"], breakpoint: FlexBreakpoints.lg}]}>
    <Flex>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
    <Flex breakpointMods={[{modifier: "column"}]}>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
  </Flex>
);
```

```js title=Controlling-width-of-text
import React from 'react';
import { Flex, FlexItem, FlexModifiers } from '@patternfly/react-core';

FlexControllingWidthOfTextExample = () => (
  <Flex breakpointMods={[{modifier: FlexModifiers["column"]}, {modifier: FlexModifiers["row"], breakpoint: FlexModifiers["lg"]}]}>
    <Flex breakpointMods={[{modifier: FlexModifiers["flex-1"]}]}>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Lorem ipsum dolor sit amet consectetur adipisicing elit. Est animi modi temporibus, alias qui obcaecati ullam dolor nam, nulla magni iste rem praesentium numquam provident amet ut nesciunt harum accusamus.</FlexItem>
    </Flex>
    <Flex breakpointMods={[{modifier: FlexModifiers["column"]}]}>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
  </Flex>
);
```

### Flex alignment

```js title=Aligning-right
import React from 'react';
import { Flex, FlexItem, FlexModifiers } from '@patternfly/react-core';

FlexAligningRightExample = () => (
  <Flex className="example-border">
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
    <FlexItem breakpointMods={[{modifier: FlexModifiers["align-right"]}]}>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
  </Flex>
);
```

```js title=Align-right-on-single-item
import React from 'react';
import { Flex, FlexItem, FlexModifiers } from '@patternfly/react-core';

FlexAligningRightOnSingleItemExample = () => (
  <Flex className="example-border">
    <FlexItem breakpointMods={[{modifier: FlexModifiers["align-right"]}]}>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
  </Flex>
);
```

```js title=Align-right-on-multiple-groups
import React from 'react';
import { Flex, FlexItem, FlexModifiers } from '@patternfly/react-core';

FlexAlignRightOnMultipleGroupsExample = () => (
  <Flex>
    <Flex>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
    <Flex breakpointMods={[{modifier: FlexModifiers["align-right"]}]}>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
    <Flex breakpointMods={[{modifier: FlexModifiers["align-right"]}]}>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
  </Flex>
);
```

```js title=Align-adjacent-content
import React from 'react';
import { Flex, FlexItem, FlexModifiers } from '@patternfly/react-core';

FlexAlignAdjacentContentExample = () => (
  <Flex>
    <Flex breakpointMods={[{modifier: FlexModifiers["flex-1"]}]}>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
    <Flex>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
  </Flex>
);
```

```js title=Align-self-flex-end
import React from 'react';
import { Flex, FlexItem, FlexModifiers } from '@patternfly/react-core';

FlexAlignSelfFlexEndExample = () => (
  <Flex>
    <Flex breakpointMods={[{modifier: FlexModifiers["column"]}]}>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
    <Flex breakpointMods={[{modifier: FlexModifiers["column"]}, {modifier: FlexModifiers["align-self-flex-end"]}]}>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
  </Flex>
);
```

```js title=Align-self-center
import React from 'react';
import { Flex, FlexItem, FlexModifiers } from '@patternfly/react-core';

FlexAlignSelfCenterExample = () => (
  <Flex>
    <Flex breakpointMods={[{modifier: FlexModifiers["column"]}]}>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
    <Flex breakpointMods={[{modifier: FlexModifiers["column"]}, {modifier: FlexModifiers["align-self-center"]}]}>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
  </Flex>
);
```

```js title=Align-self-baseline
import React from 'react';
import { Flex, FlexItem, FlexModifiers } from '@patternfly/react-core';

FlexAlignSelfBaselineExample = () => (
  <Flex>
    <Flex breakpointMods={[{modifier: FlexModifiers["column"]}]}>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
    <Flex breakpointMods={[{modifier: FlexModifiers["column"]}, {modifier: FlexModifiers["align-self-baseline"]}]}>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
  </Flex>
);
```

```js title=Align-self-stretch
import React from 'react';
import { Flex, FlexItem, FlexModifiers } from '@patternfly/react-core';

FlexAlignSelfStretchExample = () => (
  <Flex>
    <Flex breakpointMods={[{modifier: FlexModifiers["column"]}]}>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
    <Flex breakpointMods={[{modifier: FlexModifiers["column"]}, {modifier: FlexModifiers["align-self-stretch"]}]}>
      <FlexItem>Flex item</FlexItem>
      <FlexItem>Flex item</FlexItem>
    </Flex>
  </Flex>
);
```

### Flex justification

```js title=Justify-content-flex-end
import React from 'react';
import { Flex, FlexItem, FlexModifiers } from '@patternfly/react-core';

FlexJustifyContentFlexEndExample = () => (
  <Flex className="example-border" breakpointMods={[{modifier: FlexModifiers["justify-content-flex-end"]}]}>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
  </Flex>
);
```

```js title=Justify-content-space-between
import React from 'react';
import { Flex, FlexItem, FlexModifiers  } from '@patternfly/react-core';

FlexJustifyContentSpaceBetweenExample = () => (
  <Flex className="example-border" breakpointMods={[{modifier: FlexModifiers["justify-content-space-between"]}]}>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
  </Flex>
);
```

```js title=Justify-content-flex-start
import React from 'react';
import { Flex, FlexItem, FlexModifiers } from '@patternfly/react-core';

FlexJustifyContentFlexStartExample = () => (
  <Flex className="example-border" breakpointMods={[{modifier: FlexModifiers["justify-content-flex-start"]}]}>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
    <FlexItem>Flex item</FlexItem>
  </Flex>
);
```
