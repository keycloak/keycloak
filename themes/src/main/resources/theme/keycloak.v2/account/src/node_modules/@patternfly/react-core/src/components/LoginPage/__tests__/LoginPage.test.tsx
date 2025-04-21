import * as React from 'react';
import { render } from '@testing-library/react';

import { LoginPage } from '../LoginPage';
import { ListVariant } from '../../List';

const needAccountMesseage = (
  <React.Fragment>
    Login to your account <a href="https://www.patternfly.org">Need an account?</a>
  </React.Fragment>
);

test('check loginpage example against snapshot', () => {
  const { asFragment } = render(
    <LoginPage
      footerListVariants={ListVariant.inline}
      brandImgSrc="Brand src"
      brandImgAlt="Pf-logo"
      backgroundImgSrc="Background src"
      backgroundImgAlt="Pf-background"
      footerListItems="English"
      textContent="This is placeholder text only."
      loginTitle="Log into your account"
      signUpForAccountMessage={needAccountMesseage}
      socialMediaLoginContent="Footer"
    />
  );
  expect(asFragment()).toMatchSnapshot();
});
