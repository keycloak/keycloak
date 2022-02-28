/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { DropdownToggleAction } from '../../DropdownToggleAction';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DropdownToggleAction should match snapshot (auto-generated)', () => {
  const view = shallow(
    <DropdownToggleAction
      className={"''"}
      isDisabled={false}
      onClick={() => {}}
      children={<div>ReactNode</div>}
      id={'string'}
      aria-label={'string'}
    />
  );
  expect(view).toMatchSnapshot();
});
