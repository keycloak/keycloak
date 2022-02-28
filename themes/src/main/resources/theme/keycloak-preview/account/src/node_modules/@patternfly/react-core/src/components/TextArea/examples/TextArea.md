---
title: 'Text area'
section: components
cssPrefix: 'pf-c-form-control'
propComponents: ['TextArea']
typescript: true
---

import { TextArea } from '@patternfly/react-core';

## Examples
```js title=Basic
import React from 'react';
import { TextArea } from '@patternfly/react-core';

class SimpleTextArea extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: ''
    };

    this.handleTextAreaChange = value => {
      this.setState({ value });
    };
  }

  render() {
    const { value } = this.state;

    return <TextArea value={value} onChange={this.handleTextAreaChange} aria-label="text area example" />;
  }
}
```

```js title=Invalid
import React from 'react';
import { TextArea } from '@patternfly/react-core';

class InvalidTextArea extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: ''
    };
    
    this.handleInvalidTextAreaChange = value => {
      this.setState({ value });
    };
  }

  render() {
    const { value } = this.state;

    return (
      <TextArea
        value={value}
        onChange={this.handleInvalidTextAreaChange}
        isRequired
        isValid={false}
        aria-label="invalid text area example"
      />
    );
  }
}
```

```js title=Validated
import React from 'react';
import { Form, FormGroup, TextArea } from '@patternfly/react-core';

class InvalidTextArea extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: '',
      invalidText: 'You must have something to say',
      isValid: false,
      validated: 'default',
      helperText: 'Enter comments'
    };
    
    this.handleTextAreaChange = value => {
      if (this.validationTimeout) {
        clearTimeout(this.validationTimeout);
      }

      const isValid = !!value && value.length > 0;
      this.setState({
        value,
        isValid,
        helperText: 'Validating...',
        invalidText: 'You must have something to say',
        validated: 'error'
      });

      if (isValid) {
        this.validationTimeout = setTimeout(() => {
          if (!this.state.isValid) {
            this.setState({validated: 'error', invalidText: 'You must have something to say'});
          } else if (value.length > 10) {
            this.setState({isValid: true, validated: 'success', helperText: 'Thanks for your comments!'});
          } else {
            this.setState({isValid: false, validated: 'error', invalidText: 'Your being too brief, please enter at least 10 characters.'});
          }
        }, 2000);
      }
    };
  }

  render() {
    const { value, isValid, validated, helperText, invalidText } = this.state;

    return (
      <Form>
        <FormGroup
          label="Comments:"
          type="string"
          helperText={helperText}
          helperTextInvalid={invalidText}
          fieldId="selection"
          isValid={isValid}
          validated={validated}
        >
          <TextArea
            value={value}
            onChange={this.handleTextAreaChange}
            isRequired
            validated={validated}
            aria-label="invalid text area example"
          />
        </FormGroup>
      </Form>
    );
  }
}
```

```js title=Vertically-resizable-text-area
import React from 'react';
import { TextArea } from '@patternfly/react-core';

class VerticalResizeTextArea extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: ''
    };

    this.handleTextAreaChange = value => {
      this.setState({ value });
    };
  }

  render() {
    const { value } = this.state;

    return <TextArea value={value} onChange={this.handleTextAreaChange} resizeOrientation='vertical' aria-label="text vertical resize example" />;
  }
}
```

```js title=Horizontally-resizable-text-area
import React from 'react';
import { TextArea } from '@patternfly/react-core';

class horizontalResizeTextArea extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: ''
    };

    this.handleTextAreaChange = value => {
      this.setState({ value });
    };
  }

  render() {
    const { value } = this.state;

    return <TextArea value={value} onChange={this.handleTextAreaChange} resizeOrientation='horizontal' aria-label="text horizontal resize example" />;
  }
}
```

```js title=Uncontrolled
import React from 'react';
import { TextArea } from '@patternfly/react-core';

UncontrolledTextArea = () => (
  <TextArea defaultValue="default value" aria-label="uncontrolled text area example" />
)
```
