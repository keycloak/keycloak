import React from 'react';
import { Form, FormGroup, FormHelperText, TextInput, HelperText, HelperTextItem } from '@patternfly/react-core';

export const HelperTextStaticVariantDynamicText: React.FunctionComponent = () => {
  const [value, setValue] = React.useState('');
  const [inputValidation, setInputValidation] = React.useState('default');

  const handleInputChange = (inputValue: string) => {
    setValue(inputValue);
  };

  React.useEffect(() => {
    if (value === '') {
      setInputValidation('default');
    } else if (value === 'johndoe') {
      setInputValidation('error');
    } else {
      setInputValidation('success');
    }
  }, [value]);

  return (
    <Form>
      <FormGroup label="Username" isRequired fieldId="login-input-helper-text2">
        <TextInput
          isRequired
          type="text"
          id="login-input-helper-text2"
          name="login-input-helper-text2"
          onChange={handleInputChange}
          aria-describedby="helper-text2"
          aria-invalid={inputValidation === 'error'}
          value={value}
        />
        <FormHelperText isHidden={false} component="div">
          <HelperText id="helper-text2" aria-live="polite">
            {inputValidation !== 'success' && (
              <HelperTextItem variant={inputValidation as any} hasIcon={inputValidation === 'error'}>
                {inputValidation === 'default' ? 'Please enter a username' : 'Username already exists'}
              </HelperTextItem>
            )}
          </HelperText>
        </FormHelperText>
      </FormGroup>
    </Form>
  );
};
