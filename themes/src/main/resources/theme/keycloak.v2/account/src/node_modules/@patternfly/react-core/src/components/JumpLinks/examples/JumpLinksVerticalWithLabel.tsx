import React from 'react';
import { JumpLinks, JumpLinksItem } from '@patternfly/react-core';

export const JumpLinksVerticalWithLabel: React.FunctionComponent = () => (
  <JumpLinks isVertical label="Jump to section">
    <JumpLinksItem>Inactive section</JumpLinksItem>
    <JumpLinksItem isActive>Active section</JumpLinksItem>
    <JumpLinksItem>Inactive section</JumpLinksItem>
  </JumpLinks>
);
