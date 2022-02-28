import * as React from 'react';
import { shallow } from 'enzyme';

import { LoginMainHeader } from '../LoginMainHeader';

test('renders with PatternFly Core styles', () => {
  const view = shallow(<LoginMainHeader />);
  expect(view).toMatchSnapshot();
});

test('className is added to the root element', () => {
  const view = shallow(<LoginMainHeader className="extra-class" />);
  expect(view.prop('className')).toMatchSnapshot();
});

test('extra props are spread to the root element', () => {
  const testId = 'login-header';
  const view = shallow(<LoginMainHeader data-testid={testId} />);
  expect(view.prop('data-testid')).toBe(testId);
});

test('title and subtitle are rendered correctly', () => {
  const view = shallow(<LoginMainHeader title="Log in to your account" subtitle="Use LDAP credentials" />);
  expect(view.prop('className')).toMatchSnapshot();
});
