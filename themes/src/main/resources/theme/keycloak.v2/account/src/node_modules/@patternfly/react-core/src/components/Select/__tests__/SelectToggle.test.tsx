import React from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { SelectToggle } from '../SelectToggle';

describe('SelectToggle', () => {
  describe('API', () => {
    test('click on closed', () => {
      const mockToggle = jest.fn();
      render(
        <SelectToggle id="Select Toggle" onToggle={mockToggle} parentRef={{ current: document.createElement('div') }}>
          Select
        </SelectToggle>
      );

      userEvent.click(screen.getByRole('button'));
      expect(mockToggle).toHaveBeenCalledWith(true, expect.any(Object));
    });

    test('click on opened', () => {
      const mockToggle = jest.fn();
      render(
        <SelectToggle
          id="Select Toggle"
          onToggle={mockToggle}
          isOpen
          parentRef={{ current: document.createElement('div') }}
        >
          Select
        </SelectToggle>
      );

      userEvent.click(screen.getByRole('button'));
      expect(mockToggle).toHaveBeenCalledWith(false, expect.any(MouseEvent));
    });

    test('click on document', () => {
      const mockToggle = jest.fn();

      render(
        <SelectToggle
          id="Select Toggle"
          onToggle={mockToggle}
          isOpen
          parentRef={{ current: document.createElement('div') }}
        >
          Select
        </SelectToggle>
      );

      userEvent.click(screen.getByText('Select').parentElement);
      expect(mockToggle).toHaveBeenCalledWith(false, expect.any(MouseEvent));
    });

    test('on click outside has been removed', () => {
      const mockToggle = jest.fn();

      render(
        <SelectToggle
          id="Select Toggle"
          onToggle={mockToggle}
          isOpen={false}
          parentRef={{ current: document.createElement('div') }}
        >
          Select
        </SelectToggle>
      );

      userEvent.click(screen.getByText('Select').parentElement);
      expect(mockToggle).not.toHaveBeenCalled();
    });
  });

  describe('state', () => {
    test('active', () => {
      const { asFragment } = render(
        <SelectToggle id="Select Toggle" isActive parentRef={{ current: document.createElement('div') }}>
          Select
        </SelectToggle>
      );
      expect(asFragment()).toMatchSnapshot();
    });
  });
});
