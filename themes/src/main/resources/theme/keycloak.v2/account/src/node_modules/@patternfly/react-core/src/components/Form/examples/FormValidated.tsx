import React from 'react';
import { Form, FormGroup, TextInput } from '@patternfly/react-core';
import ExclamationCircleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-circle-icon';

export const FormValidated: React.FunctionComponent = () => {
  type validate = 'success' | 'warning' | 'error' | 'default';

  const [age, setAge] = React.useState('Five');
  const [invalidText, setInvalidText] = React.useState('Must be a number');
  const [validated, setValidated] = React.useState<validate>('default');
  const [helperText, setHelperText] = React.useState('Enter your age to continue');

  const handleAgeChange = (age: string, _event: React.FormEvent<HTMLInputElement>) => {
    setAge(age);
    setValidated('default');
    setHelperText('Validating...');
  };

  // useEffect is used to simulate a server call to validate the age 2000ms after the user has entered a value, preventing calling the server on every keystroke
  React.useEffect(() => {
    const timer = setTimeout(() => {
      if (/^\d+$/.test(age)) {
        if (parseInt(age) >= 21) {
          setValidated('success');
          setHelperText('Enjoy your stay');
        } else {
          setValidated('warning');
          setHelperText('You must be at least 21 years old to continue');
        }
      } else {
        setValidated('error');
        setInvalidText('Must be a number');
      }
    }, 2000);

    return () => clearTimeout(timer);
  }, [age]);

  return (
    <Form>
      <FormGroup
        label="Age"
        type="number"
        helperText={helperText}
        helperTextInvalid={invalidText}
        helperTextInvalidIcon={<ExclamationCircleIcon />}
        fieldId="age-3"
        validated={validated}
      >
        <TextInput
          validated={validated}
          value={age}
          id="age-3"
          aria-describedby="age-3-helper"
          onChange={handleAgeChange}
        />
      </FormGroup>
    </Form>
  );
};
