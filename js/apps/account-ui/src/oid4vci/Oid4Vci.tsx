import {
    Select,
    SelectList,
    SelectOption,
    PageSectionVariants,
    PageSection,
    ActionList,
    ActionListItem,
    List,
    ListItem,
    MenuToggleElement,
    MenuToggle
} from '@patternfly/react-core';
import { useEffect, useState, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useAlerts, useEnvironment } from "@keycloak/keycloak-ui-shared";
import { usePromise } from "../utils/usePromise";
import { Page } from "../components/page/Page";
import { CredentialsIssuer } from "../api/representations";
import { getIssuer, requestVCOffer } from "../api";

export const Oid4Vci = () => {
    const context = useEnvironment();
   
    const { t } = useTranslation();

    const initialSelected = t('verifiableCredentialsSelectionDefault') 

    const [selected, setSelected] = useState<string>(initialSelected);
    const [qrCode, setQrCode] = useState<string>("")
    const [isOpen, setIsOpen] = useState<boolean>(false)
    const [offerQRVisible, setOfferQRVisible] = useState<boolean>(false)
    const [credentialsIssuer, setCredentialsIssuer] = useState<CredentialsIssuer>()

    usePromise(() => getIssuer(context), setCredentialsIssuer);

    const selectOptions = useMemo(
      () => {
        if(typeof credentialsIssuer !== 'undefined') {
          return credentialsIssuer.credential_configurations_supported
        } 
        return {}
      },
      [credentialsIssuer],
    )

    const dropdownItems = useMemo(
      () => {
        if (typeof selectOptions !== 'undefined') {
         return Array.from(Object.keys(selectOptions))
        } 
        return []
      },
      [selectOptions],
    )

    useEffect(() => {
      if(initialSelected !== selected && credentialsIssuer !== undefined){
        requestVCOffer(context, selectOptions[selected], credentialsIssuer)
          .then((blob) => {
            var reader = new FileReader();
            reader.readAsDataURL(blob)
            reader.onloadend = function() {
              let result = reader.result
              if (typeof result === "string") {
                setQrCode(result);
                setOfferQRVisible(true);
                setIsOpen(false);
              }    
            }
          })
      }
    }, [selected]);
  
    const onToggleClick = () => {
      setIsOpen(!isOpen);
    };

    const toggle = (toggleRef: React.Ref<MenuToggleElement>) => (
        <MenuToggle
          ref={toggleRef}
          onClick={onToggleClick}
          isExpanded={isOpen}
          data-testid="menu-toggle"
        >
          {selected}
        </MenuToggle>
      );

    return (
      <Page title={t('verifiableCredentialsTitle')} description={t('verifiableCredentialsDescription')}>
        <PageSection isFilled variant={PageSectionVariants.light}>     
          <List isPlain>  
            <ListItem>
              <Select
                data-testid="credential-select"
                onOpenChange={(isOpen) => setIsOpen(isOpen)}
                onSelect={(_event, val) => setSelected(val as string)}
                isOpen={isOpen}
                selected={selected}
                toggle={toggle}
                shouldFocusToggleOnSelect={true}
              >
                <SelectList>
                  {dropdownItems.map((option, index) => (
                    <SelectOption 
                      value={option}
                      data-testid='select-${option}'
                    >
                    {option}
                    </SelectOption>
                  ))}
                </SelectList>
              </Select>
              </ListItem>  
              <ListItem>
                  <ActionList>
                  { offerQRVisible &&
                    <ActionListItem>
                        <img width='500' height='500' src={`${qrCode}`} data-testid="qr-code"/>
                    </ActionListItem>
                  }
                  </ActionList>
            </ListItem>
          </List>       
        </PageSection>   
      </Page>
    );
};


export default Oid4Vci;