import {
  HelpItem,
  TextAreaControl,
  TextControl,
} from "@keycloak/keycloak-ui-shared";
import { FormGroup } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";
import { FormAccess } from "../components/form/FormAccess";
import { MultiLineInput } from "../components/multi-line-input/MultiLineInput";
import { useTranslation } from "react-i18next";

type OrganizationFormProps = {
  save: (org: any) => void;
};

export const OrganizationForm = ({ save }: OrganizationFormProps) => {
  const { t } = useTranslation();
  const form = useFormContext();
  return (
    <FormAccess
      isHorizontal
      role="manage-users"
      onSubmit={form.handleSubmit(save)}
    >
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
          name="domain"
          aria-label={t("domain")}
          addButtonLabel="addDomain"
        />
      </FormGroup>
      <TextAreaControl name="description" label={t("description")} />
    </FormAccess>
  );
};
