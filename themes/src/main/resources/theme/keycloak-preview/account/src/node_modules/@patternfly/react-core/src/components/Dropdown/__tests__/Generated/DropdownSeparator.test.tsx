/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { DropdownSeparator } from '../../DropdownSeparator';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DropdownSeparator should match snapshot (auto-generated)', () => {
  const view = shallow(
    <DropdownSeparator
      className={"''"}
      onClick={(event: React.MouseEvent<HTMLAnchorElement> | React.KeyboardEvent | MouseEvent) => undefined as void}
    />
  );
  expect(view).toMatchSnapshot();
});
