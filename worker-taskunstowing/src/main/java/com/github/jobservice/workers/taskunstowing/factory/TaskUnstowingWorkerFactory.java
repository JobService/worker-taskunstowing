/*
 * Copyright 2021 Micro Focus or one of its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jobservice.workers.taskunstowing.factory;

import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.worker.document.extensibility.DocumentWorker;
import com.hpe.caf.worker.document.extensibility.DocumentWorkerFactory;
import com.hpe.caf.worker.document.model.Application;
import com.github.jobservice.workers.taskunstowing.TaskUnstowingWorker;
import com.github.jobservice.workers.taskunstowing.database.DatabaseClient;
import com.github.jobservice.workers.taskunstowing.queue.QueueServices;
import com.github.jobservice.workers.taskunstowing.queue.QueueServicesFactory;
import com.hpe.caf.api.Codec;
import com.hpe.caf.util.ModuleLoader;
import com.hpe.caf.util.ModuleLoaderException;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TaskUnstowingWorkerFactory implements DocumentWorkerFactory
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskUnstowingWorkerFactory.class);

    @Override
    public DocumentWorker createDocumentWorker(final Application application)
    {
        final ConfigurationSource configurationSource = application.getService(ConfigurationSource.class);
        final TaskUnstowingWorkerConfiguration configuration;
        try {
            configuration = configurationSource.getConfiguration(TaskUnstowingWorkerConfiguration.class);
        } catch (final ConfigurationException e) {
            LOGGER.error("Failed to load Task Unstowing Worker configuration", e);
            return new UnhealthyWorker(e.getMessage());
        }

        final Codec codec;
        try {
            codec = ModuleLoader.getService(Codec.class);
        } catch (final ModuleLoaderException e) {
            LOGGER.error("Failed to load Task Unstowing Worker codec", e);
            return new UnhealthyWorker(e.getMessage());
        }

        final QueueServices queueServices;
        try {
            queueServices = QueueServicesFactory.create(configuration, codec);
        } catch (final IOException | TimeoutException e) {
            LOGGER.error("Failed to load Task Unstowing Worker queue services", e);
            return new UnhealthyWorker(e.getMessage());
        }

        return new TaskUnstowingWorker(new DatabaseClient(configuration), configuration.getDatabaseMaximumBatchSize(), queueServices);
    }
}
