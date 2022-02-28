/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { DataListItem } from '../../DataListItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DataListItem should match snapshot (auto-generated)', () => {
  const view = shallow(
    <DataListItem
      isExpanded={false}
      children={<div>ReactNode</div>}
      className={"''"}
      aria-labelledby={'string'}
      id={"''"}
    />
  );
  expect(view).toMatchSnapshot();
});
