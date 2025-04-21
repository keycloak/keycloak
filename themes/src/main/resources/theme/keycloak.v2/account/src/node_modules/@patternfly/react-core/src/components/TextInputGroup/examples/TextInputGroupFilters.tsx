import React from 'react';
import { TextInputGroup, TextInputGroupMain, TextInputGroupUtilities, Button } from '@patternfly/react-core';
import { Chip, ChipGroup } from '@patternfly/react-core';
import SearchIcon from '@patternfly/react-icons/dist/esm/icons/search-icon';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';

export const TextInputGroupFilters: React.FunctionComponent = () => {
  const [inputValue, setInputValue] = React.useState('');
  const [currentChips, setCurrentChips] = React.useState([
    'chip one',
    'chip two',
    'chip three',
    'chip four',
    'chip five',
    'chip six',
    'chip seven',
    'chip eight',
    'chip nine',
    'chip ten',
    'chip eleven',
    'chip twelve',
    'chip thirteen',
    'chip fourteen'
  ]);

  /** show the search icon only when there are no chips to prevent the chips from being displayed behind the icon */
  const showSearchIcon = !currentChips.length;

  /** callback for updating the inputValue state in this component so that the input can be controlled */
  const handleInputChange = (value: string, _event: React.FormEvent<HTMLInputElement>) => {
    setInputValue(value);
  };

  /** callback for removing a chip from the chip selections */
  const deleteChip = (chipToDelete: string) => {
    const newChips = currentChips.filter(chip => !Object.is(chip, chipToDelete));
    setCurrentChips(newChips);
  };

  /** show the input/chip clearing button only when either the text input or chip group are not empty */
  const showClearButton = !!inputValue || !!currentChips.length;

  /** render the utilities component only when a component it contains is being rendered */
  const showUtilities = showClearButton;

  /** callback for clearing all selected chips and the text input */
  const clearChipsAndInput = () => {
    setCurrentChips([]);
    setInputValue('');
  };

  return (
    <TextInputGroup>
      <TextInputGroupMain icon={showSearchIcon && <SearchIcon />} value={inputValue} onChange={handleInputChange}>
        <ChipGroup>
          {currentChips.map(currentChip => (
            <Chip key={currentChip} onClick={() => deleteChip(currentChip)}>
              {currentChip}
            </Chip>
          ))}
        </ChipGroup>
      </TextInputGroupMain>
      {showUtilities && (
        <TextInputGroupUtilities>
          {showClearButton && (
            <Button variant="plain" onClick={clearChipsAndInput} aria-label="Clear button and input">
              <TimesIcon />
            </Button>
          )}
        </TextInputGroupUtilities>
      )}
    </TextInputGroup>
  );
};
