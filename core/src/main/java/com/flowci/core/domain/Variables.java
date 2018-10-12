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

package com.flowci.core.domain;

/**
 * @author yang
 */
public class Variables {

    public static final String SERVER_URL = "FLOWCI_SERVER_URL";

    public static final String FLOW_NAME = "FLOWCI_FLOW_NAME";

    public static final String JOB_BUILD_NUMBER = "FLOWCI_JOB_BUILD_NUM";

    /**
     * For Current job status when running
     */
    public static final String JOB_STATUS = "FLOWCI_JOB_STATUS";

    public static final String AGENT_WORKSPACE = "FLOWCI_AGENT_WORKSPACE";

    private Variables() {
    }
}