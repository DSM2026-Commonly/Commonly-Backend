package commonly.commonlybe.file.controller;

import commonly.commonlybe.file.controller.dto.FileUploadResponse;
import commonly.commonlybe.file.controller.dto.MappingConfirmRequest;
import commonly.commonlybe.file.controller.dto.MappingConfirmResponse;
import commonly.commonlybe.file.service.FileUploadService;
import commonly.commonlybe.file.service.MappingConfirmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileUploadService fileUploadService;
    private final MappingConfirmService mappingConfirmService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileUploadResponse upload(@RequestPart("file") MultipartFile file) {
        return fileUploadService.upload(file);
    }

    @PutMapping("/{fileId}/mapping")
    public MappingConfirmResponse confirmMapping(@PathVariable Long fileId,
                                                   @RequestBody @Valid MappingConfirmRequest request) {
        return mappingConfirmService.confirm(fileId, request);
    }
}
