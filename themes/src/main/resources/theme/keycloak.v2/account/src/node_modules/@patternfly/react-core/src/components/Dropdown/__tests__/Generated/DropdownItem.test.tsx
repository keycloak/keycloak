import React from 'react';

import { render } from '@testing-library/react';

import { DropdownItem } from '../../DropdownItem';
import { DropdownArrowContext } from '../../dropdownConstants';

describe('DropdownItem', () => {
  it('should match snapshot', () => {
    const { asFragment } = render(
      <DropdownArrowContext.Provider value={{ sendRef: jest.fn(), keyHandler: undefined }}>
        <DropdownItem
          children={<>ReactNode</>}
          className={"''"}
          listItemClassName={'string'}
          component={'a'}
          isDisabled={false}
          isPlainText={false}
          isHovered={false}
          href={'string'}
          tooltip={null}
          tooltipProps={undefined}
          additionalChild={<div>ReactNode</div>}
          customChild={<div>ReactNode</div>}
          icon={null}
        />
      </DropdownArrowContext.Provider>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
