import React from 'react';
import { Dropdown, DropdownToggle, DropdownGroup, DropdownItem } from '@patternfly/react-core';

export const DropdownGroups: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState(false);

  const onToggle = (isOpen: boolean) => {
    setIsOpen(isOpen);
  };

  const onFocus = () => {
    const element = document.getElementById('toggle-groups');
    element.focus();
  };

  const onSelect = () => {
    setIsOpen(false);
    onFocus();
  };

  const dropdownItems = [
    <DropdownGroup key="group 1">
      <DropdownItem key="group 1 link">Link</DropdownItem>
      <DropdownItem key="group 1 action" component="button">
        Action
      </DropdownItem>
    </DropdownGroup>,
    <DropdownGroup label="Group 2" key="group 2">
      <DropdownItem key="group 2 link">Group 2 link</DropdownItem>
      <DropdownItem key="group 2 action" component="button">
        Group 2 action
      </DropdownItem>
    </DropdownGroup>,
    <DropdownGroup label="Group 3" key="group 3">
      <DropdownItem key="group 3 link">Group 3 link</DropdownItem>
      <DropdownItem key="group 3 action" component="button">
        Group 3 action
      </DropdownItem>
    </DropdownGroup>
  ];

  return (
    <Dropdown
      onSelect={onSelect}
      toggle={
        <DropdownToggle id="toggle-groups" onToggle={onToggle}>
          Dropdown
        </DropdownToggle>
      }
      isOpen={isOpen}
      dropdownItems={dropdownItems}
      isGrouped
    />
  );
};
