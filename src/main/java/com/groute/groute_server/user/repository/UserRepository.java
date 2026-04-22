package com.groute.groute_server.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groute.groute_server.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {}
