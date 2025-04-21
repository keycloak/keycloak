import React from 'react';
import { Checkbox } from '@patternfly/react-core';

export const CheckboxDisabled: React.FunctionComponent = () => (
  <React.Fragment>
    <Checkbox id="disabled-check-1" label="Disabled CheckBox 1" defaultChecked isDisabled />
    <Checkbox id="disabled-check-2" label="Disabled CheckBox 2" isDisabled />
  </React.Fragment>
);
