import React from 'react';
import { Label } from '@patternfly/react-core';
import InfoCircleIcon from '@patternfly/react-icons/dist/esm/icons/info-circle-icon';

export const LabelFilled: React.FunctionComponent = () => (
  <React.Fragment>
    <Label>Grey</Label> <Label icon={<InfoCircleIcon />}>Grey icon</Label>{' '}
    <Label onClose={() => Function.prototype}>Grey removable</Label>{' '}
    <Label icon={<InfoCircleIcon />} onClose={() => Function.prototype}>
      Grey icon removable
    </Label>{' '}
    <Label href="#filled">Grey link</Label>{' '}
    <Label href="#filled" onClose={() => Function.prototype}>
      Grey link removable
    </Label>
    <Label icon={<InfoCircleIcon />} onClose={() => Function.prototype} isTruncated>
      Grey label with icon that overflows
    </Label>
    <br />
    <br />
    <Label color="blue">Blue</Label>{' '}
    <Label color="blue" icon={<InfoCircleIcon />}>
      Blue icon
    </Label>{' '}
    <Label color="blue" onClose={() => Function.prototype}>
      Blue removable
    </Label>{' '}
    <Label color="blue" icon={<InfoCircleIcon />} onClose={() => Function.prototype}>
      Blue icon removable
    </Label>{' '}
    <Label color="blue" href="#filled">
      Blue link
    </Label>{' '}
    <Label color="blue" href="#filled" onClose={() => Function.prototype}>
      Blue link removable
    </Label>
    <Label color="blue" icon={<InfoCircleIcon />} onClose={() => Function.prototype} isTruncated>
      Blue label with icon that overflows
    </Label>
    <br />
    <br />
    <Label color="green">Green</Label>{' '}
    <Label color="green" icon={<InfoCircleIcon />}>
      Green icon
    </Label>{' '}
    <Label color="green" onClose={() => Function.prototype}>
      Green removable
    </Label>{' '}
    <Label color="green" icon={<InfoCircleIcon />} onClose={() => Function.prototype}>
      Green icon removable
    </Label>{' '}
    <Label color="green" href="#filled">
      Green link
    </Label>{' '}
    <Label color="green" href="#filled" onClose={() => Function.prototype}>
      Green link removable
    </Label>
    <Label color="green" icon={<InfoCircleIcon />} onClose={() => Function.prototype} isTruncated>
      Green label with icon that overflows
    </Label>
    <br />
    <br />
    <Label color="orange">Orange</Label>{' '}
    <Label color="orange" icon={<InfoCircleIcon />}>
      Orange icon
    </Label>{' '}
    <Label color="orange" onClose={() => Function.prototype}>
      Orange removable
    </Label>{' '}
    <Label color="orange" icon={<InfoCircleIcon />} onClose={() => Function.prototype}>
      Orange icon removable
    </Label>{' '}
    <Label color="orange" href="#filled">
      Orange link
    </Label>{' '}
    <Label color="orange" href="#filled" onClose={() => Function.prototype}>
      Orange link removable
    </Label>
    <Label color="orange" icon={<InfoCircleIcon />} onClose={() => Function.prototype} isTruncated>
      Orange label with icon that overflows
    </Label>
    <br />
    <br />
    <Label color="red">Red</Label>{' '}
    <Label color="red" icon={<InfoCircleIcon />}>
      Red icon
    </Label>{' '}
    <Label color="red" onClose={() => Function.prototype}>
      Red removable
    </Label>{' '}
    <Label color="red" icon={<InfoCircleIcon />} onClose={() => Function.prototype}>
      Red icon removable
    </Label>{' '}
    <Label color="red" href="#filled">
      Red link
    </Label>{' '}
    <Label color="red" href="#filled" onClose={() => Function.prototype}>
      Red link removable
    </Label>
    <Label color="red" icon={<InfoCircleIcon />} onClose={() => Function.prototype} isTruncated>
      Red label with icon that overflows
    </Label>
    <br />
    <br />
    <Label color="purple">Purple</Label>{' '}
    <Label color="purple" icon={<InfoCircleIcon />}>
      Purple icon
    </Label>{' '}
    <Label color="purple" onClose={() => Function.prototype}>
      Purple removable
    </Label>{' '}
    <Label color="purple" icon={<InfoCircleIcon />} onClose={() => Function.prototype}>
      Purple icon removable
    </Label>{' '}
    <Label color="purple" href="#filled">
      Purple link
    </Label>{' '}
    <Label color="purple" href="#filled" onClose={() => Function.prototype}>
      Purple link removable
    </Label>
    <Label color="purple" icon={<InfoCircleIcon />} onClose={() => Function.prototype} isTruncated>
      Purple label with icon that overflows
    </Label>
    <br />
    <br />
    <Label color="cyan">Cyan</Label>{' '}
    <Label color="cyan" icon={<InfoCircleIcon />}>
      Cyan icon
    </Label>{' '}
    <Label color="cyan" onClose={() => Function.prototype}>
      Cyan removable
    </Label>{' '}
    <Label color="cyan" icon={<InfoCircleIcon />} onClose={() => Function.prototype}>
      Cyan icon removable
    </Label>{' '}
    <Label color="cyan" href="#filled">
      Cyan link
    </Label>{' '}
    <Label color="cyan" href="#filled" onClose={() => Function.prototype}>
      Cyan link removable
    </Label>
    <Label color="cyan" icon={<InfoCircleIcon />} onClose={() => Function.prototype} isTruncated>
      Cyan label with icon that overflows
    </Label>
    <br />
    <br />
    <Label color="gold">Gold</Label>{' '}
    <Label color="gold" icon={<InfoCircleIcon />}>
      Gold icon
    </Label>{' '}
    <Label color="gold" onClose={() => Function.prototype}>
      Gold removable
    </Label>{' '}
    <Label color="gold" icon={<InfoCircleIcon />} onClose={() => Function.prototype}>
      Gold icon removable
    </Label>{' '}
    <Label color="gold" href="#filled">
      Gold link
    </Label>{' '}
    <Label color="gold" href="#filled" onClose={() => Function.prototype}>
      Gold link removable
    </Label>
    <Label color="gold" icon={<InfoCircleIcon />} onClose={() => Function.prototype} isTruncated>
      Gold label with icon that overflows
    </Label>
  </React.Fragment>
);
