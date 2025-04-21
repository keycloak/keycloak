import React from 'react';
import { render } from '@testing-library/react';
import { ToolbarToggleGroup } from '../ToolbarToggleGroup';
import { ToolbarContentContext } from '../ToolbarUtils';

describe('ToolbarToggleGroup', () => {
  it('should warn on bad props', () => {
    const myMock = jest.fn() as any;
    global.console = { error: myMock } as any;

    render(
      <ToolbarContentContext.Provider
        value={{
          expandableContentId: 'some-id',
          expandableContentRef: { current: undefined },
          chipContainerRef: { current: undefined }
        }}
      >
        <ToolbarToggleGroup breakpoint={undefined as 'xl'} toggleIcon={null} />
      </ToolbarContentContext.Provider>
    );

    expect(myMock).toHaveBeenCalled();
  });
});
