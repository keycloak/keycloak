import React from 'react';
import { Checkbox } from '@patternfly/react-core';

export const CheckboxWithBody: React.FunctionComponent = () => (
  <Checkbox id="body-check-1" label="Checkbox with body" body="This is where custom content goes." />
);
