package com.examinai.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);

    @Query("SELECT DISTINCT u FROM UserAccount u JOIN FETCH u.stacks WHERE u.username = :username")
    Optional<UserAccount> findByUsernameWithStacks(@Param("username") String username);

    @Query("SELECT DISTINCT u FROM UserAccount u LEFT JOIN FETCH u.stacks WHERE u.id = :id")
    Optional<UserAccount> findByIdWithStacks(@Param("id") Long id);

    @Query("SELECT DISTINCT u FROM UserAccount u LEFT JOIN FETCH u.stacks ORDER BY u.username ASC")
    java.util.List<UserAccount> findAllWithStacks();

    @Query("SELECT COUNT(DISTINCT u) FROM UserAccount u JOIN u.stacks s WHERE s.id = :stackId")
    long countUsersWithStack(@Param("stackId") Long stackId);

    List<UserAccount> findAllByRole(Role role);
    boolean existsByUsername(String username);
}
