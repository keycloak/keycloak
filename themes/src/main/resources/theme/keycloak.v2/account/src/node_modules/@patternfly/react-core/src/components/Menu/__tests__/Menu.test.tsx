import React from 'react';

import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import userEvent from '@testing-library/user-event';

import { Menu } from '../Menu';
import { MenuItem } from '../MenuItem';
import { MenuList } from '../MenuList';
import { MenuContent } from '../MenuContent';

describe('Menu', () => {
  test('should render Menu successfully', () => {
    const { asFragment } = render(
      <Menu activeItemId={0} onSelect={jest.fn()}>
        content
      </Menu>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  describe('with isPlain', () => {
    test('should render Menu with plain styles applied', () => {
      render(
        <Menu activeItemId={0} onSelect={jest.fn()} isPlain>
          content
        </Menu>
      );
      expect(screen.getByText('content')).toHaveClass('pf-m-plain');
    });
  });

  describe('with isScrollable', () => {
    test('should render Menu with scrollable styles applied', () => {
      render(
        <Menu activeItemId={0} onSelect={jest.fn()} isScrollable>
          content
        </Menu>
      );
      expect(screen.getByText('content')).toHaveClass('pf-m-scrollable');
    });
  });

  describe('with isNavFlyout', () => {
    test('should render Menu with nav flyout styles applied', () => {
      render(
        <Menu activeItemId={0} onSelect={jest.fn()} isNavFlyout>
          content
        </Menu>
      );
      expect(screen.getByText('content')).toHaveClass('pf-m-nav');
    });
  });

  describe('with hasCheck', () => {
    test('should render Menu with checkbox items', () => {
      const { asFragment } = render(
        <Menu>
          <MenuContent>
            <MenuList>
              <MenuItem hasCheck itemId={0}>
                Checkbox 1
              </MenuItem>
            </MenuList>
          </MenuContent>
        </Menu>
      );
      const checkbox1 = screen.getAllByRole('checkbox')[0];
      expect(checkbox1).not.toBeChecked();
      expect(screen.getByText("Checkbox 1")).toBeInTheDocument();
    });
  });
});
