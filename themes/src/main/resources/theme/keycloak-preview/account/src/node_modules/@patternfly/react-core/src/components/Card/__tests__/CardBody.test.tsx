import React from 'react';
import { CardBody } from '../CardBody';
import { shallow } from 'enzyme';

test('renders with PatternFly Core styles', () => {
  const view = shallow(<CardBody />);
  expect(view).toMatchSnapshot();
});

test('className is added to the root element', () => {
  const view = shallow(<CardBody className="extra-class" />);
  expect(view.prop('className')).toMatchSnapshot();
});

test('extra props are spread to the root element', () => {
  const testId = 'card-body';
  const view = shallow(<CardBody data-testid={testId} />);
  expect(view.prop('data-testid')).toBe(testId);
});

test('allows passing in a string as the component', () => {
  const component = 'section';
  const view = shallow(<CardBody component={component} />);
  expect(view.type()).toBe(component);
});

test('allows passing in a React Component as the component', () => {
  const Component = () => <div>im a div</div>;
  const view = shallow(<CardBody component={Component} />);
  expect(view.type()).toBe(Component);
});

test('body with no-fill applied ', () => {
  const view = shallow(<CardBody isFilled={false} />);
  expect(view).toMatchSnapshot();
});
