import {
  Button,
  ButtonVariant,
  Form,
  Modal,
  ModalVariant,
  AlertVariant,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { FormProvider, useForm } from "react-hook-form";

import { useRealm } from "../../context/realm-context/RealmContext";
import { useAlerts } from "../../components/alert/Alerts";
import useOrgFetcher from "../useOrgFetcher";
import type { OrgRepresentation } from "../routes";
import { NewOrg } from "../form/NewOrg";

export type OrgFormType = Omit<
  OrgRepresentation,
  "id" | "attributes" | "domains"
> & { domains: string[] };

export type OrgFormSubmission = Omit<OrgRepresentation, "id" | "attributes">;

type NewOrgModalProps = {
  toggleVisibility: () => void;
  refresh: () => void;
};

export const defaultOrgState: OrgFormType = {
  name: "",
  displayName: "",
  domains: [""],
  url: "",
};

export const NewOrgModal = ({
  toggleVisibility,
  refresh,
}: NewOrgModalProps) => {
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();
  const { createOrg } = useOrgFetcher(realm);

  const form = useForm<OrgFormType>({
    defaultValues: defaultOrgState,
  });
  const { handleSubmit } = form;

  const submitForm = async (org: OrgFormType) => {
    try {
      const res = await createOrg(org);

      if (res.success) {
        addAlert(t("orgCreated"), AlertVariant.success);
        refresh();
        toggleVisibility();
      } else {
        addError("couldNotCreateOrg", new Error(res.message));
      }
    } catch (error) {
      addError("couldNotCreateOrg", error);
    }
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("createAnOrg")}
      isOpen={true}
      onClose={toggleVisibility}
      actions={[
        <Button
          data-testid="createOrg"
          key="confirm"
          variant="primary"
          type="submit"
          form="org-form"
        >
          {t("create")}
        </Button>,
        <Button
          id="modal-cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={() => {
            toggleVisibility();
          }}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <FormProvider {...form}>
        <Form id="org-form" isHorizontal onSubmit={handleSubmit(submitForm)}>
          <NewOrg />
        </Form>
      </FormProvider>
    </Modal>
  );
};
