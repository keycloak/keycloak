/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { OptionsMenu } from '../../OptionsMenu';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('OptionsMenu should match snapshot (auto-generated)', () => {
  const view = shallow(
    <OptionsMenu
      className={"''"}
      id={'string'}
      menuItems={[]}
      toggle={<p>ReactElement</p>}
      isPlain={true}
      isOpen={true}
      isText={false}
      isGrouped={false}
      ariaLabelMenu={'string'}
      position={'right'}
      direction={'up'}
    />
  );
  expect(view).toMatchSnapshot();
});
