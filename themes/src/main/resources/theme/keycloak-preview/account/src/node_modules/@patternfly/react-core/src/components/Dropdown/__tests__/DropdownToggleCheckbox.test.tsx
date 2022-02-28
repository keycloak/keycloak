import * as React from 'react';
import { shallow, mount } from 'enzyme';
import { DropdownToggleCheckbox } from '../DropdownToggleCheckbox';

const props = {
  onChange: jest.fn(),
  isChecked: false
};

test('controlled', () => {
  const view = shallow(<DropdownToggleCheckbox isChecked id="check" aria-label="check" />);
  expect(view).toMatchSnapshot();
});

test('uncontrolled', () => {
  const view = shallow(<DropdownToggleCheckbox id="check" aria-label="check" />);
  expect(view).toMatchSnapshot();
});

test('with text', () => {
  const view = shallow(
    <DropdownToggleCheckbox id="check" isDisabled aria-label="check">
      Some text
    </DropdownToggleCheckbox>
  );
  expect(view).toMatchSnapshot();
});

test('isDisabled', () => {
  const view = shallow(<DropdownToggleCheckbox id="check" isDisabled aria-label="check" />);
  expect(view).toMatchSnapshot();
});

test('3rd state', () => {
  const view = shallow(<DropdownToggleCheckbox id="check" isChecked={null} aria-label="check" />);
  expect(view).toMatchSnapshot();
});

test('passing class', () => {
  const view = shallow(
    <DropdownToggleCheckbox label="label" className="class-123" id="check" isChecked aria-label="check" />
  );
  expect(view).toMatchSnapshot();
});

test('passing HTML attribute', () => {
  const view = shallow(
    <DropdownToggleCheckbox label="label" aria-labelledby="labelId" id="check" isChecked aria-label="check" />
  );
  expect(view).toMatchSnapshot();
});

test('checkbox passes value and event to onChange handler', () => {
  const newValue = true;
  const event = {
    target: { checked: newValue }
  };
  const view = mount(<DropdownToggleCheckbox id="check" {...props} aria-label="check" />);
  view.find('input').simulate('change', event);
  expect(props.onChange.mock.calls[0][0]).toBe(newValue);
  expect(props.onChange.mock.calls[0][1]).toMatchObject(event);
});
