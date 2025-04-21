import React from 'react';
import { Backdrop, Spinner, Bullseye } from '@patternfly/react-core';

export const BackdropSpinner: React.FunctionComponent = () => (
  <Backdrop>
    <Bullseye>
      <Spinner aria-label="Backdrop content" aria-valueText="Loading..." />
    </Bullseye>
  </Backdrop>
);
