/*
 * Copyright 2019 fir.im
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

package com.flowci.core.trigger.converter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.trigger.domain.GitPingTrigger;
import com.flowci.core.trigger.domain.GitPrTrigger;
import com.flowci.core.trigger.domain.GitPrTrigger.Sender;
import com.flowci.core.trigger.domain.GitPrTrigger.Source;
import com.flowci.core.trigger.domain.GitPushTrigger;
import com.flowci.core.trigger.domain.GitPushTrigger.Author;
import com.flowci.core.trigger.domain.GitTrigger;
import com.flowci.core.trigger.domain.GitTrigger.GitEvent;
import com.flowci.core.trigger.domain.GitTrigger.GitSource;
import com.flowci.exception.ArgumentException;
import com.flowci.util.StringHelper;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Log4j2
@Component
public class GitHubConverter implements TriggerConverter {

    public static final String Header = "X-GitHub-Event";

    public static final String Ping = "ping";

    public static final String PushOrTag = "push";

    public static final String PR = "pull_request";

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Optional<GitTrigger> convert(String event, InputStream body) {
        if (event.equals(PushOrTag)) {
            return Optional.ofNullable(onPushOrTag(body));
        }

        if (event.equals(PR)) {
            return Optional.ofNullable(onPullRequest(body));
        }

        if (event.equals(Ping)) {
            return Optional.ofNullable(onPing(body));
        }

        return Optional.empty();
    }

    private GitPingTrigger onPing(InputStream stream) {
        try {
            PingObject ping = objectMapper.readValue(stream, PingObject.class);
            return ping.toTrigger();
        } catch (IOException e) {
            log.warn("Unable to parse Github ping event");
            return null;
        }
    }

    private GitPushTrigger onPushOrTag(InputStream stream) {
        try {
            PushObject push = objectMapper.readValue(stream, PushObject.class);
            return push.toTrigger();
        } catch (IOException e) {
            log.warn("Unable to parse Github push event");
            return null;
        }
    }

    private GitPrTrigger onPullRequest(InputStream stream) {
        try {
            PrObject pr = objectMapper.readValue(stream, PrObject.class);
            return pr.toTrigger();
        } catch (IOException e) {
            log.warn("Unable to parse Github PR event");
            return null;
        }
    }

    private static class PingObject {

        @JsonProperty("hook_id")
        public String hookId;

        public PingHook hook;

        private GitPingTrigger toTrigger() {
            GitPingTrigger trigger = new GitPingTrigger();
            trigger.setSource(GitSource.GITHUB);
            trigger.setEvent(GitEvent.PING);
            trigger.setActive(hook.active);
            trigger.setEvents(hook.events);
            trigger.setCreatedAt(hook.createdAt);
            return trigger;
        }
    }

    private static class PingHook {

        public boolean active;

        public Set<String> events;

        @JsonProperty("created_at")
        public String createdAt;

    }

    private static class PushObject {

        private static final String TagRefPrefix = "refs/tags";

        private static final String PushRefPrefix = "refs/heads";

        public String ref;

        public String compare;

        @JsonProperty("head_commit")
        public CommitObject commit;

        public AuthorObject pusher;

        private GitEvent getEvent() {
            return ref.startsWith(TagRefPrefix) ? GitEvent.TAG : GitEvent.PUSH;
        }

        public GitPushTrigger toTrigger() {
            if (Objects.isNull(commit)) {
                throw new ArgumentException("On commits data on Github push event");
            }

            GitPushTrigger trigger = new GitPushTrigger();
            trigger.setSource(GitSource.GITHUB);
            trigger.setEvent(getEvent());

            trigger.setCommitId(commit.id);
            trigger.setMessage(commit.message);
            trigger.setCommitUrl(commit.url);
            trigger.setCompareUrl(compare);
            trigger.setRef(getBranchName(ref));
            trigger.setTime(commit.timestamp);

            // set commit author info
            trigger.setAuthor(pusher.toAuthor());

            return trigger;
        }

        private static String getBranchName(String ref) {
            if (Strings.isNullOrEmpty(ref)) {
                return StringHelper.EMPTY;
            }

            // find first '/'
            int index = ref.indexOf('/');
            if (index == -1) {
                return StringHelper.EMPTY;
            }

            // find second '/'
            ref = ref.substring(index + 1);
            index = ref.indexOf('/');
            if (index == -1) {
                return StringHelper.EMPTY;
            }

            return ref.substring(index + 1);
        }
    }

    private static class PrObject {

        public static final String PrOpen = "opened";

        public static final String PrClosed = "closed";

        public String action;

        public String number;

        @JsonProperty("pull_request")
        public PrBody prBody;

        @JsonProperty("sender")
        public PrSender prSender;

        public GitPrTrigger toTrigger() {
            GitPrTrigger trigger = new GitPrTrigger();
            trigger.setEvent(action.equals(PrOpen) ? GitEvent.PR_OPEN : GitEvent.PR_CLOSE);
            trigger.setSource(GitSource.GITHUB);

            trigger.setNumber(number);
            trigger.setBody(prBody.body);
            trigger.setTitle(prBody.title);
            trigger.setUrl(prBody.url);
            trigger.setTime(prBody.time);
            trigger.setNumOfCommits(prBody.numOfCommits);
            trigger.setNumOfFileChanges(prBody.numOfFileChanges);
            trigger.setMerged(prBody.merged);

            Source head = new Source();
            head.setCommit(prBody.head.sha);
            head.setRef(prBody.head.ref);
            head.setRepoName(prBody.head.repo.fullName);
            head.setRepoUrl(prBody.head.repo.url);
            trigger.setHead(head);

            Source base = new Source();
            base.setCommit(prBody.base.sha);
            base.setRef(prBody.base.ref);
            base.setRepoName(prBody.base.repo.fullName);
            base.setRepoUrl(prBody.base.repo.url);
            trigger.setBase(base);

            Sender sender = new Sender();
            sender.setId(prSender.id);
            sender.setUsername(prSender.username);
            trigger.setSender(sender);

            return trigger;
        }
    }

    private static class PrBody {

        @JsonProperty("html_url")
        public String url;

        public String title;

        public String body;

        @JsonProperty("created_at")
        public String time;

        public PrSource head;

        public PrSource base;

        @JsonProperty("commits")
        public String numOfCommits;

        @JsonProperty("changed_files")
        public String numOfFileChanges;

        public Boolean merged;
    }

    private static class PrSource {

        public String ref;

        public String sha;

        public PrRepo repo;
    }

    private static class PrRepo {

        public String id;

        @JsonProperty("full_name")
        public String fullName;

        @JsonProperty("html_url")
        public String url;
    }

    private static class PrSender {

        public String id;

        @JsonProperty("login")
        public String username;
    }

    private static class CommitObject {

        public String id;

        public String message;

        public String timestamp;

        public String url;
    }

    private static class AuthorObject {

        public String name;

        public String email;

        public String username;

        public Author toAuthor() {
            Author author = new Author();
            author.setEmail(email);
            author.setName(name);
            author.setUsername(username);
            return author;
        }

    }
}