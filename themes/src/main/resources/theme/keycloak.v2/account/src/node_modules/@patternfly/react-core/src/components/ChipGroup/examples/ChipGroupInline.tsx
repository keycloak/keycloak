import React from 'react';
import { Chip, ChipGroup } from '@patternfly/react-core';

export const ChipGroupInline: React.FunctionComponent = () => {
  const [chips, setChips] = React.useState([
    'Chip one',
    'Really long chip that goes on and on',
    'Chip three',
    'Chip four',
    'Chip five'
  ]);

  const deleteItem = (id: string) => {
    const copyOfChips = [...chips];
    const filteredCopy = copyOfChips.filter(chip => chip !== id);
    setChips(filteredCopy);
  };

  return (
    <ChipGroup>
      {chips.map(currentChip => (
        <Chip key={currentChip} onClick={() => deleteItem(currentChip)}>
          {currentChip}
        </Chip>
      ))}
    </ChipGroup>
  );
};
