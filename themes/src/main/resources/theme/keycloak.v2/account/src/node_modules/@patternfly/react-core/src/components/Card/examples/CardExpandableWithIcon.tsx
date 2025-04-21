import React from 'react';
import {
  Card,
  CardHeader,
  CardActions,
  CardBody,
  CardFooter,
  CardExpandableContent,
  Checkbox,
  Dropdown,
  DropdownItem,
  DropdownSeparator,
  KebabToggle
} from '@patternfly/react-core';
import pfLogoSmall from './pf-logo-small.svg';

export const CardExpandableWithIcon: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState<boolean>(false);
  const [isChecked, setIsChecked] = React.useState<boolean>(false);
  const [isExpanded, setIsExpanded] = React.useState<boolean>(false);

  const onSelect = () => {
    setIsOpen(!isOpen);
  };

  const onClick = (checked: boolean) => {
    setIsChecked(checked);
  };

  const onExpand = (event: React.MouseEvent, id: string) => {
    // eslint-disable-next-line no-console
    console.log(id);
    setIsExpanded(!isExpanded);
  };

  const dropdownItems = [
    <DropdownItem key="link">Link</DropdownItem>,
    <DropdownItem key="action" component="button">
      Action
    </DropdownItem>,
    <DropdownItem key="disabled link" isDisabled>
      Disabled Link
    </DropdownItem>,
    <DropdownItem key="disabled action" isDisabled component="button">
      Disabled Action
    </DropdownItem>,
    <DropdownSeparator key="separator" />,
    <DropdownItem key="separated link">Separated Link</DropdownItem>,
    <DropdownItem key="separated action" component="button">
      Separated Action
    </DropdownItem>
  ];

  return (
    <Card id="expandable-card-icon" isExpanded={isExpanded}>
      <CardHeader
        onExpand={onExpand}
        toggleButtonProps={{
          id: 'toggle-button2',
          'aria-label': 'Patternfly Details',
          'aria-expanded': isExpanded
        }}
      >
        <img src={pfLogoSmall} alt="PatternFly logo" width="27px" />
        <CardActions>
          <Dropdown
            onSelect={onSelect}
            toggle={<KebabToggle onToggle={setIsOpen} />}
            isOpen={isOpen}
            isPlain
            dropdownItems={dropdownItems}
            position={'right'}
          />
          <Checkbox
            isChecked={isChecked}
            onChange={onClick}
            aria-label="card checkbox example"
            id="check-5"
            name="check5"
          />
        </CardActions>
      </CardHeader>
      <CardExpandableContent>
        <CardBody>Body</CardBody>
        <CardFooter>Footer</CardFooter>
      </CardExpandableContent>
    </Card>
  );
};
