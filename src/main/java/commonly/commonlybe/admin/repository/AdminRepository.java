package commonly.commonlybe.admin.repository;

import commonly.commonlybe.admin.entity.Admin;
import commonly.commonlybe.admin.entity.AdminRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    @EntityGraph(attributePaths = "user")
    Page<Admin> findAllByRoleAndUser_NameContaining(AdminRole role, String name, Pageable pageable);
}
