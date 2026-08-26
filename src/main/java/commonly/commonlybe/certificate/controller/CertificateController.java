package commonly.commonlybe.certificate.controller;

import commonly.commonlybe.certificate.controller.dto.CertificateDetailResponse;
import commonly.commonlybe.certificate.controller.dto.CertificateIssueRequest;
import commonly.commonlybe.certificate.controller.dto.CertificateIssueResponse;
import commonly.commonlybe.certificate.controller.dto.CertificateUpdateRequest;
import commonly.commonlybe.certificate.service.CertificateIssueService;
import commonly.commonlybe.certificate.service.CertificateService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateIssueService certificateIssueService;
    private final CertificateService certificateService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CertificateIssueResponse issue(@RequestBody @Valid CertificateIssueRequest request) {
        return certificateIssueService.issue(request);
    }

    @GetMapping("/{certificateId}")
    public CertificateDetailResponse findOne(@PathVariable Long certificateId) {
        return certificateService.findIssued(certificateId);
    }

    @PutMapping("/{certificateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable Long certificateId,
                       @RequestBody @Valid CertificateUpdateRequest request) {
        certificateService.update(certificateId, request);
    }

    @GetMapping("/{certificateId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long certificateId) {
        CertificateService.IssuedFile file = certificateService.download(certificateId);
        // 문서번호에 한글이 들어간다. filename*(RFC 5987)이 없으면 브라우저가 깨진 이름으로 저장한다.
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(file.content()));
    }
}
