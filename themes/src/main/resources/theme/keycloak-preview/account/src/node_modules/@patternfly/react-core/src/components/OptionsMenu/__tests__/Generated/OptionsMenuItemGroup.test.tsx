/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { OptionsMenuItemGroup } from '../../OptionsMenuItemGroup';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('OptionsMenuItemGroup should match snapshot (auto-generated)', () => {
  const view = shallow(
    <OptionsMenuItemGroup
      children={<>ReactNode</>}
      className={"''"}
      ariaLabel={"''"}
      groupTitle={''}
      hasSeparator={false}
    />
  );
  expect(view).toMatchSnapshot();
});
