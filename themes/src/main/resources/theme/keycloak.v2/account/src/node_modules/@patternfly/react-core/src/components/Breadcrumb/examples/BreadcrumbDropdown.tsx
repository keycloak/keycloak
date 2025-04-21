import React from 'react';
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbHeading,
  Dropdown,
  BadgeToggle,
  DropdownItem
} from '@patternfly/react-core';
import AngleLeftIcon from '@patternfly/react-icons/dist/esm/icons/angle-left-icon';

const dropdownItems: JSX.Element[] = [
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

export const BreadcrumbDropdown: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState(false);
  const badgeToggleRef = React.useRef<HTMLButtonElement>();

  const onToggle = (isOpen: boolean) => setIsOpen(isOpen);

  const onSelect = () => {
    setIsOpen((prevIsOpen: boolean) => !prevIsOpen);
    badgeToggleRef.current.focus();
  };

  return (
    <Breadcrumb>
      <BreadcrumbItem component="button">Section home</BreadcrumbItem>
      <BreadcrumbItem isDropdown>
        <Dropdown
          onSelect={onSelect}
          toggle={
            <BadgeToggle ref={badgeToggleRef} onToggle={onToggle}>
              {dropdownItems.length}
            </BadgeToggle>
          }
          isOpen={isOpen}
          dropdownItems={dropdownItems}
        />
      </BreadcrumbItem>
      <BreadcrumbHeading component="button">Section title</BreadcrumbHeading>
    </Breadcrumb>
  );
};
