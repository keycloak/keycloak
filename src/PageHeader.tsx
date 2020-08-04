import React, { useContext } from 'react';
import { PageHeader, Brand, PageHeaderTools } from '@patternfly/react-core';
import { KeycloakContext } from './auth/KeycloakContext';

export const Header = () => {
  const keycloak = useContext(KeycloakContext);
  return (
    <PageHeader
      logo={<Brand src="/logo.svg" alt="Logo" style={{ height: '35px' }} />}
      headerTools={<PageHeaderTools>{keycloak?.loggedInUser}</PageHeaderTools>}
    />
  );
};
