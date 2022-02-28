---
title: 'Empty state'
section: components
cssPrefix: 'pf-c-empty-state'
typescript: true
propComponents: ['EmptyState', 'EmptyStateBody', 'EmptyStateIcon', 'EmptyStateSecondaryActions']
---
import {
  Title,
  Button,
  EmptyState,
  EmptyStateVariant,
  EmptyStateIcon,
  EmptyStateBody,
  EmptyStateSecondaryActions,
  EmptyStatePrimary
} from '@patternfly/react-core';
import { CubesIcon } from '@patternfly/react-icons';

## Examples
```js title=Single
import React from 'react';
import {
  Title,
  Button,
  EmptyState,
  EmptyStateVariant,
  EmptyStateIcon,
  EmptyStateBody,
  EmptyStateSecondaryActions
} from '@patternfly/react-core';
import { CubesIcon } from '@patternfly/react-icons';

SimpleEmptyState = () => (
  <EmptyState variant={EmptyStateVariant.full}>
    <EmptyStateIcon icon={CubesIcon} />
    <Title headingLevel="h5" size="lg">
      Empty State
    </Title>
    <EmptyStateBody>
      This represents an the empty state pattern in Patternfly 4. Hopefully it's simple enough to use but flexible
      enough to meet a variety of needs.
    </EmptyStateBody>
    <Button variant="primary">Primary Action</Button>
    <EmptyStateSecondaryActions>
      <Button variant="link">Multiple</Button>
      <Button variant="link">Action Buttons</Button>
      <Button variant="link">Can</Button>
      <Button variant="link">Go here</Button>
      <Button variant="link">In the secondary</Button>
      <Button variant="link">Action area</Button>
    </EmptyStateSecondaryActions>
  </EmptyState>
);
```

```js title=Small
import React from 'react';
import {
  Title,
  Button,
  EmptyState,
  EmptyStateVariant,
  EmptyStateIcon,
  EmptyStateBody,
  EmptyStateSecondaryActions
} from '@patternfly/react-core';
import { CubesIcon } from '@patternfly/react-icons';

SimpleEmptyState = () => (
  <EmptyState variant={EmptyStateVariant.small}>
    <EmptyStateIcon icon={CubesIcon} />
    <Title headingLevel="h5" size="lg">
      Empty State
    </Title>
    <EmptyStateBody>
      This represents an the empty state pattern in Patternfly 4. Hopefully it's simple enough to use but flexible
      enough to meet a variety of needs.
    </EmptyStateBody>
    <Button variant="primary">Primary Action</Button>
    <EmptyStateSecondaryActions>
      <Button variant="link">Multiple</Button>
      <Button variant="link">Action Buttons</Button>
      <Button variant="link">Can</Button>
      <Button variant="link">Go here</Button>
      <Button variant="link">In the secondary</Button>
      <Button variant="link">Action area</Button>
    </EmptyStateSecondaryActions>
  </EmptyState>
);
```

```js title=Large
import React from 'react';
import {
  Title,
  Button,
  EmptyState,
  EmptyStateVariant,
  EmptyStateIcon,
  EmptyStateBody,
  EmptyStateSecondaryActions
} from '@patternfly/react-core';
import { CubesIcon } from '@patternfly/react-icons';

SimpleEmptyState = () => (
  <EmptyState variant={EmptyStateVariant.large}>
    <EmptyStateIcon icon={CubesIcon} />
    <Title headingLevel="h5" size="lg">
      Empty State
    </Title>
    <EmptyStateBody>
      This represents an the empty state pattern in Patternfly 4. Hopefully it's simple enough to use but flexible
      enough to meet a variety of needs.
    </EmptyStateBody>
    <Button variant="primary">Primary Action</Button>
    <EmptyStateSecondaryActions>
      <Button variant="link">Multiple</Button>
      <Button variant="link">Action Buttons</Button>
      <Button variant="link">Can</Button>
      <Button variant="link">Go here</Button>
      <Button variant="link">In the secondary</Button>
      <Button variant="link">Action area</Button>
    </EmptyStateSecondaryActions>
  </EmptyState>
);
```

```js title=Extra-Large
import React from 'react';
import {
  Title,
  Button,
  EmptyState,
  EmptyStateVariant,
  EmptyStateIcon,
  EmptyStateBody,
  EmptyStateSecondaryActions
} from '@patternfly/react-core';
import { CubesIcon } from '@patternfly/react-icons';

SimpleEmptyState = () => (
  <EmptyState variant={EmptyStateVariant.xl}>
    <EmptyStateIcon icon={CubesIcon} />
    <Title headingLevel="h5" size="4xl">
      Empty State
    </Title>
    <EmptyStateBody>
      This represents an the empty state pattern in Patternfly 4. Hopefully it's simple enough to use but flexible
      enough to meet a variety of needs.
    </EmptyStateBody>
    <Button variant="primary">Primary Action</Button>
    <EmptyStateSecondaryActions>
      <Button variant="link">Multiple</Button>
      <Button variant="link">Action Buttons</Button>
      <Button variant="link">Can</Button>
      <Button variant="link">Go here</Button>
      <Button variant="link">In the secondary</Button>
      <Button variant="link">Action area</Button>
    </EmptyStateSecondaryActions>
  </EmptyState>
);
```

```js title=Spinner
import React from 'react';
import {
  Title,
  Button,
  EmptyState,
  EmptyStateVariant,
  EmptyStateIcon,
  EmptyStateBody,
  EmptyStateSecondaryActions
} from '@patternfly/react-core';


EmptyStateSpinner = () => {
  const Spinner = () => (
    <span className="pf-c-spinner" role="progressbar" aria-valuetext="Loading...">
      <span className="pf-c-spinner__clipper" />
      <span className="pf-c-spinner__lead-ball" />
      <span className="pf-c-spinner__tail-ball" />
    </span>
  )
  return (
    <EmptyState>
      <EmptyStateIcon variant="container" component={Spinner} />
      <Title size="lg">
        Loading
      </Title>
    </EmptyState>
  );
}
```

```js title=No-match-found
import React from 'react';
import {
  Title,
  Button,
  EmptyState,
  EmptyStatePrimary,
  EmptyStateVariant,
  EmptyStateIcon,
  EmptyStateBody,
  EmptyStateSecondaryActions
} from '@patternfly/react-core';
import { SearchIcon } from '@patternfly/react-icons';


NoMatchEmptyState = () => {
  return (
    <EmptyState>
      <EmptyStateIcon icon={SearchIcon} />
      <Title size="lg">
        No results found
      </Title>
      <EmptyStateBody>
        No results match the filter criteria. Remove all filters or clear all filters to show results.
      </EmptyStateBody>
      <EmptyStatePrimary>
        <Button variant="link">Clear all filters</Button>
      </EmptyStatePrimary>
    </EmptyState>
  );
}
```
