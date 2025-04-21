---
id: Split
cssPrefix: pf-l-split
section: layouts
propComponents: ['Split', 'SplitItem']
---

import './split.css';

## Examples
### Basic
```js
import React from 'react';
import { Split, SplitItem } from '@patternfly/react-core';

<Split>
  <SplitItem>content</SplitItem>
  <SplitItem isFilled>pf-m-fill</SplitItem>
  <SplitItem>content</SplitItem>
</Split>
```

### With gutter
```js
import React from 'react';
import { Split, SplitItem } from '@patternfly/react-core';

<Split hasGutter>
  <SplitItem>content</SplitItem>
  <SplitItem isFilled>pf-m-fill</SplitItem>
  <SplitItem>content</SplitItem>
</Split>
```

### Wrappable
```js
import React from 'react';
import { Split, SplitItem } from '@patternfly/react-core';

<Split hasGutter isWrappable>
  <SplitItem>content</SplitItem>
  <SplitItem>content</SplitItem>
  <SplitItem>content</SplitItem>
  <SplitItem>content</SplitItem>
  <SplitItem>content</SplitItem>
  <SplitItem>content</SplitItem>
  <SplitItem>content</SplitItem>
  <SplitItem>content</SplitItem>
  <SplitItem>content</SplitItem>
  <SplitItem>content</SplitItem>
  <SplitItem>content</SplitItem>
  <SplitItem>content</SplitItem>
  <SplitItem>content</SplitItem>
  <SplitItem>content</SplitItem>
</Split>
```
