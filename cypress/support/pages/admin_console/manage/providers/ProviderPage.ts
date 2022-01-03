export default class ProviderPage {
  // KerberosSettingsRequired required input values
  private kerberosNameInput = "data-testid=kerberos-name";
  private kerberosRealmInput = "data-testid=kerberos-realm";
  private kerberosPrincipalInput = "data-testid=kerberos-principal";
  private kerberosKeytabInput = "data-testid=kerberos-keytab";

  // LdapSettingsGeneral required input values
  private ldapNameInput = "data-testid=ldap-name";
  private ldapVendorInput = "#kc-vendor";
  private ldapVendorList = "#kc-vendor + ul";

  // LdapSettingsConnection required input values
  private ldapConnectionUrlInput = "data-testid=ldap-connection-url";
  private ldapBindTypeInput = "#kc-bind-type";
  private ldapBindTypeList = "#kc-bind-type + ul";
  private ldapBindDnInput = "data-testid=ldap-bind-dn";
  private ldapBindCredsInput = "data-testid=ldap-bind-credentials";

  // LdapSettingsSearching required input values
  private ldapEditModeInput = "#kc-edit-mode";
  private ldapEditModeList = "#kc-edit-mode + ul";
  private ldapUsersDnInput = "data-testid=ldap-users-dn";
  private ldapUserLdapAttInput = "data-testid=ldap-username-attribute";
  private ldapRdnLdapAttInput = "data-testid=ldap-rdn-attribute";
  private ldapUuidLdapAttInput = "data-testid=ldap-uuid-attribute";
  private ldapUserObjClassesInput = "data-testid=ldap-user-object-classes";

  // SettingsCache input values
  private cacheDayInput = "#kc-eviction-day";
  private cacheDayList = "#kc-eviction-day + ul";
  private cacheHourInput = "#kc-eviction-hour";
  private cacheHourList = "#kc-eviction-hour + ul";
  private cacheMinuteInput = "#kc-eviction-minute";
  private cacheMinuteList = "#kc-eviction-minute + ul";
  private cachePolicyInput = "#kc-cache-policy";
  private cachePolicyList = "#kc-cache-policy + ul";

  // Mapper required input values
  private userModelAttInput = "user.model.attribute";
  private ldapAttInput = "ldap.attribute";
  private userModelAttNameInput = "user.model.attribute";
  private attValueInput = "attribute.value";
  private ldapFullNameAttInput = "ldap.full.name.attribute";
  private ldapAttNameInput = "ldap.attribute.name";
  private ldapAttValueInput = "ldap.attribute.value";
  private groupInput = "group";
  private ldapGroupsDnInput = "groups.dn";
  private ldapRolesDnInput = "roles.dn";

  // mapper types
  private msadUserAcctMapper = "msad-user-account-control-mapper";
  private msadLdsUserAcctMapper = "msad-lds-user-account-control-mapper";
  private userAttLdapMapper = "user-attribute-ldap-mapper";
  private hcAttMapper = "hardcoded-attribute-mapper";
  private certLdapMapper = "certificate-ldap-mapper";
  private fullNameLdapMapper = "full-name-ldap-mapper";
  private hcLdapAttMapper = "hardcoded-ldap-attribute-mapper";
  private hcLdapGroupMapper = "hardcoded-ldap-group-mapper";
  private groupLdapMapper = "group-ldap-mapper";
  private roleLdapMapper = "role-ldap-mapper";
  private hcLdapRoleMapper = "hardcoded-ldap-role-mapper";

  private rolesTab = "#pf-tab-roles-roles";
  private createRoleBtn = "data-testid=no-roles-for-this-client-empty-action";
  private realmRolesSaveBtn = "data-testid=realm-roles-save-button";
  private roleNameField = "#kc-name";
  private clientIdSelect = "#client\\.id-select-typeahead";

  private groupName = "aa-uf-mappers-group";
  private clientName = "aa-uf-mappers-client";

  changeCacheTime(unit: string, time: string) {
    switch (unit) {
      case "day":
        cy.get(this.cacheDayInput).click();
        cy.get(this.cacheDayList).contains(time).click();
        break;
      case "hour":
        cy.get(this.cacheHourInput).click();
        cy.get(this.cacheHourList).contains(time).click();
        break;
      case "minute":
        cy.get(this.cacheMinuteInput).click();
        cy.get(this.cacheMinuteList).contains(time).click();
        break;
      default:
        console.log("Invalid cache time, must be 'day', 'hour', or 'minute'.");
        break;
    }
    return this;
  }

  verifyChangedHourInput(expected: string, unexpected: string) {
    expect(cy.get(this.cacheHourInput).contains(expected).should("exist"));
    expect(
      cy.get(this.cacheHourInput).contains(unexpected).should("not.exist")
    );
    return this;
  }

  deleteCardFromCard(card: string) {
    cy.get(`[data-testid=${card}-dropdown]`).click();
    cy.get('[data-testid="card-delete"]').click();
    return this;
  }

  deleteCardFromMenu(providerType: string, card: string) {
    this.clickExistingCard(card);
    cy.get('[data-testid="action-dropdown"]').click();
    cy.get(`[data-testid="delete-${providerType}-cmd"]`).click();
    return this;
  }

  fillKerberosRequiredData(
    name: string,
    realm: string,
    principal: string,
    keytab: string
  ) {
    if (name) {
      cy.get(`[${this.kerberosNameInput}]`).type(name);
    }
    if (realm) {
      cy.get(`[${this.kerberosRealmInput}]`).type(realm);
    }
    if (principal) {
      cy.get(`[${this.kerberosPrincipalInput}]`).type(principal);
    }
    if (keytab) {
      cy.get(`[${this.kerberosKeytabInput}]`).type(keytab);
    }
    return this;
  }

  fillLdapRequiredGeneralData(name: string, vendor: string) {
    if (name) {
      cy.get(`[${this.ldapNameInput}]`).clear().type(name);
    }
    if (vendor) {
      cy.get(this.ldapVendorInput).click();
      cy.get(this.ldapVendorList).contains(vendor).click();
    }
    return this;
  }

  fillLdapRequiredConnectionData(
    connectionUrl: string,
    bindType: string,
    bindDn?: string,
    bindCreds?: string
  ) {
    if (connectionUrl) {
      cy.get(`[${this.ldapConnectionUrlInput}]`).type(connectionUrl);
    }
    if (bindType) {
      cy.get(this.ldapBindTypeInput).click();
      cy.get(this.ldapBindTypeList).contains(bindType).click();
    }
    if (bindDn) {
      cy.get(`[${this.ldapBindDnInput}]`).type(bindDn);
    }
    if (bindCreds) {
      cy.get(`[${this.ldapBindCredsInput}]`).type(bindCreds);
    }
    return this;
  }

  fillLdapRequiredSearchingData(
    editMode: string,
    usersDn: string,
    userLdapAtt: string,
    rdnLdapAtt: string,
    uuidLdapAtt: string,
    userObjClasses: string
  ) {
    if (editMode) {
      cy.get(this.ldapEditModeInput).click();
      cy.get(this.ldapEditModeList).contains(editMode).click();
    }

    if (usersDn) {
      cy.get(`[${this.ldapUsersDnInput}]`).type(usersDn);
    }
    if (userLdapAtt) {
      cy.get(`[${this.ldapUserLdapAttInput}]`).type(userLdapAtt);
    }
    if (rdnLdapAtt) {
      cy.get(`[${this.ldapRdnLdapAttInput}]`).type(rdnLdapAtt);
    }
    if (uuidLdapAtt) {
      cy.get(`[${this.ldapUuidLdapAttInput}]`).type(uuidLdapAtt);
    }
    if (userObjClasses) {
      cy.get(`[${this.ldapUserObjClassesInput}]`).type(userObjClasses);
    }
    return this;
  }

  selectCacheType(cacheType: string) {
    cy.get(this.cachePolicyInput).click();
    cy.get(this.cachePolicyList).contains(cacheType).click();
    return this;
  }

  goToMappers() {
    cy.get(`[data-testid="ldap-mappers-tab"]`).click();
  }

  createRole(roleName: string) {
    cy.get(this.rolesTab).click();
    cy.wait(1000);
    cy.get(`[${this.createRoleBtn}]`).click();
    cy.wait(1000);
    cy.get(this.roleNameField).type(roleName);
    cy.wait(1000);
    cy.get(`[${this.realmRolesSaveBtn}]`).click();
    cy.wait(1000);
  }

  createNewMapper(mapperType: string) {
    const userModelAttValue = "firstName";
    const ldapAttValue = "cn";
    const ldapDnValue = "ou=groups";

    cy.contains("Add").click();
    cy.wait(1000);

    cy.get("#kc-providerId").click();
    cy.get("button").contains(mapperType).click();

    cy.get(`[data-testid="ldap-mapper-name"]`).type(`${mapperType}-test`);

    switch (mapperType) {
      case this.msadUserAcctMapper:
      case this.msadLdsUserAcctMapper:
        break;
      case this.userAttLdapMapper:
      case this.certLdapMapper:
        cy.findByTestId(this.userModelAttInput).type(userModelAttValue);
        cy.findByTestId(this.ldapAttInput).type(ldapAttValue);
        break;
      case this.hcAttMapper:
        cy.findByTestId(this.userModelAttNameInput).type(userModelAttValue);
        cy.findByTestId(this.attValueInput).type(ldapAttValue);
        break;
      case this.fullNameLdapMapper:
        cy.findByTestId(this.ldapFullNameAttInput).type(ldapAttValue);
        break;
      case this.hcLdapAttMapper:
        cy.findByTestId(this.ldapAttNameInput).type(userModelAttValue);
        cy.findByTestId(this.ldapAttValueInput).type(ldapAttValue);
        break;
      case this.hcLdapGroupMapper:
        cy.findByTestId(this.groupInput).type(this.groupName);
        break;
      case this.groupLdapMapper:
        cy.findByTestId(this.ldapGroupsDnInput).type(ldapDnValue);
        break;

      case this.roleLdapMapper:
        cy.findByTestId(this.ldapRolesDnInput).type(ldapDnValue);
        // cy select clientID dropdown and choose clientName (var)
        cy.get(this.clientIdSelect).click();
        cy.get("button").contains(this.clientName).click({ force: true });
        break;

      case this.hcLdapRoleMapper:
        cy.get("#group-role-select-typeahead")
          .click()
          .get(".pf-c-select__menu-item")
          .first()
          .click();
        cy.get("#role-role-select-typeahead")
          .click()
          .get(".pf-c-select__menu-item")
          .first()
          .click();

        break;
      default:
        console.log("Invalid mapper type.");
        break;
    }
  }

  updateMapper(mapperType: string) {
    const userModelAttValue = "lastName";
    const ldapAttValue = "sn";

    switch (mapperType) {
      case this.msadUserAcctMapper:
      case this.msadLdsUserAcctMapper:
        break;
      case this.userAttLdapMapper:
      case this.certLdapMapper:
        cy.findByTestId(this.userModelAttInput).clear().type(userModelAttValue);
        cy.findByTestId(this.ldapAttInput).clear().type(ldapAttValue);
        break;
      case this.hcAttMapper:
        cy.findByTestId(this.userModelAttNameInput)
          .clear()
          .type(userModelAttValue);
        cy.findByTestId(this.attValueInput).clear().type(ldapAttValue);
        break;
      case this.fullNameLdapMapper:
        cy.findByTestId(this.ldapFullNameAttInput).clear().type(ldapAttValue);
        break;
      case this.hcLdapAttMapper:
        cy.findByTestId(this.ldapAttNameInput).clear().type(userModelAttValue);
        cy.findByTestId(this.ldapAttValueInput).clear().type(ldapAttValue);
        break;
      default:
        console.log("Invalid mapper name.");
        break;
    }
  }

  clickExistingCard(cardName: string) {
    cy.get('[data-testid="keycloak-card-title"]').contains(cardName).click();
    cy.wait(1000);
    return this;
  }

  clickMenuCommand(menu: string, command: string) {
    cy.contains("button", menu).click();
    cy.contains("li", command).click();
    return this;
  }

  clickNewCard(providerType: string) {
    cy.get(`[data-testid=${providerType}-card]`).click();
    cy.wait(1000);
    return this;
  }

  disableEnabledSwitch(providerType: string) {
    cy.get(`#${providerType}-switch`).uncheck({ force: true });
    return this;
  }

  enableEnabledSwitch(providerType: string) {
    cy.get(`#${providerType}-switch`).check({ force: true });
    return this;
  }

  save(providerType: string) {
    cy.get(`[data-testid=${providerType}-save]`).click();
    return this;
  }

  cancel(providerType: string) {
    cy.get(`[data-testid=${providerType}-cancel]`).click();
    return this;
  }
}
