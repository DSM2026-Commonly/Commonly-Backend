package commonly.commonlybe.user.controller.dto;

import commonly.commonlybe.admin.entity.Admin;
import commonly.commonlybe.petitioner.entity.Petitioner;
import commonly.commonlybe.user.entity.User;
import java.time.LocalDate;
import lombok.Builder;

/**
 * 로그인한 본인 정보.
 *
 * 담당자(ADMIN/USER)는 department만, 민원인(PETITIONER)은 phoneNumber/birthDate만 채워진다.
 * 반대쪽은 null이다 — 한 계정이 둘 다인 경우는 없다.
 */
@Builder
public record MeResponse(
        Long userId,
        String accountId,
        String name,
        String authority,
        boolean passwordChanged,
        String department,
        String phoneNumber,
        LocalDate birthDate
) {
    public static MeResponse of(User user, String authority, Admin admin, Petitioner petitioner) {
        return MeResponse.builder()
                .userId(user.getId())
                .accountId(user.getAccountId())
                .name(user.getName())
                .authority(authority)
                .passwordChanged(user.isPasswordChanged())
                .department(admin == null ? null : admin.getDepartment())
                .phoneNumber(petitioner == null ? null : petitioner.getPhoneNumber())
                .birthDate(petitioner == null ? null : petitioner.getBirthDate())
                .build();
    }
}
