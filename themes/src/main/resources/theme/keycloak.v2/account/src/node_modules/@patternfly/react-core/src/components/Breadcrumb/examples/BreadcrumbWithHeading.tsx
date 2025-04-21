import React from 'react';
import { Breadcrumb, BreadcrumbItem, BreadcrumbHeading } from '@patternfly/react-core';

export const BreadcrumbWithHeading: React.FunctionComponent = () => (
  <Breadcrumb>
    <BreadcrumbItem to="#">Section home</BreadcrumbItem>
    <BreadcrumbItem to="#">Section title</BreadcrumbItem>
    <BreadcrumbItem to="#">Section title</BreadcrumbItem>
    <BreadcrumbItem to="#">Section title</BreadcrumbItem>
    <BreadcrumbHeading to="#">Section title</BreadcrumbHeading>
  </Breadcrumb>
);
