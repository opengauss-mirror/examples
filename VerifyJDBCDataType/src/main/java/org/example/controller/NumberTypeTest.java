package org.example.controller;

import org.example.entity.DoubleEntity;
import org.example.entity.IntEntity;
import org.hibernate.transform.Transformers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

@Controller
@ResponseBody
@RequestMapping("/og")
public class NumberTypeTest {
    @Autowired
    private EntityManager entityManager;

    @GetMapping("/queryInt")
    @Transactional
    public List<Object> testInt() {
        String dropSql = "drop table if exists t_int";
        Query q1 = entityManager.createNativeQuery(dropSql);
        q1.executeUpdate();
        String createSql = "create table t_int(c1 tinyint,c2 smallint,c3 int,c4 bigint," +
                "c5 tinyint unsigned,c6 smallint unsigned,c7 int unsigned,c8 bigint unsigned)";
        Query q2 = entityManager.createNativeQuery(createSql);
        q2.executeUpdate();

        String insertSql = "insert into t_int values(18,479,-3950438,-38475983932,105,785,5902,13345)";
        Query q3 = entityManager.createNativeQuery(insertSql);
        q3.executeUpdate();

        String selectSql = "select * from t_int";
        Query q4 = entityManager.createNativeQuery(selectSql);
        q4.unwrap(org.hibernate.Query.class)
                .setResultTransformer(Transformers.aliasToBean(IntEntity.class));
        return q4.getResultList();
    }

    @GetMapping("/queryDouble")
    @Transactional
    public List<Object> testDouble() {
        String dropSql = "drop table if exists t_double";
        Query q1 = entityManager.createNativeQuery(dropSql);
        q1.executeUpdate();
        String createSql = "create table t_double(c1 float,c2 double,c3 numeric,c4 numeric(10,6),c5 decimal,c6 decimal(10,6))";
        Query q2 = entityManager.createNativeQuery(createSql);
        q2.executeUpdate();

        String insertSql = "insert into t_double values(3.5,3.6,3.75564,2.9837453,5.33847462,3.211)";
        Query q3 = entityManager.createNativeQuery(insertSql);
        q3.executeUpdate();

        String selectSql = "select * from t_double";
        Query q4 = entityManager.createNativeQuery(selectSql);
        q4.unwrap(org.hibernate.Query.class)
                .setResultTransformer(Transformers.aliasToBean(DoubleEntity.class));
        return q4.getResultList();
    }
}
