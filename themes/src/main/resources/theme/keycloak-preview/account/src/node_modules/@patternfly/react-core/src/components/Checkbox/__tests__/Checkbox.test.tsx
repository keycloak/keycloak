import * as React from 'react';
import { shallow } from 'enzyme';
import { Checkbox } from '../Checkbox';

const props = {
  onChange: jest.fn(),
  isChecked: false
};

test('controlled', () => {
  const view = shallow(<Checkbox isChecked id="check" aria-label="check" />);
  expect(view).toMatchSnapshot();
});

test('controlled - 3rd state', () => {
  const view = shallow(<Checkbox isChecked={null} id="check" aria-label="check" />);
  expect(view).toMatchSnapshot();
});

test('uncontrolled', () => {
  const view = shallow(<Checkbox id="check" aria-label="check" />);
  expect(view).toMatchSnapshot();
});

test('isDisabled', () => {
  const view = shallow(<Checkbox id="check" isDisabled aria-label="check" />);
  expect(view).toMatchSnapshot();
});

test('label is string', () => {
  const view = shallow(<Checkbox label="Label" id="check" isChecked aria-label="check" />);
  expect(view).toMatchSnapshot();
});

test('label is function', () => {
  const functionLabel = () => <h1>Header</h1>;
  const view = shallow(<Checkbox label={functionLabel()} id="check" isChecked aria-label="check" />);
  expect(view).toMatchSnapshot();
});

test('label is node', () => {
  const view = shallow(<Checkbox label={<h1>Header</h1>} id="check" isChecked aria-label="check" />);
  expect(view).toMatchSnapshot();
});

test('passing class', () => {
  const view = shallow(<Checkbox label="label" className="class-123" id="check" isChecked aria-label="check" />);
  expect(view).toMatchSnapshot();
});

test('passing HTML attribute', () => {
  const view = shallow(<Checkbox label="label" aria-labelledby="labelId" id="check" isChecked aria-label="check" />);
  expect(view).toMatchSnapshot();
});

test('passing description', () => {
  const view = shallow(<Checkbox id="check" label="checkbox" description="Text description..." />);
  const descriptionEl = view.find('div[className="pf-c-check__description"]');
  expect(descriptionEl.length).toBe(1);
  expect(descriptionEl.text()).toBe('Text description...');
});

test('checkbox passes value and event to onChange handler', () => {
  const newValue = true;
  const event = {
    currentTarget: { checked: newValue }
  };
  const view = shallow(<Checkbox id="check" {...props} aria-label="check" />);
  view.find('input').simulate('change', event);
  expect(props.onChange).toBeCalledWith(newValue, event);
});
