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

package com.flowci.core.test.job;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.domain.StatusCode;
import com.flowci.core.job.domain.CreateJob;
import com.flowci.core.job.domain.Job;
import com.flowci.core.test.MvcMockHelper;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.http.ResponseMessage;
import com.flowci.util.StringHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * @author yang
 */
@FixMethodOrder(value = MethodSorters.JVM)
public class JobControllerTest extends SpringScenario {

    private static final TypeReference<ResponseMessage<Job>> JobType = new TypeReference<ResponseMessage<Job>>() {
    };

    @Autowired
    private MvcMockHelper mvcMockHelper;

    @Autowired
    private ObjectMapper objectMapper;

    private final String flow = "hello-flow";

    @Before
    public void init() throws Exception {
        mockLogin();
        createFlow(flow);
    }

    @Test
    public void should_get_job_by_name_and_build_number() throws Exception {
        // init: create job
        Job created = createJobForFlow(flow);
        Assert.assertNotNull(created);

        // when:
        ResponseMessage<Job> response = mvcMockHelper.expectSuccessAndReturnClass(get("/jobs/hello-flow/1"), JobType);
        Assert.assertEquals(StatusCode.OK, response.getCode());

        // then:
        Job loaded = response.getData();
        Assert.assertNotNull(loaded);
        Assert.assertEquals(created, loaded);
    }

    public Job createJobForFlow(String name) throws Exception {
        return mvcMockHelper.expectSuccessAndReturnClass(post("/jobs")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(new CreateJob(name))), JobType)
            .getData();
    }

    public void createFlow(String name) throws Exception {
        String yml = StringHelper.toString(load("flow.yml"));

        ResponseMessage message = mvcMockHelper.expectSuccessAndReturnClass(post("/flows/" + name)
            .contentType(MediaType.TEXT_PLAIN)
            .content(yml), ResponseMessage.class);

        Assert.assertEquals(StatusCode.OK, message.getCode());
    }
}
