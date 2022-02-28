/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { LoginForm } from '../../LoginForm';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('LoginForm should match snapshot (auto-generated)', () => {
  const view = shallow(
    <LoginForm
      noAutoFocus={false}
      className={"''"}
      showHelperText={false}
      helperText={null}
      usernameLabel={"'Username'"}
      usernameValue={"''"}
      onChangeUsername={() => undefined as any}
      isValidUsername={true}
      passwordLabel={"'Password'"}
      passwordValue={"''"}
      onChangePassword={() => undefined as any}
      isValidPassword={true}
      loginButtonLabel={"'Log In'"}
      isLoginButtonDisabled={false}
      onLoginButtonClick={() => undefined as any}
      rememberMeLabel={"''"}
      isRememberMeChecked={false}
      onChangeRememberMe={() => undefined as any}
      rememberMeAriaLabel={"''"}
    />
  );
  expect(view).toMatchSnapshot();
});
