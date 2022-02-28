/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { Modal } from '../../Modal';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('Modal should match snapshot (auto-generated)', () => {
  const view = shallow(
    <Modal
      children={<div>ReactNode</div>}
      className={"''"}
      isOpen={false}
      header={<div>ReactNode</div>}
      title={'string'}
      hideTitle={false}
      showClose={true}
      ariaDescribedById={"''"}
      footer={<div>ReactNode</div>}
      actions={[]}
      isFooterLeftAligned={false}
      onClose={() => undefined as any}
      width={1}
      isLarge={false}
      isSmall={false}
      appendTo={(typeof document !== 'undefined' && document.body) || null}
      disableFocusTrap={true}
    />
  );
  expect(view).toMatchSnapshot();
});
