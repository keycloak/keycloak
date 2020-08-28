import React from 'react';
import { storiesOf } from '@storybook/react';

import { ClientList } from '../src/clients/ClientList';
import clientMock from '../src/clients/mock-clients.json';

storiesOf('Client list page', module)
  .add('view', () => {
    return (<ClientList clients={clientMock} baseUrl="http://test.nl"/>
    );
  })
