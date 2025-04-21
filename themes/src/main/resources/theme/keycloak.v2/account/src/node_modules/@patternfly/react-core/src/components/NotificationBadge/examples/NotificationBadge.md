---
id: Notification badge
section: components
cssPrefix: pf-c-notification-badge
propComponents: ['NotificationBadge']
---
import BellIcon from '@patternfly/react-icons/dist/esm/icons/bell-icon';
import './notificationBadge.css';

## Examples
### Basic
```js
import React from 'react';
import { NotificationBadge } from '@patternfly/react-core';
import BellIcon from '@patternfly/react-icons/dist/esm/icons/bell-icon';

class SimpleNotificationBadge extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      unreadVariant: 'unread',
      attentionVariant: 'attention'
    };
    this.onFirstClick = () => {
      this.setState({
        unreadVariant: 'read'
      });
    };
    this.onSecondClick = () => {
      this.setState({
        attentionVariant: 'read'
      });
    };
  }

  render() {
    const { unreadVariant, attentionVariant } = this.state;
    return (
      <div className="pf-t-dark">
        <NotificationBadge variant={unreadVariant} onClick={this.onFirstClick} aria-label="First notifications" />
        <NotificationBadge variant={attentionVariant} onClick={this.onSecondClick} aria-label="Second notifications" />
      </div>
    );
  }
}
```

## Examples
### With count
```js
import React from 'react';
import { NotificationBadge } from '@patternfly/react-core';
import BellIcon from '@patternfly/react-icons/dist/esm/icons/bell-icon';

class NotificationBadgeWithCount extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      firstVariant: 'unread',
      firstCount: 30,
      secondVariant: 'attention',
      secondCount: 30
    };
    this.onFirstClick = () => {
      this.setState({
        firstVariant: 'read',
      });
    };
    this.onSecondClick = () => {
      this.setState({
        secondVariant: 'read'
      });
    };
  }

  render() {
    const { firstVariant, firstCount, secondVariant, secondCount } = this.state;
    return (
      <div className="pf-t-dark">
        <NotificationBadge variant={firstVariant} onClick={this.onFirstClick} aria-label="First notifications" count={firstCount} />
        <NotificationBadge variant={secondVariant} onClick={this.onSecondClick} aria-label="Second notifications" count={secondCount} />
      </div>
    );
  }
}
```
