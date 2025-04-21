---
id: Switch
section: components
cssPrefix: pf-c-switch
propComponents: ['Switch']
ouia: true
---

## Examples

### Basic

```js
import React from 'react';
import { Switch } from '@patternfly/react-core';

class SimpleSwitch extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isChecked: true
    };
    this.handleChange = isChecked => {
      this.setState({ isChecked });
    };
  }

  render() {
    const { isChecked } = this.state;
    return (
      <Switch
        id="simple-switch"
        label="Message when on"
        labelOff="Message when off"
        isChecked={isChecked}
        onChange={this.handleChange}
      />
    );
  }
}
```

### Reversed Layout

```js
import React from 'react';
import { Switch } from '@patternfly/react-core';

class ReversedSwitch extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isChecked: true
    };
    this.handleChange = isChecked => {
      this.setState({ isChecked });
    };
  }

  render() {
    const { isChecked } = this.state;
    return (
      <Switch
        id="reversed-switch"
        label="Message when on"
        labelOff="Message when off"
        isChecked={isChecked}
        onChange={this.handleChange}
        isReversed
      />
    );
  }
}
```

### Without label

```js
import React from 'react';
import { Switch } from '@patternfly/react-core';

class NoLabelSwitch extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isChecked: true
    };
    this.handleChange = isChecked => {
      this.setState({ isChecked });
    };
  }

  render() {
    const { isChecked } = this.state;
    return (
      <Switch id="no-label-switch-on" aria-label="Message when on" isChecked={isChecked} onChange={this.handleChange} />
    );
  }
}
```

### Checked with label

```js
import React from 'react';
import { Switch } from '@patternfly/react-core';

class CheckedWithLabelSwitch extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isChecked: true
    };
    this.handleChange = isChecked => {
      this.setState({ isChecked });
    };
  }

  render() {
    const { isChecked } = this.state;
    return (
      <Switch
        label="Message when on"
        labelOff="Message when off"
        id="checked-with-label-switch-on"
        aria-label="Message when on"
        isChecked={isChecked}
        hasCheckIcon
        onChange={this.handleChange}
      />
    );
  }
}
```

### Disabled

```js
import React from 'react';
import { Switch } from '@patternfly/react-core';

<React.Fragment>
  <Switch
    id="disabled-switch-on"
    aria-label="Message when on"
    label="Message when on"
    labelOff="Message when off"
    isChecked
    isDisabled
  />
  <br />
  <Switch
    id="disabled-switch-off"
    aria-label="Message when on"
    label="Message when on"
    labelOff="Message when off"
    isChecked={false}
    isDisabled
  />
  <br />
  <Switch id="disabled-no-label-switch-on" aria-label="Message when on" isChecked isDisabled />
  <br />
  <Switch id="disabled-no-label-switch-off" aria-label="Message when on" isChecked={false} isDisabled />
</React.Fragment>;
```

### Uncontrolled

```js
import React from 'react';
import { Switch } from '@patternfly/react-core';

<React.Fragment>
  <Switch id="uncontrolled-switch-on" label="Message when on" labelOff="Message when off" defaultChecked={false} />
  <br />
  <Switch id="uncontrolled-no-label-switch-on" aria-label="Message when on" defaultChecked={true} />
</React.Fragment>;
```
