---
title: 'Text input'
section: components
cssPrefix: 'pf-c-form-control'
propComponents: ['TextInput']
typescript: true
---

import { TextInput, Button } from '@patternfly/react-core';

## Examples
```js title=Basic
import React from 'react';
import { TextInput } from '@patternfly/react-core';

class SimpleTextInput extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: ''
    };
    this.handleTextInputChange = value => {
      this.setState({ value });
    };
  }

  render() {
    const { value } = this.state;

    return (
      <TextInput value={value} type="text" onChange={this.handleTextInputChange} aria-label="text input example" />
    );
  }
}
```

```js title=Disabled
import React from 'react';
import { TextInput } from '@patternfly/react-core';

<TextInput type="text" value="disabled text input example" aria-label="disabled text input example" isDisabled />
```

```js title=Read-only
import React from 'react';
import { TextInput } from '@patternfly/react-core';

<TextInput value="read only text input example" type="text" isReadOnly aria-label="readonly input example" />
```

```js title=Invalid
import React from 'react';
import { TextInput } from '@patternfly/react-core';

class InvalidTextInput extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: ''
    };
    this.handleInvalidTextInputChange = value => {
      this.setState({ value });
    };
  }

  render() {
    const { value } = this.state;

    return (
      <TextInput
        value={value}
        onChange={this.handleInvalidTextInputChange}
        isRequired
        isValid={false}
        type="text"
        aria-label="invalid text input example"
      />
    );
  }
}
```

```js title=Select-text-using-ref
import React from 'react';
import { TextInput, Button } from '@patternfly/react-core';

TextInputSelectAll = () => {
  const [value, setValue] = React.useState('select all on click');
  const ref = React.useRef(null);
  return (
    <React.Fragment>
      <TextInput ref={ref} value={value} onFocus={() => ref && ref.current && ref.current.select()} onChange={value => setValue(value)} aria-label="select-all" />
    </React.Fragment>
  )
}
```
