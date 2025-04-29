package edu.cit.Judify.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
    List<UserEntity> findByRole(UserRole role);
    Optional<UserEntity> findByEmailAndPassword(String email, String password);
    Optional<UserEntity> findByUsername(String username);
}
