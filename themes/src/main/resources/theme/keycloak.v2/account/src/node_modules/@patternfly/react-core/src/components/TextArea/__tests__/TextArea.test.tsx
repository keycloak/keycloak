import React from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { TextArea, TextAreaBase } from '../TextArea';
import { ValidatedOptions } from '../../../helpers/constants';

const props = {
  onChange: jest.fn(),
  value: 'test textarea'
};

describe('TextArea', () => {
  test('textarea input passes value and event to onChange handler', () => {
    render(<TextAreaBase {...props} value="" aria-label="test textarea" />);

    userEvent.type(screen.getByLabelText('test textarea'), 'a');
    expect(props.onChange).toHaveBeenCalledWith('a', expect.any(Object));
  });

  test('simple text input', () => {
    const { asFragment } = render(<TextArea {...props} aria-label="simple textarea" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('disabled text input using isDisabled', () => {
    const { asFragment } = render(<TextArea {...props} aria-label="is disabled textarea" isDisabled />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('disabled text input using disabled', () => {
    const { asFragment } = render(<TextArea {...props} aria-label="disabled textarea" disabled />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('read only text input using isReadOnly', () => {
    const { asFragment } = render(<TextArea {...props} aria-label="is read only textarea" isReadOnly />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('read only text input using readOnly', () => {
    const { asFragment } = render(<TextArea {...props} aria-label="read only textarea" readOnly />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('validated text area success', () => {
    render(<TextArea {...props} required validated={ValidatedOptions.success} aria-label="validated textarea" />);
    expect(screen.getByLabelText('validated textarea')).toHaveClass('pf-m-success');
  });

  test('validated text area warning', () => {
    render(<TextArea {...props} required validated={ValidatedOptions.warning} aria-label="validated textarea" />);
    expect(screen.getByLabelText('validated textarea')).toHaveClass('pf-m-warning');
  });

  test('validated text area error', () => {
    const { asFragment } = render(
      <TextArea {...props} required validated={ValidatedOptions.error} aria-label="validated textarea" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('vertically resizable text area', () => {
    const { asFragment } = render(
      <TextArea resizeOrientation="vertical" {...props} aria-label="vertical resize textarea" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('horizontally resizable text area', () => {
    const { asFragment } = render(
      <TextArea
        resizeOrientation="horizontal"
        {...props}
        required
        validated={'error'}
        aria-label="horizontal resize textarea"
      />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('should throw console error when no aria-label or id is given', () => {
    const myMock = jest.fn();
    global.console = { ...global.console, error: myMock };

    render(<TextArea {...props} />);

    expect(myMock).toHaveBeenCalled();
  });

  test('should not throw console error when id is given but no aria-label', () => {
    const myMock = jest.fn();
    global.console = { ...global.console, error: myMock };

    render(<TextArea {...props} id="5" />);

    expect(myMock).not.toHaveBeenCalled();
  });

  test('should not throw console error when aria-label is given but no id', () => {
    const myMock = jest.fn();
    global.console = { ...global.console, error: myMock };

    render(<TextArea {...props} aria-label="test textarea" />);

    expect(myMock).not.toHaveBeenCalled();
  });
});
