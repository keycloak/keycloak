import React from 'react';
import {
  Card,
  CardHeader,
  CardActions,
  CardTitle,
  CardBody,
  CardFooter,
  Checkbox,
  Dropdown,
  DropdownItem,
  DropdownSeparator,
  KebabToggle
} from '@patternfly/react-core';

export const CardTitleInHeader: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState<boolean>(false);
  const [isChecked, setIsChecked] = React.useState<boolean>(false);

  const onSelect = () => {
    setIsOpen(!isOpen);
  };
  const onClick = (checked: boolean) => {
    setIsChecked(checked);
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
    <Card>
      <CardHeader>
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
            id="check-2"
            name="check2"
          />
        </CardActions>
        <CardTitle>
          This is a really really really really really really really really really really long header
        </CardTitle>
      </CardHeader>
      <CardBody>Body</CardBody>
      <CardFooter>Footer</CardFooter>
    </Card>
  );
};
