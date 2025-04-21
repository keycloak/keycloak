import React from 'react';
import { DualListSelector } from '@patternfly/react-core';

export const DualListSelectorBasicTooltips: React.FunctionComponent = () => {
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
      availableOptions={availableOptions}
      chosenOptions={chosenOptions}
      onListChange={onListChange}
      addAllTooltip="Add all options"
      addAllTooltipProps={{ position: 'top' }}
      addSelectedTooltip="Add selected options"
      addSelectedTooltipProps={{ position: 'right' }}
      removeSelectedTooltip="Remove selected options"
      removeSelectedTooltipProps={{ position: 'left' }}
      removeAllTooltip="Remove all options"
      removeAllTooltipProps={{ position: 'bottom' }}
      id="dual-list-selector-basic-tooltips"
    />
  );
};
