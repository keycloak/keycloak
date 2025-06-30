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
  return (
    <>
      <TextControl
        label={t("name")}
        name="name"
        rules={{ required: t("required") }}
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
