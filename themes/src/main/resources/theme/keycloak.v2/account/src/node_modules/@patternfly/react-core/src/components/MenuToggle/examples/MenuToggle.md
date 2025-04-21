---
id: Menu toggle
section: components
cssPrefix: pf-c-menu-toggle
propComponents: ['MenuToggle']
beta: true
---

import './MenuToggle.css'

import CogIcon from '@patternfly/react-icons/dist/esm/icons/cog-icon';
import EllipsisVIcon from '@patternfly/react-icons/dist/esm/icons/ellipsis-v-icon';
import imgAvatar from '@patternfly/react-core/src/components/Avatar/examples/avatarImg.svg';

## Examples

### Collapsed

```ts
import React from 'react';
import { MenuToggle } from '@patternfly/react-core';

<MenuToggle>Collapsed</MenuToggle>

```

### Expanded

```ts
import React from 'react';
import { MenuToggle } from '@patternfly/react-core';

<MenuToggle isExpanded>Expanded</MenuToggle>;

```

### Disabled

```ts
import React from 'react';
import { MenuToggle } from '@patternfly/react-core';

<MenuToggle isDisabled>Disabled</MenuToggle>

```

### Count

```ts
import React from 'react';
import { MenuToggle, Badge } from '@patternfly/react-core';

<MenuToggle badge={<Badge>4 selected</Badge>}>Count</MenuToggle>

```

### Primary

```ts
import React from 'react';
import { MenuToggle } from '@patternfly/react-core';
import CogIcon from '@patternfly/react-icons/dist/esm/icons/cog-icon';

<React.Fragment>
  <MenuToggle variant="primary">Collapsed</MenuToggle>{' '}
  <MenuToggle variant="primary" icon={<CogIcon />}>
    Icon
  </MenuToggle>{' '}
  <MenuToggle variant="primary" isExpanded>
    Expanded
  </MenuToggle>{' '}
  <MenuToggle variant="primary" isDisabled>
    Disabled
  </MenuToggle>
</React.Fragment>

```

### Secondary

```ts
import React from 'react';
import { MenuToggle } from '@patternfly/react-core';
import CogIcon from '@patternfly/react-icons/dist/esm/icons/cog-icon';

<React.Fragment>
  <MenuToggle variant="secondary">Collapsed</MenuToggle>{' '}
    <MenuToggle variant="secondary" icon={<CogIcon />}>
    Icon
  </MenuToggle>{' '}
  <MenuToggle variant="secondary" isExpanded>
    Expanded
  </MenuToggle>{' '}
  <MenuToggle variant="secondary" isDisabled>
    Disabled
  </MenuToggle>
</React.Fragment>

```

### Plain

```ts
import React from 'react';
import { MenuToggle } from '@patternfly/react-core';
import EllipsisVIcon from '@patternfly/react-icons/dist/esm/icons/ellipsis-v-icon';

<React.Fragment>
  <MenuToggle variant="plain" aria-label="plain kebab">
    <EllipsisVIcon />
  </MenuToggle>
  <MenuToggle variant="plain" isExpanded aria-label="plain expanded kebab">
    <EllipsisVIcon />
  </MenuToggle>
  <MenuToggle variant="plain" isDisabled aria-label="disabled kebab">
    <EllipsisVIcon />
  </MenuToggle>
</React.Fragment>
```
### Plain with text

```ts
import React from 'react';
import { MenuToggle } from '@patternfly/react-core';

<React.Fragment>
  <MenuToggle variant="plainText" isDisabled aria-label="Disabled plain menu toggle">
    Disabled
  </MenuToggle>
  &nbsp;
  <MenuToggle variant="plainText" aria-label="Plain menu toggle">
    Custom text
  </MenuToggle>
  <MenuToggle variant="plainText" isExpanded aria-label="Expanded plain menu toggle">
    Custom text (expanded)
  </MenuToggle>
</React.Fragment>
```


### With icon/image and text

```ts
import React from 'react';
import { MenuToggle, Avatar } from '@patternfly/react-core';
import CogIcon from '@patternfly/react-icons/dist/esm/icons/cog-icon';

<React.Fragment>
  <MenuToggle icon={<CogIcon />} variant="secondary">Icon</MenuToggle>{' '}
  <MenuToggle icon={<CogIcon />} variant="secondary" isDisabled>Icon</MenuToggle>
</React.Fragment>
```

### With avatar and text

```ts
import React from 'react';
import { MenuToggle, Avatar } from '@patternfly/react-core';
import imgAvatar from '@patternfly/react-core/src/components/Avatar/examples/avatarImg.svg';

<React.Fragment>
  <MenuToggle icon={<Avatar src={imgAvatar} alt="avatar" />}>Ned Username</MenuToggle>{' '}
  <MenuToggle icon={<Avatar src={imgAvatar} alt="avatar" />} isExpanded>Ned Username</MenuToggle>{' '}
  <MenuToggle icon={<Avatar src={imgAvatar} alt="avatar" />} isDisabled>Ned Username</MenuToggle>
</React.Fragment>
```

### Full height

```ts
import React from 'react';
import { MenuToggle } from '@patternfly/react-core';

<div style={{ height: "80px" }}>
  <MenuToggle isFullHeight aria-label="Full height menu toggle">
    Full height
  </MenuToggle>
</div>
```

### Full width

```ts
import React from 'react';
import { MenuToggle } from '@patternfly/react-core';

const fullWidth: React.FunctionComponent = () => {
  return (
    <MenuToggle isFullWidth aria-label="Full width menu toggle" >
      Full width 
    </MenuToggle>
  );
}
    
```
