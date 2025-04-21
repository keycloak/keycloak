import React from 'react';
import { Brand } from '@patternfly/react-core';
import logoXl from './pf-c-brand--logo-on-xl.svg';
import logoLg from './pf-c-brand--logo-on-lg.svg';
import logoMd from './pf-c-brand--logo-on-md.svg';
import logoSm from './pf-c-brand--logo-on-sm.svg';
import logo from './pf-c-brand--logo.svg';
import logoBase from './pf-c-brand--logo-base.jpg';

export const BrandBasic: React.FunctionComponent = () => (
  <Brand src={logoBase} alt="Fallback patternfly default logo" widths={{ default: '40px', sm: '60px', md: '220px' }}>
    <source media="(min-width: 1200px)" srcSet={logoXl} />
    <source media="(min-width: 992px)" srcSet={logoLg} />
    <source media="(min-width: 768px)" srcSet={logoMd} />
    <source media="(min-width: 576px)" srcSet={logoSm} />
    <source srcSet={logo} />
  </Brand>
);
