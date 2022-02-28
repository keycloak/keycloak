import React from 'react';
import { FormHelperText } from '../FormHelperText';
import { shallow } from 'enzyme';

test('renders with PatternFly Core styles', () => {
  const view = shallow(<FormHelperText isError isHidden={false} />);
  expect(view).toMatchSnapshot();
});

test('className is added to the root element', () => {
  const view = shallow(<FormHelperText className="extra-class" />);
  expect(view.prop('className')).toMatchSnapshot();
});

test('extra props are spread to the root element', () => {
  const testId = 'login-body';
  const view = shallow(<FormHelperText data-testid={testId} />);
  expect(view.prop('data-testid')).toBe(testId);
});

test('LoginFooterItem  with custom node', () => {
  const CustomNode = () => <div>My custom node</div>;
  const view = shallow(
    <FormHelperText>
      <CustomNode />
    </FormHelperText>
  );
  expect(view).toMatchSnapshot();
});
