---
title: 'Grid'
cssPrefix: 'pf-l-grid'
section: 'layouts'
propComponents: ['Grid', 'GridItem']
typescript: true
---
import { Grid, GridItem } from '@patternfly/react-core';
import './grid.css';

## Examples
```js title=Basic
import React from 'react';
import { Grid, GridItem } from '@patternfly/react-core';

GridBasicExample = () => (
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
);
```

```js title=With-gutters
import React from 'react';
import { Grid, GridItem } from '@patternfly/react-core';

GridWithGuttersExample = () => (
  <Grid gutter="md">
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
);
```

```js title=With-overrides
import React from 'react';
import { Grid, GridItem } from '@patternfly/react-core';

GridWithOverridesExample = () => (
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
);
```

