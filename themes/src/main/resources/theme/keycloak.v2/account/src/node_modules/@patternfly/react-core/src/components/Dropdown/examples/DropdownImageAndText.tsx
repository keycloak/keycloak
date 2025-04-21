import React from 'react';
import {
  Avatar,
  Dropdown,
  DropdownGroup,
  DropdownToggle,
  DropdownItem,
  DropdownSeparator
} from '@patternfly/react-core';
import avatarImg from '../../Avatar/examples/avatarImg.svg';

export const DropdownImageAndText: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState(false);

  const onToggle = (isOpen: boolean) => {
    setIsOpen(isOpen);
  };

  const onFocus = () => {
    const element = document.getElementById('toggle-image-and-text');
    element.focus();
  };

  const onSelect = () => {
    setIsOpen(false);
    onFocus();
  };

  const dropdownItems = [
    <DropdownGroup key="group 1">
      <DropdownItem key="group 1 plaintext" component="div" isPlainText>
        Text
      </DropdownItem>
      <DropdownItem key="group 1 plaintext2" component="div" isPlainText>
        More text
      </DropdownItem>
    </DropdownGroup>,
    <DropdownSeparator key="dropdown separator" />,
    <DropdownGroup key="group 2">
      <DropdownItem key="group 2 profile">My profile</DropdownItem>
      <DropdownItem key="group 2 user" component="button">
        User management
      </DropdownItem>
      <DropdownItem key="group 2 logout">Logout</DropdownItem>
    </DropdownGroup>
  ];

  return (
    <Dropdown
      onSelect={onSelect}
      toggle={
        <DropdownToggle
          id="toggle-image-and-text"
          onToggle={onToggle}
          icon={<Avatar src={avatarImg} alt="avatar"></Avatar>}
        >
          Ned Username
        </DropdownToggle>
      }
      isOpen={isOpen}
      dropdownItems={dropdownItems}
    />
  );
};
