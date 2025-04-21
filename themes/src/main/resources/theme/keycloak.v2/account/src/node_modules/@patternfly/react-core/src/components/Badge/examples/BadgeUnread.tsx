import React from 'react';
import { Badge } from '@patternfly/react-core';

export const BadgeUnread: React.FunctionComponent = () => (
  <React.Fragment>
    <Badge key={1}>7</Badge> <Badge key={2}>24</Badge> <Badge key={3}>240</Badge> <Badge key={4}>999+</Badge>
  </React.Fragment>
);
