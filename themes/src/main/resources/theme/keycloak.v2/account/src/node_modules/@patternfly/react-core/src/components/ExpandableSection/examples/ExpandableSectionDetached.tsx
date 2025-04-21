import React from 'react';
import { ExpandableSection, ExpandableSectionToggle, Stack, StackItem } from '@patternfly/react-core';

export const ExpandableSectionDetached: React.FunctionComponent = () => {
  const [isExpanded, setIsExpanded] = React.useState(false);

  const onToggle = (isExpanded: boolean) => {
    setIsExpanded(isExpanded);
  };

  const contentId = 'detached-toggle-content';
  return (
    <Stack hasGutter>
      <StackItem>
        <ExpandableSection isExpanded={isExpanded} isDetached contentId={contentId}>
          This content is visible only when the component is expanded.
        </ExpandableSection>
      </StackItem>
      <StackItem>
        <ExpandableSectionToggle isExpanded={isExpanded} onToggle={onToggle} contentId={contentId} direction="up">
          {isExpanded ? 'Show less' : 'Show more'}
        </ExpandableSectionToggle>
      </StackItem>
    </Stack>
  );
};
