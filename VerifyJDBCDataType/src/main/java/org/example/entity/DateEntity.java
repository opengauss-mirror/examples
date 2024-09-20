package org.example.entity;

import lombok.Data;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;

@Data
public class DateEntity {
    Date c1;
    Time c2;
    Date c3;
    Timestamp c4;
    Timestamp c5;
}
