import React from 'react';
import { Card, CardTitle, CardBody, CardFooter } from '@patternfly/react-core';

export const CardBasic: React.FunctionComponent = () => (
  <Card>
    <CardTitle>Header</CardTitle>
    <CardBody>Body</CardBody>
    <CardFooter>Footer</CardFooter>
  </Card>
);
