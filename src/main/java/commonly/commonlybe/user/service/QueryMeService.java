package commonly.commonlybe.user.service;

import commonly.commonlybe.admin.entity.Admin;
import commonly.commonlybe.admin.repository.AdminRepository;
import commonly.commonlybe.petitioner.entity.Petitioner;
import commonly.commonlybe.petitioner.repository.PetitionerRepository;
import commonly.commonlybe.global.security.auth.AuthDetails;
import commonly.commonlybe.user.controller.dto.MeResponse;
import commonly.commonlybe.user.entity.User;
import commonly.commonlybe.user.exception.UserNotFoundException;
import commonly.commonlybe.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QueryMeService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final PetitionerRepository petitionerRepository;

    @Transactional(readOnly = true)
    public MeResponse execute(AuthDetails authDetails) {
        Long userId = authDetails.user().getId();

        // 토큰 발급 이후 이름이 바뀌었을 수 있으므로 다시 읽는다.
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        Admin admin = adminRepository.findById(userId).orElse(null);
        Petitioner petitioner = petitionerRepository.findById(userId).orElse(null);

        return MeResponse.of(user, authDetails.authority(), admin, petitioner);
    }
}
