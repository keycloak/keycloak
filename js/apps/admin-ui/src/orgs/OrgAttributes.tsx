import { useEffect } from "react";
import {
  AttributeForm,
  AttributesForm,
} from "../components/key-value-form-custom/AttributeForm";
import { OrgRepresentation } from "./routes";
import { useForm } from "react-hook-form";
import { arrayToKeyValue } from "../components/key-value-form/key-value-convert";
import type { KeyValueType } from "../components/key-value-form/key-value-convert";
import useOrgFetcher from "./useOrgFetcher";
import { useRealm } from "../context/realm-context/RealmContext";
import { useAlerts } from "../components/alert/Alerts";

type OrgAttributesProps = {
  org: OrgRepresentation;
};

type AttributesForm = {
  attributes?: KeyValueType[];
};

export default function OrgAttributes({ org }: OrgAttributesProps) {
  const { addAlert } = useAlerts();
  const { realm } = useRealm();
  const { updateOrg } = useOrgFetcher(realm);

  useEffect(() => {
    if (org.attributes) {
      const attributes = convertAttributes();
      attributesForm.setValue("attributes", attributes);
    }
  }, [org]);

  const attributesForm = useForm<AttributeForm>({
    mode: "onChange",
    shouldUnregister: false,
  });

  const convertAttributes = (attr?: Record<string, any>) => {
    return arrayToKeyValue(attr || org.attributes);
  };

  async function saveAttributes(data: any) {
    if (org) {
      const attributes: any = {};
      data.attributes.forEach((item: any) => {
        attributes[item.key] = [item.value];
      });
      const updatedData: OrgRepresentation = { ...org, attributes };
      await updateOrg(updatedData);
      addAlert("Attributes updated for organization");
    }
  }

  function resetAttributes() {
    console.log("Implement attributes reset");
  }

  return (
    <div className="pf-u-pt-lg">
      <AttributesForm
        form={attributesForm}
        save={saveAttributes}
        reset={resetAttributes}
        allowFullClear
      />
    </div>
  );
}
