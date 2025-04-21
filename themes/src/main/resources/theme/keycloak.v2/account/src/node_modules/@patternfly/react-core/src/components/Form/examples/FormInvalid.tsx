import React from 'react';
import { Form, FormGroup, TextInput, FormHelperText } from '@patternfly/react-core';
import ExclamationCircleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-circle-icon';

export const FormInvalid: React.FunctionComponent = () => {
  type validate = 'success' | 'warning' | 'error' | 'default';

  const [age, setAge] = React.useState('Five');
  const [validated, setValidated] = React.useState<validate>('error');

  const handleAgeChange = (age: string, _event: React.FormEvent<HTMLInputElement>) => {
    setAge(age);
    if (age === '') {
      setValidated('default');
    } else if (/^\d+$/.test(age)) {
      setValidated('success');
    } else {
      setValidated('error');
    }
  };

  return (
    <Form>
      <FormGroup
        label="Age"
        type="number"
        helperText={
          <FormHelperText icon={<ExclamationCircleIcon />} isHidden={validated !== 'default'}>
            Please enter your age
          </FormHelperText>
        }
        helperTextInvalid="Must be a number"
        helperTextInvalidIcon={<ExclamationCircleIcon />}
        fieldId="age-1"
        validated={validated}
      >
        <TextInput
          validated={validated}
          value={age}
          id="age-1"
          aria-describedby="age-1-helper"
          onChange={handleAgeChange}
        />
      </FormGroup>
    </Form>
  );
};
