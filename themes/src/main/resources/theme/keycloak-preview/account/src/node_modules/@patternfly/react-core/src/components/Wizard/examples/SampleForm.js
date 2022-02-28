import React from 'react';
import PropTypes from 'prop-types';
import { Form } from '@patternfly/react-core/dist/js/components/Form/Form';
import { FormGroup } from '@patternfly/react-core/dist/js/components/Form/FormGroup';
import { TextInput } from '@patternfly/react-core/dist/js/components/TextInput/TextInput';

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

export default SampleForm;
