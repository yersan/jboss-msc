/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.msc.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.msc.service.util.LatchedFinishListener;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Test case used to ensure functionality for the Resolver.
 *
 * @author John Bailey
 */
public class ServiceResolverTestCase extends AbstractServiceTest {

    @Test
    public void testResolvable() throws Exception {
        perfromTest(new ServiceTestInstance() {
            @Override
            public List<BatchBuilder> initializeBatches(ServiceContainer serviceContainer, LatchedFinishListener finishListener) throws Exception {
                final BatchBuilder builder = serviceContainer.batchBuilder();
                builder.addService(ServiceName.of("7"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("8"));
                builder.addService(ServiceName.of("5"), Service.NULL).addDependencies(ServiceName.of("11"));
                builder.addService(ServiceName.of("3"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("9"));
                builder.addService(ServiceName.of("11"), Service.NULL).addDependencies(ServiceName.of("2"), ServiceName.of("9"), ServiceName.of("10"));
                builder.addService(ServiceName.of("8"), Service.NULL).addDependencies(ServiceName.of("9"));
                builder.addService(ServiceName.of("2"), Service.NULL);
                builder.addService(ServiceName.of("9"), Service.NULL);
                builder.addService(ServiceName.of("10"), Service.NULL);
                builder.addListener(finishListener);
                return Collections.singletonList(builder);
            }

            @Override
            public void performAssertions(ServiceContainer serviceContainer) throws Exception {
                // TODO:  Assert all services are started and ordered correctly
            }
        });
    }

    @Test
    public void testResolvableWithPreexistingDeps() throws Exception {
        perfromTest(new ServiceTestInstance() {
            @Override
            public List<BatchBuilder> initializeBatches(ServiceContainer serviceContainer, LatchedFinishListener finishListener) throws Exception {
                final BatchBuilder builder1 = serviceContainer.batchBuilder().addListener(finishListener);
                builder1.addService(ServiceName.of("2"), Service.NULL);
                builder1.addService(ServiceName.of("9"), Service.NULL);
                builder1.addService(ServiceName.of("10"), Service.NULL);

                final BatchBuilder builder2 = serviceContainer.batchBuilder().addListener(finishListener);
                builder2.addService(ServiceName.of("7"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("8"));
                builder2.addService(ServiceName.of("5"), Service.NULL).addDependencies(ServiceName.of("11"));
                builder2.addService(ServiceName.of("3"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("9"));
                builder2.addService(ServiceName.of("11"), Service.NULL).addDependencies(ServiceName.of("2"), ServiceName.of("9"), ServiceName.of("10"));
                builder2.addService(ServiceName.of("8"), Service.NULL).addDependencies(ServiceName.of("9"));
                return Arrays.asList(builder1, builder2);
            }

            @Override
            public void performAssertions(ServiceContainer serviceContainer) throws Exception {
                // TODO:  Assert all services are started and ordered correctly
            }
        });
    }


    @Test
    public void testMissingDependency() throws Exception {

        perfromTest(new ServiceTestInstance() {
            private boolean failed;
            
            @Override
            public List<BatchBuilder> initializeBatches(ServiceContainer serviceContainer, LatchedFinishListener finishListener) throws Exception {
                final BatchBuilder builder = serviceContainer.batchBuilder();
                builder.addService(ServiceName.of("7"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("8"));
                builder.addService(ServiceName.of("5"), Service.NULL).addDependencies(ServiceName.of("11"));
                builder.addService(ServiceName.of("3"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("9"));
                builder.addService(ServiceName.of("11"), Service.NULL).addDependencies(ServiceName.of("2"), ServiceName.of("9"), ServiceName.of("10"));
                builder.addService(ServiceName.of("8"), Service.NULL).addDependencies(ServiceName.of("9"));
                builder.addService(ServiceName.of("2"), Service.NULL).addDependencies(ServiceName.of("1"));
                builder.addService(ServiceName.of("9"), Service.NULL);
                builder.addService(ServiceName.of("10"), Service.NULL);
                return Collections.singletonList(builder);
            }

            @Override
            public void handle(BatchBuilder batch, Throwable t) {
                failed = true;
                assertTrue(t instanceof ServiceRegistryException);
                assertTrue(t.getCause() instanceof MissingDependencyException);
            }

            @Override
            public void performAssertions(ServiceContainer serviceContainer) throws Exception {
                assertTrue("Should have thrown missing dependency exception", failed);
            }
        });
    }

    @Test
    public void testCircular() throws Exception {
        perfromTest(new ServiceTestInstance() {
            private boolean failed;
            @Override
            public List<BatchBuilder> initializeBatches(ServiceContainer serviceContainer, LatchedFinishListener finishListener) throws Exception {
                final BatchBuilder builder = serviceContainer.batchBuilder();
                builder.addService(ServiceName.of("7"), Service.NULL).addDependencies(ServiceName.of("5"));
                builder.addService(ServiceName.of("5"), Service.NULL).addDependencies(ServiceName.of("11"));
                builder.addService(ServiceName.of("11"), Service.NULL).addDependencies(ServiceName.of("7"));
                return Collections.singletonList(builder);
            }

            @Override
            public void handle(BatchBuilder batch, Throwable t) {
                failed = true;
                assertTrue(t instanceof ServiceRegistryException);
            }

            @Override
            public void performAssertions(ServiceContainer serviceContainer) throws Exception {
                assertTrue("Should have thrown circular dependency exception", failed);
            }
        });
    }
}
