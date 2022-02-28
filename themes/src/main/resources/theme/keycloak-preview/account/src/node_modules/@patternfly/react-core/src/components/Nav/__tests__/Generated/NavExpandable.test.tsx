/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { NavExpandable } from '../../NavExpandable';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('NavExpandable should match snapshot (auto-generated)', () => {
  const view = shallow(
    <NavExpandable
      title={'string'}
      srText={"''"}
      isExpanded={false}
      children={''}
      className={"''"}
      groupId={null}
      isActive={false}
      id={"''"}
      onExpand={(e: React.MouseEvent<HTMLLIElement, MouseEvent>, val: boolean) => undefined as void}
    />
  );
  expect(view).toMatchSnapshot();
});
