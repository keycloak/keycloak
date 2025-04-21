---
id: Tile
section: components
cssPrefix: pf-c-tile
propComponents: ['Tile']
beta: true
---

import PlusIcon from '@patternfly/react-icons/dist/esm/icons/plus-icon';
import BellIcon from '@patternfly/react-icons/dist/esm/icons/bell-icon';

## Examples

Keyboard interaction patterns and a11y is implemented in the Tile demos, located in the [Demo section](/documentation/react/demos/tiledemo).

### Basic

```js
import React from 'react';
import { Tile } from '@patternfly/react-core';

<div role="listbox" aria-label="Basic tiles">
  <Tile title="Default" isSelected={false} />
  <Tile title="Selected" isSelected />
  <Tile title="Disabled" isDisabled isSelected={false} />
</div>;
```

### With subtext

```js
import React from 'react';
import { Tile } from '@patternfly/react-core';

<div role="listbox" aria-label="Tiles with subtext">
  <Tile title="Default" isSelected={false}>
    Subtext goes here
  </Tile>{' '}
  <Tile title="Selected" isSelected>
    Subtext goes here
  </Tile>{' '}
  <Tile title="Disabled" isDisabled isSelected={false}>
    Subtext goes here
  </Tile>
</div>;
```

### With icon

```js
import React from 'react';
import { Tile } from '@patternfly/react-core';
import PlusIcon from '@patternfly/react-icons/dist/esm/icons/plus-icon';

<div role="listbox" aria-label="Tiles with icon">
  <Tile title="Default" icon={<PlusIcon />} isSelected={false}>
    Subtext goes here
  </Tile>{' '}
  <Tile title="Selected" icon={<PlusIcon />} isSelected>
    Subtext goes here
  </Tile>{' '}
  <Tile title="Disabled" icon={<PlusIcon />} isDisabled isSelected={false}>
    Subtext goes here
  </Tile>
</div>;
```

### Stacked

```js
import React from 'react';
import { Tile } from '@patternfly/react-core';
import BellIcon from '@patternfly/react-icons/dist/esm/icons/bell-icon';

<div role="listbox" aria-label="Stacked tiles">
  <Tile title="Default" icon={<BellIcon />} isStacked isSelected={false}>
    Subtext goes here
  </Tile>{' '}
  <Tile title="Selected" icon={<BellIcon />} isStacked isSelected>
    Subtext goes here
  </Tile>{' '}
  <Tile title="Disabled" icon={<BellIcon />} isStacked isDisabled isSelected={false}>
    Subtext goes here
  </Tile>
</div>;
```

### Stacked with large icons

```js
import React from 'react';
import { Tile } from '@patternfly/react-core';
import BellIcon from '@patternfly/react-icons/dist/esm/icons/bell-icon';

<div role="listbox" aria-label="Stacked tiles with large icons">
  <Tile title="Default" icon={<BellIcon />} isStacked isDisplayLarge isSelected={false}>
    Subtext goes here
  </Tile>{' '}
  <Tile title="Selected" icon={<BellIcon />} isStacked isDisplayLarge isSelected>
    Subtext goes here
  </Tile>{' '}
  <Tile title="Disabled" icon={<BellIcon />} isStacked isDisplayLarge isDisabled isSelected={false}>
    Subtext goes here
  </Tile>
</div>;
```

### With extra content

```js
import React from 'react';
import { Tile, Flex } from '@patternfly/react-core';
import BellIcon from '@patternfly/react-icons/dist/esm/icons/bell-icon';

<div role="listbox" aria-label="Tiles with extra content">
  <Flex>
    <Flex flex={{ default: 'flex_1' }}>
      <Tile title="Default" icon={<BellIcon />} isStacked isSelected={false}>
        This is really really long subtext that goes on for so long that it has to wrap to the next line. This is really
        really long subtext that goes on for so long that it has to wrap to the next line.
      </Tile>
    </Flex>
    <Flex flex={{ default: 'flex_1' }}>
      <Tile title="Selected" icon={<BellIcon />} isStacked isSelected>
        This is really really long subtext that goes on for so long that it has to wrap to the next line.
      </Tile>
    </Flex>
    <Flex flex={{ default: 'flex_1' }}>
      <Tile title="Disabled" icon={<BellIcon />} isStacked isDisabled isSelected={false}>
        Subtext goes here
      </Tile>
    </Flex>
  </Flex>
</div>;
```
