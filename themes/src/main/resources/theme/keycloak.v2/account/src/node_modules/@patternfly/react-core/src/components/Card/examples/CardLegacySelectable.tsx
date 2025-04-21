import React from 'react';
import {
  Card,
  CardHeader,
  CardActions,
  CardTitle,
  CardBody,
  Dropdown,
  DropdownItem,
  DropdownSeparator,
  KebabToggle
} from '@patternfly/react-core';

export const CardLegacySelectable: React.FunctionComponent = () => {
  const [selected, setSelected] = React.useState<string>('');
  const [isKebabOpen, setIsKebabOpen] = React.useState<boolean>(false);

  const onKeyDown = (event: React.KeyboardEvent) => {
    if (event.target !== event.currentTarget) {
      return;
    }
    if ([13, 32].includes(event.keyCode)) {
      event.preventDefault();
      const newSelected = event.currentTarget.id === selected ? null : event.currentTarget.id;
      setSelected(newSelected);
    }
  };

  const onClick = (event: React.MouseEvent) => {
    const newSelected = event.currentTarget.id === selected ? null : event.currentTarget.id;
    setSelected(newSelected);
  };

  const onChange = (labelledById: string, _event: React.FormEvent<HTMLInputElement>) => {
    const newSelected = labelledById === selected ? null : labelledById;
    setSelected(newSelected);
  };

  const onToggle = (isOpen: boolean, event: any) => {
    event.stopPropagation();
    setIsKebabOpen(isOpen);
  };

  const onSelect = (event: React.SyntheticEvent<HTMLDivElement>) => {
    event.stopPropagation();
    setIsKebabOpen(!isKebabOpen);
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
      <Card
        id="legacy-first-card"
        onKeyDown={onKeyDown}
        onClick={onClick}
        onSelectableInputChange={onChange}
        isSelectable
        isSelected={selected === 'legacy-first-card'}
        hasSelectableInput
      >
        <CardHeader>
          <CardActions>
            <Dropdown
              onSelect={onSelect}
              toggle={<KebabToggle onToggle={onToggle} />}
              isOpen={isKebabOpen}
              isPlain
              dropdownItems={dropdownItems}
              position={'right'}
            />
          </CardActions>
        </CardHeader>
        <CardTitle>First legacy selectable card</CardTitle>
        <CardBody>This is a selectable card. Click me to select me. Click again to deselect me.</CardBody>
      </Card>
      <br />
      <Card
        id="legacy-second-card"
        onKeyDown={onKeyDown}
        onClick={onClick}
        onSelectableInputChange={onChange}
        isSelectable
        isSelected={selected === 'legacy-second-card'}
        hasSelectableInput
      >
        <CardTitle>Second legacy selectable card</CardTitle>
        <CardBody>This is a selectable card. Click me to select me. Click again to deselect me.</CardBody>
      </Card>
    </>
  );
};
