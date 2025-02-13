package com.miti.photos_manager_server.service;

import com.miti.photos_manager_server.config.MediaManagerConfig;
import com.miti.photos_manager_server.model.FileType;
import com.miti.photos_manager_server.model.MediaCurrentPath;
import com.miti.photos_manager_server.model.ScanRequestDto;
import com.miti.photos_manager_server.utils.HashUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author Dumitru Săndulache (sandulachedumitru@hotmail.com)
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class FileScannerServiceImpl implements FileScannerService {

    private final MediaManagerConfig config;
    private final Map<String, Path> fileHashes = new ConcurrentHashMap<>();
    private final ProgressServiceSSE progressServiceSSE;
    private volatile boolean isAborted = false;

    private static final List<String> organizedPhotoVideoFiles = new ArrayList<>();
    private static final List<String> duplicatedPhotoVideoFiles = new ArrayList<>();
    private static final List<String> organizedAudioFiles = new ArrayList<>();
    private static final List<String> duplicatedAudioFiles = new ArrayList<>();
    private static final List<String> organizedContainerFiles = new ArrayList<>();
    private static final List<String> duplicatedContainerFiles = new ArrayList<>();
    private static final List<String> organizedArchiveFiles = new ArrayList<>();
    private static final List<String> duplicatedArchiveFiles = new ArrayList<>();

    @Override
    public void scanAndOrganizeFiles(ScanRequestDto requestDto) throws IOException {
        config.config(requestDto);

        log.info("Starting scan of directory: {}", config.getScanPath());

        isAborted = false;
        fileHashes.clear();
        organizedPhotoVideoFiles.clear();
        duplicatedPhotoVideoFiles.clear();
        organizedAudioFiles.clear();
        duplicatedAudioFiles.clear();
        organizedContainerFiles.clear();
        duplicatedContainerFiles.clear();
        organizedArchiveFiles.clear();
        duplicatedArchiveFiles.clear();

        long startTime = System.nanoTime();

        var totalDirectoriesAndFiles = getTotalDirectoriesAndFilesToScan(config.getScanPath());
        AtomicLong scannedFiles = new AtomicLong(0L);
        AtomicLong processedFiles = new AtomicLong(0L);
        AtomicLong combinedProgress = new AtomicLong(0L);

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() -1);
        try {
            Files.walkFileTree(Paths.get(config.getScanPath()),
                    new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (isAborted) {
                                log.error("Aborting scan: waiting for all threads to stop...");
                                return FileVisitResult.TERMINATE;
                            }
                            if (Files.isRegularFile(file)) {
                                scannedFiles.incrementAndGet();

                                getFileType(file).ifPresent(currentPath -> executorService.submit(() -> {
                                    if (isAborted) {
                                        log.error("Thread Aborting scan !");
                                        return;
                                    }

                                    long fileStartTime = System.nanoTime();

                                    processFile(file, currentPath);

                                    long fileEndTime = System.nanoTime();
                                    long fileDuration = (fileEndTime - fileStartTime) / 1_000_000; // ms

                                    processedFiles.incrementAndGet();
                                    long newProgress = (scannedFiles.get() + processedFiles.get()) * 100 / (2 * totalDirectoriesAndFiles.filesCount);
                                    if (newProgress != combinedProgress.get()) {
                                        combinedProgress.set(newProgress);
                                        progressServiceSSE.sendProgressUpdate(newProgress);
                                    }

                                    log.info("Processed {} in {} ms -> {}%", file.getFileName(), fileDuration, newProgress);

                                }));
                            }

                            return FileVisitResult.CONTINUE;
                        }
                    });

        } catch (IOException e) {
            log.error("Error scanning files: ", e);
            Thread.currentThread().interrupt();
        } finally {
            try {
                executorService.shutdown();
                if (!executorService.awaitTermination(1, TimeUnit.DAYS)) {
                    log.warn("Forcing shutdown as tasks did not finish in time");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.error("Executor termination interrupted", e);
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        long endTime = System.nanoTime();
        long totalDuration = (endTime - startTime) / 1_000_000; // ms
        combinedProgress.set(100);
        progressServiceSSE.sendProgressUpdate(combinedProgress.get());
        log.info("Completed processing {} files in {} -> {}%", processedFiles.get(), formatMilliseconds(totalDuration), combinedProgress.get());
    }

    @Override
    public void abortScan() {
        log.error("ABORTING SCAN !");
        isAborted = true;
    }

    @Override
    public Map<String, List<String>> getProcessedFiles() {
        List<String> organized = new ArrayList<>(organizedPhotoVideoFiles);
        List<String> duplicated = new ArrayList<>(duplicatedPhotoVideoFiles);
        organized.addAll(organizedAudioFiles); duplicated.addAll(duplicatedAudioFiles);
        organized.addAll(organizedContainerFiles); duplicated.addAll(duplicatedContainerFiles);
        organized.addAll(organizedArchiveFiles); duplicated.addAll(duplicatedArchiveFiles);
        return Map.of("organized", organized, "duplicates", duplicated);
    }

    private Optional<MediaCurrentPath> getFileType(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();

        if (config.isImageEnabled() && config.getExtensionsPhotoVideo().stream().anyMatch(fileName::endsWith)) {
            return Optional.of(new MediaCurrentPath(config.getImageNewPath(), config.getImageDuplicatesPath(), FileType.PHOTO_VIDEO));
        } else if (config.isAudioEnabled() && config.getExtensionsAudio().stream().anyMatch(fileName::endsWith)) {
            return Optional.of(new MediaCurrentPath(config.getAudioNewPath(), config.getAudioDuplicatesPath(), FileType.AUDIO));
        } else if (config.isContainerEnabled() && config.getExtensionsContainer().stream().anyMatch(fileName::endsWith)) {
            return Optional.of(new MediaCurrentPath(config.getContainerNewPath(), config.getContainerDuplicatesPath(), FileType.CONTAINER));
        } else if (config.isArchiveEnabled() && config.getExtensionsArchive().stream().anyMatch(fileName::endsWith)) {
            return Optional.of(new MediaCurrentPath(config.getArchiveNewPath(), config.getArchiveDuplicatesPath(), FileType.ARCHIVE));
        } else {
            return Optional.empty();
        }
    }

    private void processFile(Path file, MediaCurrentPath currentPath) {
        if (isAborted) {
            log.error("Aborting file processing !");
            return;
        }

        try {
            String fileHash = HashUtils.computeFileHash_XXHash(file);

            synchronized (fileHashes) {
                if (fileHashes.containsKey(fileHash)) {
                    moveToDuplicates(file, currentPath);
                } else {
                    Path organizedPath = moveToOrganizedStructure(file, currentPath);
                    fileHashes.put(fileHash, organizedPath);
                }
            }
        } catch (IOException e) {
            log.error("ERROR computing hash for file: {}", file, e);
        }
    }

    private Path moveToOrganizedStructure(Path file, MediaCurrentPath currentPath) throws IOException {
        LocalDateTime modifiedTime = getModifiedTime(file);
        String year = String.valueOf(modifiedTime.getYear());
        String monthNumber = String.format("%02d", modifiedTime.getMonthValue());
        String monthName = modifiedTime.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String monthFolder = year + "-" + monthNumber + " " + monthName;

        Path yearDir = Paths.get(currentPath.getOrganizedPath(), year);
        Path monthDir = yearDir.resolve(monthFolder);
        Files.createDirectories(monthDir);

        Path targetFile = monthDir.resolve(file.getFileName().toString());
        switch (config.getOperation()) {
            case COPY:
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                log.info("Copied {} to {}", file.getFileName(), targetFile);
            break;
            case MOVE:
                Files.move(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                log.info("Moved {} to {}", file.getFileName(), targetFile);
            break;
            default: break;
        }

        var targetFileName = targetFile.getFileName().toString();
        switch (currentPath.getFileType()) {
            case PHOTO_VIDEO: organizedPhotoVideoFiles.add(targetFileName);
            break;
            case AUDIO: organizedAudioFiles.add(targetFileName);
            break;
            case CONTAINER: organizedContainerFiles.add(targetFileName);
            break;
            case ARCHIVE: organizedArchiveFiles.add(targetFileName);
            break;
            default: break;
        }

        return targetFile;
    }

    private void moveToDuplicates(Path file, MediaCurrentPath currentPath) throws IOException {
        Path targetDir = Paths.get(currentPath.getDuplicatesPath());
        Files.createDirectories(targetDir);

        Path targetFile = targetDir.resolve(file.getFileName().toString());
        switch (config.getOperation()) {
            case COPY:
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                log.info("Duplicated - Copied {} to {}", file.getFileName(), targetFile);
                break;
            case MOVE:
                Files.move(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                log.info("Duplicated - Moved {} to {}", file.getFileName(), targetFile);
                break;
            default: break;
        }

        var targetFileName = targetFile.getFileName().toString();
        switch (currentPath.getFileType()) {
            case PHOTO_VIDEO: duplicatedPhotoVideoFiles.add(targetFileName);
                break;
            case AUDIO: duplicatedAudioFiles.add(targetFileName);
                break;
            case CONTAINER: duplicatedContainerFiles.add(targetFileName);
                break;
            case ARCHIVE: duplicatedArchiveFiles.add(targetFileName);
                break;
            default: break;
        }
    }

    private LocalDateTime getModifiedTime(Path file) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
        return LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());
    }

    private TotalDirectoriesAndFiles getTotalDirectoriesAndFilesToScan(String parentDir) throws IOException {
        Path parentDirPath = Paths.get(parentDir);
        Map<Boolean, Long> result = Files.walk(parentDirPath)
                .collect(Collectors.partitioningBy(Files::isDirectory, Collectors.counting()));

        long directoriesCount = result.get(true);
        long filesCount = result.get(false);

        TotalDirectoriesAndFiles totalDirectoriesAndFiles = new TotalDirectoriesAndFiles(directoriesCount, filesCount);

        log.info("Total directories: {}", directoriesCount);
        log.info("Total files: {}", filesCount);

        return totalDirectoriesAndFiles;
    }

    private record TotalDirectoriesAndFiles(long directoriesCount, long filesCount) {}

    public static String formatMilliseconds(long milliseconds) {
        Duration duration = Duration.of(milliseconds, ChronoUnit.MILLIS);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        long millis = duration.toMillisPart();

        return String.format("%02dh:%02dm:%02ds.%03dms", hours, minutes, seconds, millis);
    }
}
