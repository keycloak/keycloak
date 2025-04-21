---
id: Spinner
section: components
cssPrefix: pf-c-spinner
propComponents: ['Spinner']
---

## Examples
### Basic
```js
import React from 'react';
import { Spinner } from '@patternfly/react-core';

<Spinner isSVG aria-label="Contents of the basic example"/>
```

### Size variations
```js
import React from 'react';
import { Spinner } from '@patternfly/react-core';

<React.Fragment>
  <Spinner isSVG size="sm" aria-label="Contents of the small example"/>
  <Spinner isSVG size="md" aria-label="Contents of the medium example"/>
  <Spinner isSVG size="lg" aria-label="Contents of the large example"/>
  <Spinner isSVG size="xl" aria-label="Contents of the extra large example"/>
</React.Fragment>
```

### Custom size
```js
import React from 'react';
import { Spinner } from '@patternfly/react-core';

<Spinner isSVG diameter="80px" aria-label="Contents of the custom size example"/>
```

