---
id: Modal
section: components
cssPrefix: pf-c-modal-box
propComponents: ['Modal', 'ModalBox', 'ModalBoxBody', 'ModalBoxCloseButton', 'ModalBoxFooter', 'ModalContent']
ouia: true
---

import WarningTriangleIcon from '@patternfly/react-icons/dist/esm/icons/warning-triangle-icon';
import CaretDownIcon from '@patternfly/react-icons/dist/esm/icons/caret-down-icon';
import BullhornIcon from '@patternfly/react-icons/dist/esm/icons/bullhorn-icon';
import HelpIcon from '@patternfly/react-icons/dist/esm/icons/help-icon';

## Examples

### Basic

```js
import React from 'react';
import { Modal, Button } from '@patternfly/react-core';

class SimpleModal extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isModalOpen: false
    };
    this.handleModalToggle = () => {
      this.setState(({ isModalOpen }) => ({
        isModalOpen: !isModalOpen
      }));
    };
  }

  render() {
    const { isModalOpen } = this.state;

    return (
      <React.Fragment>
        <Button variant="primary" onClick={this.handleModalToggle}>
          Show modal
        </Button>
        <Modal
          title="Simple modal header"
          isOpen={isModalOpen}
          onClose={this.handleModalToggle}
          actions={[
            <Button key="confirm" variant="primary" onClick={this.handleModalToggle}>
              Confirm
            </Button>,
            <Button key="cancel" variant="link" onClick={this.handleModalToggle}>
              Cancel
            </Button>
          ]}
        >
          Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore
          magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
          consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla
          pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id
          est laborum.
        </Modal>
      </React.Fragment>
    );
  }
}
```

### With description

```js
import React from 'react';
import { Modal, Button } from '@patternfly/react-core';

class SimpleModal extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isModalOpen: false
    };
    this.handleModalToggle = () => {
      this.setState(({ isModalOpen }) => ({
        isModalOpen: !isModalOpen
      }));
    };
  }

  render() {
    const { isModalOpen } = this.state;

    return (
      <React.Fragment>
        <Button variant="primary" onClick={this.handleModalToggle}>
          Show modal
        </Button>
        <Modal
          aria-label="My modal context"
          title="Modal header with description"
          isOpen={isModalOpen}
          onClose={this.handleModalToggle}
          description="A description is used when you want to provide more info about the modal than the title is able to describe. The content in the description is static and will not scroll with the rest of the modal body."
          actions={[
            <Button key="confirm" variant="primary" onClick={this.handleModalToggle}>
              Confirm
            </Button>,
            <Button key="cancel" variant="link" onClick={this.handleModalToggle}>
              Cancel
            </Button>
          ]}
        >
          Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore
          magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
          consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla
          pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id
          est laborum.
        </Modal>
      </React.Fragment>
    );
  }
}
```

### Top aligned

```js
import React from 'react';
import { Modal, ModalVariant, Button } from '@patternfly/react-core';

class TopModal extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isModalOpen: false
    };
    this.handleModalToggle = () => {
      this.setState(({ isModalOpen }) => ({
        isModalOpen: !isModalOpen
      }));
    };
  }

  render() {
    const { isModalOpen } = this.state;

    return (
      <React.Fragment>
        <Button variant="primary" onClick={this.handleModalToggle}>
          Show top aligned modal
        </Button>
        <Modal
          position="top"
          title="Top modal header"
          isOpen={isModalOpen}
          onClose={this.handleModalToggle}
          actions={[
            <Button key="confirm" variant="primary" onClick={this.handleModalToggle}>
              Confirm
            </Button>,
            <Button key="cancel" variant="link" onClick={this.handleModalToggle}>
              Cancel
            </Button>
          ]}
        >
          Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore
          magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
          consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla
          pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id
          est laborum.
        </Modal>
      </React.Fragment>
    );
  }
}
```

### Small

```js
import React from 'react';
import { Modal, ModalVariant, Button } from '@patternfly/react-core';

class SmallModal extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isModalOpen: false
    };
    this.handleModalToggle = () => {
      this.setState(({ isModalOpen }) => ({
        isModalOpen: !isModalOpen
      }));
    };
  }

  render() {
    const { isModalOpen } = this.state;

    return (
      <React.Fragment>
        <Button variant="primary" onClick={this.handleModalToggle}>
          Show small modal
        </Button>
        <Modal
          variant={ModalVariant.small}
          title="Small modal header"
          isOpen={isModalOpen}
          onClose={this.handleModalToggle}
          actions={[
            <Button key="confirm" variant="primary" onClick={this.handleModalToggle}>
              Confirm
            </Button>,
            <Button key="cancel" variant="link" onClick={this.handleModalToggle}>
              Cancel
            </Button>
          ]}
        >
          Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore
          magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
          consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla
          pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id
          est laborum.
        </Modal>
      </React.Fragment>
    );
  }
}
```

### Medium

```js
import React from 'react';
import { Modal, ModalVariant, Button } from '@patternfly/react-core';

class MediumModal extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isModalOpen: false
    };
    this.handleModalToggle = () => {
      this.setState(({ isModalOpen }) => ({
        isModalOpen: !isModalOpen
      }));
    };
  }

  render() {
    const { isModalOpen } = this.state;

    return (
      <React.Fragment>
        <Button variant="primary" onClick={this.handleModalToggle}>
          Show medium modal
        </Button>
        <Modal
          variant={ModalVariant.medium}
          title="Medium modal header"
          isOpen={isModalOpen}
          onClose={this.handleModalToggle}
          actions={[
            <Button key="confirm" variant="primary" onClick={this.handleModalToggle}>
              Confirm
            </Button>,
            <Button key="cancel" variant="link" onClick={this.handleModalToggle}>
              Cancel
            </Button>
          ]}
        >
          Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore
          magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
          consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla
          pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id
          est laborum.
        </Modal>
      </React.Fragment>
    );
  }
}
```

### Large

```js
import React from 'react';
import { Modal, ModalVariant, Button } from '@patternfly/react-core';

class LargeModal extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isModalOpen: false
    };
    this.handleModalToggle = () => {
      this.setState(({ isModalOpen }) => ({
        isModalOpen: !isModalOpen
      }));
    };
  }

  render() {
    const { isModalOpen } = this.state;

    return (
      <React.Fragment>
        <Button variant="primary" onClick={this.handleModalToggle}>
          Show large modal
        </Button>
        <Modal
          variant={ModalVariant.large}
          title="Large modal header"
          isOpen={isModalOpen}
          onClose={this.handleModalToggle}
          actions={[
            <Button key="confirm" variant="primary" onClick={this.handleModalToggle}>
              Confirm
            </Button>,
            <Button key="cancel" variant="link" onClick={this.handleModalToggle}>
              Cancel
            </Button>
          ]}
        >
          Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore
          magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
          consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla
          pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id
          est laborum.
        </Modal>
      </React.Fragment>
    );
  }
}
```

### Width

```js
import React from 'react';
import { Modal, Button } from '@patternfly/react-core';

class WidthModal extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isModalOpen: false
    };
    this.handleModalToggle = () => {
      this.setState(({ isModalOpen }) => ({
        isModalOpen: !isModalOpen
      }));
    };
  }

  render() {
    const { isModalOpen } = this.state;

    return (
      <React.Fragment>
        <Button variant="primary" onClick={this.handleModalToggle}>
          Show 50% width modal
        </Button>
        <Modal
          width={'50%'}
          title="Modal header for set width example"
          isOpen={isModalOpen}
          onClose={this.handleModalToggle}
          actions={[
            <Button key="confirm" variant="primary" onClick={this.handleModalToggle}>
              Confirm
            </Button>,
            <Button key="cancel" variant="link" onClick={this.handleModalToggle}>
              Cancel
            </Button>
          ]}
        >
          Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore
          magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
          consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla
          pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id
          est laborum.
        </Modal>
      </React.Fragment>
    );
  }
}
```

### Custom header and footer

```js
import React from 'react';
import { Modal, ModalVariant, Button, Title, TitleSizes } from '@patternfly/react-core';
import WarningTriangleIcon from '@patternfly/react-icons/dist/esm/icons/warning-triangle-icon';

class CustomHeaderFooter extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isModalOpen: false
    };
    this.handleModalToggle = () => {
      this.setState(({ isModalOpen }) => ({
        isModalOpen: !isModalOpen
      }));
    };
  }

  render() {
    const { isModalOpen } = this.state;

    const header = (
      <React.Fragment>
        <Title id="custom-header-label" headingLevel="h1" size={TitleSizes['2xl']}>
          With custom modal header/footer
        </Title>
        <p className="pf-u-pt-sm">Allows for custom content in the header and/or footer by passing components.</p>
      </React.Fragment>
    );

    const footer = (
      <Title headingLevel="h4" size={TitleSizes.md}>
        <WarningTriangleIcon />
        <span className="pf-u-pl-sm">Custom modal footer.</span>
      </Title>
    );

    return (
      <React.Fragment>
        <Button variant="primary" onClick={this.handleModalToggle}>
          Show custom header/footer modal
        </Button>
        <Modal
          variant={ModalVariant.large}
          isOpen={isModalOpen}
          header={header}
          aria-label="My dialog"
          aria-labelledby="custom-header-label"
          aria-describedby="custom-header-description"
          onClose={this.handleModalToggle}
          footer={footer}
        >
          <span id="custom-header-description">
            When static text describing the modal is available, it can be wrapped with an ID referring to the modal's
            aria-describedby value.
          </span>
          <br />
          <br />
          Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis
          aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint
          occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
        </Modal>
      </React.Fragment>
    );
  }
}
```

### No header

```js
import React from 'react';
import { Modal, ModalVariant, Button } from '@patternfly/react-core';

class NoHeader extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isModalOpen: false
    };
    this.handleModalToggle = () => {
      this.setState(({ isModalOpen }) => ({
        isModalOpen: !isModalOpen
      }));
    };
  }

  render() {
    const { isModalOpen } = this.state;
    const footer = <React.Fragment>Modal Footer</React.Fragment>;

    return (
      <React.Fragment>
        <Button variant="primary" onClick={this.handleModalToggle}>
          Show no header modal
        </Button>
        <Modal
          variant={ModalVariant.large}
          isOpen={isModalOpen}
          aria-label="No header example"
          showClose={true}
          aria-describedby="no-header-example"
          onClose={this.handleModalToggle}
          footer={footer}
        >
          <span id="no-header-example">
            When static text describing the modal is available, it can be wrapped with an ID referring to the modal's
            aria-describedby value.
          </span>
          <br />
          <br />
          Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis
          aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint
          occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
        </Modal>
      </React.Fragment>
    );
  }
}
```

### Custom Icon

```js
import React from 'react';
import { Modal, Button } from '@patternfly/react-core';
import BullhornIcon from '@patternfly/react-icons/dist/esm/icons/bullhorn-icon';

class NoHeader extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isModalOpen: false
    };
    this.handleModalToggle = () => {
      this.setState(({ isModalOpen }) => ({
        isModalOpen: !isModalOpen
      }));
    };
  }

  render() {
    const { isModalOpen } = this.state;

    return (
      <React.Fragment>
        <Button variant="primary" onClick={this.handleModalToggle}>
          Show icon modal
        </Button>
        <Modal
          isOpen={isModalOpen}
          aria-label="Modal custom icon example"
          title="Modal Header"
          titleIconVariant={BullhornIcon}
          showClose={true}
          aria-describedby="no-header-example"
          onClose={this.handleModalToggle}
        >
          <span id="no-header-example">
            When static text describing the modal is available, it can be wrapped with an ID referring to the modal's
            aria-describedby value.
          </span>
          <br />
          <br />
          Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis
          aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint
          occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
        </Modal>
      </React.Fragment>
    );
  }
}
```

### Warning Alert

```js
import React from 'react';
import { Modal, Button } from '@patternfly/react-core';

class NoHeader extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isModalOpen: false
    };
    this.handleModalToggle = () => {
      this.setState(({ isModalOpen }) => ({
        isModalOpen: !isModalOpen
      }));
    };
  }

  render() {
    const { isModalOpen } = this.state;

    return (
      <React.Fragment>
        <Button variant="primary" onClick={this.handleModalToggle}>
          Show icon modal
        </Button>
        <Modal
          isOpen={isModalOpen}
          aria-label="Modal warning example"
          title="Modal Header"
          titleIconVariant="warning"
          showClose={true}
          aria-describedby="no-header-example"
          onClose={this.handleModalToggle}
        >
          <span id="no-header-example">
            When static text describing the modal is available, it can be wrapped with an ID referring to the modal's
            aria-describedby value.
          </span>
          <br />
          <br />
          Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis
          aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint
          occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
        </Modal>
      </React.Fragment>
    );
  }
}
```

### With wizard

```js
import React from 'react';
import { Modal, Button, Wizard } from '@patternfly/react-core';

class WithWizard extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isModalOpen: false
    };
    this.handleModalToggle = () => {
      this.setState(({ isModalOpen }) => ({
        isModalOpen: !isModalOpen
      }));
    };
  }

  render() {
    const { isModalOpen } = this.state;

    const steps = [
      { name: 'Step 1', component: <p>Step 1</p> },
      { name: 'Step 2', component: <p>Step 2</p> },
      { name: 'Step 3', component: <p>Step 3</p> },
      { name: 'Step 4', component: <p>Step 4</p> },
      { name: 'Review', component: <p>Review Step</p>, nextButtonText: 'Finish' }
    ];

    return (
      <React.Fragment>
        <Button variant="primary" onClick={this.handleModalToggle}>
          Show modal
        </Button>
        <Modal
          isOpen={isModalOpen}
          variant={ModalVariant.large}
          showClose={false}
          onClose={this.handleModalToggle}
          hasNoBodyWrapper
          aria-describedby="wiz-modal-example-description"
          aria-labelledby="wiz-modal-example-title"
        >
          <Wizard
            titleId="wiz-modal-example-title"
            descriptionId="wiz-modal-example-description"
            title="Simple Wizard"
            description="Simple Wizard Description"
            steps={steps}
            onClose={this.handleModalToggle}
            height={400}
          />
        </Modal>
      </React.Fragment>
    );
  }
}
```

### With dropdown

```js
import React from 'react';
import { Modal, Button, Dropdown, DropdownToggle, DropdownItem, KebabToggle } from '@patternfly/react-core';
import CaretDownIcon from '@patternfly/react-icons/dist/esm/icons/caret-down-icon';

class WithDropdown extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isModalOpen: false,
      isDropdownOpen: false
    };
    this.handleModalToggle = () => {
      const { isModalOpen } = this.state;
      this.setState({
        isModalOpen: !isModalOpen,
        isDropdownOpen: false
      });
    };
    this.onToggle = isDropdownOpen => {
      this.setState({
        isDropdownOpen
      });
    };
    this.onSelect = event => {
      this.setState({
        isDropdownOpen: !this.state.isDropdownOpen
      });
      this.onFocus();
    };
    this.onFocus = () => {
      const element = document.getElementById('toggle-id-menu-document-body');
      element.focus();
    };
    this.onEscapePress = () => {
      const { isDropdownOpen } = this.state;
      if (isDropdownOpen) {
        this.setState(
          {
            isDropdownOpen: !isDropdownOpen
          },
          () => {
            this.onFocus();
          }
        );
      } else {
        this.handleModalToggle();
      }
    };
  }

  render() {
    const { isModalOpen, isDropdownOpen } = this.state;

    const dropdownItems = [
      <DropdownItem key="link">Link</DropdownItem>,
      <DropdownItem key="action" component="button">
        Action
      </DropdownItem>,
      <DropdownItem key="disabled link" isDisabled>
        Disabled Link
      </DropdownItem>,
      <DropdownItem key="disabled action" isDisabled component="button">
        Disabled Action
      </DropdownItem>,
      <DropdownItem key="separated link">Separated Link</DropdownItem>,
      <DropdownItem key="separated action" component="button">
        Separated Action
      </DropdownItem>
    ];

    return (
      <React.Fragment>
        <Button variant="primary" onClick={this.handleModalToggle}>
          Show modal
        </Button>
        <Modal
          title="Modal with dropdown"
          variant={ModalVariant.small}
          isOpen={isModalOpen}
          onClose={this.handleModalToggle}
          actions={[
            <Button key="confirm" variant="primary" onClick={this.handleModalToggle}>
              Confirm
            </Button>,
            <Button key="cancel" variant="link" onClick={this.handleModalToggle}>
              Cancel
            </Button>
          ]}
          onEscapePress={this.onEscapePress}
        >
          <div>
            Set the dropdown <strong>menuAppendTo</strong> prop to <em>parent</em> in order to allow the dropdown menu
            break out of the modal container. You'll also want to handle closing of the modal yourself, by listening to
            the <strong>onEscapePress</strong> callback on the Modal component, so you can close the Dropdown first if
            it's open.
          </div>
          <div>
            <Dropdown
              onSelect={this.onSelect}
              toggle={
                <DropdownToggle
                  id="toggle-id-menu-document-body"
                  onToggle={this.onToggle}
                  toggleIndicator={CaretDownIcon}
                >
                  Dropdown with a menu that can break out
                </DropdownToggle>
              }
              isOpen={isDropdownOpen}
              dropdownItems={dropdownItems}
              menuAppendTo="parent"
            />
          </div>
        </Modal>
      </React.Fragment>
    );
  }
}
```

### With help

```js
import React from 'react';
import { Modal, Button, Popover } from '@patternfly/react-core';
import HelpIcon from '@patternfly/react-icons/dist/esm/icons/help-icon';

class HelpModal extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isModalOpen: false
    };
    this.handleModalToggle = () => {
      this.setState(({ isModalOpen }) => ({
        isModalOpen: !isModalOpen
      }));
    };
  }

  render() {
    const { isModalOpen } = this.state;

    return (
      <React.Fragment>
        <Button variant="primary" onClick={this.handleModalToggle}>
          Show modal
        </Button>
        <Modal
          title="Simple modal header"
          help={
            <Popover
              headerContent={<div>Help Popover</div>}
              bodyContent={
                <div>
                  Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam id feugiat augue, nec fringilla
                  turpis.
                </div>
              }
              footerContent="Popover Footer"
            >
              <Button variant="plain" aria-label="Help">
                <HelpIcon />
              </Button>
            </Popover>
          }
          isOpen={isModalOpen}
          onClose={this.handleModalToggle}
          actions={[
            <Button key="confirm" variant="primary" onClick={this.handleModalToggle}>
              Confirm
            </Button>,
            <Button key="cancel" variant="link" onClick={this.handleModalToggle}>
              Cancel
            </Button>
          ]}
        >
          Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore
          magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
          consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla
          pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id
          est laborum.
        </Modal>
      </React.Fragment>
    );
  }
}
```

### With form

```ts file="ModalWithForm.tsx"

```

### With overflowing content

If the content that you're passing to the modal is likely to overflow the modal content area, pass tabIndex={0} to the modal to enable keyboard accessible scrolling.

```ts file="ModalWithOverflowingContent.tsx"
```


