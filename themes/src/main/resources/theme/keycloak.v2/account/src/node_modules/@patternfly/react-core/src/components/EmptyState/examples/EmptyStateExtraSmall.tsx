import React from 'react';
import {
  Title,
  Button,
  EmptyState,
  EmptyStateVariant,
  EmptyStateBody,
  EmptyStateSecondaryActions
} from '@patternfly/react-core';

export const EmptyStateExtraSmall: React.FunctionComponent = () => (
  <EmptyState variant={EmptyStateVariant.xs}>
    <Title headingLevel="h4" size="md">
      Empty state
    </Title>
    <EmptyStateBody>
      This represents an the empty state pattern in Patternfly 4. Hopefully it's simple enough to use but flexible
      enough to meet a variety of needs.
    </EmptyStateBody>
    <EmptyStateSecondaryActions>
      <Button variant="link">Multiple</Button>
      <Button variant="link">Action Buttons</Button>
      <Button variant="link">Can</Button>
      <Button variant="link">Go here</Button>
      <Button variant="link">In the secondary</Button>
      <Button variant="link">Action area</Button>
    </EmptyStateSecondaryActions>
  </EmptyState>
);
