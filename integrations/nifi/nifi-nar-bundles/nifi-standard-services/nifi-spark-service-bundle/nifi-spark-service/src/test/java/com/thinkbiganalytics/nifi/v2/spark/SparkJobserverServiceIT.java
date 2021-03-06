package com.thinkbiganalytics.nifi.v2.spark;

/*-
 * #%L
 * thinkbig-nifi-core-service
 * %%
 * Copyright (C) 2017 ThinkBig Analytics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.google.common.collect.ImmutableList;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

public class SparkJobserverServiceIT {

    /**
     * Identifier for the spark jobserver service
     */
    private static final String SPARK_JOBSERVER_SERVICE_IDENTIFIER = "sparkJobServerService";

    /**
     * Processor property for the cleanup event service
     */
    private static final PropertyDescriptor SPARK_JOBSERVER_SERVICE_PROPERTY = new PropertyDescriptor.Builder()
        .name("Spark Jobserver Service")
        .description("Provides long running spark contexts and shared RDDs using Spark jobserver.")
        .identifiesControllerService(SparkJobserverService.class)
        .required(true)
        .build();

    /**
     * Spark Jobserver service for testing
     */
    private static final SparkJobserverService sparkJobserverService = new SparkJobserverService();
    private static final String sparkJobserverUrl = "http://localhost:8089";
    private static final String syncTimeout = "600";

    /**
     * Default Context Creation Properties
     */
    private static final String numExecutors = "1";
    private static final String memPerNode = "512m";
    private static final String numCPUCores = "2";
    private static final SparkContextType sparkContextType = SparkContextType.SPARK_CONTEXT;
    private static final int contextTimeout = 0;
    private static boolean async = false;

    /**
     * Default Execute Spark Context Job Properties
     */
    private static final String appName = "test-app";
    private static final String classPath = "spark.jobserver.WordCountExample";
    private static final String args = "input.string = a b c a b see";

    /**
     * Test runner
     */
    private final TestRunner runner = TestRunners.newTestRunner(new AbstractProcessor() {
        @Override
        protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
            return ImmutableList.of(SPARK_JOBSERVER_SERVICE_PROPERTY);
        }

        @Override
        public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
            // do nothing
        }
    });

    /**
     * Initialize instance variables.
     */
    @Before
    public void setUp() throws Exception {

        // Setup test runner
        runner.addControllerService(SPARK_JOBSERVER_SERVICE_IDENTIFIER, sparkJobserverService);
        runner.setProperty(SPARK_JOBSERVER_SERVICE_PROPERTY, SPARK_JOBSERVER_SERVICE_IDENTIFIER);
        runner.setProperty(sparkJobserverService, SparkJobserverService.JOBSERVER_URL, sparkJobserverUrl);
        runner.setProperty(sparkJobserverService, SparkJobserverService.SYNC_TIMEOUT, syncTimeout);
        runner.enableControllerService(sparkJobserverService);
    }

    /**
     * Verify creating Spark Context and then deleting the Context
     */
    @Test
    @Ignore
    public void testContextCreationAndDeletion() throws Exception {
        // Test creating context
        String contextName = "testContextCreationAndDeletion";

        boolean created = sparkJobserverService.createContext(contextName, numExecutors, memPerNode, numCPUCores, sparkContextType, contextTimeout, async);
        Assert.assertTrue(created);

        boolean deleted = sparkJobserverService.deleteContext(contextName);
        Assert.assertTrue(deleted);
    }

    /**
     * Verify two threads attempting to create a Spark Context at once
     */
    @Test
    @Ignore
    public void testDuplicateContextCreation() throws Exception {
        // Test creating context
        String contextName = "testDuplicateContextCreation";

        CreateSparkContext createSparkContext1 = new CreateSparkContext(contextName, numExecutors, memPerNode, numCPUCores, sparkContextType, contextTimeout, async, sparkJobserverService);
        Thread thread1 = new Thread(createSparkContext1);

        CreateSparkContext createSparkContext2 = new CreateSparkContext(contextName, numExecutors, memPerNode, numCPUCores, sparkContextType, contextTimeout, async, sparkJobserverService);
        Thread thread2 = new Thread(createSparkContext2);

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        boolean contextExists = sparkJobserverService.checkIfContextExists(contextName);

        Assert.assertTrue(contextExists);

        sparkJobserverService.deleteContext(contextName);
    }

    /**
     * Verify creating a Spark Context which times out and gets deleted
     */
    @Test
    @Ignore
    public void testContextTimeout() throws Exception {
        // Test creating context
        String contextName = "testContextTimeout";
        int contextTimeout = 1;

        sparkJobserverService.createContext(contextName, numExecutors, memPerNode, numCPUCores, sparkContextType, contextTimeout, async);
        Thread.sleep(15000);

        boolean contextExists = sparkJobserverService.checkIfContextExists(contextName);
        Assert.assertFalse(contextExists);
    }

    /**
     * Verify creating two Spark Contexts
     */
    @Test
    @Ignore
    public void testMultipleContextCreation() throws Exception {
        // Test creating context
        String contextOneName = "testMultipleContextCreationOne";
        String contextTwoName = "testMultipleContextCreationTwo";

        CreateSparkContext createSparkContext1 = new CreateSparkContext(contextOneName, numExecutors, memPerNode, numCPUCores, sparkContextType, contextTimeout, async, sparkJobserverService);
        Thread thread1 = new Thread(createSparkContext1);

        CreateSparkContext createSparkContext2 = new CreateSparkContext(contextTwoName, numExecutors, memPerNode, numCPUCores, sparkContextType, contextTimeout, async, sparkJobserverService);
        Thread thread2 = new Thread(createSparkContext2);

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        boolean contextOneExists = sparkJobserverService.checkIfContextExists(contextOneName);
        boolean contextTwoExists = sparkJobserverService.checkIfContextExists(contextTwoName);

        sparkJobserverService.deleteContext(contextOneName);
        sparkJobserverService.deleteContext(contextTwoName);

        Assert.assertTrue(contextOneExists && contextTwoExists);
    }

    /**
     * Verify executing Spark Context Job
     */
    @Test
    @Ignore
    public void testExecuteSparkContextJob() throws Exception {
        String contextName = "testExecuteSparkContextJob";

        sparkJobserverService.createContext(contextName, numExecutors, memPerNode, numCPUCores, sparkContextType, contextTimeout, async);
        SparkJobResult executed = sparkJobserverService.executeSparkContextJob(appName, classPath, contextName, args, async);
        sparkJobserverService.deleteContext(contextName);

        Assert.assertTrue(executed.success);
    }

    /**
     * Verify executing Spark Context Jobs in Parallel
     */
    @Test
    @Ignore
    public void testParallelExecuteSparkContextJobs() throws Exception {
        String contextName = "testParallelExecuteSparkContextJobs";

        sparkJobserverService.createContext(contextName, numExecutors, memPerNode, numCPUCores, sparkContextType, contextTimeout, async);

        ExecuteSparkContextJob executeSparkContextJob1 = new ExecuteSparkContextJob(appName, classPath, contextName, args, async, sparkJobserverService);
        Thread thread1 = new Thread(executeSparkContextJob1);

        ExecuteSparkContextJob executeSparkContextJob2 = new ExecuteSparkContextJob(appName, classPath, contextName, args, async, sparkJobserverService);
        Thread thread2 = new Thread(executeSparkContextJob2);

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        sparkJobserverService.deleteContext(contextName);

        Assert.assertTrue(executeSparkContextJob1.jobSuccessfull() && executeSparkContextJob2.jobSuccessfull());
    }

    /**
     * Shutdown the runner
     */
    @After
    public void shutdown() {
        runner.shutdown();
    }
}
