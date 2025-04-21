import React from 'react';
import { Card, CardTitle, CardBody } from '@patternfly/react-core';

export const CardSelectableA11yHighlight: React.FunctionComponent = () => {
  const [selected, setSelected] = React.useState<string>('');

  const onKeyDown = (event: React.KeyboardEvent) => {
    if (event.target !== event.currentTarget) {
      return;
    }
    if ([' ', 'Enter'].includes(event.key)) {
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

  return (
    <React.Fragment>
      <Card
        id="selectable-first-card"
        onKeyDown={onKeyDown}
        onClick={onClick}
        hasSelectableInput
        onSelectableInputChange={onChange}
        isSelectableRaised
        isSelected={selected === 'selectable-first-card'}
      >
        <CardTitle>Selectable card with proper accessibility considerations</CardTitle>
        <CardBody>
          When using a screen reader a checkbox will become navigable that indicates this card is selectable and
          communicate if it is currently selected.
        </CardBody>
      </Card>
      <br />
      <Card
        id="selectable-second-card"
        onKeyDown={onKeyDown}
        onClick={onClick}
        isSelectableRaised
        isSelected={selected === 'selectable-second-card'}
      >
        <CardTitle>Selectable card without proper accessibility considerations</CardTitle>
        <CardBody>
          When using a screen reader there are no indications that this card is selectable or if it is currently
          selected.
        </CardBody>
      </Card>
    </React.Fragment>
  );
};
