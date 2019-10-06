/*
 * Copyright 2019 flow.ci
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

package com.flowci.core.stats;

import com.flowci.core.job.domain.Job;
import com.flowci.core.stats.domain.StatsCounter;
import com.flowci.core.stats.domain.StatsItem;

/**
 * Statistic Service
 *
 * @author yang
 */
public interface StatsService {


    /**
     * Add statistic item
     * @param job related job
     * @param type statistic type
     * @param counter statistic value
     */
    StatsItem add(Job job, String type, StatsCounter counter);

}
