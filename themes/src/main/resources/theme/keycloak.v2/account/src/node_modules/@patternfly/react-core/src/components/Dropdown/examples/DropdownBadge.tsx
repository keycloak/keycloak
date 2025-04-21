import React from 'react';
import { Dropdown, DropdownItem, BadgeToggle } from '@patternfly/react-core';
import AngleLeftIcon from '@patternfly/react-icons/dist/esm/icons/angle-left-icon';

export const DropdownBadge: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState(false);

  const onToggle = (isOpen: boolean) => {
    setIsOpen(isOpen);
  };

  const onFocus = () => {
    const element = document.getElementById('toggle-badge');
    element.focus();
  };

  const onSelect = () => {
    setIsOpen(false);
    onFocus();
  };

  const dropdownItems = [
    <DropdownItem key="edit" component="button" icon={<AngleLeftIcon />}>
      Edit
    </DropdownItem>,
    <DropdownItem key="action" component="button" icon={<AngleLeftIcon />}>
      Deployment
    </DropdownItem>,
    <DropdownItem key="apps" component="button" icon={<AngleLeftIcon />}>
      Applications
    </DropdownItem>
  ];

  return (
    <Dropdown
      onSelect={onSelect}
      toggle={
        <BadgeToggle id="toggle-badge" onToggle={onToggle}>
          {dropdownItems.length}
        </BadgeToggle>
      }
      isOpen={isOpen}
      dropdownItems={dropdownItems}
    />
  );
};
