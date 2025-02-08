package com.miti.photos_manager_server.controller;

import com.miti.photos_manager_server.config.MediaManagerConfig;
import com.miti.photos_manager_server.model.ScanRequestDto;
import com.miti.photos_manager_server.service.FileScannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Dumitru Săndulache (sandulachedumitru@hotmail.com)
 */

@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class FileScannerController {
    private final FileScannerService fileScannerService;
    private final MediaManagerConfig mediaManagerConfig;

    @PostMapping("/scan")
    public String triggerScan(@RequestBody ScanRequestDto requestData) throws IOException {
        fileScannerService.scanAndOrganizeFiles(requestData.scanDirectory());
        return "Scan started with custom settings.";
    }

    @GetMapping("/files")
    public Map<String, List<String>> getProcessedFiles() {
        return fileScannerService.getProcessedFiles();
    }

    @GetMapping
    public String display() {
        mediaManagerConfig.config("D:/TestPhotosVideosManager/Documents");
        return mediaManagerConfig.toString();
    }
}
