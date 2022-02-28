import * as React from 'react';
import { shallow } from 'enzyme';

import { LoginForm } from '../LoginForm';

test('should render Login form', () => {
  const view = shallow(<LoginForm />);
  expect(view).toMatchSnapshot();
});

test('should call onChangeUsername callback', () => {
  const mockFn = jest.fn();
  const view = shallow(<LoginForm onChangeUsername={mockFn} rememberMeLabel="Login Form" />);
  view.find('#pf-login-username-id').simulate('change');
  expect(mockFn).toHaveBeenCalled();
});

test('should call onChangePassword callback', () => {
  const mockFn = jest.fn();
  const view = shallow(<LoginForm onChangePassword={mockFn} rememberMeLabel="Login Form" />);
  view.find('#pf-login-password-id').simulate('change');
  expect(mockFn).toHaveBeenCalled();
});

test('should call onChangeRememberMe callback', () => {
  const mockFn = jest.fn();
  const view = shallow(<LoginForm onChangeRememberMe={mockFn} rememberMeLabel="Login Form" />);
  view.find('#pf-login-remember-me-id').simulate('change');
  expect(mockFn).toHaveBeenCalled();
});

test('LoginForm with rememberMeLabel', () => {
  const view = shallow(<LoginForm rememberMeLabel="remember me" />);
  expect(view).toMatchSnapshot();
});

test('LoginForm with rememberMeLabel and rememberMeAriaLabel uses the rememberMeAriaLabel', () => {
  const view = shallow(<LoginForm rememberMeAriaLabel="ARIA remember me" rememberMeLabel="remember me" />);
  expect(view).toMatchSnapshot();
});
