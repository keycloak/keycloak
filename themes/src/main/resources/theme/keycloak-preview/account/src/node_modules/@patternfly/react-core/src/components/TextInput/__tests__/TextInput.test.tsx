import React from 'react';
import { mount, shallow } from 'enzyme';
import { TextInput, TextInputBase } from '../TextInput';
import { ValidatedOptions } from '../../../helpers/constants';

const props = {
  onChange: jest.fn(),
  value: 'test input'
};

test('input passes value and event to onChange handler', () => {
  const newValue = 'new test input';
  const event = {
    currentTarget: { value: newValue }
  };
  const view = shallow(<TextInputBase {...props} aria-label="test input" />);
  view.find('input').simulate('change', event);
  expect(props.onChange).toBeCalledWith(newValue, event);
});

test('simple text input', () => {
  const view = mount(<TextInput {...props} aria-label="simple text input" />);
  expect(view.find('input')).toMatchSnapshot();
});

test('disabled text input', () => {
  const view = mount(<TextInput isDisabled aria-label="disabled text input" />);
  expect(view.find('input')).toMatchSnapshot();
});

test('readonly text input', () => {
  const view = mount(<TextInput isReadOnly value="read only" aria-label="readonly text input" />);
  expect(view.find('input')).toMatchSnapshot();
});

test('invalid text input', () => {
  const view = mount(<TextInput {...props} required isValid={false} aria-label="invalid text input" />);
  expect(view.find('input')).toMatchSnapshot();
});

test('validated text input success', () => {
  const view = mount(
    <TextInput {...props} required validated={ValidatedOptions.success} aria-label="validated text input" />
  );
  expect(view.find('.pf-c-form-control.pf-m-success').length).toBe(1);
  expect(view).toMatchSnapshot();
});

test('validated text input', () => {
  const view = shallow(
    <TextInput {...props} required validated={ValidatedOptions.error} aria-label="validated text input" />
  );
  expect(view).toMatchSnapshot();
});

test('should throw console error when no aria-label, id or aria-labelledby is given', () => {
  const myMock = jest.fn();
  global.console = { ...global.console, error: myMock };
  mount(<TextInput {...props} />);
  expect(myMock).toBeCalled();
});

test('should not throw console error when id is given but no aria-label or aria-labelledby', () => {
  const myMock = jest.fn();
  global.console = { ...global.console, error: myMock };
  mount(<TextInput {...props} id="5" />);
  expect(myMock).not.toBeCalled();
});

test('should not throw console error when aria-label is given but no id or aria-labelledby', () => {
  const myMock = jest.fn();
  global.console = { ...global.console, error: myMock };
  mount(<TextInput {...props} aria-label="test input" />);
  expect(myMock).not.toBeCalled();
});

test('should not throw console error when aria-labelledby is given but no id or aria-label', () => {
  const myMock = jest.fn();
  global.console = { ...global.console, error: myMock };
  mount(<TextInput {...props} aria-labelledby="test input" />);
  expect(myMock).not.toBeCalled();
});
