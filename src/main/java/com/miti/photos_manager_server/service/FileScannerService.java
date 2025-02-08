package com.miti.photos_manager_server.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Dumitru Săndulache (sandulachedumitru@hotmail.com)
 */

public interface FileScannerService {
    void scanAndOrganizeFiles(String scanDirectory) throws IOException;
    Map<String, List<String>> getProcessedFiles();
}
