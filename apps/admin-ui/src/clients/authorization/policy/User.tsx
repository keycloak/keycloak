import { useTranslation } from "react-i18next";
import { UserSelect } from "../../../components/users/UserSelect";

export const User = () => {
  const { t } = useTranslation();
  return (
    <UserSelect
      name="users"
      label="users"
      helpText={t("clients-help:policyUsers")}
      defaultValue={[]}
      isRequired
    />
  );
};
