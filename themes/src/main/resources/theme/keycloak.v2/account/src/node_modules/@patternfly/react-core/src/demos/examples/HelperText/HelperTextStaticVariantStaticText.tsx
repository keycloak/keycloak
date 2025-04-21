import React from 'react';
import { Form, FormGroup, FormHelperText, TextInput, HelperText, HelperTextItem } from '@patternfly/react-core';

export const HelperTextStaticVariantStaticText: React.FunctionComponent = () => {
  const [value, setValue] = React.useState('');

  const handleInputChange = (inputValue: string) => {
    setValue(inputValue);
  };

  return (
    <Form>
      <FormGroup label="Middle Name" fieldId="login-input-helper-text1">
        <TextInput
          type="text"
          id="login-input-helper-text1"
          name="login-input-helper-text1"
          onChange={handleInputChange}
          aria-describedby="helper-text1"
          value={value}
        />
        <FormHelperText isHidden={false} component="div">
          <HelperText id="helper-text1">
            <HelperTextItem variant={'default'}>Enter your middle name or your middle initial</HelperTextItem>
          </HelperText>
        </FormHelperText>
      </FormGroup>
    </Form>
  );
};
