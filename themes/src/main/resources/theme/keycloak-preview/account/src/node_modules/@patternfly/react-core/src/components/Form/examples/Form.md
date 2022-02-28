---
title: 'Form'
section: components
cssPrefix: 'pf-c-form'
typescript: true
propComponents: ['ActionGroup', 'Form', 'FormGroup', 'FormHelperText']
---
import {
  Form,
  FormGroup,
  TextInput,
  TextArea,
  FormSelectionOption,
  FormSelect,
  Checkbox,
  ActionGroup,
  Button,
  Radio
} from '@patternfly/react-core';

## Examples
```js title=Basic
import React from 'react';
import {
  Form,
  FormGroup,
  TextInput,
  TextArea,
  FormSelectionOption,
  FormSelect,
  Checkbox,
  ActionGroup,
  Button,
  Radio
} from '@patternfly/react-core';

class SimpleForm extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value1: '',
      value2: '',
      value3: ''
    };
    this.handleTextInputChange1 = value1 => {
      this.setState({ value1 });
    };
    this.handleTextInputChange2 = value2 => {
      this.setState({ value2 });
    };
    this.handleTextInputChange3 = value3 => {
      this.setState({ value3 });
    };
  }

  render() {
    const { value1, value2, value3 } = this.state;

    return (
      <Form>
        <FormGroup
          label="Name"
          isRequired
          fieldId="simple-form-name"
          helperText="Please provide your full name"
        >
          <TextInput
            isRequired
            type="text"
            id="simple-form-name"
            name="simple-form-name"
            aria-describedby="simple-form-name-helper"
            value={value1}
            onChange={this.handleTextInputChange1}
          />
        </FormGroup>
        <FormGroup label="Email" isRequired fieldId="simple-form-email">
          <TextInput
            isRequired
            type="email"
            id="simple-form-email"
            name="simple-form-email"
            value={value2}
            onChange={this.handleTextInputChange2}
          />
        </FormGroup>
        <FormGroup label="Phone number" isRequired fieldId="simple-form-number">
          <TextInput
            isRequired
            type="tel"
            id="simple-form-number"
            placeholder="555-555-5555"
            name="simple-form-number"
            value={value3}
            onChange={this.handleTextInputChange3}
          />
        </FormGroup>
        <FormGroup isInline label="How can we contact you?" isRequired>
          <Checkbox label="Email" aria-label="Email" id="inlinecheck1" />
          <Checkbox label="Phone" aria-label="Phone" id="inlinecheck2" />
          <Checkbox label="Please don't contact me" aria-label="Please don't contact me" id="inlinecheck3"/>
        </FormGroup>
        <FormGroup label="Additional Note:" fieldId="simple-form-note">
          <TextInput isDisabled type="text" id="simple-form-note" name="simple-form-number" value="disabled" />
        </FormGroup>
        <FormGroup fieldId="checkbox1">
          <Checkbox label="I'd like updates via email" id="checkbox1" name="checkbox1" aria-label="Update via email" />
        </FormGroup>
        <ActionGroup>
          <Button variant="primary">Submit form</Button>
          <Button variant="secondary">Cancel</Button>
        </ActionGroup>
      </Form>
    );
  }
}
```

```js title=Horizontal
import React from 'react';
import {
  Form,
  FormGroup,
  TextInput,
  TextArea,
  FormSelect,
  FormSelectOption,
  Checkbox,
  ActionGroup,
  Button,
  Radio
} from '@patternfly/react-core';

class HorizontalForm extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: 'please choose',
      value1: '',
      value2: '',
      value3: ''
    };
    this.onChange = (value, event) => {
      this.setState({ value });
    };
    this.handleTextInputChange1 = value1 => {
      this.setState({ value1 });
    };
    this.handleTextInputChange2 = value2 => {
      this.setState({ value2 });
    };
    this.handleTextInputChange3 = value3 => {
      this.setState({ value3 });
    };
    this.options = [
      { value: 'please choose', label: 'Please Choose', disabled: false },
      { value: 'mr', label: 'Mr', disabled: false },
      { value: 'miss', label: 'Miss', disabled: false },
      { value: 'mrs', label: 'Mrs', disabled: false },
      { value: 'ms', label: 'Ms', disabled: false },
      { value: 'dr', label: 'Dr', disabled: false },
      { value: 'other', label: 'Other', disabled: false }
    ];
  }

  render() {
    const { value1, value2, value3 } = this.state;

    return (
      <Form isHorizontal>
        <FormGroup
          label="Name"
          isRequired
          fieldId="horizontal-form-name"
          helperText="Please provide your full name"
        >
          <TextInput
            value={value1}
            isRequired
            type="text"
            id="horizontal-form-name"
            aria-describedby="horizontal-form-name-helper"
            name="horizontal-form-name"
            onChange={this.handleTextInputChange1}
          />
        </FormGroup>
        <FormGroup label="Email" isRequired fieldId="horizontal-form-email">
          <TextInput
            value={value2}
            onChange={this.handleTextInputChange2}
            isRequired
            type="email"
            id="horizontal-form-email"
            name="horizontal-form-email"
          />
        </FormGroup>
        <FormGroup label="Your title" fieldId="horizontal-form-title">
          <FormSelect
            value={this.state.value}
            onChange={this.onChange}
            id="horzontal-form-title"
            name="horizontal-form-title"
            aria-label="Your title"
          >
            {this.options.map((option, index) => (
              <FormSelectOption isDisabled={option.disabled} key={index} value={option.value} label={option.label} />
            ))}
          </FormSelect>
        </FormGroup>
        <FormGroup label="Your experience" fieldId="horizontal-form-exp">
          <TextArea
            value={value3}
            onChange={this.handleTextInputChange3}
            name="horizontal-form-exp"
            id="horizontal-form-exp"
          />
        </FormGroup>
        <FormGroup>
          <Checkbox label="Follow up via email" id="alt-form-checkbox-1" name="alt-form-checkbox-1" />
        </FormGroup>
        <FormGroup>
          <Checkbox label="Remember my password for 30 days" id="alt-form-checkbox-2" name="alt-form-checkbox-2" />
        </FormGroup>
        <ActionGroup>
          <Button variant="primary">Submit form</Button>
          <Button variant="secondary">Cancel</Button>
        </ActionGroup>
      </Form>
    );
  }
}
```

```js title=Invalid
import React from 'react';
import {
  Form,
  FormGroup,
  TextInput,
  TextArea,
  FormSelectionOption,
  FormSelect,
  Checkbox,
  ActionGroup,
  Button,
  Radio
} from '@patternfly/react-core';

class InvalidForm extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: 'Five',
      isValid: false
    };
    this.handleTextInputChange = value => {
      this.setState({ value, isValid: /^\d+$/.test(value) });
    };
  }

  render() {
    const { value, isValid } = this.state;

    return (
      <Form>
        <FormGroup
          label="Age:"
          type="number"
          helperText="Please write your age"
          helperTextInvalid="Age has to be a number"
          fieldId="age"
          isValid={isValid}
        >
          <TextInput
            isValid={isValid}
            value={value}
            id="age"
            aria-describedby="age-helper"
            onChange={this.handleTextInputChange}
          />
        </FormGroup>
      </Form>
    );
  }
}
```

```js title=Validated
import React from 'react';
import {
  Form,
  FormGroup,
  TextInput,
  TextArea,
  FormSelectionOption,
  FormSelect,
  Checkbox,
  ActionGroup,
  Button,
  Radio
} from '@patternfly/react-core';

class InvalidForm extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: '',
      invalidText: 'Age has to be a number',
      isValid: true,
      validated: 'default',
      helperText: 'Enter your age to continue'
    };
    this.handleTextInputChange = value => {
      const isValid = /^\d+$/.test(value)
      this.setState({ value, isValid, invalidText: 'Age has to be a number', helperText: 'Validating...', validated: 'error' });
      if (isValid) {
        setTimeout(() => {
          if (this.state.isValid && parseInt(this.state.value, 10) >= 21) {
            this.setState({isValid: true, validated: 'success', helperText: 'Enjoy your stay'});
          } else {
            this.setState({isValid: false, validated: 'error', invalidText: 'You must be at least 21 to continue'});
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
          label="Age:"
          type="number"
          helperText={helperText}
          helperTextInvalid={invalidText}
          fieldId="age"
          validated={validated}
        >
          <TextInput
            validated={validated}
            value={value}
            id="age"
            aria-describedby="age-helper"
            onChange={this.handleTextInputChange}
          />
        </FormGroup>
      </Form>
    );
  }
}
```
