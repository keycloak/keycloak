import React from 'react';
import {
  Brand,
  Card,
  CardHeader,
  CardHeaderMain,
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
import pfLogo from './pfLogo.svg';

export const CardWithImageAndActions: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState<boolean>(false);
  const [isChecked, setIsChecked] = React.useState<boolean>(false);
  const [hasNoOffset, setHasNoOffset] = React.useState<boolean>(false);

  const onSelect = () => {
    setIsOpen(!isOpen);
  };
  const onClick = (checked: boolean) => {
    setIsChecked(checked);
  };
  const toggleOffset = (checked: boolean) => {
    setHasNoOffset(checked);
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
    <>
      <Card>
        <CardHeader>
          <CardHeaderMain>
            <Brand src={pfLogo} alt="PatternFly logo" style={{ height: '50px' }} />
          </CardHeaderMain>
          <CardActions hasNoOffset={hasNoOffset}>
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
              id="check-1"
              name="check1"
            />
          </CardActions>
        </CardHeader>
        <CardTitle>Header</CardTitle>
        <CardBody>Body</CardBody>
        <CardFooter>Footer</CardFooter>
      </Card>
      <div style={{ marginTop: '20px' }}>
        <Checkbox
          label="actions hasNoOffset"
          isChecked={hasNoOffset}
          onChange={toggleOffset}
          aria-label="remove actions offset"
          id="toggle-actions-offset"
          name="toggle-actions-offset"
        />
      </div>
    </>
  );
};
