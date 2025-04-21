import React from 'react';
import { FormSelect, FormSelectOption, FormSelectOptionGroup } from '@patternfly/react-core';

export const FormSelectGrouped: React.FunctionComponent = () => {
  const [formSelectValue, setFormSelectValue] = React.useState('2');

  const onChange = (value: string) => {
    setFormSelectValue(value);
  };

  const groups = [
    {
      groupLabel: 'Group1',
      disabled: false,
      options: [
        { value: '1', label: 'The first option', disabled: false },
        { value: '2', label: 'Second option is selected by default', disabled: false }
      ]
    },
    {
      groupLabel: 'Group2',
      disabled: false,
      options: [
        { value: '3', label: 'The third option', disabled: false },
        { value: '4', label: 'The fourth option', disabled: false }
      ]
    },
    {
      groupLabel: 'Group3',
      disabled: true,
      options: [
        { value: '5', label: 'The fifth option', disabled: false },
        { value: '6', label: 'The sixth option', disabled: false }
      ]
    }
  ];

  return (
    <FormSelect value={formSelectValue} onChange={onChange} aria-label="FormSelect Input">
      {groups.map((group, index) => (
        <FormSelectOptionGroup isDisabled={group.disabled} key={index} label={group.groupLabel}>
          {group.options.map((option, i) => (
            <FormSelectOption isDisabled={option.disabled} key={i} value={option.value} label={option.label} />
          ))}
        </FormSelectOptionGroup>
      ))}
    </FormSelect>
  );
};
