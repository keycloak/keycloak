import React from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { Checkbox } from '../Checkbox';

describe('Checkbox', () => {
  test('controlled', () => {
    const { asFragment } = render(<Checkbox isChecked id="check" aria-label="check" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('controlled - 3rd state', () => {
    const { asFragment } = render(<Checkbox isChecked={null} id="check" aria-label="check" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('uncontrolled', () => {
    const { asFragment } = render(<Checkbox id="check" aria-label="check" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('isDisabled', () => {
    const { asFragment } = render(<Checkbox id="check" isDisabled aria-label="check" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('label is string', () => {
    const { asFragment } = render(<Checkbox label="Label" id="check" isChecked aria-label="check" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('label is function', () => {
    const functionLabel = () => <h1>Header</h1>;
    const { asFragment } = render(<Checkbox label={functionLabel()} id="check" isChecked aria-label="check" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('label is node', () => {
    const { asFragment } = render(<Checkbox label={<h1>Header</h1>} id="check" isChecked aria-label="check" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('passing class', () => {
    const { asFragment } = render(
      <Checkbox label="label" className="class-123" id="check" isChecked aria-label="check" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('passing HTML attribute', () => {
    const { asFragment } = render(
      <Checkbox label="label" aria-labelledby="labelId" id="check" isChecked aria-label="check" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('passing description', () => {
    render(<Checkbox id="check" label="checkbox" description="Text description..." />);
    expect(screen.getByText('Text description...')).toBeInTheDocument();
  });

  test('passing body', () => {
    render(<Checkbox id="check" label="checkbox" body="This is where custom content goes." />);

    expect(screen.getByText('This is where custom content goes.')).toBeInTheDocument();
  });

  test('checkbox onChange handler called when component is clicked', () => {
    const onChangeHandler = jest.fn();
    render(<Checkbox id="check" onChange={onChangeHandler} aria-label="check" isChecked={false} />);

    userEvent.click(screen.getByLabelText('check'));
    expect(onChangeHandler).toHaveBeenCalled();
  });

  test('should throw console error when no id is given', () => {
    const myMock = jest.fn();
    global.console = { ...global.console, error: myMock };

    render(<Checkbox id={undefined} />);
    expect(myMock).toHaveBeenCalled();
  });

  test('renders component wrapper as span', () => {
    const { container } = render(
      <Checkbox component="span" label="label" aria-labelledby="labelId" id="check" isChecked aria-label="check" />
    );
    const span = container.querySelector('span');
    expect(span).toHaveClass('pf-c-check');
   
  });
});
