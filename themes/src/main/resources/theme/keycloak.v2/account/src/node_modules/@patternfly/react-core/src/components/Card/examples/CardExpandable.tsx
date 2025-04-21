import React from 'react';
import {
  Card,
  CardHeader,
  CardActions,
  CardTitle,
  CardBody,
  CardFooter,
  CardExpandableContent,
  Checkbox,
  Dropdown,
  DropdownItem,
  DropdownSeparator,
  KebabToggle
} from '@patternfly/react-core';

export const CardExpandable: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState<boolean>(false);
  const [isChecked, setIsChecked] = React.useState<boolean>(false);
  const [isExpanded, setIsExpanded] = React.useState<boolean>(false);
  const [isToggleRightAligned, setIsToggleRightAligned] = React.useState<boolean>(false);

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

  const onRightAlign = () => {
    setIsToggleRightAligned(!isToggleRightAligned);
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
    <React.Fragment>
      <div style={{ marginBottom: '12px' }}>
        <Checkbox
          id={'isToggleRightAligned-1'}
          key={'isToggleRightAligned'}
          label={'isToggleRightAligned'}
          isChecked={isToggleRightAligned}
          onChange={onRightAlign}
        />
      </div>
      <Card id="expandable-card" isExpanded={isExpanded}>
        <CardHeader
          onExpand={onExpand}
          isToggleRightAligned={isToggleRightAligned}
          toggleButtonProps={{
            id: 'toggle-button1',
            'aria-label': 'Details',
            'aria-labelledby': 'expandable-card-title toggle-button1',
            'aria-expanded': isExpanded
          }}
        >
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
              id="check-4"
              name="check4"
            />
          </CardActions>
          <CardTitle id="expandable-card-title">Header</CardTitle>
        </CardHeader>
        <CardExpandableContent>
          <CardBody>Body</CardBody>
          <CardFooter>Footer</CardFooter>
        </CardExpandableContent>
      </Card>
    </React.Fragment>
  );
};
