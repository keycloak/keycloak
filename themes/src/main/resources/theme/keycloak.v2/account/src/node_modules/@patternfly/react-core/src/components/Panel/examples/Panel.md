---
id: Panel
section: components
cssPrefix: pf-c-panel
propComponents: [Panel, PanelMain, PanelMainBody, PanelHeader, PanelFooter]
beta: true
---

## Examples

### Basic

```js
import React from 'react';
import { Panel, PanelMain, PanelMainBody } from '@patternfly/react-core';

const BasicPanel = () => (
  <Panel>
    <PanelMain>
      <PanelMainBody>Main content</PanelMainBody>
    </PanelMain>
  </Panel>
);
```

### Header

```js
import React from 'react';
import { Panel, PanelMain, PanelMainBody, PanelHeader, Divider } from '@patternfly/react-core';

const HeaderPanel = () => (
  <Panel>
    <PanelHeader>Header content</PanelHeader>
    <Divider />
    <PanelMain>
      <PanelMainBody>Main content</PanelMainBody>
    </PanelMain>
  </Panel>
);
```

### Footer

```js
import React from 'react';
import { Panel, PanelMain, PanelMainBody, PanelFooter } from '@patternfly/react-core';

const FooterPanel = () => (
  <Panel>
    <PanelMain>
      <PanelMainBody>Main content</PanelMainBody>
    </PanelMain>
    <PanelFooter>Footer content</PanelFooter>
  </Panel>
);
```

### Header and footer

```js
import React from 'react';
import { Panel, PanelMain, PanelMainBody, PanelHeader, Divider, PanelFooter } from '@patternfly/react-core';

const HeaderFooterPanel = () => (
  <Panel>
    <PanelHeader>Header content</PanelHeader>
    <Divider />
    <PanelMain>
      <PanelMainBody>Main content</PanelMainBody>
    </PanelMain>
    <PanelFooter>Footer content</PanelFooter>
  </Panel>
);
```

### No body

```js
import React from 'react';
import { Panel, PanelMain } from '@patternfly/react-core';

const NoBodyPanel = () => (
  <Panel>
    <PanelMain>Main content</PanelMain>
  </Panel>
);
```

### Raised

```js
import React from 'react';
import { Panel, PanelMain, PanelMainBody } from '@patternfly/react-core';

const RaisedPanel = () => (
  <Panel variant="raised">
    <PanelMain>
      <PanelMainBody>Main content</PanelMainBody>
    </PanelMain>
  </Panel>
);
```

### Bordered

```js
import React from 'react';
import { Panel, PanelMain, PanelMainBody } from '@patternfly/react-core';

const BorderedPanel = () => (
  <Panel variant="bordered">
    <PanelMain>
      <PanelMainBody>Main content</PanelMainBody>
    </PanelMain>
  </Panel>
);
```

### Scrollable

```js
import React from 'react';
import { Panel, PanelMain, PanelMainBody } from '@patternfly/react-core';

const ScrollablePanel = () => (
  <Panel isScrollable>
    <PanelMain tabIndex={0}>
      <PanelMainBody>
        Main content
        <br />
        <br />
        Main content
        <br />
        <br />
        Main content
        <br />
        <br />
        Main content
        <br />
        <br />
        Main content
        <br />
        <br />
        Main content
        <br />
        <br />
        Main content
        <br />
        <br />
        Main content
        <br />
        <br />
        Main content
      </PanelMainBody>
    </PanelMain>
  </Panel>
);
```

### Scrollable with header and footer

```js
import React from 'react';
import { Panel, PanelMain, PanelMainBody, PanelHeader, Divider, PanelFooter } from '@patternfly/react-core';

const ScrollableHeaderFooterPanel = () => (
  <Panel isScrollable>
    <PanelHeader>Header content</PanelHeader>
    <Divider />
    <PanelMain tabIndex={0}>
      <PanelMainBody>
        Main content
        <br />
        <br />
        Main content
        <br />
        <br />
        Main content
        <br />
        <br />
        Main content
        <br />
        <br />
        Main content
        <br />
        <br />
        Main content
        <br />
        <br />
        Main content
        <br />
        <br />
        Main content
        <br />
        <br />
        Main content
      </PanelMainBody>
    </PanelMain>
    <PanelFooter>Footer content</PanelFooter>
  </Panel>
);
```
