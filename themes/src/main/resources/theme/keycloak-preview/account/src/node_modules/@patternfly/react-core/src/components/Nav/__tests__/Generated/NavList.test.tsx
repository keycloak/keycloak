/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { NavList } from '../../NavList';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('NavList should match snapshot (auto-generated)', () => {
  const view = shallow(
    <NavList
      children={<>ReactNode</>}
      className={"''"}
      variant={'default'}
      ariaLeftScroll={"'Scroll left'"}
      ariaRightScroll={"'Scroll right'"}
    />
  );
  expect(view).toMatchSnapshot();
});
