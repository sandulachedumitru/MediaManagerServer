package com.miti.photos_manager_server.config;

import com.miti.photos_manager_server.model.FileOperation;
import com.miti.photos_manager_server.model.ScanRequestDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.text.MessageFormat.format;

/**
 * @author Dumitru Săndulache (sandulachedumitru@hotmail.com)
 */

@Configuration
@ConfigurationProperties(prefix = "media-manager")
@Setter
@Getter
@ToString
@NoArgsConstructor
public class MediaManagerConfig {
    private FileOperation operation;

    private String basePath;
    private String scanPath;

    private String imageNewPath;
    private String imageDuplicatesPath;
    private boolean imageEnabled;

    private String audioNewPath;
    private String audioDuplicatesPath;
    private boolean audioEnabled;

    private String containerNewPath;
    private String containerDuplicatesPath;
    private boolean containerEnabled;

    private String archiveNewPath;
    private String archiveDuplicatesPath;
    private boolean archiveEnabled;

    private List<String> extensionsPhotoVideo;
    private List<String> extensionsAudio;
    private List<String> extensionsContainer;
    private List<String> extensionsArchive;

    private final static String PHOTO_VIDEO_DIRECTORY = "photo_video";
    private final static String AUDIO_DIRECTORY = "audio";
    private final static String CONTAINER_DIRECTORY = "container";
    private final static String ARCHIVE_DIRECTORY = "archive";

    private final static String ORGANIZED_DIRECTORY = "organized";
    private final static String DUPLICATES_DIRECTORY = "duplicates";

    public void config(ScanRequestDto scanRequestDto) {
        Path scanDirectoryPath = Paths.get(scanRequestDto.scanDirectory());
        Path parentPath = scanDirectoryPath.getParent();

        this.operation = scanRequestDto.operation();

        this.basePath = parentPath != null ? parentPath.toString() : "";
        this.scanPath = scanRequestDto.scanDirectory();

        this.imageNewPath = format("{0}/{1}/{2}", basePath, PHOTO_VIDEO_DIRECTORY, ORGANIZED_DIRECTORY);
        this.imageDuplicatesPath = format("{0}/{1}/{2}", basePath, PHOTO_VIDEO_DIRECTORY, DUPLICATES_DIRECTORY);
        this.imageEnabled = scanRequestDto.imageEnabled();

        this.audioNewPath = format("{0}/{1}/{2}", basePath, AUDIO_DIRECTORY, ORGANIZED_DIRECTORY);
        this.audioDuplicatesPath = format("{0}/{1}/{2}", basePath, AUDIO_DIRECTORY, DUPLICATES_DIRECTORY);
        this.audioEnabled = scanRequestDto.audioEnabled();

        this.containerNewPath = format("{0}/{1}/{2}", basePath, CONTAINER_DIRECTORY, ORGANIZED_DIRECTORY);
        this.containerDuplicatesPath = format("{0}/{1}/{2}", basePath, CONTAINER_DIRECTORY, DUPLICATES_DIRECTORY);
        this.containerEnabled = scanRequestDto.containerEnabled();

        this.archiveNewPath = format("{0}/{1}/{2}", basePath, ARCHIVE_DIRECTORY, ORGANIZED_DIRECTORY);
        this.archiveDuplicatesPath = format("{0}/{1}/{2}", basePath, ARCHIVE_DIRECTORY, DUPLICATES_DIRECTORY);
        this.archiveEnabled = scanRequestDto.archiveEnabled();
    }
}
