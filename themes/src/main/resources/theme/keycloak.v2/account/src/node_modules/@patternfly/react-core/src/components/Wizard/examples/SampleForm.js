import React from 'react';
import PropTypes from 'prop-types';
import { Form, FormGroup, TextInput } from '@patternfly/react-core';

const propTypes = {
  formValue: PropTypes.string,
  isFormValid: PropTypes.bool,
  onChange: PropTypes.func
};

const defaultProps = {
  formValue: '',
  isFormValid: false,
  onChange: () => undefined
};

class SampleForm extends React.Component {
  static propTypes = propTypes;
  static defaultProps = defaultProps;

  state = {
    value: this.props.formValue,
    isValid: this.props.isFormValid
  };

  handleTextInputChange = value => {
    const isValid = /^\d+$/.test(value);
    this.setState({ value, isValid });
    this.props.onChange && this.props.onChange(isValid, value);
  };

  render() {
    const { value, isValid } = this.state;
    const validated = isValid ? 'default' : 'error';

    return (
      <Form>
        <FormGroup
          label="Age:"
          type="number"
          helperText="Write your age in numbers."
          helperTextInvalid="Age has to be a number"
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

export default SampleForm;
