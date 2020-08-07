import React, { useContext } from 'react';
import {
  Page,
  PageSection,
  Button,
  AlertVariant,
} from '@patternfly/react-core';

import { ClientList } from './clients/ClientList';
import { DataLoader } from './components/data-loader/DataLoader';
import { HttpClientContext } from './http-service/HttpClientContext';
import { Client } from './clients/client-model';
import { Header } from './PageHeader';
import { PageNav } from './PageNav';
import { AlertPanel } from './components/alert/AlertPanel';
import { useAlerts, withAlerts } from './components/alert/Alerts';

const AppComponent = () => {
  const [alerts, add, hide] = useAlerts();
  const httpClient = useContext(HttpClientContext);

  const loader = async () => {
    return await httpClient
      ?.doGet('/realms/master/clients?first=0&max=20&search=true')
      .then((r) => r.data as Client[]);
  };
  return (
    <Page header={<Header />} sidebar={<PageNav />}>
      <PageSection>
        <AlertPanel alerts={alerts} onCloseAlert={hide} />
        <DataLoader loader={loader}>
          {(clients) => <ClientList clients={clients} />}
        </DataLoader>
        <Button
          onClick={() => add('Crazy stuff happened', AlertVariant.danger)}
        >
          Click
        </Button>
      </PageSection>
    </Page>
  );
};

export const App = withAlerts(AppComponent);
