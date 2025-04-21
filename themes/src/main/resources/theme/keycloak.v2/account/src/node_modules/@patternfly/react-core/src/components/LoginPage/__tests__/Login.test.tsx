import * as React from 'react';
import { render } from '@testing-library/react';
import { Login } from '../Login';
import { LoginFooter } from '../LoginFooter';
import { LoginHeader } from '../LoginHeader';

test('Check login layout example against snapshot', () => {
  const Header = <LoginHeader headerBrand="HeaderBrand">Header Text</LoginHeader>;
  const Footer = <LoginFooter>Footer</LoginFooter>;
  const { asFragment } = render(
    <Login footer={Footer} header={Header}>
      Main
    </Login>
  );
  expect(asFragment()).toMatchSnapshot();
});
