/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { DropdownMenu } from '../../DropdownMenu';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DropdownMenu should match snapshot (auto-generated)', () => {
  const view = shallow(
    <DropdownMenu
      children={<div>ReactNode</div>}
      className={"''"}
      isOpen={true}
      openedOnEnter={false}
      autoFocus={true}
      component={'ul'}
      position={'right'}
      isGrouped={false}
    />
  );
  expect(view).toMatchSnapshot();
});
