import React from 'react';

import { render, screen } from '@testing-library/react';

import styles from '@patternfly/react-styles/css/components/OverflowMenu/overflow-menu';
import { OverflowMenuGroup } from '../OverflowMenuGroup';
import { OverflowMenuContext } from '../OverflowMenuContext';

describe('OverflowMenuGroup', () => {
  test('isPersistent and below breakpoint should still show', () => {
    render(
      <OverflowMenuContext.Provider value={{ isBelowBreakpoint: true }}>
        <OverflowMenuGroup isPersistent data-testid="test-id" />
      </OverflowMenuContext.Provider>
    );
    expect(screen.getByTestId('test-id')).toHaveClass(styles.overflowMenuGroup);
  });

  test('Below breakpoint but not isPersistent should not show', () => {
    render(
      <OverflowMenuContext.Provider value={{ isBelowBreakpoint: true }}>
        <OverflowMenuGroup data-testid="test-id" />
      </OverflowMenuContext.Provider>
    );
    expect(screen.queryByTestId('test-id')).toBeNull();
  });

  test('Button group', () => {
    render(
      <OverflowMenuContext.Provider value={{ isBelowBreakpoint: false }}>
        <OverflowMenuGroup groupType="button" data-testid="test-id" />
      </OverflowMenuContext.Provider>
    );
    expect(screen.getByTestId('test-id')).toHaveClass(styles.modifiers.buttonGroup);
  });

  test('Icon group', () => {
    render(
      <OverflowMenuContext.Provider value={{ isBelowBreakpoint: false }}>
        <OverflowMenuGroup groupType="icon" data-testid="test-id" />
      </OverflowMenuContext.Provider>
    );
    expect(screen.getByTestId('test-id')).toHaveClass(styles.modifiers.iconButtonGroup);
  });
});
