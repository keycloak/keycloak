import React from 'react';
import { Checkbox } from '@patternfly/react-core';

export const CheckboxControlled: React.FunctionComponent = () => {
  const [isChecked1, setIsChecked1] = React.useState<boolean>(false);
  const [isChecked2, setIsChecked2] = React.useState<boolean>(false);
  const [isChecked3, setIsChecked3] = React.useState<boolean>(false);
  const [isChecked4, setIsChecked4] = React.useState<boolean>(false);

  const handleChange = (checked: boolean, event: React.FormEvent<HTMLInputElement>) => {
    const target = event.currentTarget;
    const name = target.name;

    switch (name) {
      case 'check1':
        setIsChecked1(checked);
        break;
      case 'check2':
        setIsChecked2(checked);
        break;
      case 'check3':
        setIsChecked3(checked);
        break;
      case 'check4':
        setIsChecked4(checked);
        break;
      default:
        // eslint-disable-next-line no-console
        console.log(name);
    }
  };

  React.useEffect(() => {
    if (isChecked1 !== null) {
      setIsChecked2(isChecked1);
      setIsChecked3(isChecked1);
    }
  }, [isChecked1]);

  React.useEffect(() => {
    setIsChecked1((isChecked2 && isChecked3) || (isChecked2 || isChecked3 ? null : false));
  }, [isChecked2, isChecked3]);

  return (
    <React.Fragment>
      <Checkbox
        label="Parent CheckBox"
        isChecked={isChecked1}
        onChange={handleChange}
        id="controlled-check-1"
        name="check1"
      />
      <Checkbox
        className="nested"
        label="Child CheckBox 1"
        isChecked={isChecked2}
        onChange={handleChange}
        id="controlled-check-2"
        name="check2"
      />
      <Checkbox
        className="nested"
        label="Child CheckBox 2"
        isChecked={isChecked3}
        onChange={handleChange}
        id="controlled-check-3"
        name="check3"
      />
      <Checkbox
        label="Controlled CheckBox"
        isChecked={isChecked4}
        onChange={handleChange}
        id="controlled-check-4"
        name="check4"
      />
    </React.Fragment>
  );
};
