import * as React from 'react';

import { Select, SelectOption, SelectVariant, SelectOptionObject } from '@patternfly/react-core';
import { Scope } from './resource-model';
import { Msg } from '../../widgets/Msg';

interface PermissionSelectState {
  selected: ScopeValue[];
  isExpanded: boolean;
  scopes: JSX.Element[];
}

interface PermissionSelectProps {
  scopes: Scope[];
  selected?: Scope[];
  direction?: 'up' | 'down';
  onSelect: (selected: Scope[]) => void;
}

class ScopeValue implements SelectOptionObject {
  value: Scope;
  constructor(value: Scope) {
    this.value = value;
  }

  toString() {
    return this.value.displayName ? this.value.displayName : this.value.name;
  }

  compareTo(selectOption: Scope): boolean {
    return selectOption.name === this.value.name;
  }
}

export class PermissionSelect extends React.Component<PermissionSelectProps, PermissionSelectState> {
  constructor(props: PermissionSelectProps) {
    super(props);

    let values: ScopeValue[] = [];
    if (this.props.selected) {
      values = this.props.selected!.map(s => new ScopeValue(s))
    }

    this.state = {
      isExpanded: false,
      selected: values,
      scopes: this.props.scopes.map((option, index) => (
        <SelectOption key={index} value={values.find(s => s.compareTo(option)) || new ScopeValue(option)} />
      ))
    };
  }

  private onSelect = (event: React.MouseEvent | React.ChangeEvent, value: string | SelectOptionObject): void => {
    const { selected } = this.state;
    const { onSelect } = this.props;

    if (!(value instanceof ScopeValue)) {
      return;
    }

    if (selected.includes(value)) {
      this.setState(
        prevState => ({ selected: prevState.selected.filter(item => item !== value) }),
        () => onSelect(this.state.selected.map(sv => sv.value))
      );
    } else {
      this.setState(
        prevState => ({ selected: [...prevState.selected, value] }),
        () => onSelect(this.state.selected.map(sv => sv.value))
      );
    }
  }

  private onToggle = (isExpanded: boolean) => {
    this.setState({
      isExpanded
    });
  }

  private clearSelection = () => {
    this.setState({
      selected: [],
      isExpanded: false
    });
    this.props.onSelect([]);
  };

  render() {
    const { isExpanded, selected } = this.state;
    const titleId = 'permission-id';

    return (
      <div>
        <span id={titleId} hidden>
          <Msg msgKey='selectPermissions' />
        </span>
        <Select
          maxHeight={300}
          direction={this.props.direction || 'down'}
          variant={SelectVariant.typeaheadMulti}
          typeAheadAriaLabel={Msg.localize("selectPermissions")}
          onToggle={this.onToggle}
          onSelect={this.onSelect}
          onClear={this.clearSelection}
          selections={selected}
          isOpen={isExpanded}
          aria-labelledby={titleId}
          placeholderText={Msg.localize("selectPermissions")}
          menuAppendTo="parent"
        >
          {this.state.scopes}
        </Select>
      </div>
    );
  }
}
