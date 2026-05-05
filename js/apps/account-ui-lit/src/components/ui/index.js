// UI Component Library
// Import all components for side-effect registration

import "./kc-button.js";
import "./kc-input.js";
import "./kc-select.js";
import "./kc-dropdown.js";
import "./kc-spinner.js";
import "./kc-alert.js";
import "./kc-empty-state.js";

// Re-export classes for type checking if needed
export { KcButton } from "./kc-button.js";
export { KcTextInput, KcTextarea, KcFormGroup } from "./kc-input.js";
export {
  KcSelect,
  KcRadioGroup,
  KcCheckboxGroup,
  KcCheckbox,
} from "./kc-select.js";
export { KcDropdown } from "./kc-dropdown.js";
export { KcSpinner } from "./kc-spinner.js";
export { KcAlert } from "./kc-alert.js";
export { KcEmptyState } from "./kc-empty-state.js";
