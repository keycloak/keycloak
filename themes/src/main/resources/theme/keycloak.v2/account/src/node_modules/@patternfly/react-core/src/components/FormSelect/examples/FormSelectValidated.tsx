import React from 'react';
import { Form, FormGroup, FormSelect, FormSelectOption, ValidatedOptions } from '@patternfly/react-core';

export const FormSelectValidated: React.FunctionComponent = () => {
  const [formValue, setFormValue] = React.useState('');
  const [invalidText, setInvalidText] = React.useState('You must choose something');
  const [helperText, setHelperText] = React.useState('');
  const [validated, setValidated] = React.useState<ValidatedOptions>(ValidatedOptions.default);

  const simulateNetworkCall = (callback: () => void) => {
    setTimeout(callback, 2000);
  };

  const onChange = (value: string) => {
    setFormValue(value);
    setValidated(ValidatedOptions.default);
    setHelperText('Validating...');
    simulateNetworkCall(() => {
      if (value === '3') {
        setValidated(ValidatedOptions.success);
        setHelperText('You chose wisely');
      } else if (value === '') {
        setValidated(ValidatedOptions.warning);
        setHelperText('You must select a value');
      } else {
        setValidated(ValidatedOptions.error);
        setInvalidText('You must chose Three (thought that was obvious)');
      }
    });
  };

  const options = [
    { value: '', label: 'Select a number', disabled: false, isPlaceholder: true },
    { value: '1', label: 'One', disabled: false, isPlaceholder: false },
    { value: '2', label: 'Two', disabled: false, isPlaceholder: false },
    { value: '3', label: 'Three - the only valid option', disabled: false, isPlaceholder: false }
  ];

  return (
    <Form>
      <FormGroup
        label="Selection:"
        type="string"
        helperText={helperText}
        helperTextInvalid={invalidText}
        fieldId="selection"
        validated={validated}
      >
        <FormSelect
          id="selection"
          validated={validated}
          value={formValue}
          onChange={onChange}
          aria-label="FormSelect Input"
        >
          {options.map((option, index) => (
            <FormSelectOption
              isDisabled={option.disabled}
              key={index}
              value={option.value}
              label={option.label}
              isPlaceholder={option.isPlaceholder}
            />
          ))}
        </FormSelect>
      </FormGroup>
    </Form>
  );
};
