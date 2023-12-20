/*
 * Copyright 2021-2024 Open Text.
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
package com.github.jobservice.workers.taskunstowing.queue;

import com.github.jobservice.workers.taskunstowing.factory.TaskUnstowingWorkerConfiguration;
import com.hpe.caf.api.Codec;
import com.hpe.caf.util.rabbitmq.RabbitUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public final class QueueServicesFactory
{
    public static QueueServices create(final TaskUnstowingWorkerConfiguration configuration, final Codec codec)
        throws IOException, TimeoutException
    {
        final Connection connection = createConnection(configuration);
        final Channel publisherChannel = connection.createChannel();
        publisherChannel.confirmSelect();
        return new QueueServices(connection, publisherChannel, codec);
    }

    private static Connection createConnection(final TaskUnstowingWorkerConfiguration configuration)
        throws IOException, TimeoutException
    {
        return RabbitUtil.createRabbitConnection(
            configuration.getRabbitMQHost(),
            configuration.getRabbitMQPort(),
            configuration.getRabbitMQUsername(),
            configuration.getRabbitMQPassword());
    }

    private QueueServicesFactory()
    {
    }
}
