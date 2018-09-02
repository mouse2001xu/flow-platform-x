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

package com.flowci.agent.dao.test;

import com.flowci.agent.SpringScenario;
import com.flowci.agent.dao.AgentCmdDao;
import com.flowci.agent.domain.AgentCmd;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
public class AgentCmdDaoTest extends SpringScenario {

    @Autowired
    private AgentCmdDao agentCmdDao;

    @Test
    public void should_save_and_load_agent_cmd() {
        AgentCmd cmd = new AgentCmd();
        cmd.setId("1-hello/world");
        cmd.getInputs().putString("hello", "world");
        cmd.setScripts(Lists.newArrayList("h2"));
        cmd.setEnvFilters(Sets.newHashSet("FLOW_"));

        AgentCmd saved = agentCmdDao.save(cmd);
        Assert.assertNotNull(saved);

        AgentCmd loaded = agentCmdDao.findById(cmd.getId()).get();
        Assert.assertNotNull(loaded);
        Assert.assertEquals(cmd, loaded);
    }
}
