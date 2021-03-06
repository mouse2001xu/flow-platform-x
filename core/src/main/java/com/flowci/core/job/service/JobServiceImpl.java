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

package com.flowci.core.job.service;

import static com.flowci.core.trigger.domain.Variables.GIT_AUTHOR;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.common.config.ConfigProperties;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.common.rabbit.RabbitQueueOperation;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.dao.JobItemDao;
import com.flowci.core.job.dao.JobNumberDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.domain.JobItem;
import com.flowci.core.job.domain.JobNumber;
import com.flowci.core.job.domain.JobYml;
import com.flowci.core.job.event.JobCreatedEvent;
import com.flowci.core.job.event.JobDeletedEvent;
import com.flowci.core.job.event.JobStatusChangeEvent;
import com.flowci.core.job.manager.CmdManager;
import com.flowci.core.job.manager.FlowJobQueueManager;
import com.flowci.core.job.manager.YmlManager;
import com.flowci.core.job.util.JobKeyBuilder;
import com.flowci.domain.Agent;
import com.flowci.domain.CmdIn;
import com.flowci.domain.StringVars;
import com.flowci.exception.NotFoundException;
import com.flowci.exception.StatusException;
import com.flowci.store.FileManager;
import com.flowci.tree.Node;
import com.flowci.tree.YmlParser;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Log4j2
@Service
public class JobServiceImpl implements JobService {

    private static final Sort SortByBuildNumber = Sort.by(Direction.DESC, "buildNumber");

    //====================================================================
    //        %% Spring injection
    //====================================================================

    @Autowired
    private String serverUrl;

    @Autowired
    private ConfigProperties.Job jobProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobDao jobDao;

    @Autowired
    private JobItemDao jobItemDao;

    @Autowired
    private JobNumberDao jobNumberDao;

    @Autowired
    private ThreadPoolTaskExecutor jobDeleteExecutor;

    @Autowired
    private CmdManager cmdManager;

    @Autowired
    private YmlManager ymlManager;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private SpringEventManager eventManager;

    @Qualifier("fileManager")
    @Autowired
    private FileManager fileManager;

    @Autowired
    private AgentService agentService;

    @Autowired
    private StepService stepService;

    @Autowired
    private FlowJobQueueManager flowJobQueueManager;

    //====================================================================
    //        %% Public functions
    //====================================================================

    @Override
    public Job get(String jobId) {
        Optional<Job> job = jobDao.findById(jobId);

        if (job.isPresent()) {
            return job.get();
        }

        throw new NotFoundException("Job '{}' not found", jobId);
    }

    @Override
    public Job get(Flow flow, Long buildNumber) {
        String key = JobKeyBuilder.build(flow, buildNumber);
        Optional<Job> optional = jobDao.findByKey(key);

        if (optional.isPresent()) {
            return optional.get();
        }

        throw new NotFoundException(
            "The job {0} for build number {1} cannot found", flow.getName(), Long.toString(buildNumber));
    }

    @Override
    public JobYml getYml(Job job) {
        return ymlManager.get(job);
    }

    @Override
    public Job getLatest(Flow flow) {
        Optional<JobNumber> optional = jobNumberDao.findById(flow.getId());

        if (optional.isPresent()) {
            JobNumber latest = optional.get();
            return get(flow, latest.getNumber());
        }

        throw new NotFoundException("No jobs for flow {0}", flow.getName());
    }

    @Override
    public Page<JobItem> list(Flow flow, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, SortByBuildNumber);
        return jobItemDao.findAllByFlowId(flow.getId(), pageable);
    }

    @Override
    public Job create(Flow flow, Yml yml, Trigger trigger, StringVars input) {
        // verify yml and parse to Node
        Node root = YmlParser.load(flow.getName(), yml.getRaw());

        // create job number
        JobNumber jobNumber = jobNumberDao.increaseBuildNumber(flow.getId());

        // create job
        Job job = new Job();
        job.setKey(JobKeyBuilder.build(flow, jobNumber.getNumber()));
        job.setFlowId(flow.getId());
        job.setTrigger(trigger);
        job.setBuildNumber(jobNumber.getNumber());
        job.setCurrentPath(root.getPathAsString());
        job.setAgentSelector(root.getSelector());
        job.setCreatedAt(Date.from(Instant.now()));
        job.setTimeout(jobProperties.getTimeoutInSeconds());
        job.setExpire(jobProperties.getExpireInSeconds());

        // init job context
        initJobContext(job, flow, input);

        // setup created by form login user or git event author
        if (sessionManager.exist()) {
            job.setCreatedBy(sessionManager.getUserId());
            job.getContext().put(Variables.Job.TriggerBy, sessionManager.get().getEmail());
        } else {
            String createdBy = job.getContext().get(GIT_AUTHOR, "Unknown");
            job.setCreatedBy(createdBy);
            job.getContext().put(Variables.Job.TriggerBy, createdBy);
        }

        long totalExpire = job.getExpire() + job.getTimeout();
        Instant expireAt = Instant.now().plus(totalExpire, ChronoUnit.SECONDS);
        job.setExpireAt(Date.from(expireAt));

        // save
        jobDao.insert(job);

        // create job file space
        try {
            fileManager.create(flow, job);
        } catch (IOException e) {
            jobDao.delete(job);
            throw new StatusException("Cannot create workspace for job");
        }

        // create job yml
        ymlManager.create(flow, job, yml);

        // init job steps as executed cmd
        stepService.init(job);

        eventManager.publish(new JobCreatedEvent(this, job));
        return job;
    }

    @Override
    public Job start(Job job) {
        if (job.getStatus() != Job.Status.PENDING) {
            throw new StatusException("Job not in pending status");
        }

        try {
            return enqueue(job);
        } catch (StatusException e) {
            return setJobStatusAndSave(job, Job.Status.FAILURE, e.getMessage());
        }
    }

    @Override
    public Job cancel(Job job) {
        if (job.isQueuing()) {
            setJobStatusAndSave(job, Job.Status.CANCELLED, "canceled while queued up");
            return job;
        }

        // send stop cmd when is running
        if (!job.isRunning()) {
            return job;
        }

        try {
            Agent agent = agentService.get(job.getAgentId());

            if (agent.isOnline()) {
                CmdIn killCmd = cmdManager.createKillCmd();
                agentService.dispatch(killCmd, agent);
                logInfo(job, " cancel cmd been send to {}", agent.getName());
            } else {
                setJobStatusAndSave(job, Job.Status.CANCELLED, "cancel while agent offline");
            }
        } catch (NotFoundException e) {
            setJobStatusAndSave(job, Job.Status.CANCELLED, "cancel while agent deleted");
        }

        return job;
    }

    @Override
    public void delete(Flow flow) {
        jobDeleteExecutor.execute(() -> {
            jobNumberDao.deleteByFlowId(flow.getId());
            log.info("Deleted: job number of flow {}", flow.getName());

            Long numOfJobDeleted = jobDao.deleteByFlowId(flow.getId());
            log.info("Deleted: {} jobs of flow {}", numOfJobDeleted, flow.getName());

            Long numOfStepDeleted = stepService.delete(flow.getId());
            log.info("Deleted: {} steps of flow {}", numOfStepDeleted, flow.getName());

            eventManager.publish(new JobDeletedEvent(this, flow, numOfJobDeleted));
        });
    }

    @Override
    public boolean isExpired(Job job) {
        Instant expireAt = job.getExpireAt().toInstant();
        return Instant.now().compareTo(expireAt) > 0;
    }

    @Override
    public Job setJobStatusAndSave(Job job, Job.Status newStatus, String message) {
        if (job.getStatus() == newStatus) {
            return jobDao.save(job);
        }

        job.setStatus(newStatus);
        job.setMessage(message);
        job.getContext().put(Variables.Job.Status, newStatus.name());
        jobDao.save(job);
        eventManager.publish(new JobStatusChangeEvent(this, job));
        return job;
    }

    //====================================================================
    //        %% Utils
    //====================================================================

    private void initJobContext(Job job, Flow flow, StringVars... inputs) {
        StringVars context = new StringVars(flow.getVariables());
        context.mergeFromTypedVars(flow.getLocally());

        context.put(Variables.App.Url, serverUrl);
        context.put(Variables.Flow.Name, flow.getName());

        context.put(Variables.Job.Status, Job.Status.PENDING.name());
        context.put(Variables.Job.Trigger, job.getTrigger().toString());
        context.put(Variables.Job.BuildNumber, job.getBuildNumber().toString());
        context.put(Variables.Job.StartAt, job.startAtInStr());
        context.put(Variables.Job.FinishAt, job.finishAtInStr());

        if (!Objects.isNull(inputs)) {
            for (StringVars vars : inputs) {
                context.merge(vars);
            }
        }

        job.getContext().merge(context);
    }

    private Job enqueue(Job job) {
        if (isExpired(job)) {
            setJobStatusAndSave(job, Job.Status.TIMEOUT, "expired before enqueue");
            log.debug("[Job: Timeout] {} has expired", job.getKey());
            return job;
        }

        try {
            RabbitQueueOperation manager = flowJobQueueManager.get(job.getQueueName());

            setJobStatusAndSave(job, Job.Status.QUEUED, null);
            byte[] body = objectMapper.writeValueAsBytes(job);

            manager.send(body, job.getPriority(), job.getExpire());
            logInfo(job, "enqueue");

            return job;
        } catch (Throwable e) {
            throw new StatusException("Unable to enqueue the job {0} since {1}", job.getId(), e.getMessage());
        }
    }

    private void logInfo(Job job, String message, Object... params) {
        log.info("[Job] " + job.getKey() + " " + message, params);
    }
}
