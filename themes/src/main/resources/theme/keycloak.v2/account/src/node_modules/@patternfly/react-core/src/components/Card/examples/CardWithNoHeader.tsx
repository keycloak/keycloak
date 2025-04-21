import React from 'react';
import { Card, CardBody, CardFooter } from '@patternfly/react-core';

export const CardWithNoHeader: React.FunctionComponent = () => (
  <Card>
    <CardBody>This card has no header</CardBody>
    <CardFooter>Footer</CardFooter>
  </Card>
);
