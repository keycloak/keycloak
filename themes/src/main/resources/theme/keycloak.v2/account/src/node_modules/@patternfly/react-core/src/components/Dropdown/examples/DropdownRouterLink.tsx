import React from 'react';
import { Dropdown, DropdownToggle, DropdownItem } from '@patternfly/react-core';
import { Link } from '@reach/router';

export const DropdownRouterLink: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState(false);

  const onToggle = (isOpen: boolean) => {
    setIsOpen(isOpen);
  };

  const onFocus = () => {
    const element = document.getElementById('toggle-router-link');
    element.focus();
  };

  const onSelect = () => {
    setIsOpen(false);
    onFocus();
  };

  const dropdownItems = [<DropdownItem key="routerlink" component={<Link to="/">@reach/router Link</Link>} />];

  return (
    <Dropdown
      onSelect={onSelect}
      toggle={
        <DropdownToggle id="toggle-router-link" onToggle={onToggle}>
          Dropdown
        </DropdownToggle>
      }
      isOpen={isOpen}
      dropdownItems={dropdownItems}
    />
  );
};
