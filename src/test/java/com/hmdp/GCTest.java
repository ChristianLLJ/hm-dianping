package com.hmdp;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Program : hm-dianping
 * Author : llj
 * Create : 2024-07-29 16:43
 **/

public class GCTest {
    public static void main(String[] args) {
        byte[] allocation1, allocation2;
        allocation1 = new byte[30900*1024];

        ArrayList<String> arrayList = new ArrayList<>(Arrays.asList("1","sa "));
        arrayList.get(1);
        HashMap<String, String> map = new HashMap<String, String>();
        // 键不能重复，值可以重复
        map.put("san", "张三");
        map.put("si", "李四");
        map.put("wu", "王五");
        map.put("wang", "老王");
        map.put("wang", "老王2");// 老王被覆盖
        map.put("lao", "老王");

        Set<Map.Entry<String, String>> entries = map.entrySet();
        for (Map.Entry<String, String> entry: entries){
            System.out.println(entry.getKey() + " " + entry.getValue());
        }
        List myList = Arrays.asList(1, 2, 3);
        int[] myArray = {1, 2, 3};
        List list = new ArrayList<>(Arrays.asList(1,2));
    }

    BlockingQueue<Runnable> runnables = new BlockingQueue<Runnable>() {
        @Override
        public boolean add(Runnable runnable) {
            return false;
        }

        @Override
        public boolean offer(Runnable runnable) {
            return false;
        }

        @Override
        public void put(Runnable runnable) throws InterruptedException {

        }

        @Override
        public boolean offer(Runnable runnable, long timeout, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public Runnable take() throws InterruptedException {
            return null;
        }

        @Override
        public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
            return null;
        }

        @Override
        public int remainingCapacity() {
            return 0;
        }

        @Override
        public boolean remove(Object o) {
            return false;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public int drainTo(Collection<? super Runnable> c) {
            return 0;
        }

        @Override
        public int drainTo(Collection<? super Runnable> c, int maxElements) {
            return 0;
        }

        @Override
        public Runnable remove() {
            return null;
        }

        @Override
        public Runnable poll() {
            return null;
        }

        @Override
        public Runnable element() {
            return null;
        }

        @Override
        public Runnable peek() {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Iterator<Runnable> iterator() {
            return null;
        }

        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return null;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends Runnable> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {

        }
    }
    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(10, 20, 2L, TimeUnit.SECONDS, runnables);
    })

}
