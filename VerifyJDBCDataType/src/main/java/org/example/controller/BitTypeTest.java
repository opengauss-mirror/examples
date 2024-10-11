package org.example.controller;

import org.example.entity.BitEntity;
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
public class BitTypeTest {
    @Autowired
    private EntityManager entityManager;

    @GetMapping("/queryBit")
    @Transactional
    public List<Object> testBit() {
        String dropSql = "drop table if exists t_bit";
        Query q1 = entityManager.createNativeQuery(dropSql);
        q1.executeUpdate();
        String createSql = "create table t_bit(c1 bit(1),c2 bit(5),c3 bit(10),c4 boolean)";
        Query q2 = entityManager.createNativeQuery(createSql);
        q2.executeUpdate();

        String insertSql = "insert into t_bit values(1,28,369,false)";
        Query q3 = entityManager.createNativeQuery(insertSql);
        q3.executeUpdate();

        String selectSql = "select * from t_bit";
        Query q4 = entityManager.createNativeQuery(selectSql);
        q4.unwrap(org.hibernate.Query.class)
                .setResultTransformer(Transformers.aliasToBean(BitEntity.class));
        return q4.getResultList();
    }
}
