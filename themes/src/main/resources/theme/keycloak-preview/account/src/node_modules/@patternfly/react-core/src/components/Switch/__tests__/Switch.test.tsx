import * as React from 'react';
import { mount } from 'enzyme';
import { Switch } from '../Switch';

const props = {
  onChange: jest.fn(),
  isChecked: false
};

test('switch label for attribute equals input id attribute', () => {
  const view = mount(<Switch id="foo" />);
  expect(view.find('input').prop('id')).toBe('foo');
  expect(view.find('label').prop('htmlFor')).toBe('foo');
});

test('switch label id is auto generated', () => {
  const view = mount(<Switch aria-label="..." />);
  expect(view.find('input').prop('id')).toBeDefined();
});

test('switch is checked', () => {
  const view = mount(<Switch id="switch-is-checked" label="On" labelOff="Off" isChecked />);
  expect(view).toMatchSnapshot();
});

test('switch is not checked', () => {
  const view = mount(<Switch id="switch-is-not-checked" label="On" labelOff="Off" isChecked={false} />);
  expect(view).toMatchSnapshot();
});

test('switch with only label is checked', () => {
  const check = true;
  const view = mount(<Switch id="switch-is-checked" label={check ? 'On' : 'Off'} isChecked={check} />);
  expect(view).toMatchSnapshot();
});

test('switch with only label is not checked', () => {
  const check = false;
  const view = mount(<Switch id="switch-is-not-checked" label={check ? 'On' : 'Off'} isChecked={check} />);
  expect(view).toMatchSnapshot();
});

test('no label switch is checked', () => {
  const view = mount(<Switch id="no-label-switch-is-checked" isChecked />);
  expect(view).toMatchSnapshot();
});

test('no label switch is not checked', () => {
  const view = mount(<Switch id="no-label-switch-is-not-checked" isChecked={false} />);
  expect(view).toMatchSnapshot();
});

test('switch is checked and disabled', () => {
  const view = mount(<Switch id="switch-is-checked-and-disabled" isChecked isDisabled />);
  expect(view).toMatchSnapshot();
});

test('switch is not checked and disabled', () => {
  const view = mount(<Switch id="switch-is-not-checked-and-disabled" isChecked={false} isDisabled />);
  expect(view).toMatchSnapshot();
});

test('switch passes value and event to onChange handler', () => {
  const view = mount(<Switch id="onChange-switch" {...props} />);
  const input = view.find('input');
  expect(input.prop('checked')).toBe(false);
  input.simulate('change', { target: { checked: true } });
  expect(props.onChange.mock.calls[0][0]).toBe(true);
});
