import React from 'react';
import { JumpLinks, JumpLinksItem } from '@patternfly/react-core';

export const JumpLinksWithLabel: React.FunctionComponent = () => (
  <>
    <JumpLinks label="Jump to section">
      <JumpLinksItem>Inactive section</JumpLinksItem>
      <JumpLinksItem isActive>Active section</JumpLinksItem>
      <JumpLinksItem>Inactive section</JumpLinksItem>
    </JumpLinks>
    <JumpLinks isCentered label="Jump to section">
      <JumpLinksItem>Inactive section</JumpLinksItem>
      <JumpLinksItem isActive>Active section</JumpLinksItem>
      <JumpLinksItem>Inactive section</JumpLinksItem>
    </JumpLinks>
  </>
);
