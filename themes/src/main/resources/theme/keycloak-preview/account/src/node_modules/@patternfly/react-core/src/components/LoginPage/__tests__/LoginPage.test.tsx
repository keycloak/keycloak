import * as React from 'react';
import { shallow } from 'enzyme';

import { LoginPage } from '../LoginPage';
import { ListVariant } from '../../List';

const needAccountMesseage = (
  <React.Fragment>
    Login to your account <a href="https://www.patternfly.org">Need an account?</a>
  </React.Fragment>
);

test('check loginpage example against snapshot', () => {
  const view = shallow(
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
  expect(view).toMatchSnapshot();
});
