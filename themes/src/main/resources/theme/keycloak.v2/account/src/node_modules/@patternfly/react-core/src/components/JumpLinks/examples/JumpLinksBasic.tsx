import React from 'react';
import { JumpLinks, JumpLinksItem } from '@patternfly/react-core';

export const JumpLinksBasic: React.FunctionComponent = () => (
  <JumpLinks>
    <JumpLinksItem>Inactive section</JumpLinksItem>
    <JumpLinksItem isActive>Active section</JumpLinksItem>
    <JumpLinksItem>Inactive section</JumpLinksItem>
  </JumpLinks>
);
