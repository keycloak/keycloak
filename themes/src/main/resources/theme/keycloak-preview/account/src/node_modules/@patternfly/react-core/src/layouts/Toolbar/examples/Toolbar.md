---
title: 'Toolbar'
cssPrefix: 'pf-c-avatar'
section: 'layouts'
propComponents: ['Toolbar', 'ToolbarGroup', 'ToolbarSection', 'ToolbarItem']
typescript: true
---

import { Toolbar, ToolbarGroup, ToolbarSection, ToolbarItem } from '@patternfly/react-core';
import './toolbar.css';

## Examples
```js title=Basic
import React from 'react';
import { Toolbar, ToolbarGroup, ToolbarSection, ToolbarItem } from '@patternfly/react-core';

ToolbarBasicExample = () => (
  <Toolbar>
    <ToolbarGroup>
      <ToolbarItem>Item 1</ToolbarItem>
    </ToolbarGroup>
    <ToolbarGroup>
      <ToolbarItem>Item 2</ToolbarItem>
      <ToolbarItem>Item 3</ToolbarItem>
    </ToolbarGroup>
    <ToolbarGroup>
      <ToolbarItem>Item 4</ToolbarItem>
    </ToolbarGroup>
  </Toolbar>
);
```

```js title=With-sections
import React from 'react';
import { Toolbar, ToolbarGroup, ToolbarSection, ToolbarItem } from '@patternfly/react-core';

ToolbarWithSectionsExample = () => (
  <Toolbar>
    <ToolbarSection aria-label="First section">
      <ToolbarGroup>
        <ToolbarItem>Item 1</ToolbarItem>
      </ToolbarGroup>
      <ToolbarGroup>
        <ToolbarItem>Item 2</ToolbarItem>
        <ToolbarItem>Item 3</ToolbarItem>
      </ToolbarGroup>
    </ToolbarSection>
    <ToolbarSection aria-label="Second section">
      <ToolbarGroup>
        <ToolbarItem>Item 4</ToolbarItem>
        <ToolbarItem>Item 5</ToolbarItem>
        <ToolbarItem>Item 6</ToolbarItem>
      </ToolbarGroup>
      <ToolbarGroup>
        <ToolbarItem>Item 7</ToolbarItem>
      </ToolbarGroup>
    </ToolbarSection>
  </Toolbar>
);
```
