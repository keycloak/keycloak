/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { DataListCheck } from '../../DataListCheck';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DataListCheck should match snapshot (auto-generated)', () => {
  const view = shallow(
    <DataListCheck
      className={"''"}
      isValid={true}
      isDisabled={false}
      isChecked={null}
      checked={null}
      onChange={(checked: boolean, event: React.FormEvent<HTMLInputElement>) => {}}
      aria-labelledby={'string'}
    />
  );
  expect(view).toMatchSnapshot();
});
