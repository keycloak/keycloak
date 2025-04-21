import React from 'react';
import { ExpandableSection, Badge } from '@patternfly/react-core';
import CheckCircleIcon from '@patternfly/react-icons/dist/esm/icons/check-circle-icon';

export const ExpandableSectionCustomToggle: React.FunctionComponent = () => {
  const [isExpanded, setIsExpanded] = React.useState(false);

  const onToggle = (isExpanded: boolean) => {
    setIsExpanded(isExpanded);
  };

  return (
    <ExpandableSection
      toggleContent={
        <div>
          <span>You can also use icons </span>
          <CheckCircleIcon />
          <span> or badges </span>
          <Badge isRead={true}>4</Badge>
          <span> !</span>
        </div>
      }
      onToggle={onToggle}
      isExpanded={isExpanded}
    >
      This content is visible only when the component is expanded.
    </ExpandableSection>
  );
};
