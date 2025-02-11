package com.miti.photos_manager_server.model;

/**
 * @author Dumitru Săndulache (sandulachedumitru@hotmail.com)
 */

public record ScanRequestDto(
        String scanDirectory,
        FileOperation operation,
        boolean imageEnabled,
        boolean audioEnabled,
        boolean containerEnabled,
        boolean archiveEnabled) { }
