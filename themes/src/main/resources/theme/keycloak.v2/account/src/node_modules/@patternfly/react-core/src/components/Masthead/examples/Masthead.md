---
id: Masthead
section: components
cssPrefix: pf-c-masthead
propComponents: ['Masthead', 'MastheadToggle', 'MastheadMain', 'MastheadBrand', 'MastheadContent']
beta: true
---

import BarsIcon from '@patternfly/react-icons/dist/js/icons/bars-icon';
import { Link } from '@reach/router';
import pfIcon from './pf-logo-small.svg';

`Masthead` should contain the following components to maintain proper layout and formatting: `MastheadToggle`, `MastheadMain`, and `MastheadContent`.

`MastheadMain` represents the smaller area taken up by a logo, and will typically contain a `MastheadBrand`. `MastheadContent` represents the main portion of the masthead area and will typically contain a `Toolbar` or other menu-like content such as `Dropdown`.

## Examples

### Basic

```ts
import React from 'react';
import { Masthead, MastheadToggle, MastheadMain, MastheadBrand, MastheadContent, Button } from '@patternfly/react-core';
import BarsIcon from '@patternfly/react-icons/dist/js/icons/bars-icon';

<Masthead id="basic">
  <MastheadToggle>
    <Button variant="plain" onClick={() => {}} aria-label="Global navigation">
      <BarsIcon />
    </Button>
  </MastheadToggle>
  <MastheadMain>
    <MastheadBrand>Logo</MastheadBrand>
  </MastheadMain>
  <MastheadContent>
    <span>Content</span>
  </MastheadContent>
</Masthead>;
```

### Basic with mixed content

```ts
import React from 'react';
import {
  Masthead,
  MastheadToggle,
  MastheadMain,
  MastheadBrand,
  MastheadContent,
  Button,
  Flex,
  FlexItem
} from '@patternfly/react-core';
import BarsIcon from '@patternfly/react-icons/dist/js/icons/bars-icon';

<Masthead id="basic-mixed">
  <MastheadToggle>
    <Button variant="plain" onClick={() => {}} aria-label="Global navigation">
      <BarsIcon />
    </Button>
  </MastheadToggle>
  <MastheadMain>
    <MastheadBrand>Logo</MastheadBrand>
  </MastheadMain>
  <MastheadContent>
    <Flex>
      <span>Testing text color</span>
      <Button>testing</Button>
      <FlexItem alignSelf={{ default: 'alignSelfFlexEnd' }}>
        <Button>testing</Button>
      </FlexItem>
    </Flex>
  </MastheadContent>
</Masthead>;
```

### Display inline

```ts
import React from 'react';
import { Masthead, MastheadToggle, MastheadMain, MastheadBrand, MastheadContent, Button } from '@patternfly/react-core';
import BarsIcon from '@patternfly/react-icons/dist/js/icons/bars-icon';

<Masthead id="inline-masthead" display={{ default: 'inline' }}>
  <MastheadToggle>
    <Button variant="plain" onClick={() => {}} aria-label="Global navigation">
      <BarsIcon />
    </Button>
  </MastheadToggle>
  <MastheadMain>
    <MastheadBrand>Logo</MastheadBrand>
  </MastheadMain>
  <MastheadContent>
    <span>Content</span>
  </MastheadContent>
</Masthead>;
```

### Display stack

```ts
import React from 'react';
import { Masthead, MastheadToggle, MastheadMain, MastheadBrand, MastheadContent, Button } from '@patternfly/react-core';
import BarsIcon from '@patternfly/react-icons/dist/js/icons/bars-icon';

<Masthead id="stack-masthead" display={{ default: 'stack' }}>
  <MastheadToggle>
    <Button variant="plain" onClick={() => {}} aria-label="Global navigation">
      <BarsIcon />
    </Button>
  </MastheadToggle>
  <MastheadMain>
    <MastheadBrand>Logo</MastheadBrand>
  </MastheadMain>
  <MastheadContent>
    <span>Content</span>
  </MastheadContent>
</Masthead>;
```

### Display stack, display inline responsive

```ts
import React from 'react';
import { Masthead, MastheadToggle, MastheadMain, MastheadBrand, MastheadContent, Button } from '@patternfly/react-core';
import BarsIcon from '@patternfly/react-icons/dist/js/icons/bars-icon';

<Masthead id="stack-masthead" display={{ default: 'inline', lg: 'stack', '2xl': 'inline' }}>
  <MastheadToggle>
    <Button variant="plain" onClick={() => {}} aria-label="Global navigation">
      <BarsIcon />
    </Button>
  </MastheadToggle>
  <MastheadMain>
    <MastheadBrand>Logo</MastheadBrand>
  </MastheadMain>
  <MastheadContent>
    <span>Content</span>
  </MastheadContent>
</Masthead>;
```

### Light variant

```ts
import React from 'react';
import { Masthead, MastheadToggle, MastheadMain, MastheadBrand, MastheadContent, Button } from '@patternfly/react-core';
import BarsIcon from '@patternfly/react-icons/dist/js/icons/bars-icon';

<Masthead id="light-masthead" backgroundColor="light">
  <MastheadToggle>
    <Button variant="plain" onClick={() => {}} aria-label="Global navigation">
      <BarsIcon />
    </Button>
  </MastheadToggle>
  <MastheadMain>
    <MastheadBrand>Logo</MastheadBrand>
  </MastheadMain>
  <MastheadContent>
    <span>Content</span>
  </MastheadContent>
</Masthead>;
```

### Light 200 variant

```ts
import React from 'react';
import { Masthead, MastheadToggle, MastheadMain, MastheadBrand, MastheadContent, Button } from '@patternfly/react-core';
import BarsIcon from '@patternfly/react-icons/dist/js/icons/bars-icon';

<Masthead id="light200-masthead" backgroundColor="light200">
  <MastheadToggle>
    <Button variant="plain" onClick={() => {}} aria-label="Global navigation">
      <BarsIcon />
    </Button>
  </MastheadToggle>
  <MastheadMain>
    <MastheadBrand>Logo</MastheadBrand>
  </MastheadMain>
  <MastheadContent>
    <span>Content</span>
  </MastheadContent>
</Masthead>;
```

### Inset

```ts
import React from 'react';
import { Masthead, MastheadToggle, MastheadMain, MastheadBrand, MastheadContent, Button } from '@patternfly/react-core';
import BarsIcon from '@patternfly/react-icons/dist/js/icons/bars-icon';

<Masthead id="inset-masthead" inset={{ default: 'insetSm' }}>
  <MastheadToggle>
    <Button variant="plain" onClick={() => {}} aria-label="Global navigation">
      <BarsIcon />
    </Button>
  </MastheadToggle>
  <MastheadMain>
    <MastheadBrand>Logo</MastheadBrand>
  </MastheadMain>
  <MastheadContent>
    <span>Content</span>
  </MastheadContent>
</Masthead>;
```

### With icon router link

```ts
import React from 'react';
import { Masthead, MastheadToggle, MastheadMain, MastheadBrand, MastheadContent, Button } from '@patternfly/react-core';
import BarsIcon from '@patternfly/react-icons/dist/js/icons/bars-icon';
import { Link } from '@reach/router';
import pfIcon from './pf-logo-small.svg';

<Masthead id="basic">
  <MastheadToggle>
    <Button variant="plain" onClick={() => {}} aria-label="Global navigation">
      <BarsIcon />
    </Button>
  </MastheadToggle>
  <MastheadMain>
    <MastheadBrand component={props => <Link {...props} to="#" />}>
      <img src={pfIcon} alt="Patterfly Logo" />
    </MastheadBrand>
  </MastheadMain>
  <MastheadContent>
    <span>Content</span>
  </MastheadContent>
</Masthead>;
```
