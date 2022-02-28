/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { DataListContent } from '../../DataListContent';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DataListContent should match snapshot (auto-generated)', () => {
  const view = shallow(
    <DataListContent
      children={<>ReactNode</>}
      className={"''"}
      id={"''"}
      rowid={"''"}
      isHidden={false}
      noPadding={false}
      aria-label={'string'}
    />
  );
  expect(view).toMatchSnapshot();
});
