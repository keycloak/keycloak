import * as React from 'react';
import { shallow } from 'enzyme';

import { LoginFooterItem } from '../LoginFooterItem';

test('renders with PatternFly Core styles', () => {
  const view = shallow(<LoginFooterItem target="_self" href="#" />);
  expect(view).toMatchSnapshot();
});

test('className is added to the root element', () => {
  const view = shallow(<LoginFooterItem className="extra-class" />);
  expect(view.prop('className')).toMatchSnapshot();
});

test('extra props are spread to the root element', () => {
  const testId = 'login-body';
  const view = shallow(<LoginFooterItem data-testid={testId} />);
  expect(view.prop('data-testid')).toBe(testId);
});

test('LoginFooterItem  with custom node', () => {
  const CustomNode = () => <div>My custom node</div>;
  const view = shallow(
    <LoginFooterItem>
      <CustomNode />
    </LoginFooterItem>
  );
  expect(view).toMatchSnapshot();
});
