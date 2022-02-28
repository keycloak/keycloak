/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { ModalBoxFooter } from '../../ModalBoxFooter';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ModalBoxFooter should match snapshot (auto-generated)', () => {
  const view = shallow(<ModalBoxFooter children={<>ReactNode</>} className={"''"} isLeftAligned={false} />);
  expect(view).toMatchSnapshot();
});
