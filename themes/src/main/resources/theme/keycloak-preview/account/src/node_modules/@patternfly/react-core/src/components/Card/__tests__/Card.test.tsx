import React from 'react';
import { Card } from '../Card';
import { shallow } from 'enzyme';

test('renders with PatternFly Core styles', () => {
  const view = shallow(<Card />);
  expect(view).toMatchSnapshot();
});

test('className is added to the root element', () => {
  const view = shallow(<Card className="extra-class" />);
  expect(view.prop('className')).toMatchSnapshot();
});

test('extra props are spread to the root element', () => {
  const testId = 'card';
  const view = shallow(<Card data-testid={testId} />);
  expect(view.prop('data-testid')).toBe(testId);
});

test('allows passing in a string as the component', () => {
  const component = 'section';
  const view = shallow(<Card component={component} />);
  expect(view.type()).toBe(component);
});

test('allows passing in a React Component as the component', () => {
  const Component = () => <div>im a div</div>;
  const view = shallow(<Card component={Component} />);
  expect(view.type()).toBe(Component);
});

test('card with isHoverable applied ', () => {
  const view = shallow(<Card isHoverable />);
  expect(view).toMatchSnapshot();
});

test('card with isCompact applied ', () => {
  const view = shallow(<Card isCompact />);
  expect(view).toMatchSnapshot();
});

test('card with isSelectable applied ', () => {
  const view = shallow(<Card isSelectable />);
  expect(view.prop('className')).toMatch(/selectable/);
  expect(view.prop('tabIndex')).toBe('0');
});

test('card with isSelectable and isSelected applied ', () => {
  const view = shallow(<Card isSelectable isSelected />);
  expect(view.prop('className')).toMatch(/selectable/);
  expect(view.prop('className')).toMatch(/selected/);
  expect(view.prop('tabIndex')).toBe('0');
});

test('card with only isSelected applied - not change', () => {
  const view = shallow(<Card isSelected />);
  expect(view.prop('className')).not.toMatch(/selected/);
  expect(view.prop('tabIndex')).toBe(undefined);
});
