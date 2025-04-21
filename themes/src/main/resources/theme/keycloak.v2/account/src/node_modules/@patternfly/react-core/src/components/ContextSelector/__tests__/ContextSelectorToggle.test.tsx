import React from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { ContextSelectorToggle } from '../ContextSelectorToggle';

describe('ContextSelectorToggle', () => {
  test('Renders ContextSelectorToggle', () => {
    const { asFragment } = render(<ContextSelectorToggle id="toggle-id" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('Verify onToggle is called ', () => {
    const mockfnOnToggle = jest.fn();

    render(<ContextSelectorToggle onToggle={mockfnOnToggle} id="toggle-id" />);

    userEvent.click(screen.getByRole('button'));
    expect(mockfnOnToggle).toHaveBeenCalledTimes(1);
  });

  test('Verify ESC press', () => {
    const { asFragment } = render(<ContextSelectorToggle isOpen id="toggle-id" />);

    userEvent.type(screen.getByRole('button'), '{esc}');
    expect(asFragment()).toMatchSnapshot();
  });

  test('Verify ESC press with not isOpen', () => {
    const { asFragment } = render(<ContextSelectorToggle onToggle={jest.fn()} id="toggle-id" />);

    userEvent.type(screen.getByRole('button'), '{esc}');
    expect(asFragment()).toMatchSnapshot();
  });

  test('Verify keydown tab ', () => {
    const { asFragment } = render(<ContextSelectorToggle isOpen id="toggle-id" />);

    userEvent.type(screen.getByRole('button'), '{tab}');
    expect(asFragment()).toMatchSnapshot();
  });

  test('Verify keydown enter ', () => {
    const { asFragment } = render(<ContextSelectorToggle onToggle={jest.fn()} onEnter={jest.fn()} id="toggle-id" />);

    userEvent.type(screen.getByRole('button'), '{enter}');
    expect(asFragment()).toMatchSnapshot();
  });
});
