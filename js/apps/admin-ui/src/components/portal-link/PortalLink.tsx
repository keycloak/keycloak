import {
  Alert,
  AlertVariant,
  ClipboardCopy,
  Form,
  FormGroup,
  ModalVariant,
  Stack,
  StackItem,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { useRealm } from "../../context/realm-context/RealmContext";
import type { OrgParams } from "../../orgs/routes/Org";
import useOrgFetcher from "../../orgs/useOrgFetcher";
import { ConfirmDialogModal } from "../confirm-dialog/ConfirmDialog";

type PortalLinkProps = {
  id: string;
  open: boolean;
  toggleDialog: () => void;
};

export const PortalLink = ({ id, open, toggleDialog }: PortalLinkProps) => {
  const { t } = useTranslation();
  const { orgId } = useParams<OrgParams>();
  const { realm } = useRealm();
  const { getPortalLink } = useOrgFetcher(realm);
  const [portalLink, setPortalLink] = useState("");

  useEffect(() => {
    if (open) {
      getPortalLink(orgId!, "")
        .then((pl) => setPortalLink(pl?.link || ""))
        .catch((e) => console.log(e));
    }
  }, [open]);

  return (
    <ConfirmDialogModal
      titleKey={t("organizationPortal")}
      continueButtonLabel={t("Close")}
      onConfirm={() => {
        return;
      }}
      open={open}
      toggleDialog={toggleDialog}
      variant={ModalVariant.medium}
      noCancelButton
    >
      <Form>
        <Stack hasGutter>
          <StackItem>
            <Alert
              id={id}
              title={t("organizationPortalHelpTitle")}
              variant={AlertVariant.info}
              isInline
            >
              {t("portalLinkHelp")}
            </Alert>
          </StackItem>
          <StackItem>
            <FormGroup fieldId="type" label={t("organizationPortalLink")}>
              <ClipboardCopy isReadOnly>{portalLink}</ClipboardCopy>
            </FormGroup>
          </StackItem>
        </Stack>
      </Form>
    </ConfirmDialogModal>
  );
};
