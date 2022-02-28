/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { TabButton } from '../../TabButton';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('TabButton should match snapshot (auto-generated)', () => {
  const view = shallow(
    <TabButton
      children={<div>ReactNode</div>}
      className={"''"}
      href={'string'}
      tabContentRef={{ current: document.createElement('div') }}
    />
  );
  expect(view).toMatchSnapshot();
});
