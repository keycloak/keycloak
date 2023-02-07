import { Form } from "@patternfly/react-core";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { TextControl } from "ui-shared";
import { getPersonalInfo } from "../api/methods";
import { UserRepresentation } from "../api/representations";
import { Page } from "../components/page/Page";
import { usePromise } from "../utils/usePromise";

const PersonalInfo = () => {
  const { t } = useTranslation();
  const { control, reset } = useForm<UserRepresentation>({
    mode: "onChange",
  });

  usePromise((signal) => getPersonalInfo({ signal }), reset);

  return (
    <Page title={t("personalInfo")} description={t("personalInfoDescription")}>
      <Form isHorizontal>
        <TextControl
          control={control}
          name="username"
          rules={{ maxLength: 254, required: true }}
          label={t("username")}
        />
        <TextControl
          control={control}
          name="firstName"
          label={t("firstName")}
        />
        <TextControl control={control} name="lastName" label={t("lastName")} />
      </Form>
    </Page>
  );
};

export default PersonalInfo;
