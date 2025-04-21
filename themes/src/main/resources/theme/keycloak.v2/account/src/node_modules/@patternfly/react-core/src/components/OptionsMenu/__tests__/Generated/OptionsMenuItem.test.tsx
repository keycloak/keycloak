import React from 'react';

import { render } from '@testing-library/react';

import { OptionsMenuItem } from '../../OptionsMenuItem';
import { DropdownArrowContext } from '../../../Dropdown';

describe('OptionsMenuItem', () => {
  it('should match snapshot', () => {
    const { asFragment } = render(
      <DropdownArrowContext.Provider value={{ sendRef: jest.fn(), keyHandler: undefined }}>
        <OptionsMenuItem
          children={<>ReactNode</>}
          className={'string'}
          isSelected={false}
          isDisabled={true}
          onSelect={() => null as any}
          id={"''"}
        />{' '}
      </DropdownArrowContext.Provider>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
