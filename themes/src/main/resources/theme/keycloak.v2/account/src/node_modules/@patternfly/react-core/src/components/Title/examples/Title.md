---
id: Title
section: components
cssPrefix: pf-c-title
propComponents: ['Title']
---

## Examples
### Sizes
```js
import React from 'react';
import { Title, TitleSizes } from '@patternfly/react-core';

<React.Fragment>
  <Title headingLevel="h1" size={TitleSizes['4xl']}>
    4xl Title
  </Title>
  <Title headingLevel="h2" size="3xl">
    3xl Title
  </Title>
  <Title headingLevel="h3" size={TitleSizes['2xl']}>
    2xl Title
  </Title>
  <Title headingLevel="h4" size="xl">
    xl Title
  </Title>
  <Title headingLevel="h5" size={TitleSizes.lg}>
    lg Title
  </Title>
  <Title headingLevel="h6" size="md">
    md Title
  </Title>
</React.Fragment>
```

### Default size mappings
```js
import React from 'react';
import { Title } from '@patternfly/react-core';

<React.Fragment>
  <Title headingLevel="h1">
    h1 default to 2xl
  </Title>
  <Title headingLevel="h2">
    h2 defaults to xl
  </Title>
  <Title headingLevel="h3">
    h3 defaults to lg
  </Title>
  <Title headingLevel="h4">
    h4 defaults to md
  </Title>
  <Title headingLevel="h5">
    h5 defaults to md
  </Title>
  <Title headingLevel="h6">
    h6 defaults to md
  </Title>
</React.Fragment>
```
