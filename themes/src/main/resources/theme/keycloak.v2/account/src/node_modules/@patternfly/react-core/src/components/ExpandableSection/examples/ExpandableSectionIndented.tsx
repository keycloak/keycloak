import React from 'react';
import { ExpandableSection } from '@patternfly/react-core';

export const ExpandableSectionIndented: React.FunctionComponent = () => {
  const [isExpanded, setIsExpanded] = React.useState(false);

  const onToggle = (isExpanded: boolean) => {
    setIsExpanded(isExpanded);
  };

  return (
    <ExpandableSection
      toggleText={isExpanded ? 'Show less' : 'Show more'}
      onToggle={onToggle}
      isExpanded={isExpanded}
      isIndented
    >
      This content is visible only when the component is expanded.
    </ExpandableSection>
  );
};
