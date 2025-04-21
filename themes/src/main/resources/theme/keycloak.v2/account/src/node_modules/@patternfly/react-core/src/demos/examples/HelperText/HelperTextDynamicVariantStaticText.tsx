import React from 'react';
import { Form, FormGroup, FormHelperText, TextInput, HelperText, HelperTextItem } from '@patternfly/react-core';

export const HelperTextDynamicVariantDynamicText: React.FunctionComponent = () => {
  const [value, setValue] = React.useState('');
  const [inputValidation, setInputValidation] = React.useState({
    ruleLength: 'indeterminate',
    ruleCharacterTypes: 'indeterminate'
  });
  const { ruleLength, ruleCharacterTypes } = inputValidation;

  React.useEffect(() => {
    let lengthStatus = ruleLength;
    let typeStatus = ruleCharacterTypes;

    if (value === '') {
      setInputValidation({
        ruleLength: 'indeterminate',
        ruleCharacterTypes: 'indeterminate'
      });
      return;
    }

    if (!/\d/g.test(value)) {
      typeStatus = 'error';
    } else {
      typeStatus = 'success';
    }

    if (value.length < 5) {
      lengthStatus = 'error';
    } else {
      lengthStatus = 'success';
    }

    setInputValidation({ ruleLength: lengthStatus, ruleCharacterTypes: typeStatus });
  }, [value, ruleLength, ruleCharacterTypes]);

  const handleInputChange = (inputValue: string) => {
    setValue(inputValue);
  };

  const filterValidations = () => Object.keys(inputValidation).filter(item => inputValidation[item] !== 'success');

  return (
    <Form>
      <FormGroup label="Username" isRequired fieldId="login-input-helper-text3">
        <TextInput
          isRequired
          type="text"
          id="login-input-helper-text3"
          name="login-input-helper-text3"
          onChange={handleInputChange}
          aria-describedby={filterValidations().join(' ')}
          aria-invalid={ruleCharacterTypes === 'error' || ruleLength === 'error'}
          value={value}
        />
        <FormHelperText isHidden={false} component="div">
          <HelperText component="ul">
            <HelperTextItem component="li" id="ruleLength" isDynamic variant={ruleLength as any}>
              Must be at least 5 characters in length
            </HelperTextItem>
            <HelperTextItem component="li" id="ruleCharacterTypes" isDynamic variant={ruleCharacterTypes as any}>
              Must include at least 1 number
            </HelperTextItem>
          </HelperText>
        </FormHelperText>
      </FormGroup>
    </Form>
  );
};
