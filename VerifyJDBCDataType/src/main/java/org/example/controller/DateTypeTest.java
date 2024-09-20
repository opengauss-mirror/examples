package org.example.controller;

import org.example.entity.DateEntity;
import org.hibernate.transform.Transformers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Controller
@ResponseBody
@RequestMapping("/og")
public class DateTypeTest {
    @Autowired
    private EntityManager entityManager;

    @GetMapping("/queryDate1")
    @Transactional
    public List<Object> testCastTime() {
        String dropSql = "drop table if exists t_as_time";
        Query q1 = entityManager.createNativeQuery(dropSql);
        q1.executeUpdate();
        String createSql = "create table t_as_time(c1 varchar(32), c2 varchar(32), c3 varchar(32))";
        Query q2 = entityManager.createNativeQuery(createSql);
        q2.executeUpdate();

        String insertSql = "insert into t_as_time values(?1,?2,?3)";
        Query q3 = entityManager.createNativeQuery(insertSql);
        q3.setParameter(1, "12345");
        q3.setParameter(2, "-1948");
        q3.setParameter(3, "0");
        q3.executeUpdate();
        q3.setParameter(1, "2024-04-05");
        q3.setParameter(2, "12:12:12");
        q3.setParameter(3, "2018/5/9");
        q3.executeUpdate();

        String selectSql = "select cast(c1 as time),cast(c2 as time),cast(c3 as time) from t_as_time";
        Query q4 = entityManager.createNativeQuery(selectSql);
        return q4.getResultList();
    }

    @GetMapping("/queryDate2")
    @Transactional
    public List<Object> testCastDate() {
        String dropSql = "drop table if exists t_as_date";
        Query q1 = entityManager.createNativeQuery(dropSql);
        q1.executeUpdate();
        String createSql = "create table t_as_date(c1 varchar(32), c2 varchar(32), c3 varchar(32))";
        Query q2 = entityManager.createNativeQuery(createSql);
        q2.executeUpdate();

        String insertSql = "insert into t_as_date values(?1,?2,?3)";
        Query q3 = entityManager.createNativeQuery(insertSql);
        q3.setParameter(1, "2024-04-05");
        q3.setParameter(2, "12:12:12");
        q3.setParameter(3, "2018/5/9");
        q3.executeUpdate();

        String selectSql = "select cast(c1 as date),cast(c2 as date),cast(c3 as date) from t_as_date";
        Query q4 = entityManager.createNativeQuery(selectSql);
        return q4.getResultList();
    }

    @GetMapping("/queryDate3")
    @Transactional
    public List<Object> testDate() {
        String dropSql = "drop table if exists t_date";
        Query q1 = entityManager.createNativeQuery(dropSql);
        q1.executeUpdate();
        String createSql = "create table t_date(c1 date, c2 time, c3 year, c4 datetime, c5 timestamp)";
        Query q2 = entityManager.createNativeQuery(createSql);
        q2.executeUpdate();

        String insertSql = "insert into t_date values(?1,?2,?3,?4,?5)";
        Query q3 = entityManager.createNativeQuery(insertSql);
        Date date = new Date();
        q3.setParameter(1, date);
        q3.setParameter(2, date);
        q3.setParameter(3, "2024");
        LocalDateTime dateTime = LocalDateTime.now();
        q3.setParameter(4, dateTime);
        q3.setParameter(5, dateTime);
        q3.executeUpdate();

        String selectSql = "select * from t_date";
        Query q4 = entityManager.createNativeQuery(selectSql);
        q4.unwrap(org.hibernate.Query.class)
                .setResultTransformer(Transformers.aliasToBean(DateEntity.class));
        return q4.getResultList();
    }
}
