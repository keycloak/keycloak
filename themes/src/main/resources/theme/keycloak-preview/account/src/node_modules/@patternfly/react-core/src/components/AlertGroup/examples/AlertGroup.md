---
title: Alert group
section: components
cssPrefix: "pf-c-alert-group"
typescript: true
propComponents: ['Alert', 'AlertGroup', 'AlertActionCloseButton', 'AlertActionLink']
---

import * as React from 'react';
import { Alert, AlertGroup, AlertVariant, AlertActionCloseButton, InputGroup } from '@patternfly/react-core';

## Examples
```js title=Static-alert-group
import * as React from 'react';
import { Alert, AlertGroup, AlertVariant } from '@patternfly/react-core';

class StaticAlertGroup extends React.Component {
  render() {
    return (
      <React.Fragment>
        <AlertGroup>
          <Alert title="Success Alert" variant={AlertVariant['success']} isInline />
          <Alert title="Info Alert" variant={AlertVariant['info']} isInline/>
        </AlertGroup>
      </React.Fragment>
    );
  }
}
```

```js title=Toast-alert-group
import * as React from 'react';
import { Alert, AlertGroup, AlertActionCloseButton, AlertVariant, InputGroup } from '@patternfly/react-core';
class ToastAlertGroup extends React.Component {
  constructor() {
    super();
    this.state = { alerts: [] };
    this.addAlert = (title, variant, key) => {
      this.setState({
        alerts: [ ...this.state.alerts, { title: title, variant: variant, key }]
      });
    };
    this.removeAlert = key => {
      this.setState({ alerts: [...this.state.alerts.filter(el => el.key !== key)] });
    };
  }
  render() {
    const btnClasses = ['pf-c-button', 'pf-m-secondary'].join(' ');
    const getUniqueId = () => (new Date().getTime());
    const addSuccessAlert = () => { this.addAlert('Toast Success Alert', 'success', getUniqueId()) };
    const addDangerAlert = () => { this.addAlert('Toast Danger Alert', 'danger', getUniqueId()) };
    const addInfoAlert = () => { this.addAlert('Toast Info Alert', 'info', getUniqueId()) };
    return (
      <React.Fragment>
        <InputGroup style={{ marginBottom: '16px' }}>
          <button onClick={addSuccessAlert} type="button" className={btnClasses}>Add Toast Success Alert</button>
          <button onClick={addDangerAlert} type="button" className={btnClasses}>Add Toast Danger Alert</button>
          <button onClick={addInfoAlert} type="button" className={btnClasses}>Add Toast Info Alert</button>
        </InputGroup>
        <AlertGroup isToast>
          {this.state.alerts.map(({key, variant, title}) => (
            <Alert
              isLiveRegion
              variant={AlertVariant[variant]}
              title={title}
              action={
                <AlertActionCloseButton
                  title={title}
                  variantLabel={`${variant} alert`}
                  onClose={() => this.removeAlert(key)}
                />
              }
              key={key} />
          ))}
        </AlertGroup>
      </React.Fragment>
    );
  }
}
```

```js title=Singular-dynamic-alert-group
import * as React from 'react';
import { Alert, AlertGroup, AlertVariant, AlertActionCloseButton, InputGroup } from '@patternfly/react-core';
class SingularAdditiveAlertGroup extends React.Component {
  constructor() {
    super();
    this.state = {
      alerts: []
    }
  }
  render() {
    const addAlert = (title, variant, key) => {
      this.setState({ alerts: [...this.state.alerts, { title: title, variant: variant, key }] });
    };
    const btnClasses = ['pf-c-button', 'pf-m-secondary'].join(' ');
    const getUniqueId = () => (new Date().getTime());
    const addSuccessAlert = () => { addAlert('Single Success Alert', 'success', getUniqueId()) };
    const addDangerAlert = () => { addAlert('Single Danger Alert', 'danger', getUniqueId()) };
    const addInfoAlert = () => { addAlert('Single Info Alert', 'info', getUniqueId()) };
    this.removeAlert = key => {
      this.setState({ alerts: [...this.state.alerts.filter(el => el.key !== key)] });
    };
    return (
      <React.Fragment>
        <InputGroup style={{ marginBottom: '16px' }}>
          <button onClick={addSuccessAlert} type="button" className={btnClasses}>Add Single Success Alert</button>
          <button onClick={addDangerAlert} type="button" className={btnClasses}>Add Single Danger Alert</button>
          <button onClick={addInfoAlert} type="button" className={btnClasses}>Add Single Info Alert</button>
        </InputGroup>
        <AlertGroup>
          {this.state.alerts.map(({ title, variant, key }) => (
            <Alert
              isInline
              isLiveRegion
              variant={AlertVariant[variant]}
              title={title}
              key={key}
              action={
                <AlertActionCloseButton
                  title={title}
                  variantLabel={`${variant} alert`}
                  onClose={() => this.removeAlert(key)}
                />
              }/>
          ))}
        </AlertGroup>
      </React.Fragment>
    );
  }
}
```

```js title=Multiple-dynamic-alert-group
import * as React from 'react';
import { Alert, AlertGroup, AlertVariant, AlertActionCloseButton, InputGroup } from '@patternfly/react-core';
class MultipleAdditiveAlertGroup extends React.Component {
  constructor() {
    super();
    this.state = {
      alerts: []
    }
  }
  render() {
    const addAlerts = (incomingAlerts) => {
      this.setState({ alerts: [...this.state.alerts, ...incomingAlerts] });
    };
    const getUniqueId = () => (
      (String.fromCharCode(65 + Math.floor(Math.random() * 26))+ Date.now())
    );
    const btnClasses = ['pf-c-button', 'pf-m-secondary'].join(' ');
    const addAlertCollection = () => {
      addAlerts([
        { title: 'First Alert Notification.', variant: 'success', key: getUniqueId() },
        { title: 'Second Alert Notification.', variant: 'warning', key: getUniqueId() },
        { title: 'Third Alert Notification.', variant: 'danger', key: getUniqueId() }
      ])
    };
    this.removeAlert = key => {
      this.setState({ alerts: [...this.state.alerts.filter(el => el.key !== key)] });
    };
    return (
      <React.Fragment>
        <InputGroup style={{ marginBottom: '16px' }}>
          <button onClick={addAlertCollection} type="button" className={btnClasses}>Add Alert Collection</button>
        </InputGroup>
        <AlertGroup isToast>
          {this.state.alerts.map(({ title, variant, key, action }) => (
            <Alert
              isLiveRegion
              variant={AlertVariant[variant]}
              title={title}
              key={key}
              action={
                <AlertActionCloseButton
                  title={title}
                  variantLabel={`${variant} alert`}
                  onClose={() => this.removeAlert(key)}
                />
              }/>
          ))}
        </AlertGroup>
      </React.Fragment>
    );
  }
}
```

```js title=Async-alert-group
import * as React from 'react';
import { Alert, AlertGroup, AlertActionCloseButton, AlertVariant, InputGroup } from '@patternfly/react-core';
class AsyncAdditiveAlertGroup extends React.Component {
  constructor() {
    super();
    this.state = {
      alerts: [],
      timer: null
    }
    this.stopAsyncAlerts = () => { clearInterval(this.state.timer); }
  }
  componentWillUnmount() { this.stopAsyncAlerts(); }
  render() {
    const addAlerts = (incomingAlerts) => { this.setState({ alerts: [...this.state.alerts, ...incomingAlerts] }); };
    const getUniqueId = () => (new Date().getTime());
    const btnClasses = ['pf-c-button', 'pf-m-secondary'].join(' ');
    this.removeAlert = key => {
      this.setState({ alerts: [...this.state.alerts.filter(el => el.key !== key)] });
    };
    const startAsyncAlerts = () => {
      let timerValue = setInterval(() => {
        addAlerts([
          {
            title: `Async Notification ${this.state.alerts.length + 1} was added to the queue.`,
            variant: 'info',
            key: getUniqueId()
          }
        ])
      }, 1500);
      this.setState({timer: timerValue});
    };
    return (
      <React.Fragment>
        <InputGroup style={{ marginBottom: '16px' }}>
          <button onClick={startAsyncAlerts} type="button" className={btnClasses}>Start Async Alerts</button>
          <button onClick={this.stopAsyncAlerts} type="button" className={btnClasses}>Stop Async Alerts</button>
        </InputGroup>
        <AlertGroup isToast>
          {this.state.alerts.map(({ title, variant, key }) => (
            <Alert
              isLiveRegion
              variant={AlertVariant[variant]}
              title={title}
              key={key}
              action={
                <AlertActionCloseButton
                  title={title}
                  variantLabel={`${variant} alert`}
                  onClose={() => this.removeAlert(key)}
                />
              }/>
          ))}
        </AlertGroup>
      </React.Fragment>
    );
  }
}
```
