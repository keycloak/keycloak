import React from 'react';
import { Brand } from '@patternfly/react-core';
import pfLogo from './pfLogo.svg';

export const BrandBasic: React.FunctionComponent = () => <Brand src={pfLogo} alt="Patternfly Logo" />;
