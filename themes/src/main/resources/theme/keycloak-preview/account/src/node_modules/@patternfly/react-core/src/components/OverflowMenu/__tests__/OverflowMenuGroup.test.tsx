import * as React from 'react';
import { mount } from 'enzyme';
import styles from '@patternfly/react-styles/css/components/OverflowMenu/overflow-menu';
import { OverflowMenuGroup } from '../OverflowMenuGroup';
import { OverflowMenuContext } from '../OverflowMenuContext';

describe('OverflowMenuGroup', () => {
  test('isPersistent and below breakpoint should still show', () => {
    const view = mount(
      <OverflowMenuContext.Provider value={{ isBelowBreakpoint: true }}>
        <OverflowMenuGroup isPersistent />
      </OverflowMenuContext.Provider>
    );
    expect(view.find(`.${styles.overflowMenuGroup}`).length).toBe(1);
    expect(view).toMatchSnapshot();
  });

  test('Below breakpoint but not isPersistent should not show', () => {
    const view = mount(
      <OverflowMenuContext.Provider value={{ isBelowBreakpoint: true }}>
        <OverflowMenuGroup />
      </OverflowMenuContext.Provider>
    );
    expect(view.find(`.${styles.overflowMenuGroup}`).length).toBe(0);
    expect(view).toMatchSnapshot();
  });

  test('Button group', () => {
    const view = mount(
      <OverflowMenuContext.Provider value={{ isBelowBreakpoint: false }}>
        <OverflowMenuGroup groupType="button" />
      </OverflowMenuContext.Provider>
    );
    expect(view.find(`.${styles.modifiers.buttonGroup}`).length).toBe(1);
    expect(view).toMatchSnapshot();
  });

  test('Icon group', () => {
    const view = mount(
      <OverflowMenuContext.Provider value={{ isBelowBreakpoint: false }}>
        <OverflowMenuGroup groupType="icon" />
      </OverflowMenuContext.Provider>
    );
    expect(view.find(`.${styles.modifiers.iconButtonGroup}`).length).toBe(1);
    expect(view).toMatchSnapshot();
  });
});
