---
id: Number input
section: components
cssPrefix: pf-c-number-input
propComponents: ['NumberInput']
---

## Examples

### Default

```js
import React from 'react';
import { NumberInput } from '@patternfly/react-core';

class BasicNumberInput extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: 90
    };

    this.onMinus = () => {
      this.setState({
        value: this.state.value - 1
      });
    };

    this.onChange = event => {
      this.setState({
        value: Number(event.target.value)
      });
    };

    this.onPlus = () => {
      this.setState({
        value: this.state.value + 1
      });
    };
  }

  render() {
    const { value } = this.state;
    return (
      <NumberInput
        value={value}
        onMinus={this.onMinus}
        onChange={this.onChange}
        onPlus={this.onPlus}
        inputName="input"
        inputAriaLabel="number input"
        minusBtnAriaLabel="minus"
        plusBtnAriaLabel="plus"
      />
    );
  }
}
```

### With unit

```js
import React from 'react';
import { NumberInput } from '@patternfly/react-core';

class UnitNumberInput extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value1: 90,
      value2: (1.0).toFixed(2)
    };

    this.onMinus1 = () => {
      this.setState({
        value1: this.state.value1 - 1
      });
    };

    this.onChange1 = event => {
      this.setState({
        value1: Number(event.target.value)
      });
    };

    this.onPlus1 = () => {
      this.setState({
        value1: this.state.value1 + 1
      });
    };

    this.onMinus2 = () => {
      this.setState({
        value2: (Number(this.state.value2) - 0.01).toFixed(2)
      });
    };

    this.onChange2 = event => {
      console.log(event.target.value);
      this.setState({
        value2: event.target.value
      });
    };

    this.onPlus2 = () => {
      this.setState({
        value2: (Number(this.state.value2) + 0.01).toFixed(2)
      });
    };
  }

  render() {
    const { value1, value2 } = this.state;
    return (
      <React.Fragment>
        <NumberInput
          value={value1}
          onMinus={this.onMinus1}
          onChange={this.onChange1}
          onPlus={this.onPlus1}
          inputName="input 1"
          inputAriaLabel="number input 1"
          minusBtnAriaLabel="minus 1"
          plusBtnAriaLabel="plus 1"
          unit="%"
        />
        <br />
        <br />
        <NumberInput
          value={value2}
          onMinus={this.onMinus2}
          onChange={this.onChange2}
          onPlus={this.onPlus2}
          inputName="input 2"
          inputAriaLabel="number input 2"
          minusBtnAriaLabel="minus 0.01"
          plusBtnAriaLabel="plus 0.01"
          unit="$"
          unitPosition="before"
        />
      </React.Fragment>
    );
  }
}
```

### With unit and thresholds

```js
import React from 'react';
import { NumberInput } from '@patternfly/react-core';

class UnitThresholdNumberInput extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: 0
    };
    this.minValue = 0;
    this.maxValue = 10;

    this.normalizeBetween = (value, min, max) => {
      if (min !== undefined && max !== undefined) {
        return Math.max(Math.min(value, max), min);
      } else if (value <= min) {
        return min;
      } else if (value >= max) {
        return max;
      }
      return value;
    };

    this.onMinus = () => {
      this.setState({
        value: this.normalizeBetween(this.state.value - 1, this.minValue, this.maxValue)
      });
    };

    this.onChange = event => {
      const newValue = isNaN(event.target.value) ? 0 : Number(event.target.value);
      this.setState({
        value: newValue > this.maxValue ? this.maxValue : newValue < this.minValue ? this.minValue : newValue
      });
    };

    this.onPlus = () => {
      this.setState({
        value: this.normalizeBetween(this.state.value + 1, this.minValue, this.maxValue)
      });
    };
  }

  render() {
    const { value } = this.state;

    return (
      <React.Fragment>
        With a minimum value of 0 and maximum value of 10
        <br />
        <NumberInput
          value={value}
          min={this.minValue}
          max={this.maxValue}
          onMinus={this.onMinus}
          onChange={this.onChange}
          onPlus={this.onPlus}
          inputName="input"
          inputAriaLabel="number input"
          minusBtnAriaLabel="minus"
          plusBtnAriaLabel="plus"
          unit="%"
        />
      </React.Fragment>
    );
  }
}
```

### Disabled

```js
import React from 'react';
import { NumberInput } from '@patternfly/react-core';

class DisabledNumberInput extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: 100
    };

    this.onMinus = () => {
      this.setState({
        value: this.state.value - 1
      });
    };

    this.onChange = event => {
      this.setState({
        value: Number(event.target.value)
      });
    };

    this.onPlus = () => {
      this.setState({
        value: this.state.value + 1
      });
    };
  }

  render() {
    const { value } = this.state;
    const minValue = 0;
    const maxValue = 100;

    return (
      <NumberInput
        value={value}
        min={minValue}
        max={maxValue}
        onMinus={this.onMinus}
        onChange={this.onChange}
        onPlus={this.onPlus}
        inputName="input"
        inputAriaLabel="number input"
        minusBtnAriaLabel="minus"
        plusBtnAriaLabel="plus"
        unit="%"
        isDisabled
      />
    );
  }
}
```

### Varying sizes

```js
import React from 'react';
import { NumberInput } from '@patternfly/react-core';

class NumberInputSizes extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      input1Value: 1,
      input2Value: 1234567890,
      input3Value: 5,
      input4Value: 12345
    };

    this.onChange = event => {
      this.setState({
        [`${event.target.name}Value`]: Number(event.target.value)
      });
    };

    this.onMinus = (e, inputName) => {
      this.setState({
        [`${inputName}Value`]: this.state[`${inputName}Value`] - 1
      });
    };

    this.onPlus = (e, inputName) => {
      this.setState({
        [`${inputName}Value`]: this.state[`${inputName}Value`] + 1
      });
    };
  }

  render() {
    const { input1Value, input2Value, input3Value, input4Value } = this.state;

    return (
      <React.Fragment>
        <NumberInput
          value={input1Value}
          onMinus={this.onMinus}
          onChange={this.onChange}
          onPlus={this.onPlus}
          inputName="input1"
          inputAriaLabel="number input 1"
          minusBtnAriaLabel="input 2 minus"
          plusBtnAriaLabel="input 2 plus"
          widthChars={1}
        />
        <br />
        <br />
        <NumberInput
          value={input2Value}
          onMinus={this.onMinus}
          onChange={this.onChange}
          onPlus={this.onPlus}
          inputName="input2"
          inputAriaLabel="number input 2"
          minusBtnAriaLabel="input 2 minus"
          plusBtnAriaLabel="input 2 plus"
          widthChars={10}
        />
        <br />
        <br />
        <NumberInput
          value={input3Value}
          onMinus={this.onMinus}
          onChange={this.onChange}
          onPlus={this.onPlus}
          inputName="input3"
          inputAriaLabel="number input 3"
          minusBtnAriaLabel="input 3 minus"
          plusBtnAriaLabel="input 3 plus"
          widthChars={5}
        />
        <br />
        <br />
        <NumberInput
          value={input4Value}
          onMinus={this.onMinus}
          onChange={this.onChange}
          onPlus={this.onPlus}
          inputName="input4"
          inputAriaLabel="number input 4"
          minusBtnAriaLabel="input 4 minus"
          plusBtnAriaLabel="input 4 plus"
          widthChars={5}
        />
      </React.Fragment>
    );
  }
}
```

### Custom increment/decrement

```js
import React from 'react';
import { NumberInput } from '@patternfly/react-core';

class CustomStepNumberInput extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: 90,
      step: 3
    };

    this.stepper = step => () => {
      this.setState(prev => ({ ...prev, value: prev.value + step }));
    };

    this.onChange = event => {
      this.setState({
        value: Number(event.target.value)
      });
    };
  }

  render() {
    const { value } = this.state;
    return (
      <NumberInput
        value={value}
        onMinus={this.stepper(-3)}
        onChange={this.onChange}
        onPlus={this.stepper(3)}
        inputName="input"
        inputAriaLabel="number input"
        minusBtnAriaLabel="minus"
        plusBtnAriaLabel="plus"
      />
    );
  }
}
```

### Custom increment/decrement and thresholds

```js
import React from 'react';
import { NumberInput } from '@patternfly/react-core';

class CustomStepNumberInput extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: 90,
      step: 3
    };
    this.minValue = 90;
    this.maxValue = 100;

    this.normalizeBetween = (value, min, max) => {
      if (min !== undefined && max !== undefined) {
        return Math.max(Math.min(value, max), min);
      } else if (value <= min) {
        return min;
      } else if (value >= max) {
        return max;
      }
      return value;
    };

    this.stepper = step => () => {
      this.setState(prev => ({
        ...prev,
        value: this.normalizeBetween(prev.value + step, this.minValue, this.maxValue)
      }));
    };

    this.onChange = event => {
      const newValue = isNaN(event.target.value) ? 0 : Number(event.target.value);
      this.setState({
        value: newValue
      });
    };

    this.onBlur = event => {
      const newValue = isNaN(event.target.value) ? 0 : Number(event.target.value);
      this.setState({
        value: newValue > this.maxValue ? this.maxValue : newValue < this.minValue ? this.minValue : newValue
      });
    };
  }

  render() {
    const { value } = this.state;
    return (
      <NumberInput
        value={value}
        min={this.minValue}
        max={this.maxValue}
        onMinus={this.stepper(-3)}
        onChange={this.onChange}
        onBlur={this.onBlur}
        onPlus={this.stepper(3)}
        inputName="input"
        inputAriaLabel="number input"
        minusBtnAriaLabel="minus"
        plusBtnAriaLabel="plus"
      />
    );
  }
}
```
