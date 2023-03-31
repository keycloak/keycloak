import { useTranslation } from "react-i18next";
import { TextControl, TextAreaControl } from "ui-shared";

import { FormAccess } from "../components/form-access/FormAccess";
import { DefaultSwitchControl } from "../components/SwitchControl";

type ClientDescriptionProps = {
  protocol?: string;
  hasConfigureAccess?: boolean;
};

export const ClientDescription = ({
  hasConfigureAccess: configure,
}: ClientDescriptionProps) => {
  const { t } = useTranslation("clients");
  return (
    <FormAccess role="manage-clients" fineGrainedAccess={configure} unWrap>
      <TextControl
        name="clientId"
        label={t("common:clientId")}
        labelIcon={t("clients-help:clientId")}
        rules={{ required: { value: true, message: t("common:required") } }}
      />
      <TextControl
        name="name"
        label={t("common:name")}
        labelIcon={t("clients-help:clientName")}
      />
      <TextAreaControl
        name="description"
        label={t("common:description")}
        labelIcon={t("clients-help:description")}
        rules={{
          maxLength: {
            value: 255,
            message: t("common:maxLength", { length: 255 }),
          },
        }}
      />
      <DefaultSwitchControl
        name="alwaysDisplayInConsole"
        label={t("alwaysDisplayInUI")}
        labelIcon={t("clients-help:alwaysDisplayInUI")}
      />
    </FormAccess>
  );
};
