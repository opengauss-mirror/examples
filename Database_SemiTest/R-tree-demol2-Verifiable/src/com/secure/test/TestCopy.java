package com.secure.test;


import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;

import java.util.Collections;
import java.util.List;

public class TestCopy {

    public static void main(String[] args) {

        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add(i);
        }

        //list深度拷贝
        List<Integer> newList = new ArrayList<>();
        CollectionUtils.addAll(newList, new Object[list.size()]);
        Collections.copy(newList, list);
        newList.set(0,7);

        System.out.println("原list值：" + list);
        System.out.println("新list值：" + newList);
    }
}
