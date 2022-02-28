import * as React from 'react';
import { mount } from 'enzyme';
import styles from '@patternfly/react-styles/css/components/OverflowMenu/overflow-menu';
import { OverflowMenuControl } from '../OverflowMenuControl';
import { OverflowMenuContext } from '../OverflowMenuContext';

describe('OverflowMenuControl', () => {
  test('basic', () => {
    const view = mount(
      <OverflowMenuContext.Provider value={{ isBelowBreakpoint: true }}>
        <OverflowMenuControl />
      </OverflowMenuContext.Provider>
    );
    expect(view.find(`.${styles.overflowMenuControl}`).length).toBe(1);
    expect(view).toMatchSnapshot();
  });

  test('Additional Options', () => {
    const view = mount(<OverflowMenuControl hasAdditionalOptions />);
    expect(view).toMatchSnapshot();
  });
});
