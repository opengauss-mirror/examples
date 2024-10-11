package org.example.controller;

import org.example.entity.StringEntity;
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

/**
 * other
 */
@Controller
@ResponseBody
@RequestMapping("/og")
public class OtherTypeTest {
    @Autowired
    private EntityManager entityManager;

    @GetMapping("/queryString")
    @Transactional
    public List<Object> testString() {
        String dropSql = "drop table if exists t_varchar";
        Query q1 = entityManager.createNativeQuery(dropSql);
        q1.executeUpdate();
        String createSql = "create table t_varchar(c1 varchar(5),c2 text,c3 json)";
        Query q2 = entityManager.createNativeQuery(createSql);
        q2.executeUpdate();

        String insertSql = "insert into t_varchar values('abc','mytest','{\"a\":\"b\",\"c\":\"d\"}')";
        Query q3 = entityManager.createNativeQuery(insertSql);
        q3.executeUpdate();

        String selectSql = "select * from t_varchar";
        Query q4 = entityManager.createNativeQuery(selectSql);
        q4.unwrap(org.hibernate.Query.class)
                .setResultTransformer(Transformers.aliasToBean(StringEntity.class));
        return q4.getResultList();
    }
}
