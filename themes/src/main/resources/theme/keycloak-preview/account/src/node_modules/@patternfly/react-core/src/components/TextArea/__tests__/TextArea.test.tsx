import React from 'react';
import { shallow } from 'enzyme';
import { TextArea } from '../TextArea';
import { ValidatedOptions } from '../../../helpers/constants';

const props = {
  onChange: jest.fn(),
  value: 'test textarea'
};

test('textarea input passes value and event to onChange handler', () => {
  const newValue = 'new test textarea';
  const event = {
    currentTarget: { value: newValue }
  };
  const view = shallow(<TextArea {...props} aria-label="test textarea" />);
  view.find('textarea').simulate('change', event);
  expect(props.onChange).toBeCalledWith(newValue, event);
});

test('simple text input', () => {
  const view = shallow(<TextArea {...props} aria-label="simple textarea" />);
  expect(view).toMatchSnapshot();
});

test('invalid text area', () => {
  const view = shallow(<TextArea {...props} required isValid={false} aria-label="invalid textarea" />);
  expect(view).toMatchSnapshot();
});

test('validated text area success', () => {
  const view = shallow(
    <TextArea {...props} required validated={ValidatedOptions.success} aria-label="validated textarea" />
  );
  expect(view.find('.pf-c-form-control.pf-m-success').length).toBe(1);
  expect(view).toMatchSnapshot();
});

test('validated text area error', () => {
  const view = shallow(
    <TextArea {...props} required validated={ValidatedOptions.error} aria-label="validated textarea" />
  );
  expect(view).toMatchSnapshot();
});

test('vertically resizable text area', () => {
  const view = shallow(<TextArea resizeOrientation="vertical" {...props} aria-label="vertical resize textarea" />);
  expect(view).toMatchSnapshot();
});

test('horizontally resizable text area', () => {
  const view = shallow(
    <TextArea
      resizeOrientation="horizontal"
      {...props}
      required
      isValid={false}
      aria-label="horizontal resize textarea"
    />
  );
  expect(view).toMatchSnapshot();
});

test('should throw console error when no aria-label or id is given', () => {
  const myMock = jest.fn();
  global.console = { ...global.console, error: myMock };
  shallow(<TextArea {...props} />);
  expect(myMock).toBeCalled();
});

test('should not throw console error when id is given but no aria-label', () => {
  const myMock = jest.fn();
  global.console = { ...global.console, error: myMock };
  shallow(<TextArea {...props} id="5" />);
  expect(myMock).not.toBeCalled();
});

test('should not throw console error when aria-label is given but no id', () => {
  const myMock = jest.fn();
  global.console = { ...global.console, error: myMock };
  shallow(<TextArea {...props} aria-label="test textarea" />);
  expect(myMock).not.toBeCalled();
});
