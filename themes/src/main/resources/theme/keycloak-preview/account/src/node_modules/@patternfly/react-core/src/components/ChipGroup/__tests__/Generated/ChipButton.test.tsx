/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { ChipButton } from '../../ChipButton';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ChipButton should match snapshot (auto-generated)', () => {
  const view = shallow(
    <ChipButton ariaLabel={"'close'"} children={<>ReactNode</>} className={"''"} onClick={() => undefined} />
  );
  expect(view).toMatchSnapshot();
});
