import React from 'react';
import { FormSelect, FormSelectOption } from '@patternfly/react-core';

export const FormSelectBasic: React.FunctionComponent = () => {
  const [formSelectValue, setFormSelectValue] = React.useState('mrs');

  const onChange = (value: string) => {
    setFormSelectValue(value);
  };

  const options = [
    { value: 'please choose', label: 'Select one', disabled: true },
    { value: 'mr', label: 'Mr', disabled: false },
    { value: 'miss', label: 'Miss', disabled: false },
    { value: 'mrs', label: 'Mrs', disabled: false },
    { value: 'ms', label: 'Ms', disabled: false },
    { value: 'dr', label: 'Dr', disabled: false },
    { value: 'other', label: 'Other', disabled: false }
  ];

  return (
    <FormSelect value={formSelectValue} onChange={onChange} aria-label="FormSelect Input">
      {options.map((option, index) => (
        <FormSelectOption isDisabled={option.disabled} key={index} value={option.value} label={option.label} />
      ))}
    </FormSelect>
  );
};
