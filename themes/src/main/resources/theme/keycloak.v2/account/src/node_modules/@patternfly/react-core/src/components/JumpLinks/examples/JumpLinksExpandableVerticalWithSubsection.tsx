import React from 'react';
import { JumpLinks, JumpLinksItem, JumpLinksList } from '@patternfly/react-core';

export const JumpLinksExpandableVerticalWithSubsection: React.FunctionComponent = () => (
  <JumpLinks isVertical label="Jump to section" expandable={{ default: 'expandable' }}>
    <JumpLinksItem>Inactive section</JumpLinksItem>
    <JumpLinksItem>
      Section with active subsection
      <JumpLinksList>
        <JumpLinksItem isActive>Active subsection</JumpLinksItem>
        <JumpLinksItem>Inactive subsection</JumpLinksItem>
        <JumpLinksItem>Inactive subsection</JumpLinksItem>
      </JumpLinksList>
    </JumpLinksItem>
    <JumpLinksItem>Inactive section</JumpLinksItem>
    <JumpLinksItem>Inactive section</JumpLinksItem>
  </JumpLinks>
);
