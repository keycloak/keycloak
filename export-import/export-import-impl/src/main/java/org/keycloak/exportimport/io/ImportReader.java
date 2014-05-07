package org.keycloak.exportimport.io;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ImportReader {

    <T> List<T> readEntities(String fileName, Class<T> entityClass);

    void closeImportReader();
}
