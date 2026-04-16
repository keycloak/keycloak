import ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";

import type { IndexedValidations } from "../../NewAttributeSettings";
import { AddComponentTypeDialog } from "./AddComponentTypeDialog";

export type AddValidatorDialogProps = {
  selectedValidators: IndexedValidations[];
  toggleDialog: () => void;
  onConfirm: (newValidator: ComponentRepresentation) => void;
};

export const AddValidatorDialog = ({
  selectedValidators,
  toggleDialog,
  onConfirm,
}: AddValidatorDialogProps) => (
  <AddComponentTypeDialog
    componentType="org.keycloak.validate.Validator"
    name="validator"
    titleKey="addValidator"
    emptyKey="emptyValidators"
    selectLabelKey="validatorType"
    selected={selectedValidators}
    toggleDialog={toggleDialog}
    onConfirm={onConfirm}
  />
);
