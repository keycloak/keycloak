/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { SelectMenu } from '../../SelectMenu';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('SelectMenu should match snapshot (auto-generated)', () => {
  const view = shallow(
    <SelectMenu
      children={[undefined as React.ReactElement]}
      isCustomContent={false}
      className={"''"}
      isExpanded={false}
      isGrouped={false}
      selected={''}
      checked={[]}
      openedOnEnter={false}
      maxHeight={''}
      noResultsFoundText={'string'}
      createText={'string'}
      sendRef={() => {}}
      keyHandler={() => {}}
    />
  );
  expect(view).toMatchSnapshot();
});
