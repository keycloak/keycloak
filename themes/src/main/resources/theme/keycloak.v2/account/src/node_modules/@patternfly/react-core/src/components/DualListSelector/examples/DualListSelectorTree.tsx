import React from 'react';
import { DualListSelector } from '@patternfly/react-core';

export const DualListSelectorTree: React.FunctionComponent = () => {
  const [availableOptions, setAvailableOptions] = React.useState<React.ReactNode[]>([
    {
      id: 'F1',
      text: 'Folder 1',
      isChecked: false,
      checkProps: { 'aria-label': 'Folder 1' },
      hasBadge: true,
      badgeProps: { isRead: true },
      children: [
        { id: 'O1', text: 'Option 1', isChecked: false, checkProps: { 'aria-label': 'Option 1' } },
        {
          id: 'F1A',
          text: 'Folder 1A',
          isChecked: false,
          checkProps: { 'aria-label': 'Folder 1A' },
          children: [
            { id: 'O2', text: 'Option 2', isChecked: false, checkProps: { 'aria-label': 'Option 2' } },
            { id: 'O3', text: 'Option 3', isChecked: false, checkProps: { 'aria-label': 'Option 3' } }
          ]
        },
        { id: 'O4', text: 'Option 4', isChecked: false, checkProps: { 'aria-label': 'Option 4' } }
      ]
    },
    { id: 'O5', text: 'Option 5', isChecked: false, checkProps: { 'aria-label': 'Option 5' } },
    {
      id: 'F2',
      text: 'Folder 2',
      isChecked: false,
      checkProps: { 'aria-label': 'Folder 2' },
      children: [
        { id: 'O6', text: 'Option 6', isChecked: false, checkProps: { 'aria-label': 'Option 6' } },
        { id: 'O7', text: 'Option 5', isChecked: false, checkProps: { 'aria-label': 'Option 5 duplicate' } }
      ]
    }
  ]);

  const [chosenOptions, setChosenOptions] = React.useState<React.ReactNode[]>([
    {
      id: 'CF1',
      text: 'Chosen Folder 1',
      isChecked: false,
      checkProps: { 'aria-label': 'Chosen Folder 1' },
      hasBadge: true,
      badgeProps: { isRead: true },
      children: [
        { id: 'CO1', text: 'Chosen Option 1', isChecked: false, checkProps: { 'aria-label': 'Chosen Option 1' } },
        {
          id: 'CF1A',
          text: 'Chosen Folder 1A',
          isChecked: false,
          checkProps: { 'aria-label': 'Chosen Folder 1A' },
          children: [
            {
              id: 'CO2',
              text: 'Chosen Option 2',
              isChecked: false,
              checkProps: { 'aria-label': 'Chosen Option 2' }
            },
            {
              id: 'CO3',
              text: 'Chosen Option 3',
              isChecked: false,
              checkProps: { 'aria-label': 'Chosen Option 3' }
            }
          ]
        },
        { id: 'CO4', text: 'Chosen Option 4', isChecked: false, checkProps: { 'aria-label': 'Chosen Option 4' } }
      ]
    }
  ]);

  const onListChange = (newAvailableOptions: React.ReactNode[], newChosenOptions: React.ReactNode[]) => {
    setAvailableOptions(newAvailableOptions.sort());
    setChosenOptions(newChosenOptions.sort());
  };

  return (
    <DualListSelector
      isSearchable
      isTree
      availableOptions={availableOptions}
      chosenOptions={chosenOptions}
      onListChange={onListChange}
      id="dual-list-selector-tree"
    />
  );
};
