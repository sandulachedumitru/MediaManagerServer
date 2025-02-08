package com.miti.photos_manager_server.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Dumitru Săndulache (sandulachedumitru@hotmail.com)
 */

@Getter
@Setter
@AllArgsConstructor
public class MediaCurrentPath {
    private String organizedPath;
    private String duplicatesPath;
    private FileType fileType;
}
