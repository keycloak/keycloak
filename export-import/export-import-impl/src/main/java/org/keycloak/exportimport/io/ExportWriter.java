package org.keycloak.exportimport.io;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ExportWriter {

    <T> void writeEntities(String fileName, List<T> entities);

    void closeExportWriter();
}
