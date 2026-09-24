package com.meghadri.offlineupi.user.repository;

import com.meghadri.offlineupi.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}