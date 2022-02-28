/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { ToggleTemplate } from '../../ToggleTemplate';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ToggleTemplate should match snapshot (auto-generated)', () => {
  const view = shallow(<ToggleTemplate firstIndex={0} lastIndex={0} itemCount={0} itemsTitle={"'items'"} />);
  expect(view).toMatchSnapshot();
});
