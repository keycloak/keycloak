package org.keycloak.exportimport.io;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ExportImportIOProvider {

    String getId();

    ExportWriter getExportWriter();

    ImportReader getImportReader();

}
