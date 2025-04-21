import React from 'react';
import { ExpandableSection } from '@patternfly/react-core';

export const ExpandableSectionUncontrolled: React.FunctionComponent = () => (
  <ExpandableSection toggleText="Show more">
    This content is visible only when the component is expanded.
  </ExpandableSection>
);
