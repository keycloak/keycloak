import React from 'react';
import { JumpLinks, JumpLinksItem } from '@patternfly/react-core';

export const JumpLinksWithCenteredList: React.FunctionComponent = () => (
  <JumpLinks isCentered>
    <JumpLinksItem>Inactive section</JumpLinksItem>
    <JumpLinksItem isActive>Active section</JumpLinksItem>
    <JumpLinksItem>Inactive section</JumpLinksItem>
  </JumpLinks>
);
