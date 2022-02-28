---
title: 'Spinner'
section: components
cssPrefix: 'pf-c-spinner'
typescript: true
propComponents: ['Spinner']
---

import { Spinner } from '@patternfly/react-core';
import { Alert } from '@patternfly/react-core';

## Spinner

## Examples
```js title=Basic
import React from 'react';
import { Spinner } from '@patternfly/react-core';

SpinnerBasic = () => (<Spinner/>);
```

```js title=Size-variations
import React from 'react';
import { Spinner } from '@patternfly/react-core';

SpinnerSizeVariations = () => (
<React.Fragment>
    <Spinner size="sm"/>
    <Spinner size="md"/>
    <Spinner size="lg"/>
    <Spinner size="xl"/>
</React.Fragment>
);
```
