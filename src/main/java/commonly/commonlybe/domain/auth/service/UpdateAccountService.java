package commonly.commonlybe.domain.auth.service;

import commonly.commonlybe.domain.auth.presentation.dto.request.UpdateAccountRequest;
import commonly.commonlybe.domain.petitioner.domain.repository.PetitionerRepository;
import commonly.commonlybe.domain.user.domain.User;
import commonly.commonlybe.domain.user.domain.repository.UserRepository;
import commonly.commonlybe.domain.user.exception.ForbiddenUserException;
import commonly.commonlybe.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateAccountService {
    private final UserRepository userRepository;
    private final PetitionerRepository petitionerRepository;

    @Transactional
    public void execute(Long currentUserId, Long userId, UpdateAccountRequest request) {
        if (!currentUserId.equals(userId)) {
            throw new ForbiddenUserException();
        }

        User user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);

        if (request.getName() != null) {
            user.updateName(request.getName());
        }

        petitionerRepository.findById(userId).ifPresent(petitioner -> {
            if (request.getPhoneNumber() != null) {
                petitioner.updatePhoneNumber(request.getPhoneNumber());
            }
            if (request.getBirthDate() != null) {
                petitioner.updateBirthDate(request.getBirthDate());
            }
        });
    }
}
