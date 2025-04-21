import React from 'react';
import { Alert, AlertGroup } from '@patternfly/react-core';

export const AlertGroupStatic: React.FunctionComponent = () => (
  <React.Fragment>
    <AlertGroup>
      <Alert title="Success alert" variant="success" isInline />
      <Alert title="Info alert" variant="info" isInline />
    </AlertGroup>
  </React.Fragment>
);
