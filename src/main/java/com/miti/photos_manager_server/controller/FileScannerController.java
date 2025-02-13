package com.miti.photos_manager_server.controller;

import com.miti.photos_manager_server.model.ScanRequestDto;
import com.miti.photos_manager_server.service.FileScannerService;
import com.miti.photos_manager_server.service.ProgressServiceSSE;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    private final ProgressServiceSSE progressServiceSSE;

    @PostMapping("/scan")
    public String triggerScan(@RequestBody ScanRequestDto requestDto) throws IOException {
        fileScannerService.scanAndOrganizeFiles(requestDto);
        return "Scan started with custom settings.";
    }

    @GetMapping("/media-files")
    public Map<String, List<String>> getProcessedFiles() {
        return fileScannerService.getProcessedFiles();
    }

    @GetMapping("/progress/subscribe")
    public SseEmitter subscribe() {
        return progressServiceSSE.addEEmitter();
    }

    @GetMapping("/progress/abort")
    public void abortScan() {
        fileScannerService.abortScan();
    }
}
