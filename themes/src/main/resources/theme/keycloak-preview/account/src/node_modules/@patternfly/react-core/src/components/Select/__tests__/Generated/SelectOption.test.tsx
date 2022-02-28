/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { SelectOption } from '../../SelectOption';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('SelectOption should match snapshot (auto-generated)', () => {
  const view = shallow(
    <SelectOption
      children={<div>ReactNode</div>}
      className={"''"}
      index={0}
      component={'button'}
      value={''}
      isDisabled={false}
      isPlaceholder={false}
      isSelected={false}
      isChecked={false}
      isFocused={false}
      sendRef={() => {}}
      keyHandler={() => {}}
      onClick={() => {}}
    />
  );
  expect(view).toMatchSnapshot();
});
