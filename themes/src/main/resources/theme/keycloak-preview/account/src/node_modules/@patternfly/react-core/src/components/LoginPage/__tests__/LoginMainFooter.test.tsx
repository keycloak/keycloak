import * as React from 'react';
import { shallow } from 'enzyme';

import { LoginMainFooter } from '../LoginMainFooter';

test('renders with PatternFly Core styles', () => {
  const view = shallow(<LoginMainFooter />);
  expect(view).toMatchSnapshot();
});

test('className is added to the root element', () => {
  const view = shallow(<LoginMainFooter className="extra-class" />);
  expect(view.prop('className')).toMatchSnapshot();
});

test('extra props are spread to the root element', () => {
  const testId = 'login-body';
  const view = shallow(<LoginMainFooter data-testid={testId} />);
  expect(view.prop('data-testid')).toBe(testId);
});
