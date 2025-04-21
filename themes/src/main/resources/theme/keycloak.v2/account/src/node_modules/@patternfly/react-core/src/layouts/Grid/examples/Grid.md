---
id: Grid
cssPrefix: pf-l-grid
section: layouts
propComponents: ['Grid', 'GridItem']
---
import './grid.css';

## Examples
### Basic
```js
import React from 'react';
import { Grid, GridItem } from '@patternfly/react-core';

<Grid>
  <GridItem span={8}>span = 8</GridItem>
  <GridItem span={4} rowSpan={2}>
    span = 4, rowSpan = 2
  </GridItem>
  <GridItem span={2} rowSpan={3}>
    span = 2, rowSpan = 3
  </GridItem>
  <GridItem span={2}>span = 2</GridItem>
  <GridItem span={4}>span = 4</GridItem>
  <GridItem span={2}>span = 2</GridItem>
  <GridItem span={2}>span = 2</GridItem>
  <GridItem span={2}>span = 2</GridItem>
  <GridItem span={4}>span = 4</GridItem>
  <GridItem span={2}>span = 2</GridItem>
  <GridItem span={4}>span = 4</GridItem>
  <GridItem span={4}>span = 4</GridItem>
</Grid>
```

### With gutters
```js
import React from 'react';
import { Grid, GridItem } from '@patternfly/react-core';

<Grid hasGutter>
  <GridItem span={8}>span = 8</GridItem>
  <GridItem span={4} rowSpan={2}>
    span = 4, rowSpan = 2
  </GridItem>
  <GridItem span={2} rowSpan={3}>
    span = 2, rowSpan = 3
  </GridItem>
  <GridItem span={2}>span = 2</GridItem>
  <GridItem span={4}>span = 4</GridItem>
  <GridItem span={2}>span = 2</GridItem>
  <GridItem span={2}>span = 2</GridItem>
  <GridItem span={2}>span = 2</GridItem>
  <GridItem span={4}>span = 4</GridItem>
  <GridItem span={2}>span = 2</GridItem>
  <GridItem span={4}>span = 4</GridItem>
  <GridItem span={4}>span = 4</GridItem>
</Grid>
```

### With overrides
```js
import React from 'react';
import { Grid, GridItem } from '@patternfly/react-core';

<Grid sm={6} md={4} lg={3} xl2={1}>
  <GridItem span={3} rowSpan={2}>
    span = 3 rowSpan= 2
  </GridItem>
  <GridItem>Grid Item</GridItem>
  <GridItem>Grid Item</GridItem>
  <GridItem>Grid Item</GridItem>
  <GridItem>Grid Item</GridItem>
  <GridItem>Grid Item</GridItem>
  <GridItem>Grid Item</GridItem>
  <GridItem>Grid Item</GridItem>
  <GridItem>Grid Item</GridItem>
  <GridItem>Grid Item</GridItem>
  <GridItem>Grid Item</GridItem>
  <GridItem>Grid Item</GridItem>
</Grid>
```

## Ordering

### Ordering

```js
import React from 'react';
import { Grid, GridItem } from '@patternfly/react-core';

<Grid hasGutter span={3}>
  <GridItem order={{default: "2"}}>Item A</GridItem>
  <GridItem>Item B</GridItem>
  <GridItem order={{default: "-1"}}>Item C</GridItem>
</Grid>
```

### Responsive ordering

```js
import React from 'react';
import { Grid, GridItem } from '@patternfly/react-core';

<Grid hasGutter span={3}>
  <GridItem order={{lg: "2"}}>Item A</GridItem>
  <GridItem>Item B</GridItem>
  <GridItem order={{default: "-1", md: "1"}}>Item C</GridItem>
</Grid>
```

### Grouped ordering

```js
import React from 'react';
import { Grid, GridItem } from '@patternfly/react-core';

<Grid hasGutter span={12}>
  <Grid hasGutter span={6} order={{default: "2"}}>
    <GridItem order={{default: "3"}}>Set 1, Item A</GridItem>
    <GridItem order={{default: "1"}}>Set 1, Item B</GridItem>
    <GridItem>Set 1, Item C</GridItem>
    <GridItem order={{default: "2"}}>Set 1, Item D</GridItem>
  </Grid>
  <Grid hasGutter span={6}>
    <GridItem order={{default: "2"}}>Set 2, Item A</GridItem>
    <GridItem order={{default: "1"}}>Set 2, Item B</GridItem>
    <GridItem>Set 2, Item C</GridItem>
    <GridItem order={{default: "2"}}>Set 2, Item D</GridItem>
  </Grid>
</Grid>
```

### Alternative components

```js
import React from 'react';
import { Grid, GridItem } from '@patternfly/react-core';

<Grid component='ul'>
  <GridItem component='li'>Grid item</GridItem>
  <GridItem component='li'>Grid item</GridItem>
  <GridItem component='li'>Grid item</GridItem>
  <GridItem component='li'>Grid item</GridItem>
  <GridItem component='li'>Grid item</GridItem>
</Grid>
```
