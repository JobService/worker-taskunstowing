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
package com.github.jobservice.workers.taskunstowing.queue;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

public final class QueueServices implements AutoCloseable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueServices.class);
    private static final int WAIT_FOR_PUBLISHER_CHANNEL_CONFIRMS_TIMEOUT_MS = 10000;
    private final Connection connection;
    private final Channel publisherChannel;
    private final Codec codec;

    public QueueServices(final Connection connection, final Channel publisherChannel, final Codec codec)
    {
        this.connection = Objects.requireNonNull(connection);
        this.publisherChannel = Objects.requireNonNull(publisherChannel);
        this.codec = Objects.requireNonNull(codec);
    }

    public void sendTaskMessage(final TaskMessage taskMessage) throws IOException, InterruptedException, TimeoutException
    {
        final String targetQueue = taskMessage.getTo();
        this.publisherChannel.queueDeclarePassive(targetQueue);
        final byte[] taskMessageBytes;
        try {
            taskMessageBytes = codec.serialise(taskMessage);
        } catch (final CodecException e) {
            throw new RuntimeException(e);
        }

        try {
            publishTaskMessage(taskMessageBytes, targetQueue);
            LOGGER.debug("Published a task message to the {} queue: {}", targetQueue, new String(taskMessageBytes, "UTF-8"));
        } catch (IOException | TimeoutException e) {
            LOGGER.warn("Failed to publish a task message to the {} queue. Will retry", targetQueue, e);
            publishTaskMessage(taskMessageBytes, targetQueue);
        }
    }

    private void publishTaskMessage(final byte[] taskMessageBytes, final String targetQueue)
        throws IOException, InterruptedException, TimeoutException
    {
        publisherChannel.basicPublish("", targetQueue, MessageProperties.PERSISTENT_TEXT_PLAIN, taskMessageBytes);
        publisherChannel.waitForConfirmsOrDie(WAIT_FOR_PUBLISHER_CHANNEL_CONFIRMS_TIMEOUT_MS);
    }

    @Override
    public void close()
    {
        try {
            if (publisherChannel != null) {
                publisherChannel.close();
            }

            if (connection != null) {
                connection.close();
            }
        } catch (IOException | TimeoutException e) {
            LOGGER.error("Failed to close the queuing connection", e);
        }
    }
}
