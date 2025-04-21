import React from 'react';
import { Button, InputGroup, TextInput, Dropdown, DropdownToggle, DropdownItem } from '@patternfly/react-core';

export const InputGroupWithDropdown: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState<boolean>(false);

  const onToggle = (isOpen: boolean) => {
    setIsOpen(isOpen);
  };

  const onSelect = () => {
    setIsOpen(false);
  };

  const dropdownItems = [
    <DropdownItem key="opt-1" value="Option 1" component="button">
      Option 1
    </DropdownItem>,
    <DropdownItem key="opt-2" value="Option 2" component="button">
      Option 2
    </DropdownItem>,
    <DropdownItem key="opt-3" value="Option 3" component="button">
      Option 3
    </DropdownItem>
  ];

  return (
    <React.Fragment>
      <InputGroup>
        <Dropdown
          onSelect={onSelect}
          toggle={<DropdownToggle onToggle={onToggle}>Dropdown</DropdownToggle>}
          isOpen={isOpen}
          dropdownItems={dropdownItems}
        />
        <TextInput id="textInput3" aria-label="input with dropdown and button" />
        <Button id="inputDropdownButton1" variant="control">
          Button
        </Button>
      </InputGroup>
    </React.Fragment>
  );
};
