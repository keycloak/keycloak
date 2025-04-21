import React from 'react';
import { Form, FormGroup, FormSelect, FormSelectOption, ValidatedOptions } from '@patternfly/react-core';

export const FormSelectIconSpriteVariant: React.FunctionComponent = () => {
  const [formSelectValue, setFormSelectValue] = React.useState('');
  const [validated, setValidated] = React.useState<ValidatedOptions>(ValidatedOptions.default);
  const [invalidText, setInvalidText] = React.useState('You must choose something');
  const [helperText, setHelperText] = React.useState('');

  const onChange = (value: string) => {
    if (value === '3') {
      setFormSelectValue(value);
      setValidated(ValidatedOptions.success);
      setHelperText('You chose wisely');
    } else if (value === '') {
      setFormSelectValue(value);
      setValidated(ValidatedOptions.warning);
      setHelperText('You must select a value');
    } else {
      setFormSelectValue(value);
      setValidated(ValidatedOptions.error);
      setInvalidText('You must chose Three (thought that was obvious)');
    }
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
          validated={validated}
          isIconSprite
          value={formSelectValue}
          onChange={onChange}
          aria-label="FormSelect Input"
        >
          {options.map((option, index) => (
            <FormSelectOption isDisabled={option.disabled} key={index} value={option.value} label={option.label} />
          ))}
        </FormSelect>
      </FormGroup>
    </Form>
  );
};
