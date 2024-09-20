package org.example.controller;

import org.example.entity.BinaryEntity;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

@Controller
@ResponseBody
@RequestMapping("/og")
public class BinaryTypeController {
    @Autowired
    private EntityManager entityManager;

    @GetMapping("/queryBinary")
    @Transactional
    public List<Object> testBinary() {
        String dropSql = "drop table if exists t_binary";
        Query q1 = entityManager.createNativeQuery(dropSql);
        q1.executeUpdate();
        String createSql = "create table t_binary(c1 binary,c2 binary(5))";
        Query q2 = entityManager.createNativeQuery(createSql);
        q2.executeUpdate();

        String paramSql = "set bytea_output=escape;";
        Query q5 = entityManager.createNativeQuery(paramSql);
        q5.executeUpdate();

        String insertSql = "insert into t_binary values('a','abcde')";
        Query q3 = entityManager.createNativeQuery(insertSql);
        q3.executeUpdate();

        String selectSql = "select * from t_binary";
        Query q4 = entityManager.createNativeQuery(selectSql);
        q4.unwrap(org.hibernate.Query.class)
                .setResultTransformer(Transformers.aliasToBean(BinaryEntity.class));
        List<BinaryEntity> list = q4.getResultList();
        List<Object> result = new ArrayList<>();
        result.add(new String(list.get(0).getC1()));
        result.add(new String(list.get(0).getC2()));
        return result;
    }
}
