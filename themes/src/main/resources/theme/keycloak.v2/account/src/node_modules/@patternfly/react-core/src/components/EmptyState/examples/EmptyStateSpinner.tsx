import React from 'react';
import { Title, EmptyState, EmptyStateIcon, Spinner } from '@patternfly/react-core';

export const EmptyStateSpinner: React.FunctionComponent = () => (
  <EmptyState>
    <EmptyStateIcon variant="container" component={Spinner} />
    <Title size="lg" headingLevel="h4">
      Loading
    </Title>
  </EmptyState>
);
