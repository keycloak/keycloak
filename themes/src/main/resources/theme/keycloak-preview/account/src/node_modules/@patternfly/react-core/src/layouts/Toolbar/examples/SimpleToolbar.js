import React from 'react';
import { Toolbar } from '@patternfly/react-core/dist/js/layouts/Toolbar/Toolbar';
import { ToolbarGroup } from '@patternfly/react-core/dist/js/layouts/Toolbar/ToolbarGroup';
import { ToolbarItem } from '@patternfly/react-core/dist/js/layouts/Toolbar/ToolbarItem';

class SimpleToolbar extends React.Component {
  render() {
    return (
      <Toolbar>
        <ToolbarGroup>
          <ToolbarItem>Item 1</ToolbarItem>
        </ToolbarGroup>
        <ToolbarGroup>
          <ToolbarItem>Item 2</ToolbarItem>
          <ToolbarItem>Item 3</ToolbarItem>
        </ToolbarGroup>
        <ToolbarGroup>
          <ToolbarItem>Item 4</ToolbarItem>
        </ToolbarGroup>
      </Toolbar>
    );
  }
}

export default SimpleToolbar;
