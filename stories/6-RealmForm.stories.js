import React from 'react';
import { storiesOf } from '@storybook/react';
import { Page } from '@patternfly/react-core';
import { NewRealmForm } from '../src/forms/realm/NewRealmForm';

storiesOf('Realm Form', module)
  .add('view', () => {
    return (
      <Page>
        <NewRealmForm  />
      </Page>
    );
  })
