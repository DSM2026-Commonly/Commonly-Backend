package commonly.commonlybe.global.security.auth;

import commonly.commonlybe.admin.entity.Admin;
import commonly.commonlybe.admin.repository.AdminRepository;
import commonly.commonlybe.user.entity.User;
import commonly.commonlybe.user.repository.UserRepository;
import commonly.commonlybe.user.exception.UserNotFoundException;
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
