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

import com.hpe.caf.api.Configuration;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Configuration
public final class TaskUnstowingWorkerConfiguration
{
    @NotNull
    private String databaseHost;

    @Min(1)
    private int databasePort;

    @NotNull
    private String databaseName;

    @NotNull
    private String databaseTableName;

    @NotNull
    private String databaseUsername;

    @NotNull
    private String databasePassword;

    @NotNull
    private String databaseAppName;

    @Min(1)
    private int databaseMaximumPoolSize;

    @NotNull
    private String rabbitMQHost;

    @Min(1)
    private int rabbitMQPort;

    @NotNull
    private String rabbitMQUsername;

    @NotNull
    private String rabbitMQPassword;

    public String getDatabaseHost()
    {
        return databaseHost;
    }

    public void setDatabaseHost(final String databaseHost)
    {
        this.databaseHost = databaseHost;
    }

    public int getDatabasePort()
    {
        return databasePort;
    }

    public void setDatabasePort(final int databasePort)
    {
        this.databasePort = databasePort;
    }

    public String getDatabaseName()
    {
        return databaseName;
    }

    public void setDatabaseName(final String databaseName)
    {
        this.databaseName = databaseName;
    }

    public String getDatabaseTableName()
    {
        return databaseTableName;
    }

    public void setDatabaseTableName(final String databaseTableName)
    {
        this.databaseTableName = databaseTableName;
    }

    public String getDatabaseUsername()
    {
        return databaseUsername;
    }

    public void setDatabaseUsername(final String databaseUsername)
    {
        this.databaseUsername = databaseUsername;
    }

    public String getDatabasePassword()
    {
        return databasePassword;
    }

    public void setDatabasePassword(final String databasePassword)
    {
        this.databasePassword = databasePassword;
    }

    public String getDatabaseAppName()
    {
        return databaseAppName;
    }

    public void setDatabaseAppName(final String databaseAppName)
    {
        this.databaseAppName = databaseAppName;
    }

    public int getDatabaseMaximumPoolSize()
    {
        return databaseMaximumPoolSize;
    }

    public void setDatabaseMaximumPoolSize(final int databaseMaximumPoolSize)
    {
        this.databaseMaximumPoolSize = databaseMaximumPoolSize;
    }

    public String getRabbitMQHost()
    {
        return rabbitMQHost;
    }

    public void setRabbitMQHost(final String rabbitMQHost)
    {
        this.rabbitMQHost = rabbitMQHost;
    }

    public int getRabbitMQPort()
    {
        return rabbitMQPort;
    }

    public void setRabbitMQPort(final int rabbitMQPort)
    {
        this.rabbitMQPort = rabbitMQPort;
    }

    public String getRabbitMQUsername()
    {
        return rabbitMQUsername;
    }

    public void setRabbitMQUsername(final String rabbitMQUsername)
    {
        this.rabbitMQUsername = rabbitMQUsername;
    }

    public String getRabbitMQPassword()
    {
        return rabbitMQPassword;
    }

    public void setRabbitMQPassword(final String rabbitMQPassword)
    {
        this.rabbitMQPassword = rabbitMQPassword;
    }
}
