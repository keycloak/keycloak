import { Button, ButtonVariant } from '../Button';
import React from 'react';
import { mount } from 'enzyme';
import CartArrowDownIcon from '@patternfly/react-icons/dist/js/icons/cart-arrow-down-icon';

Object.values(ButtonVariant).forEach(variant => {
  test(`${variant} button`, () => {
    const view = mount(
      <Button variant={variant} aria-label={variant}>
        {variant} Button
      </Button>
    );
    expect(view).toMatchSnapshot();
  });
});

test('it adds an aria-label to plain buttons', () => {
  const label = 'aria-label test';
  const view = mount(<Button aria-label={label} />);
  expect(view.find('button').props()['aria-label']).toBe(label);
});

test('link with icon', () => {
  const view = mount(
    <Button variant={ButtonVariant.link} icon={<CartArrowDownIcon />}>
      Block Button
    </Button>
  );
  expect(view).toMatchSnapshot();
});

test('isBlock', () => {
  const view = mount(<Button isBlock>Block Button</Button>);
  expect(view).toMatchSnapshot();
});

test('isDisabled', () => {
  const view = mount(<Button isDisabled>Disabled Button</Button>);
  expect(view).toMatchSnapshot();
});

test('isFocus', () => {
  const view = mount(<Button isFocus>Focused Button</Button>);
  expect(view).toMatchSnapshot();
});

test('isHover', () => {
  const view = mount(<Button isHover>Hovered Button</Button>);
  expect(view).toMatchSnapshot();
});

test('isInline', () => {
  const view = mount(
    <Button variant={ButtonVariant.link} isInline>
      Hovered Button
    </Button>
  );
  expect(view).toMatchSnapshot();
});

test('allows passing in a string as the component', () => {
  const component = 'a';
  const view = mount(<Button component={component} />);
  expect(view.find(component).type()).toBe(component);
});

test('allows passing in a React Component as the component', () => {
  const Component = () => <div>im a div</div>;
  const view = mount(<Button component={Component} />);
  expect(view.find(Component).type()).toBe(Component);
});

test('aria-disabled is set to true and tabIndex to -1 if component is not a button and is disabled', () => {
  const view = mount(
    <Button component="a" isDisabled>
      Disabled Anchor Button
    </Button>
  );
  expect(view).toMatchSnapshot();
});

test('setting tab index through props', () => {
  const view = mount(<Button tabIndex={0}>TabIndex 0 Button</Button>);
  expect(view.find('button').props().tabIndex).toBe(0);
});
