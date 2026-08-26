package commonly.commonlybe.global.security.auth;

import commonly.commonlybe.domain.admin.domain.Admin;
import commonly.commonlybe.domain.admin.domain.repository.AdminRepository;
import commonly.commonlybe.domain.user.domain.User;
import commonly.commonlybe.domain.user.domain.repository.UserRepository;
import commonly.commonlybe.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class AuthDetailsService implements UserDetailsService {
    static final String PETITIONER_AUTHORITY = "PETITIONER";

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String accountId) throws UsernameNotFoundException {
        User user = userRepository.findByAccountId(accountId).orElseThrow(UserNotFoundException::new);

        String authority = adminRepository.findById(user.getId())
            .map(Admin::getRole)
            .map(Enum::name)
            .orElse(PETITIONER_AUTHORITY);

        return new AuthDetails(user, authority);
    }
}
