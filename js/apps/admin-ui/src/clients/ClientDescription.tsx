import { useTranslation } from "react-i18next";
import { TextControl, TextAreaControl } from "ui-shared";

import { FormAccess } from "../components/form/FormAccess";
import { DefaultSwitchControl } from "../components/SwitchControl";

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
        label={t("common:clientId")}
        labelIcon={t("clientIdHelp")}
        rules={{ required: { value: true, message: t("common:required") } }}
      />
      <TextControl
        name="name"
        label={t("common:name")}
        labelIcon={t("clientNameHelp")}
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
        labelIcon={t("alwaysDisplayInUIHelp")}
      />
    </FormAccess>
  );
};
