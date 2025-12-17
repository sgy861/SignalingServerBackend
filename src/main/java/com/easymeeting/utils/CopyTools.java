package com.easymeeting.utils;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

public class CopyTools {
    public static <T , S> List<T> copyList(List<S> sList, Class<T> tClass) {
        List<T> list = new ArrayList<T>();
        for (S s : sList) {
            T t = null;
            try {
                t = tClass.newInstance();
            }catch (Exception e) {}
            BeanUtils.copyProperties(s, t);
            list.add(t);
        }
        return list;
    }

    public static <T , S> T copy(S source, Class<T> tClass) {
        T t = null;
        try {
            t = tClass.newInstance();

        } catch (Exception e) {}
        BeanUtils.copyProperties(source, t);
        return t;
    }
}
