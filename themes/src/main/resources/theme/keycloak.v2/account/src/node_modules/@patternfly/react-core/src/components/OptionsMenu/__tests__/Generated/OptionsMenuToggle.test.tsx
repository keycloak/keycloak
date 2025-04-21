/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { OptionsMenuToggle } from '../../OptionsMenuToggle';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('OptionsMenuToggle should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <OptionsMenuToggle
      parentId={"''"}
      onToggle={(isOpen: boolean) => undefined as void}
      isOpen={false}
      isPlain={false}
      isSplitButton={false}
      isActive={false}
      isDisabled={false}
      hideCaret={false}
      aria-label={"'Options menu'"}
      onEnter={(event: React.MouseEvent<HTMLButtonElement>) => undefined as void}
      parentRef={document.body}
      toggleTemplate={<div>ReactNode</div>}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});
