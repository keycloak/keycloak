import React from 'react';
import { Card, CardTitle, CardBody, CardFooter } from '@patternfly/react-core';

export const CardWithHeadingElement: React.FunctionComponent = () => (
  <Card>
    <CardTitle component="h4">Header within an {'<h4>'} element</CardTitle>
    <CardBody>Body</CardBody>
    <CardFooter>Footer</CardFooter>
  </Card>
);
