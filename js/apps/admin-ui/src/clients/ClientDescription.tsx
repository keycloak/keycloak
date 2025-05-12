import {
  HelpItem,
  TextAreaControl,
  TextControl,
} from "@keycloak/keycloak-ui-shared";
import { FormGroup } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { FormAccess } from "../components/form/FormAccess";
import { DefaultSwitchControl } from "../components/SwitchControl";
import { TranslatableField } from "../realm-settings/user-profile/attribute/TranslatableField";

type ClientDescriptionProps = {
  protocol?: string;
  hasConfigureAccess?: boolean;
};

export const ClientDescription = ({
  hasConfigureAccess: configure,
}: ClientDescriptionProps) => {
  const { t } = useTranslation();
  return (
    <FormAccess role="manage-clients" fineGrainedAccess={configure} unWrap>
      <TextControl
        name="clientId"
        label={t("clientId")}
        labelIcon={t("clientIdHelp")}
        rules={{ required: t("required") }}
      />
      <FormGroup
        label={t("name")}
        labelIcon={
          <HelpItem helpText={t("clientNameHelp")} fieldLabelId="name" />
        }
        fieldId="kc-attribute-name"
      >
        <TranslatableField
          fieldName="name"
          attributeName="clientId"
          prefix="client.name"
        />
      </FormGroup>
      <TextAreaControl
        name="description"
        label={t("description")}
        labelIcon={t("clientDescriptionHelp")}
        rules={{
          maxLength: {
            value: 255,
            message: t("maxLength", { length: 255 }),
          },
        }}
      />
      <DefaultSwitchControl
        name="alwaysDisplayInConsole"
        label={t("alwaysDisplayInUI")}
        labelIcon={t("alwaysDisplayInUIHelp")}
      />
    </FormAccess>
  );
};
