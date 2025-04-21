import React from 'react';
import { Card, CardTitle, CardBody, CardFooter } from '@patternfly/react-core';

export const CardWithMultipleBodySections: React.FunctionComponent = () => (
  <Card>
    <CardTitle>Header</CardTitle>
    <CardBody>Body</CardBody>
    <CardBody>Body</CardBody>
    <CardBody>Body</CardBody>
    <CardFooter>Footer</CardFooter>
  </Card>
);
