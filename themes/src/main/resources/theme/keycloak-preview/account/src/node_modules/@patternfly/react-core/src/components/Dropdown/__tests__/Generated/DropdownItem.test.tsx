/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { DropdownItem } from '../../DropdownItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DropdownItem should match snapshot (auto-generated)', () => {
  const view = shallow(
    <DropdownItem
      children={<>ReactNode</>}
      className={"''"}
      listItemClassName={'string'}
      component={'a'}
      variant={'item'}
      isDisabled={false}
      isHovered={false}
      href={"''"}
      tooltip={null}
      tooltipProps={undefined}
      additionalChild={<div>ReactNode</div>}
      customChild={<div>ReactNode</div>}
    />
  );
  expect(view).toMatchSnapshot();
});
