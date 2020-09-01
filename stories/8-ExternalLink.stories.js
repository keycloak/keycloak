import React from 'react';
import { storiesOf } from '@storybook/react';
import { ExternalLink } from '../src/components/external-link/ExternalLink';

storiesOf('External Link', module).add('view', () => {
  return (
    <>
      <p>
        <ExternalLink href="http://test.nl/" />
      </p>
      <p>
        <ExternalLink href="http://test.nl/" title="With title" />
      </p>
    </>
  );
});
