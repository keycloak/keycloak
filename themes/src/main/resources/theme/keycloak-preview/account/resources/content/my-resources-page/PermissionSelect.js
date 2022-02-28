function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from "../../../../common/keycloak/web_modules/react.js";
import { Select, SelectOption, SelectVariant } from "../../../../common/keycloak/web_modules/@patternfly/react-core.js";

class ScopeValue {
  constructor(value) {
    _defineProperty(this, "value", void 0);

    this.value = value;
  }

  toString() {
    return this.value.displayName ? this.value.displayName : this.value.name;
  }

  compareTo(selectOption) {
    return selectOption.name === this.value.name;
  }

}

export class PermissionSelect extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "onSelect", (_event, selection) => {
      const {
        selected
      } = this.state;
      const {
        onSelect
      } = this.props;

      if (selected.includes(selection)) {
        this.setState(prevState => ({
          selected: prevState.selected.filter(item => item !== selection)
        }), () => onSelect(this.state.selected.map(sv => sv.value)));
      } else {
        this.setState(prevState => ({
          selected: [...prevState.selected, selection]
        }), () => onSelect(this.state.selected.map(sv => sv.value)));
      }
    });

    _defineProperty(this, "onToggle", isExpanded => {
      this.setState({
        isExpanded
      });
    });

    _defineProperty(this, "clearSelection", () => {
      this.setState({
        selected: [],
        isExpanded: false
      });
      this.props.onSelect([]);
    });

    let values = [];

    if (this.props.selected) {
      values = this.props.selected.map(s => new ScopeValue(s));
    }

    this.state = {
      isExpanded: false,
      selected: values,
      scopes: this.props.scopes.map((option, index) => React.createElement(SelectOption, {
        key: index,
        value: values.find(s => s.compareTo(option)) || new ScopeValue(option)
      }))
    };
  }

  render() {
    const {
      isExpanded,
      selected
    } = this.state;
    const titleId = 'permission-id';
    return React.createElement("div", null, React.createElement("span", {
      id: titleId,
      hidden: true
    }, "Select the permissions"), React.createElement(Select, {
      direction: this.props.direction || 'down',
      variant: SelectVariant.typeaheadMulti,
      ariaLabelTypeAhead: "Select the permissions",
      onToggle: this.onToggle,
      onSelect: this.onSelect,
      onClear: this.clearSelection,
      selections: selected,
      isExpanded: isExpanded,
      ariaLabelledBy: titleId,
      placeholderText: "Select the permissions"
    }, this.state.scopes));
  }

}
//# sourceMappingURL=PermissionSelect.js.map