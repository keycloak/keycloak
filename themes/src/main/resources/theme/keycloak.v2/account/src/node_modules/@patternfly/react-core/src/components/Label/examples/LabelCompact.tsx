import React from 'react';
import { Label } from '@patternfly/react-core';
import InfoCircleIcon from '@patternfly/react-icons/dist/esm/icons/info-circle-icon';

export const LabelCompact: React.FunctionComponent = () => (
  <React.Fragment>
    <Label isCompact>Grey</Label>{' '}
    <Label isCompact icon={<InfoCircleIcon />}>
      Compact icon
    </Label>{' '}
    <Label isCompact onClose={() => Function.prototype}>
      Compact removable
    </Label>{' '}
    <Label isCompact icon={<InfoCircleIcon />} onClose={() => Function.prototype}>
      Compact icon removable
    </Label>{' '}
    <Label isCompact href="#outline">
      Compact link
    </Label>{' '}
    <Label isCompact href="#outline" onClose={() => Function.prototype}>
      Compact link removable
    </Label>
    <Label isCompact icon={<InfoCircleIcon />} onClose={() => Function.prototype} isTruncated>
      Compact label with icon that overflows
    </Label>
  </React.Fragment>
);
