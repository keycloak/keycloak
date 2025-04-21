---
id: Progress
section: components
cssPrefix: pf-c-progress
propComponents: ['Progress']
---

## Examples
### Basic
```ts
import React from 'react';
import { Progress, ProgressSize, ProgressMeasureLocation, ProgressVariant } from '@patternfly/react-core';

<Progress value={33} title="Title" />
```

### Small
```ts
import React from 'react';
import { Progress, ProgressSize, ProgressMeasureLocation, ProgressVariant } from '@patternfly/react-core';

<Progress value={33} title="Title" size={ProgressSize.sm} />
```

### Large
```ts
import React from 'react';
import { Progress, ProgressSize, ProgressMeasureLocation, ProgressVariant } from '@patternfly/react-core';

<Progress value={33} title="Title" size={ProgressSize.lg} />
```

### Outside
```ts
import React from 'react';
import { Progress, ProgressSize, ProgressMeasureLocation, ProgressVariant } from '@patternfly/react-core';

<Progress value={33} title="Title" measureLocation={ProgressMeasureLocation.outside} />
```

### Inside
```ts
import React from 'react';
import { Progress, ProgressSize, ProgressMeasureLocation, ProgressVariant } from '@patternfly/react-core';

<Progress value={33} title="Title" measureLocation={ProgressMeasureLocation.inside} />
```

### Success
```ts
import React from 'react';
import { Progress, ProgressSize, ProgressMeasureLocation, ProgressVariant } from '@patternfly/react-core';

<Progress value={100} title="Title" variant={ProgressVariant.success} />
```

### Failure
```ts
import React from 'react';
import { Progress, ProgressSize, ProgressMeasureLocation, ProgressVariant } from '@patternfly/react-core';

<Progress value={33} title="Title" variant={ProgressVariant.danger} />
```

### Warning
```ts
import React from 'react';
import { Progress, ProgressSize, ProgressMeasureLocation, ProgressVariant } from '@patternfly/react-core';

<Progress value={90} title="Title" variant={ProgressVariant.warning} />
```

### Inside success
```ts
import React from 'react';
import { Progress, ProgressSize, ProgressMeasureLocation, ProgressVariant } from '@patternfly/react-core';

<Progress
  value={100}
  title="Title"
  measureLocation={ProgressMeasureLocation.inside}
  variant={ProgressVariant.success}
/>
```

### Outside failure
```ts
import React from 'react';
import { Progress, ProgressMeasureLocation, ProgressVariant } from '@patternfly/react-core';

<Progress
  value={33}
  title="Title"
  measureLocation={ProgressMeasureLocation.outside}
  variant={ProgressVariant.danger}
/>
```

### Single line
```ts
import React from 'react';
import { Progress, ProgressMeasureLocation, ProgressVariant } from '@patternfly/react-core';

<Progress value={33} measureLocation={ProgressMeasureLocation.outside} aria-label="Title"/>
```

### Without measure
```ts
import React from 'react';
import { Progress, ProgressMeasureLocation } from '@patternfly/react-core';

<Progress value={33} title="Title" measureLocation={ProgressMeasureLocation.none} />
```

### Failure without measure
```ts
import React from 'react';
import { Progress, ProgressMeasureLocation, ProgressVariant } from '@patternfly/react-core';

<Progress
  value={33}
  title="Title"
  measureLocation={ProgressMeasureLocation.none}
  variant={ProgressVariant.danger}
/>
```


### Finite step
```ts
import React from 'react';
import { Progress, ProgressMeasureLocation } from '@patternfly/react-core';

<Progress value={2} min={0} max={5} title="Title" measureLocation={ProgressMeasureLocation.top} label="2 of 5" valueText="2 of 5"/>
```

### Progress (step instruction)
```ts
import React from 'react';
import { Progress} from '@patternfly/react-core';

<Progress value={2} title="Title" min={0} max={5} label="Step 2: Copying files" valueText="Step 2: Copying files" />
```

### Truncate title
```ts
import React from 'react';
import { Progress } from '@patternfly/react-core';

<Progress value={33} title="Very very very very very very very very very very very long title which should be truncated if it does not fit onto one line above the progress bar" isTitleTruncated />
```

### Title outside of progress bar
```ts
import React from 'react';
import {
    DescriptionList, 
    DescriptionListGroup, 
    DescriptionListTerm, 
    DescriptionListDescription,
    Progress, 
    ProgressMeasureLocation,
} from '@patternfly/react-core';

<DescriptionList>
  <DescriptionListGroup>
    <DescriptionListTerm id="progress-label">
        Title outside of progress bar
    </DescriptionListTerm>
    <DescriptionListDescription>
      <Progress value={33} measureLocation={ProgressMeasureLocation.outside} aria-labelledby="progress-label"/>
    </DescriptionListDescription>
  </DescriptionListGroup>
</DescriptionList>
```
