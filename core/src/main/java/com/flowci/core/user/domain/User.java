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

package com.flowci.core.user.domain;

import com.flowci.core.common.domain.Mongoable;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author yang
 */

@Document(collection = "user")
@Getter
@Setter
@ToString(of = {"email"}, callSuper = true)
public class User extends Mongoable {

    public enum Role {
        Admin,

        Developer
    }

    @NonNull
    @Indexed(unique = true, name = "index_user_email")
    private String email;

    @NonNull
    private String passwordOnMd5;

    @NonNull
    private Role role;

    public User(String email, String passwordOnMd5) {
        this.email = email;
        this.passwordOnMd5 = passwordOnMd5;
    }

    public boolean isAdmin() {
        return role == Role.Admin;
    }
}
