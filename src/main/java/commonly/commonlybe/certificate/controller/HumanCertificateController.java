package commonly.commonlybe.certificate.controller;

import commonly.commonlybe.certificate.controller.dto.CertificateItemDto;
import commonly.commonlybe.certificate.service.CertificateService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * /api/certificates/{certificateId}와 경로가 겹치지 않도록 humans 하위로 뺐다.
 * 둘 다 /api/certificates/{x}면 Spring이 시작 시 Ambiguous mapping으로 죽는다.
 */
@RestController
@RequestMapping("/api/humans/{humanId}/certificates")
@RequiredArgsConstructor
public class HumanCertificateController {

    private final CertificateService certificateService;

    @GetMapping
    public List<CertificateItemDto> findAllByHuman(@PathVariable Long humanId) {
        return certificateService.findAllByHuman(humanId);
    }
}
