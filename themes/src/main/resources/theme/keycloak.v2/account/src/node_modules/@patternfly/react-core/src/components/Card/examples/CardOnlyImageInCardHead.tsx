import React from 'react';
import { Brand, Card, CardBody, CardFooter, CardHeader, CardHeaderMain, CardTitle } from '@patternfly/react-core';
import pfLogo from './pfLogo.svg';

export const CardOnlyImageInCardHead: React.FunctionComponent = () => (
  <Card>
    <CardHeader>
      <CardHeaderMain>
        <Brand src={pfLogo} alt="PatternFly logo" style={{ height: '50px' }} />
      </CardHeaderMain>
    </CardHeader>
    <CardTitle>Header</CardTitle>
    <CardBody>Body</CardBody>
    <CardFooter>Footer</CardFooter>
  </Card>
);
