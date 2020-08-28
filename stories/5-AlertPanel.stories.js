import React from 'react';
import { AlertVariant, Button } from '@patternfly/react-core';
import { storiesOf } from '@storybook/react';

import { AlertPanel } from '../src/components/alert/AlertPanel';
import { useAlerts } from '../src/components/alert/Alerts';

storiesOf('Alert Panel', module)
  .add('api', () => <AlertPanel alerts={[{ key: 1, message: 'Hello', variant: AlertVariant.default }]} onCloseAlert={() => { }} />)
  .add('add alert', () => {
    const [add, alerts, hide] = useAlerts();
    return (
      <>
        <AlertPanel alerts={alerts} onCloseAlert={hide} />
        <Button onClick={() => add('Hello', AlertVariant.default)}>Add</Button>
      </>
    );
  });
