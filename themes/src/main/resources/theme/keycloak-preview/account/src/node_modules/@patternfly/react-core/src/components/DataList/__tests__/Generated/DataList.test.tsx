/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { DataList } from '../../DataList';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DataList should match snapshot (auto-generated)', () => {
  const view = shallow(
    <DataList
      children={<>ReactNode</>}
      className={"''"}
      aria-label={'string'}
      onSelectDataListItem={(id: string) => undefined as void}
      selectedDataListItemId={"''"}
    />
  );
  expect(view).toMatchSnapshot();
});
