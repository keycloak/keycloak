import React from 'react';
import { Checkbox } from '@patternfly/react-core';

export const CheckboxWithDescription: React.FunctionComponent = () => (
  <Checkbox
    id="description-check-1"
    label="Checkbox with description"
    description="Single-tenant cloud service hosted and managed by Red Hat that offers high-availability enterprise-grade clusters in a virtual private cloud on AWS or GCP."
  />
);
