/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { ModalContent } from '../../ModalContent';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ModalContent should match snapshot (auto-generated)', () => {
  const view = shallow(
    <ModalContent
      children={<div>ReactNode</div>}
      className={"''"}
      isLarge={false}
      isSmall={false}
      isOpen={false}
      header={null}
      title={'string'}
      hideTitle={false}
      showClose={true}
      width={-1}
      footer={null}
      actions={[]}
      isFooterLeftAligned={false}
      onClose={() => undefined as any}
      ariaDescribedById={"''"}
      id={"''"}
      disableFocusTrap={false}
    />
  );
  expect(view).toMatchSnapshot();
});
