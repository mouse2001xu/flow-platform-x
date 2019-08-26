/*
 * Copyright 2018 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flowci.core.job.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.core.common.domain.Mongoable;
import com.flowci.core.common.domain.Pathable;
import com.flowci.domain.VariableMap;
import com.flowci.tree.Selector;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * @author yang
 */
@Getter
@Setter
@Document(collection = "job")
public class Job extends Mongoable implements Pathable {

    public static Pathable path(String id) {
        Job job = new Job();
        job.setId(id);
        return job;
    }

    public enum Trigger {

        /**
         * Scheduler trigger
         */
        SCHEDULER,

        /**
         * Api trigger
         */
        API,

        /**
         * Manual trigger
         */
        MANUAL,

        /**
         * Git push event
         */
        PUSH,

        /**
         * Git PR open event
         */
        PR_OPEN,

        /**
         * Git PR close event
         */
        PR_CLOSE,

        /**
         * Git tag event
         */
        TAG
    }

    public enum Status {

        /**
         * Initial job state
         */
        PENDING,

        /**
         * Been put to job queue
         */
        QUEUED,

        /**
         * Agent take over the job, and been start to execute
         */
        RUNNING,

        /**
         * Job been executed
         */
        SUCCESS,

        /**
         * Job been executed but failure
         */
        FAILURE,

        /**
         * Job been cancelled by user
         */
        CANCELLED,

        /**
         * Job execution time been over the expiredAt
         */
        TIMEOUT
    }

    private final static Integer MinPriority = 1;

    private final static Integer MaxPriority = 255;

    /**
     * Job key is generated from {flow id}-{build number}
     */
    @Indexed(name = "index_job_key", unique = true)
    private String key;

    @Indexed(name = "index_flow_id", sparse = true)
    private String flowId;

    private Long buildNumber;

    private Trigger trigger;

    private Status status = Status.PENDING;

    private Date expireAt;

    private Selector agentSelector;

    private String agentId;

    private String currentPath;

    private VariableMap context = new VariableMap();

    private String message;

    private Integer priority = MinPriority;

    @JsonIgnore
    public boolean isRunning() {
        return status == Status.RUNNING;
    }

    @JsonIgnore
    public boolean isQueuing() {
        return status == Status.QUEUED;
    }

    @JsonIgnore
    public boolean isPending() {
        return status == Status.PENDING;
    }

    @JsonIgnore
    public boolean isDone() {
        return status == Status.TIMEOUT
                || status == Status.CANCELLED
                || status == Status.FAILURE
                || status == Status.SUCCESS;
    }

    @JsonIgnore
    public Integer increase() {
        if (this.priority < MaxPriority) {
            this.priority++;
        }

        return this.priority;
    }

    @JsonIgnore
    public String getQueueName() {
        return "queue.flow." + flowId + ".job";
    }

    @JsonIgnore
    @Override
    public String pathName() {
        return getId();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
