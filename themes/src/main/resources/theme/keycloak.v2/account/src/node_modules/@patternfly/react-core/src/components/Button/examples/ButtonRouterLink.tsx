import React from 'react';
import { Button } from '@patternfly/react-core';
import { Link } from '@reach/router';

export const ButtonRouterLink: React.FunctionComponent = () => (
  <Button variant="link" component={(props: any) => <Link {...props} to="#" />}>
    Router link
  </Button>
);
