package commonly.commonlybe.global.security.auth;

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
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String accountId) throws UsernameNotFoundException {
        return new AuthDetails(
            userRepository.findByAccountId(accountId).orElseThrow(UserNotFoundException::new)
        );
    }
}
