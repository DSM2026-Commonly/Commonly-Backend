package commonly.commonlybe.domain.admin.domain.repository;

import commonly.commonlybe.domain.admin.domain.Admin;
import commonly.commonlybe.domain.user.domain.Authority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    @EntityGraph(attributePaths = "user")
    Page<Admin> findAllByUser_AuthorityAndUser_NameContaining(Authority authority, String name, Pageable pageable);
}
