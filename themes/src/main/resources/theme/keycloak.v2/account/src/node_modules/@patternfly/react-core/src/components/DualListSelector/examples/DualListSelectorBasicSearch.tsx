import React from 'react';
import { DualListSelector } from '@patternfly/react-core';

export const DualListSelectorBasicSearch: React.FunctionComponent = () => {
  const [availableOptions, setAvailableOptions] = React.useState<React.ReactNode[]>([
    'Option 1',
    'Option 2',
    'Option 3',
    'Option 4'
  ]);
  const [chosenOptions, setChosenOptions] = React.useState<React.ReactNode[]>([]);

  const onListChange = (newAvailableOptions: React.ReactNode[], newChosenOptions: React.ReactNode[]) => {
    setAvailableOptions(newAvailableOptions.sort());
    setChosenOptions(newChosenOptions.sort());
  };

  return (
    <DualListSelector
      isSearchable
      availableOptions={availableOptions}
      chosenOptions={chosenOptions}
      onListChange={onListChange}
      id="dual-list-selector-basic-search"
    />
  );
};
