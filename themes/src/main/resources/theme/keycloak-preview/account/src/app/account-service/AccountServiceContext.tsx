import * as React from 'react';
import { AccountServiceClient } from './account.service';

export const AccountServiceContext = React.createContext<AccountServiceClient | undefined>(undefined);