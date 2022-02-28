import React from 'react';
import { Toolbar } from '@patternfly/react-core/dist/js/layouts/Toolbar/Toolbar';
import { ToolbarGroup } from '@patternfly/react-core/dist/js/layouts/Toolbar/ToolbarGroup';
import { ToolbarSection } from '@patternfly/react-core/dist/js/layouts/Toolbar/ToolbarSection';
import { ToolbarItem } from '@patternfly/react-core/dist/js/layouts/Toolbar/ToolbarItem';

class SimpleToolbarSection extends React.Component {
  render() {
    return (
      <Toolbar>
        <ToolbarSection aria-label="First section">
          <ToolbarGroup>
            <ToolbarItem>Item 1</ToolbarItem>
          </ToolbarGroup>
          <ToolbarGroup>
            <ToolbarItem>Item 2</ToolbarItem>
            <ToolbarItem>Item 3</ToolbarItem>
          </ToolbarGroup>
        </ToolbarSection>
        <ToolbarSection aria-label="Second section">
          <ToolbarGroup>
            <ToolbarItem>Item 4</ToolbarItem>
            <ToolbarItem>Item 5</ToolbarItem>
            <ToolbarItem>Item 6</ToolbarItem>
          </ToolbarGroup>
          <ToolbarGroup>
            <ToolbarItem>Item 7</ToolbarItem>
          </ToolbarGroup>
        </ToolbarSection>
      </Toolbar>
    );
  }
}

export default SimpleToolbarSection;
