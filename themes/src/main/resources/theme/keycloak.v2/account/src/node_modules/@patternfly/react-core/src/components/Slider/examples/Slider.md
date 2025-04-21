---
id: Slider
section: components
cssPrefix: pf-c-slider
propComponents: ['Slider', 'SliderStepObject']
beta: true
---

import { Slider, Button, Text, TextVariants } from '@patternfly/react-core';
import MinusIcon from '@patternfly/react-icons/dist/esm/icons/minus-icon';
import PlusIcon from '@patternfly/react-icons/dist/esm/icons/plus-icon';
import LockIcon from '@patternfly/react-icons/dist/esm/icons/lock-icon';
import LockOpenIcon from '@patternfly/react-icons/dist/esm/icons/lock-open-icon';

## Examples

### Discrete

```js
import React from 'react';
import { Slider, Text, TextVariants } from '@patternfly/react-core';

class DiscreteInput extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value1: 50,
      value2: 50,
      value3: 25,
      value4: 50,
      value5: 50,
      value6: 3,
      value7: 25
    };

    this.steps = [
      { value: 0, label: '0' },
      { value: 12.5, label: '1', isLabelHidden: true },
      { value: 25, label: '2' },
      { value: 37.5, label: '3', isLabelHidden: true },
      { value: 50, label: '4' },
      { value: 62.5, label: '5', isLabelHidden: true },
      { value: 75, label: '6' },
      { value: 87.5, label: '7', isLabelHidden: true },
      { value: 100, label: '8' }
    ];

    this.stepsDiscreteWithMax = [
      { value: 0, label: "A" },
      { value: 1, label: "B" },
      { value: 2, label: "C" },
      { value: 3, label: "D" },
      { value: 4, label: "E" },
      { value: 5, label: "F" }
    ];

    this.stepsDiscreteNoLinearWithMaxMin = [
      { value: 12, label: '12' },
      { value: 15, label: '15' },
      { value: 25, label: '25' },
      { value: 54, label: '54' },
      { value: 67, label: '67' },
      { value: 86, label: '86' }
  ];

    this.onChange = value => {
        this.setState({
          value1: value
        });
    };

    this.onChange2 = value => {
        this.setState({
          value2: value
        });
    };

    this.onChange3 = value => {
        this.setState({
          value3: value
        });
    };

    this.onChange4 = value => {
        this.setState({
          value4: value
        });
    };

    this.onChange5 = value => {
        this.setState({
          value5: value
        });
    };

    this.onChange6 = value => {
        this.setState({
          value6: value
        });
    };

      this.onChange7 = value => {
        this.setState({
          value7: value
        });
    };
  }

  render() {
    const step = this.steps.find(step => step.value === this.state.value1);
    const displayValue = step ? step.label : 0;
    return (
      <>
        <Text component={TextVariants.h3}>Slider value is: {displayValue}</Text>
        <Slider value={this.state.value1} onChange={this.onChange} customSteps={this.steps} />
        <br />
        <Text component={TextVariants.h3}>Slider value is: {Math.floor(this.state.value2)}</Text>
        <Text component={TextVariants.small}>(min = 0, max = 200, step = 50) </Text>
        <Slider value={this.state.value2} onChange={this.onChange2} max={200} step={50} showTicks/>
        <br />
        <Text component={TextVariants.h3}>Slider value is: {Math.floor(this.state.value3)}</Text>
        <Text component={TextVariants.small}>(min = -25, max = 75, step = 10, boundaries not shown) </Text>
        <Slider value={this.state.value3} onChange={this.onChange3} min={-25} max={75} step={10} showTicks showBoundaries={false}/>
        <br />
        <Text component={TextVariants.h3}>Slider value is: {Math.floor(this.state.value4)}</Text>
        <Text component={TextVariants.small}>(min = -25, max = 75, step = 10, boundaries shown) </Text>
        <Slider value={this.state.value4} onChange={this.onChange4} min={-25} max={75} step={10} showTicks />
        <br />
        <Text component={TextVariants.h3}>Slider value is: {Math.floor(this.state.value5)}</Text>
        <Text component={TextVariants.small}>(min = -25, max = 75, step = 10, boundaries shown, ticks not shown) </Text>
        <Slider value={this.state.value5} onChange={this.onChange5} min={-25} max={75} step={10} />
        <br />
        <Text component={TextVariants.h3}>Slider value is: {Math.floor(this.state.value6)}</Text>
        <Text component={TextVariants.small}>(max = 5, custom steps) </Text>
        <Slider
            value={this.state.value6}
            showTicks
            max={5}
            customSteps={this.stepsDiscreteWithMax}
            onChange={this.onChange6}
          />
        <br />
        <Text component={TextVariants.h3}>Slider value is: {Math.floor(this.state.value7)}</Text>
        <Text component={TextVariants.small}>(min = 12, max = 86, custom steps with non linear data) </Text>
        <Slider
            value={this.state.value7}
            showTicks
            customSteps={this.stepsDiscreteNoLinearWithMaxMin}
            onChange={this.onChange7}
            min={12}
            max={86}
          />
        <br />
      </>
    );
  }
}
```

### Continuous

```js
import React from 'react';
import { Checkbox, Slider, Text, TextVariants } from '@patternfly/react-core';

class ContinuousInput extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      hasTooltipOverThumb: false,
      value: 50,
      valueCustom: 50
    };

    this.onChange = value => {
        this.setState({
          value: value
        });
    };

    this.onChangeCustom = value => {
        this.setState({
          valueCustom: value
        });
    };
  }

  render() {
    return (
      <>
        <Checkbox
          id="thumb-has-tooltip"
          label="hasTooltipOverThumb"
          isChecked={this.state.hasTooltipOverThumb}
          onChange={hasTooltipOverThumb => this.setState({ hasTooltipOverThumb })}
          style={{ marginBottom: 20 }} />
        <Text component={TextVariants.h3}>Slider Value is: {this.state.value}</Text>
        <Slider
          hasTooltipOverThumb={this.state.hasTooltipOverThumb}
          value={this.state.value}
          onChange={this.onChange} />
        <br />
        <Text component={TextVariants.h3}>Slider value is: {this.state.valueCustom}</Text>
        <Slider
          onChange={this.onChangeCustom}
          value={this.state.valueCustom}
          areCustomStepsContinuous
          hasTooltipOverThumb={this.state.hasTooltipOverThumb}
          customSteps={[
            { value: 0, label: '0%' },
            { value: 100, label: '100%' }
          ]}
        />
      </>
    );
  }
}
```

### Value input

```js
import React from 'react';
import { Slider } from '@patternfly/react-core';

class ValueInput extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      valueDiscrete: 62.5,
      inputValueDiscrete: 5,
      valuePercent: 50,
      inputValuePercent: 50,
      valueContinuous: 50,
      inputValueContinuous: 50
    };

    this.stepsDiscrete = [
      { value: 0, label: '0' },
      { value: 12.5, label: '1', isLabelHidden: true },
      { value: 25, label: '2' },
      { value: 37.5, label: '3', isLabelHidden: true },
      { value: 50, label: '4' },
      { value: 62.5, label: '5', isLabelHidden: true },
      { value: 75, label: '6' },
      { value: 87.5, label: '7', isLabelHidden: true },
      { value: 100, label: '8' }
    ];

    this.stepsPercent = [
      { value: 0, label: '0%' },
      { value: 25, label: '25%', isLabelHidden: true },
      { value: 50, label: '50%' },
      { value: 75, label: '75%', isLabelHidden: true },
      { value: 100, label: '100%' }
    ];

    this.onChangeDiscrete = (value, inputValue, setLocalInputValue) => {

      let newValue;
      let newInputValue;

      if (inputValue === undefined) { 
        const step = this.stepsDiscrete.find(step => step.value === value);
        newInputValue = step ? step.label : 0;
        newInputValue = Number(newInputValue);
        newValue = value;
      } else {
        const maxValue =  Number(this.stepsDiscrete[this.stepsDiscrete.length -1].label);
        if (inputValue > maxValue) {
          newValue = Number(this.stepsDiscrete[this.stepsDiscrete.length -1].value);
          newInputValue =  maxValue;
          setLocalInputValue(maxValue);
        } else {
          const minValue =  Number(this.stepsDiscrete[0].label);
          if (inputValue < minValue) {
            newValue = Number(this.stepsDiscrete[0].value);
            newInputValue =  minValue;
            setLocalInputValue(minValue);
          } else {
            const stepIndex = this.stepsDiscrete.findIndex(step => Number(step.label) >= inputValue);
            if (Number(this.stepsDiscrete[stepIndex].label) === inputValue) {
              newValue = this.stepsDiscrete[stepIndex].value;
              newInputValue = inputValue;
            } else {
              const midpoint = (Number(this.stepsDiscrete[stepIndex].label) + Number(this.stepsDiscrete[stepIndex - 1].label)) / 2;
              if (midpoint > inputValue) {
                newValue = this.stepsDiscrete[stepIndex - 1].value;
                newInputValue = Number(this.stepsDiscrete[stepIndex - 1].label);
              } else {
                newValue = this.stepsDiscrete[stepIndex].value;
                newInputValue = Number(this.stepsDiscrete[stepIndex].label);
              }
            }
          }
        }
      }

      this.setState({
        inputValueDiscrete: newInputValue,
        valueDiscrete: newValue
      });
    };

    this.onChangePercent = (value, inputValue, setLocalInputValue) => {
      let newValue;
      let newInputValue;

      if (inputValue === undefined) { 
        const step = this.stepsPercent.find(step => step.value === value);
        newInputValue = step ? step.label.slice(0, -1) : 0;
        newInputValue = Number(newInputValue);
        newValue = value;
      } else {
        const maxValue =  Number(this.stepsPercent[this.stepsPercent.length -1].label.slice(0, -1));
        if (inputValue > maxValue) {
          newValue = Number(this.stepsPercent[this.stepsPercent.length -1].value);
          newInputValue =  maxValue;
          setLocalInputValue(maxValue);
        } else {
          const minValue =  Number(this.stepsPercent[0].label.slice(0, -1));
          if (inputValue < minValue) {
            newValue = minValue;
            setLocalInputValue(minValue);
          } else {
            const stepIndex = this.stepsPercent.findIndex(step => Number(step.label.slice(0, -1)) >= inputValue);
            if (Number(this.stepsPercent[stepIndex].label.slice(0, -1)) === inputValue) {
              newValue = this.stepsPercent[stepIndex].value;
              newInputValue = inputValue;
            } else {
              const midpoint = (Number(this.stepsPercent[stepIndex].label.slice(0, -1)) + Number(this.stepsPercent[stepIndex - 1].label.slice(0, -1))) / 2;
              if (midpoint > inputValue) {
                newValue = this.stepsPercent[stepIndex - 1].value;
                newInputValue = Number(this.stepsPercent[stepIndex - 1].label.slice(0, -1));
              } else {
                newValue = this.stepsPercent[stepIndex].value;
                newInputValue = Number(this.stepsPercent[stepIndex].label.slice(0, -1));
              }
            }
          }
        }
      }
      
      this.setState({
        inputValuePercent: newInputValue,
        valuePercent: newValue
      });
    };

    this.onChangeContinuous = (value, inputValue, setLocalInputValue) => { 
      let newValue;
      if (inputValue === undefined) { 
        newValue = Math.floor(value);
      } else {
        if (inputValue > 100) {
          newValue = 100;
          setLocalInputValue(100);
        } else if (inputValue < 0) {
          newValue = 0;
          setLocalInputValue(0);
        } else {
          newValue = Math.floor(inputValue);
        }
      }
      this.setState({
        inputValueContinuous: newValue,
        valueContinuous: newValue
      });
    };
  }

  render() {
    return (
      <>
        <Slider
          value={this.state.valueDiscrete}
          isInputVisible
          inputValue={this.state.inputValueDiscrete}
          customSteps={this.stepsDiscrete}
          onChange={this.onChangeDiscrete}
        />
        <br />
        <Slider
          value={this.state.valuePercent}
          isInputVisible
          inputValue={this.state.inputValuePercent}
          inputLabel="%"
          onChange={this.onChangePercent}
          customSteps={this.stepsPercent}
        />
        <br />
        <Slider
          value={this.state.valueContinuous}
          isInputVisible
          inputValue={this.state.inputValueContinuous}
          inputLabel="%"
          onChange={this.onChangeContinuous}
        />
      </>
    );
  }
}
```

### Thumb value input

```js
import React from 'react';
import { Slider } from '@patternfly/react-core';

class ThumbValueInput extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: 50,
      inputValue: 50
    };

    this.onChange = (value, inputValue, setLocalInputValue) => { 
      let newValue;
      if (inputValue === undefined) { 
        newValue = Number(value);
      } else {
        if (inputValue > 100) {
          newValue = 100;
          setLocalInputValue(100);
        } else if (inputValue < 0) {
          newValue = 0;
          setLocalInputValue(0);
        } else {
          newValue = Math.floor(inputValue);
        }
      }
      this.setState({
        value: newValue,
        inputValue: newValue
      });
    };
  }

  render() {
    return (
      <Slider
        value={this.state.value}
        isInputVisible
        inputValue={this.state.inputValue}
        inputLabel="%"
        inputPosition="aboveThumb"
        onChange={this.onChange}
      />
    );
  }
}
```

### Actions

```js
import React from 'react';
import { Slider, Button, Text, TextVariants } from '@patternfly/react-core';
import MinusIcon from '@patternfly/react-icons/dist/esm/icons/minus-icon';
import PlusIcon from '@patternfly/react-icons/dist/esm/icons/plus-icon';
import LockIcon from '@patternfly/react-icons/dist/esm/icons/lock-icon';
import LockOpenIcon from '@patternfly/react-icons/dist/esm/icons/lock-open-icon';

class SliderActions extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value1: 50,
      value2: 50,
      inputValue: 50,
      isDisabled: false
    };

    this.onChange1 = value => {
      const newValue = Math.floor(Number(value));
      this.setState({
        value1: newValue
      });
    };

    this.onChange2 =(value, inputValue, setLocalInputValue) => { 
      let newValue;
      if (inputValue === undefined) { 
        newValue = Math.floor(Number(value));
      } else {
        if (inputValue > 100) {
          newValue = 100;
          setLocalInputValue(100);
        } else if (inputValue < 0) {
          newValue = 0;
          setLocalInputValue(0);
        } else {
          newValue = Math.floor(inputValue);
        }
      }
      this.setState({
        value2: newValue,
        inputValue: newValue
      });
    };

    this.onClick = () => {
      this.setState({
        isDisabled: !this.state.isDisabled
      });
    };

    this.onMinusClick = () => {
      const newValue = this.state.value1 - 1;
      if (newValue >= 0) {
        this.setState({
          value1: newValue
        });
      }
    };

    this.onPlusClick = () => {
      const newValue = this.state.value1 + 1;
      if (newValue <= 100) {
        this.setState({
          value1: newValue
        });
      }
    };
  }

  render() {
    const disabledAction = (
      <Button variant="plain" aria-label="Lock" onClick={this.onClick}>
        <LockIcon />
      </Button>
    );

    const enabledAction = (
      <Button variant="plain" aria-label="Unlock" onClick={this.onClick}>
        <LockOpenIcon />
      </Button>
    );

    return (
      <>
        <Text component={TextVariants.h3}>Slider value is: {this.state.value1}</Text>
        <Slider
          value={this.state.value1}
          onChange={this.onChange1}
          leftActions={
            <Button variant="plain" aria-label="Minus" onClick={this.onMinusClick}>
              <MinusIcon />
            </Button>
          }
          rightActions={
            <Button variant="plain" aria-label="Plus" onClick={this.onPlusClick}>
              <PlusIcon />
            </Button>
          }
        />
        <br />
        <br />
        <Slider
          value={this.state.value2}
          inputValue={this.state.inputValue}
          onChange={this.onChange2}
          inputLabel="%"
          isInputVisible
          isDisabled={this.state.isDisabled}
          rightActions={this.state.isDisabled ? disabledAction : enabledAction}
        />
      </>
    );
  }
}
```

### Disabled

```js
import React from 'react';
import { Slider, Text, TextVariants } from '@patternfly/react-core';

class DiscreteInput extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: 50
    };

    this.steps = [
      { value: 0, label: '0' },
      { value: 12.5, label: '1', isLabelHidden: true },
      { value: 25, label: '2' },
      { value: 37.5, label: '3', isLabelHidden: true },
      { value: 50, label: '4' },
      { value: 62.5, label: '5', isLabelHidden: true },
      { value: 75, label: '6' },
      { value: 87.5, label: '7', isLabelHidden: true },
      { value: 100, label: '8' }
    ];

    this.onValueChange = value => {
        this.setState({
          value
        });
    };
  }

  render() {
    const step = this.steps.find(step => step.value === this.state.value);
    const displayValue = step ? step.label : 0;
    return (
      <>
        <Text component={TextVariants.h3}>Slider value is: {displayValue}</Text>
        <Slider isDisabled value={this.state.value} onChange={this.onValueChange} customSteps={this.steps} />
      </>
    );
  }
}
```
