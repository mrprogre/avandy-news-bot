package com.avandy.bot.repository;

import com.avandy.bot.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AdminRepository extends JpaRepository<Admin, Integer> {

    @Query(value = "SELECT value FROM admin WHERE key = :key", nativeQuery = true)
    String findValueByKey(String key);

}
