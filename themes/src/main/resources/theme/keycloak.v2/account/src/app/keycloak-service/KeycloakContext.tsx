import * as React from 'react';
import { KeycloakService } from './keycloak.service';

export const KeycloakContext = React.createContext<KeycloakService | undefined>(undefined);