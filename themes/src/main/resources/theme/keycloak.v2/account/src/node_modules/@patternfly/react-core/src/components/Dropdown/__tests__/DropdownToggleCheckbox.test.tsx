import React from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { DropdownToggleCheckbox } from '../DropdownToggleCheckbox';

const props = {
  onChange: jest.fn(),
  isChecked: false
};

describe('DropdownToggleCheckbox', () => {
  test('controlled', () => {
    const { asFragment } = render(<DropdownToggleCheckbox isChecked id="check" aria-label="check" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('uncontrolled', () => {
    const { asFragment } = render(<DropdownToggleCheckbox id="check" aria-label="check" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('with text', () => {
    const { asFragment } = render(
      <DropdownToggleCheckbox id="check" isDisabled aria-label="check">
        Some text
      </DropdownToggleCheckbox>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('isDisabled', () => {
    const { asFragment } = render(<DropdownToggleCheckbox id="check" isDisabled aria-label="check" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('3rd state', () => {
    const { asFragment } = render(<DropdownToggleCheckbox id="check" isChecked={null} aria-label="check" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('passing class', () => {
    const { asFragment } = render(
      <DropdownToggleCheckbox label="label" className="class-123" id="check" isChecked aria-label="check" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('passing HTML attribute', () => {
    const { asFragment } = render(
      <DropdownToggleCheckbox label="label" aria-labelledby="labelId" id="check" isChecked aria-label="check" />
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('checkbox passes value and event to onChange handler', () => {
    render(<DropdownToggleCheckbox id="check" {...props} aria-label="check" />);

    userEvent.click(screen.getByRole('checkbox'));
    expect(props.onChange).toHaveBeenCalledWith(true, expect.any(Object));
  });
});
