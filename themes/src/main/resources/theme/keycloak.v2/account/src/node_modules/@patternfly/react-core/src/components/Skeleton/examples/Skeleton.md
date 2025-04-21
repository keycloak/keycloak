---
id: Skeleton
section: components
cssPrefix: pf-c-skeleton
propComponents: ['Skeleton']
---

## Examples

### Default

```js
import React from 'react';
import { Skeleton } from '@patternfly/react-core';

<Skeleton screenreaderText="Loading contents" />;
```

### Percentage widths

```js
import React from 'react';
import { Skeleton } from '@patternfly/react-core';

<React.Fragment>
  <Skeleton width="25%" screenreaderText="Loading contents" />
  <br />
  <Skeleton width="33%" />
  <br />
  <Skeleton width="50%" />
  <br />
  <Skeleton width="66%" />
  <br />
  <Skeleton width="75%" />
  <br />
  <Skeleton />
</React.Fragment>;
```

### Percentage heights

```js
import React from 'react';
import { Skeleton } from '@patternfly/react-core';

<div style={{ height: '400px', display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between' }}>
  <Skeleton height="25%" width="15%" screenreaderText="Loading contents" />
  <Skeleton height="33%" width="15%" />
  <Skeleton height="50%" width="15%" />
  <Skeleton height="66%" width="15%" />
  <Skeleton height="75%" width="15%" />
  <Skeleton height="100%" width="15%" />
</div>;
```

### Text modifiers

```js
import React from 'react';
import { Skeleton } from '@patternfly/react-core';

<React.Fragment>
  --pf-global--FontSize--4xl
  <Skeleton fontSize="4xl" screenreaderText="Loading font size 4xl" />
  <br />
  --pf-global--FontSize--3xl
  <Skeleton fontSize="3xl" screenreaderText="Loading font size 3xl" />
  <br />
  --pf-global--FontSize--2xl
  <Skeleton fontSize="2xl" screenreaderText="Loading font size 2xl" />
  <br />
  --pf-global--FontSize--xl
  <Skeleton fontSize="xl" screenreaderText="Loading font size xl" />
  <br />
  --pf-global--FontSize--lg
  <Skeleton fontSize="lg" screenreaderText="Loading font size lg" />
  <br />
  --pf-global--FontSize--md
  <Skeleton fontSize="md" screenreaderText="Loading font size md" />
  <br />
  --pf-global--FontSize--sm
  <Skeleton fontSize="sm" screenreaderText="Loading font size sm" />
</React.Fragment>;
```

### Shapes

```js
import React from 'react';
import { Skeleton } from '@patternfly/react-core';

<React.Fragment>
  Small circle
  <Skeleton shape="circle" width="15%" screenreaderText="Loading small circle contents" />
  <br />
  Medium circle
  <Skeleton shape="circle" width="30%" screenreaderText="Loading medium circle contents" />
  <br />
  Large circle
  <Skeleton shape="circle" width="50%" screenreaderText="Loading large circle contents" />
  <br />
  Small square
  <Skeleton shape="square" width="15%" screenreaderText="Loading small square contents" />
  <br />
  Medium square
  <Skeleton shape="square" width="30%" screenreaderText="Loading medium square contents" />
  <br />
  Large square
  <Skeleton shape="square" width="50%" screenreaderText="Loading large square contents" />
  <br />
  Small rectangle
  <div style={{ height: '200px' }}>
    <Skeleton height="50%" width="50%" screenreaderText="Loading small rectangle contents" />
  </div>
  <br />
  Medium rectangle
  <div style={{ height: '200px' }}>
    <Skeleton height="75%" width="75%" screenreaderText="Loading medium rectangle contents" />
  </div>
  <br />
  Large rectangle
  <div style={{ height: '200px' }}>
    <Skeleton height="100%" screenreaderText="Loading large rectangle contents" />
  </div>
</React.Fragment>;
```
