---
title: 'Divider'
section: components
cssPrefix: 'pf-c-divider'
typescript: true 
propComponents: ['Divider']
---
import { Divider } from '@patternfly/react-core';

```js title=Using-hr-(default)
import React from 'react';
import { Divider } from '@patternfly/react-core';

DividerHr = () => (
  <Divider/>
);
```

```js title=Using-li
import React from 'react';
import { Divider } from '@patternfly/react-core/dist';

DividerLi = () => (
  <ul>
    <li>List item one</li>
    <Divider component="li"/>
    <li>List item two</li>
  </ul>
);
```

```js title=Using-div
import React from 'react';
import { Divider } from '@patternfly/react-core';

DividerDiv = () => (
  <Divider component="div"/>
);
```
