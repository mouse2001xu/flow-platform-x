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

package com.flowci.core.flow.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.core.common.domain.Mongoable;
import com.flowci.core.common.domain.Pathable;
import com.flowci.core.common.domain.Variables;
import com.flowci.domain.VariableMap;
import com.flowci.util.StringHelper;
import java.util.Set;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author yang
 */
@Document(collection = "flow")
@Getter
@Setter
@NoArgsConstructor
@ToString(of = {"name"}, callSuper = true)
public final class Flow extends Mongoable implements Pathable {

    public enum Status {
        PENDING,

        CONFIRMED
    }

    @NonNull
    @Indexed(name = "index_flow_name")
    private String name;

    @NonNull
    private Status status = Status.PENDING;

    @NonNull
    private VariableMap variables = new VariableMap();

    private WebhookStatus webhookStatus;

    public Flow(String name) {
        this.name = name;
    }

    @JsonIgnore
    public String getQueueName() {
        return "queue.flow." + id + ".job";
    }

    @JsonIgnore
    public boolean hasGitUrl() {
        String val = variables.get(Variables.Flow.GitUrl);
        return StringHelper.hasValue(val);
    }

    @JsonIgnore
    public boolean hasCredential() {
        String val = variables.get(Variables.Flow.SSH_RSA);
        return StringHelper.hasValue(val);
    }

    @JsonIgnore
    @Override
    public String pathName() {
        return getId();
    }

    @Data
    public static class WebhookStatus {

        private boolean added;

        private String createdAt;

        private Set<String> events;
    }
}
