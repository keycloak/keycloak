import React from 'react';
import { Checkbox } from '@patternfly/react-core';

export const CheckboxWithDescriptionBody: React.FunctionComponent = () => (
  <Checkbox
    id="description-body-check"
    label="Checkbox with description and body"
    description="Single-tenant cloud service hosted and managed by Red Hat that offers high-availability enterprise-grade clusters in a virtual private cloud on AWS or GCP."
    body="This is where custom content goes."
  />
);
