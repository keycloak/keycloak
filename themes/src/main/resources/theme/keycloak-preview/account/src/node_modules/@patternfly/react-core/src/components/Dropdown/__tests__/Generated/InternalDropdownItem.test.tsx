/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { InternalDropdownItem } from '../../InternalDropdownItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('InternalDropdownItem should match snapshot (auto-generated)', () => {
  const view = shallow(
    <InternalDropdownItem
      children={<div>ReactNode</div>}
      className={"''"}
      listItemClassName={'string'}
      component={'a'}
      variant={'item'}
      role={"'none'"}
      isDisabled={false}
      isHovered={false}
      href={"''"}
      tooltip={<div>ReactNode</div>}
      tooltipProps={undefined}
      index={-1}
      context={{
        keyHandler: () => {},
        sendRef: () => {}
      }}
      onClick={(event: React.MouseEvent<any> | React.KeyboardEvent | MouseEvent) => undefined as any}
      id={'string'}
      componentID={'string'}
      additionalChild={<div>ReactNode</div>}
      customChild={<div>ReactNode</div>}
      enterTriggersArrowDown={false}
    />
  );
  expect(view).toMatchSnapshot();
});
