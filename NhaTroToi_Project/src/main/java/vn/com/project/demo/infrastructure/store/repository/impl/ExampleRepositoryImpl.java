package vn.com.project.demo.infrastructure.store.repository.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import vn.com.project.demo.core.util.DbMapper;
import vn.com.project.demo.infrastructure.store.repository.ExampleRepositoryCustom;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * This class using when you need get many data from multiple table
 *
 * @author thanhlq
 * @created 12/6/2022 - 9:04 AM
 * @project NhaTroToi_Project
 * @since 1.0
 **/
@Repository
public class ExampleRepositoryImpl implements ExampleRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private final DbMapper dbMapper;

    @Autowired
    public ExampleRepositoryImpl(DbMapper dbMapper) {
        this.dbMapper = dbMapper;
    }
}
