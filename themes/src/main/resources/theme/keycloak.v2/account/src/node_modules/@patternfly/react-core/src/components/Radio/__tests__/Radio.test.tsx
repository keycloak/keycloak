import React from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { Radio } from '../Radio';

const props = {
  onChange: jest.fn()
};

describe('Radio', () => {
  test('controlled', () => {
    const { asFragment } = render(<Radio isChecked id="check" aria-label="check" name="check" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('uncontrolled', () => {
    const { asFragment } = render(<Radio id="check" aria-label="check" name="check" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('isDisabled', () => {
    const { asFragment } = render(<Radio id="check" isDisabled aria-label="check" name="check" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('isLabelBeforeButton', () => {
    const { asFragment } = render(
      <Radio id="check" isLabelBeforeButton label="Radio label" aria-label="check" name="check" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('isLabelWrapped', () => {
    const { asFragment } = render(
      <Radio id="check" isLabelWrapped label="Radio label" aria-label="check" name="check" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('label is string', () => {
    const { asFragment } = render(<Radio label="Label" id="check" isChecked aria-label="check" name="check" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('label is function', () => {
    const functionLabel = () => <h1>Header</h1>;
    const { asFragment } = render(
      <Radio label={functionLabel()} id="check" isChecked aria-label="check" name="check" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('label is node', () => {
    const { asFragment } = render(
      <Radio label={<h1>Header</h1>} id="check" isChecked aria-label="check" name="check" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('passing class', () => {
    const { asFragment } = render(
      <Radio label="label" className="class-123" id="check" isChecked aria-label="check" name="check" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('passing HTML attribute', () => {
    const { asFragment } = render(
      <Radio label="label" aria-labelledby="labelId" id="check" isChecked aria-label="check" name="check" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('Radio passes value and event to onChange handler', () => {
    render(<Radio id="check" {...props} aria-label="check" name="check" />);

    userEvent.click(screen.getByRole('radio'));
    expect(props.onChange).toHaveBeenCalledWith(true, expect.any(Object));
  });

  test('Radio description', () => {
    render(<Radio id="check" name="check" aria-label="check" description="Text description..." />);
    expect(screen.getByText('Text description...')).toBeInTheDocument();
  });

  test('Radio body', () => {
    render(<Radio id="check" name="check" aria-label="check" body="Text body..." />);
    expect(screen.getByText('Text body...')).toBeInTheDocument();
  });

  test('should throw console error when no id is given', () => {
    const myMock = jest.fn();
    global.console = { ...global.console, error: myMock };

    render(<Radio id={undefined} name="check" aria-label="check" description="Text description..." />);

    expect(myMock).toHaveBeenCalled();
  });
});
