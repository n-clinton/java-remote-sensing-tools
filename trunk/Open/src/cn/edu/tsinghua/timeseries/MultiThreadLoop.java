package cn.edu.tsinghua.timeseries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 *
 * @author Remi Alkemade
 * 
 * This is the main multi-threading class of the program. It provides an easy
 * way to convert a normal for-loop into a multi-threaded for-loop, dividing the
 * domain over the available threads.
 */
public abstract class MultiThreadLoop
{
	// The ExecutorService to run threads
    protected ExecutorService executorService;

    /**
     * Creates a new instance of MultiThreadLoop
     * @param ExecutorService e The desired ExecutorService to run all threads on. 
     */
    public MultiThreadLoop(ExecutorService e)
    {
        executorService = e;
    }

    /**
     * This method should contain the for-loop to be parallelized. Instead of the
     * numbers in the for-definition, iMin and iMax should be used. These indicate the
     * borders of the domain for each single thread.
     * @param iMin The minimum value of the loop
     * @param iMax The maximum value of the loop
     */
    protected abstract void loop(int iMin, int iMax);
    
    /**
     * This method is used to run the for-loop. The domain (iMin to iMax) is divided
     * over the specified number of threads and each thread calls the loop-method with
     * its own domain as parameters.
     * @param iMin The minimum value of the complete domain.
     * @param iMax The maximum value of the complete domain.
     * @param numThreads The number of threads to use.
     */
    public void run(int iMin, int iMax, int numThreads)
    {
    	// Compute the size of the divisions of the total domain.
    	final int iSlice = (int)Math.ceil(1.0*(iMax-iMin) / numThreads);

    	// Keep a list of Futures, obtained from the started threads.
    	ArrayList<Future<?>> runningFutures = new ArrayList<Future<?>>();
    	
    	// Start the specified number of threads (as Runnables)
        for(int t=0; t<numThreads; t++)
        {
        	// Define lower and upper bounds of domain
            final int min = t * iSlice;
            final int max = Math.min(iMax, (t+1) * iSlice);
            // Create new Runnable, looping through its own domain
            Runnable task = new Runnable() {
                public void run() {
                    loop(min,max);
                }
            };
            // and submit to ExecutorService
            Future<?> f = executorService.submit(task);
            runningFutures.add(f);
        }
        
        // Wait for all Runnables to complete
        for(Iterator<Future<?>> i=runningFutures.iterator(); i.hasNext();)
        {
        	Future<?> f = i.next();
        	try {
				f.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
        }
    }
}