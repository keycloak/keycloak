import { Form } from "@patternfly/react-core";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { fetchPersonalInfo } from "../api";
import { TextControl } from "../components/controls/TextControl";
import { Page } from "../components/page/Page";
import { UserRepresentation } from "../representations";
import { usePromise } from "../utils/usePromise";

const PersonalInfo = () => {
  const { t } = useTranslation();
  const { control, reset } = useForm<UserRepresentation>({
    mode: "onChange",
  });

  usePromise((signal) => fetchPersonalInfo({ signal }), reset);

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
