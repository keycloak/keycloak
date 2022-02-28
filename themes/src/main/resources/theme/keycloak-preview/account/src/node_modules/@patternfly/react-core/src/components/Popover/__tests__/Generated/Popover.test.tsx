/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { Popover } from '../../Popover';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('Popover should match snapshot (auto-generated)', () => {
  const view = shallow(
    <Popover
      aria-label={"''"}
      appendTo={() => document.body}
      bodyContent={<div>ReactNode</div>}
      boundary={'window'}
      children={<p>ReactElement</p>}
      className={"''"}
      closeBtnAriaLabel={"'Close'"}
      distance={25}
      enableFlip={true}
      flipBehavior={['top', 'right', 'bottom', 'left', 'top', 'right', 'bottom']}
      footerContent={null}
      headerContent={null}
      hideOnOutsideClick={true}
      isVisible={null}
      minWidth={'string'}
      maxWidth={'string'}
      onHidden={(): void => null}
      onHide={(): void => null}
      onMount={(): void => null}
      onShow={(): void => null}
      onShown={(): void => null}
      position={'top'}
      shouldClose={(): void => null}
      zIndex={9999}
      tippyProps={undefined}
    />
  );
  expect(view).toMatchSnapshot();
});
