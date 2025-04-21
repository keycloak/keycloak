import React from 'react';
import { Button } from '@patternfly/react-core';

export const ButtonTypes: React.FunctionComponent = () => (
  <React.Fragment>
    <Button type="submit">Submit</Button> <Button type="reset">Reset</Button> <Button>Default</Button>
  </React.Fragment>
);
