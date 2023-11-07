import { useTranslation } from "react-i18next";
import { MultiLineInput } from "../../components/multi-line-input/MultiLineInput";
import { UserProfileFieldProps } from "../UserProfileFields";
import { fieldName, labelAttribute } from "../utils";
import { UserProfileGroup } from "./UserProfileGroup";

export const MultiInputComponent = ({
  form,
  attribute,
}: UserProfileFieldProps) => {
  const { t } = useTranslation();

  return (
    <UserProfileGroup form={form} attribute={attribute}>
      <MultiLineInput
        aria-label={labelAttribute(attribute, t)}
        name={fieldName(attribute)!}
        addButtonLabel={t("addMultivaluedLabel", {
          fieldLabel: labelAttribute(attribute, t),
        })}
      />
    </UserProfileGroup>
  );
};
