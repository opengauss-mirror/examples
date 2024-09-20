package org.example.controller;

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
public class BlobTypeTest {
    @Autowired
    private EntityManager entityManager;

    @GetMapping("/queryBlob")
    @Transactional
    public List<Object> testBlob() {
        String dropSql = "drop table if exists t_blob";
        Query q1 = entityManager.createNativeQuery(dropSql);
        q1.executeUpdate();
        String createSql = "create table t_blob(id blob)";
        Query q2 = entityManager.createNativeQuery(createSql);
        q2.executeUpdate();

        String insertSql = "insert into t_blob values('abc')";
        Query q3 = entityManager.createNativeQuery(insertSql);
        q3.executeUpdate();

        String selectSql = "select * from t_blob";
        Query q4 = entityManager.createNativeQuery(selectSql);
        List<byte[]> list = q4.getResultList();
        List<Object> result = new ArrayList<>();
        result.add(new String(list.get(0)));
        return result;
    }
}
