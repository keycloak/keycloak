---
title: 'Badge'
section: components
cssPrefix: 'pf-c-badge'
typescript: true 
propComponents: ['Badge']
---
import { Badge } from '@patternfly/react-core';

## Examples
```js title=Read
import React from 'react';
import { Badge } from '@patternfly/react-core';

ReadBadge = () => (
  <React.Fragment>
    <Badge key={1} isRead>7</Badge>
    {' '}
    <Badge key={2} isRead>24</Badge>
    {' '}
    <Badge key={3} isRead>240</Badge>
    {' '}
    <Badge key={4} isRead>999+</Badge>
  </React.Fragment>
);
```

```js title=Unread
import React from 'react';
import { Badge } from '@patternfly/react-core';

UnreadBadge = () => (
  <React.Fragment>
    <Badge key={1}>7</Badge>
    {' '}
    <Badge key={2}>24</Badge>
    {' '}
    <Badge key={3}>240</Badge>
    {' '}
    <Badge key={4}>999+</Badge>
  </React.Fragment>
);
```
