---
id: Radio
section: components
cssPrefix: pf-c-radio
propComponents: ['Radio']
ouia: true
---

## Examples
### Controlled
```js
import React from 'react';
import { Radio } from '@patternfly/react-core';

class ControlledRadio extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      check1: false,
    };

    this.handleChange = (_, event) => {
      const { value } = event.currentTarget;
      this.setState({ [value]: true });
    };
  }

  render() {
    return (
      <React.Fragment>
        <Radio
          isChecked={this.state.check1}
          name="radio-1"
          onChange={this.handleChange}
          label="Controlled radio"
          id="radio-controlled"
          value="check1"
        />
      </React.Fragment>
    );
  }
}
```

### Uncontrolled
```js
import React from 'react';
import { Radio } from '@patternfly/react-core';

<Radio label="Uncontrolled radio example" id="radio-uncontrolled" name="radio-2" />
```

### Reversed
```js
import React from 'react';
import { Radio } from '@patternfly/react-core';

<Radio isLabelBeforeButton label="Reversed radio example" id="radio-reversed" name="radio-3" />
```

### Label wraps
```js
import React from 'react';
import { Radio } from '@patternfly/react-core';

<Radio isLabelWrapped label="Label wraps input example" id="radio-label-wraps-input" name="radio-4" />
```

### Disabled
```js
import React from 'react';
import { Radio } from '@patternfly/react-core';

<React.Fragment>
  <Radio id="radio-disabled" label="Disabled radio example" isDisabled name="radio-5" />
  <Radio id="radio-disabled-checked" defaultChecked label="Disabled and checked radio example" isDisabled name="radio-6" />
</React.Fragment>
```

### With description
```js
import React from 'react';
import { Radio } from '@patternfly/react-core';

<Radio id="radio-with-description" label="Radio with description example" description="Single-tenant cloud service hosted and managed by Red Hat that offers high-availability enterprise-grade clusters in a virtual private cloud on AWS or GCP." />
```

### With body
```js
import React from 'react';
import { Radio } from '@patternfly/react-core';

<Radio id="radio-with-body" label="Radio with body" body="This is where custom content goes." />
```

### With description and body
```js
import React from 'react';
import { Radio } from '@patternfly/react-core';

<Radio id="radio-description-body" label="Radio with description and body" description="Single-tenant cloud service hosted and managed by Red Hat that offers high-availability enterprise-grade clusters in a virtual private cloud on AWS or GCP." body="This is where custom content goes." />
```

### Standalone input
```js
import React from 'react';
import { Radio } from '@patternfly/react-core';

<Radio id="radio-standalone" aria-label="Standalone input" name="exampleRadioStandalone"/>
```
