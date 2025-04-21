/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { ClipboardCopy } from '../../ClipboardCopy';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ClipboardCopy should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <ClipboardCopy
      className={'string'}
      hoverTip={"'Copy to clipboard'"}
      clickTip={"'Successfully copied to clipboard!'"}
      textAriaLabel={"'Copyable input'"}
      toggleAriaLabel={"'Show content'"}
      isReadOnly={false}
      isExpanded={false}
      isCode={false}
      variant={'inline'}
      position={'auto'}
      maxWidth={"'150px'"}
      exitDelay={1600}
      entryDelay={100}
      switchDelay={2000}
      onCopy={(event: React.ClipboardEvent<HTMLDivElement>, text?: React.ReactNode) => {
        const clipboard = event.currentTarget.parentElement;
        const el = document.createElement('input');
        el.value = text.toString();
        clipboard.appendChild(el);
        el.select();
        document.execCommand('copy');
        clipboard.removeChild(el);
      }}
      onChange={(): any => undefined}
      children={<div>ReactNode</div>}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});
