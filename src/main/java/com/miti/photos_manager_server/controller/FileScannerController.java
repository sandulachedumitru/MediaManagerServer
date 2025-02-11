package com.miti.photos_manager_server.controller;

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
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class FileScannerController {
    private final FileScannerService fileScannerService;

    @PostMapping("/scan")
    public String triggerScan(@RequestBody ScanRequestDto requestDto) throws IOException {
        fileScannerService.scanAndOrganizeFiles(requestDto);
        return "Scan started with custom settings.";
    }

    @GetMapping("/media-files")
    public Map<String, List<String>> getProcessedFiles() {
        return fileScannerService.getProcessedFiles();
    }
}
