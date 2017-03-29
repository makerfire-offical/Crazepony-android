package com.makerfire.threadpool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by lindengfu on 17-3-15.
 */

public class ThreadPool
{
    public static ExecutorService threadPool;

    static
    {
        threadPool = Executors.newCachedThreadPool();
    }

}
