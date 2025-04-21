import * as React from 'react';

import { render, screen } from '@testing-library/react';

import styles from '@patternfly/react-styles/css/components/OverflowMenu/overflow-menu';
import { OverflowMenuItem } from '../OverflowMenuItem';
import { OverflowMenuContext } from '../OverflowMenuContext';

describe('OverflowMenuItem', () => {
  test('isPersistent and below breakpoint should still show', () => {
    render(
      <OverflowMenuContext.Provider value={{ isBelowBreakpoint: false }}>
        <OverflowMenuItem isPersistent>Some item value</OverflowMenuItem>
      </OverflowMenuContext.Provider>
    );
    expect(screen.getByText('Some item value')).toHaveClass(styles.overflowMenuItem);
  });

  test('Below breakpoint and not isPersistent should not show', () => {
    const { asFragment } = render(
      <OverflowMenuContext.Provider value={{ isBelowBreakpoint: false }}>
        <OverflowMenuItem>Some item value</OverflowMenuItem>
      </OverflowMenuContext.Provider>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
