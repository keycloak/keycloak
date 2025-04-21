import React from 'react';
import { Dropdown, DropdownToggle, DropdownItem } from '@patternfly/react-core';
import BellIcon from '@patternfly/react-icons/dist/esm/icons/bell-icon';
import CubesIcon from '@patternfly/react-icons/dist/esm/icons/cubes-icon';

export const DropdownDescriptions: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState(false);

  const onToggle = (isOpen: boolean) => {
    setIsOpen(isOpen);
  };

  const onFocus = () => {
    const element = document.getElementById('toggle-descriptions');
    element.focus();
  };

  const onSelect = () => {
    setIsOpen(false);
    onFocus();
  };

  const dropdownItems = [
    <DropdownItem key="link" description="This is a description" icon={<CubesIcon />}>
      Link
    </DropdownItem>,
    <DropdownItem
      key="action"
      component="button"
      icon={<BellIcon />}
      description="This is a very long description that describes the menu item"
    >
      Action
    </DropdownItem>,
    <DropdownItem key="disabled link" isDisabled description="Disabled link description">
      Disabled link
    </DropdownItem>,
    <DropdownItem key="disabled action" isDisabled component="button" description="This is a description">
      Disabled action
    </DropdownItem>
  ];

  return (
    <Dropdown
      onSelect={onSelect}
      toggle={
        <DropdownToggle id="toggle-descriptions" onToggle={onToggle}>
          Dropdown
        </DropdownToggle>
      }
      isOpen={isOpen}
      dropdownItems={dropdownItems}
    />
  );
};
