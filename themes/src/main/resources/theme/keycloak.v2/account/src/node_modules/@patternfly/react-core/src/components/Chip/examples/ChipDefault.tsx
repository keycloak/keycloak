import React from 'react';
import { Badge, Chip } from '@patternfly/react-core';

export const ChipDefault: React.FunctionComponent = () => {
  const [chips, setChips] = React.useState({
    chip: {
      name: 'Chip 1'
    },
    longchip: {
      name: 'Really long chip that goes on and on'
    },
    badgechip: {
      name: 'Chip',
      isRead: true,
      count: 7
    },
    readonlychip: {
      name: 'Read-only chip'
    },
    overflowchip: {
      name: 'Overflow chip'
    }
  });

  const deleteItem = (id: string) => {
    setChips({ ...chips, [id]: null });
  };

  const { chip, longchip, badgechip, readonlychip, overflowchip } = chips;
  return (
    <React.Fragment>
      {chip && (
        <React.Fragment>
          <Chip key="chip1" onClick={() => deleteItem('chip')}>
            {chip.name}
          </Chip>
          <br /> <br />
        </React.Fragment>
      )}
      {longchip && (
        <React.Fragment>
          <Chip key="chip2" onClick={() => deleteItem('longchip')}>
            {longchip.name}
          </Chip>
          <br /> <br />
        </React.Fragment>
      )}
      {badgechip && (
        <React.Fragment>
          <Chip key="chip3" onClick={() => deleteItem('badgechip')}>
            {badgechip.name}
            <Badge isRead={badgechip.isRead}>{badgechip.count}</Badge>
          </Chip>
          <br /> <br />
        </React.Fragment>
      )}
      <Chip key="chip4" onClick={() => deleteItem('readonlychip')} isReadOnly>
        {readonlychip.name}
      </Chip>
      <br /> <br />
      {overflowchip && (
        <Chip key="chip5" component="button" onClick={() => deleteItem('overflowchip')} isOverflowChip>
          {overflowchip.name}
        </Chip>
      )}
    </React.Fragment>
  );
};
