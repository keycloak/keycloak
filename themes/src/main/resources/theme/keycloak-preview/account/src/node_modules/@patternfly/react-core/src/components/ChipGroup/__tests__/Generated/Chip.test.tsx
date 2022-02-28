/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { Chip } from '../../Chip';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('Chip should match snapshot (auto-generated)', () => {
  const view = shallow(
    <Chip
      children={<div>ReactNode</div>}
      closeBtnAriaLabel={"'close'"}
      className={"''"}
      isOverflowChip={false}
      isReadOnly={false}
      onClick={(_e: React.MouseEvent) => undefined as any}
      component={'div'}
      tooltipPosition={'top'}
    />
  );
  expect(view).toMatchSnapshot();
});
