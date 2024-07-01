import OrganizationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationRepresentation";
import {
  HelpItem,
  TextAreaControl,
  TextControl,
} from "@keycloak/keycloak-ui-shared";
import { FormGroup } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { AttributeForm } from "../components/key-value-form/AttributeForm";
import { MultiLineInput } from "../components/multi-line-input/MultiLineInput";
import { keyValueToArray } from "../components/key-value-form/key-value-convert";
import { useParams } from "react-router-dom";
import { EditOrganizationParams } from "./routes/EditOrganization";
import { useFormContext, useWatch } from "react-hook-form";
import { useEffect } from "react";

export type OrganizationFormType = AttributeForm &
  Omit<OrganizationRepresentation, "domains" | "attributes"> & {
    domains?: string[];
  };

export const convertToOrg = (
  org: OrganizationFormType,
): OrganizationRepresentation => ({
  ...org,
  domains: org.domains?.map((d) => ({ name: d, verified: false })),
  attributes: keyValueToArray(org.attributes),
});

export const OrganizationForm = () => {
  const { t } = useTranslation();
  const { tab } = useParams<EditOrganizationParams>();
  const { setValue, getFieldState } = useFormContext();
  const name = useWatch({ name: "name" });
  const isEditable = tab !== "settings";

  useEffect(() => {
    const { isDirty } = getFieldState("alias");

    if (isEditable && !isDirty) {
      setValue("alias", name);
    }
  }, [name, isEditable]);

  return (
    <>
      <TextControl
        label={t("name")}
        name="name"
        rules={{ required: t("required") }}
      />
      <TextControl
        label={t("alias")}
        name="alias"
        labelIcon={t("organizationAliasHelp")}
        isDisabled={!isEditable}
      />
      <FormGroup
        label={t("domain")}
        fieldId="domain"
        labelIcon={
          <HelpItem
            helpText={t("organizationDomainHelp")}
            fieldLabelId="domain"
          />
        }
      >
        <MultiLineInput
          id="domain"
          name="domains"
          aria-label={t("domain")}
          addButtonLabel="addDomain"
        />
      </FormGroup>
      <TextAreaControl name="description" label={t("description")} />
    </>
  );
};
