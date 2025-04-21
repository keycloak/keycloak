import React from 'react';
import { Card, CardTitle, CardBody, CardFooter } from '@patternfly/react-core';

export const CardWithBodySectionFills: React.FunctionComponent = () => (
  <Card style={{ minHeight: '30em' }}>
    <CardTitle>Header</CardTitle>
    <CardBody isFilled={false}>Body pf-m-no-fill</CardBody>
    <CardBody isFilled={false}>Body pf-m-no-fill</CardBody>
    <CardBody>Body</CardBody>
    <CardFooter>Footer</CardFooter>
  </Card>
);
