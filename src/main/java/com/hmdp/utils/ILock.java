package com.hmdp.utils;

/**
 * Program : hm-dianping
 * Author : llj
 * Create : 2024-05-15 15:21
 **/
public interface ILock {

    boolean tryLock(long timeoutSec);

    void unlock();


}
