import React from 'react';
import { ExpandableSection } from '@patternfly/react-core';

export const ExpandableSectionUncontrolledDynamicToggle: React.FunctionComponent = () => (
  <ExpandableSection toggleTextExpanded="Show less" toggleTextCollapsed="Show more">
    This content is visible only when the component is expanded.
  </ExpandableSection>
);
