import React from 'react';
import { JumpLinks, JumpLinksItem } from '@patternfly/react-core';

export const JumpLinksVertical: React.FunctionComponent = () => (
  <JumpLinks isVertical>
    <JumpLinksItem>Inactive section</JumpLinksItem>
    <JumpLinksItem isActive>Active section</JumpLinksItem>
    <JumpLinksItem>Inactive section</JumpLinksItem>
  </JumpLinks>
);
