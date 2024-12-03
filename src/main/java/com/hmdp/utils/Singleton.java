package com.hmdp.utils;

/**
 * Program : hm-dianping
 * Author : llj
 * Create : 2024-08-26 18:39
 **/

public class Singleton {
    private static Singleton singleton;
    private Singleton(){}

    private static Singleton getInstance(){
        if(singleton == null){
            synchronized (Singleton.class){
                if(singleton ==null) singleton = new Singleton();
            }
        }
        return singleton;

    }

}
