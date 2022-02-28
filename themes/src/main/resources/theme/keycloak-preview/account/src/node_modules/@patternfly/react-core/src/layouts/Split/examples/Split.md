---
title: 'Split'
cssPrefix: 'pf-l-split'
section: 'layouts'
propComponents: ['Split', 'SplitItem']
typescript: true
---

import { Split, SplitItem } from '@patternfly/react-core';
import './split.css';

## Examples
```js title=Basic
import React from 'react';
import { Split, SplitItem } from '@patternfly/react-core';

SplitBasicExample = () => (
  <Split>
    <SplitItem>content</SplitItem>
    <SplitItem isFilled>pf-m-fill</SplitItem>
    <SplitItem>content</SplitItem>
  </Split>
);
```

```js title=With-gutter
import React from 'react';
import { Split, SplitItem } from '@patternfly/react-core';

SplitWithGutterExample = () => (
  <Split gutter="md">
    <SplitItem>content</SplitItem>
    <SplitItem isFilled>pf-m-fill</SplitItem>
    <SplitItem>content</SplitItem>
  </Split>
);
```
