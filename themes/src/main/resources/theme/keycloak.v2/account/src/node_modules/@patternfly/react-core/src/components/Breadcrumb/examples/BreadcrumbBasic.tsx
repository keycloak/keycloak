import React from 'react';
import { Breadcrumb, BreadcrumbItem } from '@patternfly/react-core';

export const BreadcrumbBasic: React.FunctionComponent = () => (
  <Breadcrumb>
    <BreadcrumbItem to="#">Section home</BreadcrumbItem>
    <BreadcrumbItem to="#">Section title</BreadcrumbItem>
    <BreadcrumbItem to="#">Section title</BreadcrumbItem>
    <BreadcrumbItem to="#" isActive>
      Section landing
    </BreadcrumbItem>
  </Breadcrumb>
);
