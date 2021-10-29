import React from 'react';
import { PageSection } from '@patternfly/react-core';
import quickRefPdf from './reference-docs/PF-quick-ref.pdf';

const HomePage = () => (
  <PageSection variant="light">
    Just getting started? Take a look at the <a href="/contribution">Contribution guide</a> and{' '}
    <a href="/guidelines">Coding guidelines.</a> Always keep our <a href="/accessibility-guide">Accessibility guide</a>{' '}
    in mind. Download a <a href={quickRefPdf}>quick reference sheet.</a>
  </PageSection>
);

export default HomePage;
