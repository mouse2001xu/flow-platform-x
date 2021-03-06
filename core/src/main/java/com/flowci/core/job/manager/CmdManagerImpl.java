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

package com.flowci.core.job.manager;

import com.flowci.core.job.domain.Job;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.domain.Input;
import com.flowci.core.plugin.service.PluginService;
import com.flowci.domain.*;
import com.flowci.exception.ArgumentException;
import com.flowci.tree.Node;
import com.flowci.util.ObjectsHelper;
import com.flowci.util.StringHelper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

/**
 * @author yang
 */
@Repository
public class CmdManagerImpl implements CmdManager {

    @Autowired
    private PluginService pluginService;

    @Override
    public CmdId createId(Job job, Node node) {
        return new CmdId(job.getId(), node.getPath().getPathInStr());
    }

    @Override
    public CmdIn createShellCmd(Job job, Node node) {
        // node envs has top priority;
        Vars<String> inputs = new StringVars()
                .merge(job.getContext())
                .merge(node.getEnvironments());

        String script = node.getScript();
        boolean allowFailure = node.isAllowFailure();
        Set<String> exports = node.getExports();

        if (node.hasPlugin()) {
            Plugin plugin = pluginService.get(node.getPlugin());
            verifyPluginInput(inputs, plugin);

            script = plugin.getScript();
            exports.addAll(plugin.getExports());

            if (plugin.getAllowFailure() != null) {
                allowFailure = plugin.getAllowFailure();
            }
        }

        // create cmd based on plugin
        CmdIn cmd = new CmdIn(createId(job, node).toString(), CmdType.SHELL);
        cmd.setInputs(inputs);
        cmd.setAllowFailure(allowFailure);
        cmd.setEnvFilters(Sets.newHashSet(exports));
        cmd.setScripts(Lists.newArrayList(script));
        cmd.setPlugin(node.getPlugin());

        // default work dir is {agent dir}/{flow id}
        cmd.setWorkDir(job.getFlowId());

        return cmd;
    }

    @Override
    public CmdIn createKillCmd() {
        return new CmdIn(UUID.randomUUID().toString(), CmdType.KILL);
    }

    private void verifyPluginInput(Vars<String> context, Plugin plugin) {
        for (Input input : plugin.getInputs()) {
            String value = context.get(input.getName());

            // setup plugin default value to context
            if (!StringHelper.hasValue(value) && input.hasDefaultValue()) {
                context.put(input.getName(), input.getValue());
                continue;
            }

            // verify value from context
            if (!input.verify(value)) {
                throw new ArgumentException(
                        "The illegal input {0} for plugin {1}", input.getName(), plugin.getName());
            }
        }
    }
}
