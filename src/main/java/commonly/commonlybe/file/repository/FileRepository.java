package commonly.commonlybe.file.repository;

import commonly.commonlybe.file.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

    /**
     * 아직 확정되지 않은 파일만 확정 상태로 표시한다. 0을 반환하면 이미 확정된 파일이다.
     */
    @Modifying(clearAutomatically = true)
    @Query("update FileEntity f set f.confirmed = true where f.fileId = :fileId and f.confirmed = false")
    int markConfirmed(@Param("fileId") Long fileId);
}
