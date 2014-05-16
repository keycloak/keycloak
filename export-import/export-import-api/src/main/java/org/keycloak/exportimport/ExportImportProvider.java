package org.keycloak.exportimport;

import org.keycloak.provider.ProviderSessionFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ExportImportProvider {

    void checkExportImport(ProviderSessionFactory identitySessionFactory);

}
