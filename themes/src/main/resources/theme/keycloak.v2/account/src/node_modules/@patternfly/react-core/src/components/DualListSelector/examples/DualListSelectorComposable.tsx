import React from 'react';
import {
  Button,
  ButtonVariant,
  DualListSelector,
  DualListSelectorPane,
  DualListSelectorList,
  DualListSelectorListItem,
  DualListSelectorControlsWrapper,
  DualListSelectorControl,
  SearchInput
} from '@patternfly/react-core';
import AngleDoubleLeftIcon from '@patternfly/react-icons/dist/esm/icons/angle-double-left-icon';
import AngleLeftIcon from '@patternfly/react-icons/dist/esm/icons/angle-left-icon';
import AngleDoubleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-double-right-icon';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import PficonSortCommonAscIcon from '@patternfly/react-icons/dist/esm/icons/pficon-sort-common-asc-icon';

export const DualListSelectorComposable: React.FunctionComponent = () => {
  const [availableOptions, setAvailableOptions] = React.useState([
    { text: 'Apple', selected: false, isVisible: true },
    { text: 'Banana', selected: false, isVisible: true },
    { text: 'Pineapple', selected: false, isVisible: true },
    { text: 'Orange', selected: false, isVisible: true },
    { text: 'Grape', selected: false, isVisible: true },
    { text: 'Peach', selected: false, isVisible: true },
    { text: 'Strawberry', selected: false, isVisible: true }
  ]);
  const [chosenOptions, setChosenOptions] = React.useState([]);
  const [availableFilter, setAvailableFilter] = React.useState('');
  const [chosenFilter, setChosenFilter] = React.useState('');

  // callback for moving selected options between lists
  const moveSelected = (fromAvailable: boolean) => {
    const sourceOptions = fromAvailable ? availableOptions : chosenOptions;
    const destinationOptions = fromAvailable ? chosenOptions : availableOptions;
    for (let i = 0; i < sourceOptions.length; i++) {
      const option = sourceOptions[i];
      if (option.selected && option.isVisible) {
        sourceOptions.splice(i, 1);
        destinationOptions.push(option);
        option.selected = false;
        i--;
      }
    }
    if (fromAvailable) {
      setAvailableOptions([...sourceOptions]);
      setChosenOptions([...destinationOptions]);
    } else {
      setChosenOptions([...sourceOptions]);
      setAvailableOptions([...destinationOptions]);
    }
  };

  // callback for moving all options between lists
  const moveAll = (fromAvailable: boolean) => {
    if (fromAvailable) {
      setChosenOptions([...availableOptions.filter(option => option.isVisible), ...chosenOptions]);
      setAvailableOptions([...availableOptions.filter(option => !option.isVisible)]);
    } else {
      setAvailableOptions([...chosenOptions.filter(option => option.isVisible), ...availableOptions]);
      setChosenOptions([...chosenOptions.filter(option => !option.isVisible)]);
    }
  };

  // callback when option is selected
  const onOptionSelect = (
    event: React.MouseEvent | React.ChangeEvent | React.KeyboardEvent,
    index: number,
    isChosen: boolean
  ) => {
    if (isChosen) {
      const newChosen = [...chosenOptions];
      newChosen[index].selected = !chosenOptions[index].selected;
      setChosenOptions(newChosen);
    } else {
      const newAvailable = [...availableOptions];
      newAvailable[index].selected = !availableOptions[index].selected;
      setAvailableOptions(newAvailable);
    }
  };

  // builds a search input - used in each dual list selector pane
  const buildSearchInput = (isAvailable: boolean) => {
    const onChange = (value: string) => {
      isAvailable ? setAvailableFilter(value) : setChosenFilter(value);
      const toFilter = isAvailable ? [...availableOptions] : [...chosenOptions];
      toFilter.forEach(option => {
        option.isVisible = value === '' || option.text.toLowerCase().includes(value.toLowerCase());
      });
    };

    return (
      <SearchInput
        value={isAvailable ? availableFilter : chosenFilter}
        onChange={onChange}
        onClear={() => onChange('')}
      />
    );
  };

  // builds a sort control - passed to both dual list selector panes
  const buildSort = (isAvailable: boolean) => {
    const onSort = () => {
      const toSort = isAvailable ? [...availableOptions] : [...chosenOptions];
      toSort.sort((a, b) => {
        if (a.text > b.text) {
          return 1;
        }
        if (a.text < b.text) {
          return -1;
        }
        return 0;
      });
      if (isAvailable) {
        setAvailableOptions(toSort);
      } else {
        setChosenOptions(toSort);
      }
    };

    return (
      <Button variant={ButtonVariant.plain} onClick={onSort} aria-label="Sort" key="sortButton">
        <PficonSortCommonAscIcon />
      </Button>
    );
  };

  return (
    <DualListSelector>
      <DualListSelectorPane
        title="Available"
        status={`${availableOptions.filter(option => option.selected && option.isVisible).length} of ${
          availableOptions.filter(option => option.isVisible).length
        } options selected`}
        searchInput={buildSearchInput(true)}
        actions={[buildSort(true)]}
      >
        <DualListSelectorList>
          {availableOptions.map((option, index) =>
            option.isVisible ? (
              <DualListSelectorListItem
                key={index}
                isSelected={option.selected}
                id={`composable-available-option-${index}`}
                onOptionSelect={e => onOptionSelect(e, index, false)}
              >
                {option.text}
              </DualListSelectorListItem>
            ) : null
          )}
        </DualListSelectorList>
      </DualListSelectorPane>
      <DualListSelectorControlsWrapper aria-label="Selector controls">
        <DualListSelectorControl
          isDisabled={!availableOptions.some(option => option.selected)}
          onClick={() => moveSelected(true)}
          aria-label="Add selected"
          tooltipContent="Add selected"
        >
          <AngleRightIcon />
        </DualListSelectorControl>
        <DualListSelectorControl
          isDisabled={availableOptions.length === 0}
          onClick={() => moveAll(true)}
          aria-label="Add all"
          tooltipContent="Add all"
        >
          <AngleDoubleRightIcon />
        </DualListSelectorControl>
        <DualListSelectorControl
          isDisabled={chosenOptions.length === 0}
          onClick={() => moveAll(false)}
          aria-label="Remove all"
          tooltipContent="Remove all"
        >
          <AngleDoubleLeftIcon />
        </DualListSelectorControl>
        <DualListSelectorControl
          onClick={() => moveSelected(false)}
          isDisabled={!chosenOptions.some(option => option.selected)}
          aria-label="Remove selected"
          tooltipContent="Remove selected"
        >
          <AngleLeftIcon />
        </DualListSelectorControl>
      </DualListSelectorControlsWrapper>
      <DualListSelectorPane
        title="Chosen"
        status={`${chosenOptions.filter(option => option.selected && option.isVisible).length} of ${
          chosenOptions.filter(option => option.isVisible).length
        } options selected`}
        searchInput={buildSearchInput(false)}
        actions={[buildSort(false)]}
        isChosen
      >
        <DualListSelectorList>
          {chosenOptions.map((option, index) =>
            option.isVisible ? (
              <DualListSelectorListItem
                key={index}
                isSelected={option.selected}
                id={`composable-chosen-option-${index}`}
                onOptionSelect={e => onOptionSelect(e, index, true)}
              >
                {option.text}
              </DualListSelectorListItem>
            ) : null
          )}
        </DualListSelectorList>
      </DualListSelectorPane>
    </DualListSelector>
  );
};
