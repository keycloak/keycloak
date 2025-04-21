import React from 'react';
import { Title, Button, EmptyState, EmptyStatePrimary, EmptyStateIcon, EmptyStateBody } from '@patternfly/react-core';
import SearchIcon from '@patternfly/react-icons/dist/esm/icons/search-icon';

export const EmptyStateNoMatchFound: React.FunctionComponent = () => (
  <EmptyState>
    <EmptyStateIcon icon={SearchIcon} />
    <Title size="lg" headingLevel="h4">
      No results found
    </Title>
    <EmptyStateBody>No results match the filter criteria. Clear all filters and try again.</EmptyStateBody>
    <EmptyStatePrimary>
      <Button variant="link">Clear all filters</Button>
    </EmptyStatePrimary>
  </EmptyState>
);
