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

package com.flowci.core.test;

import com.flowci.core.config.ConfigProperties;
import com.flowci.core.user.User;
import com.flowci.core.user.UserService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
public class UserServiceTest extends SpringScenario {

    @Autowired
    private UserService userService;

    @Autowired
    private ConfigProperties.Admin adminProperties;

    @Test
    public void should_init_admin_user() {
        User admin = userService.getByEmail(adminProperties.getDefaultEmail());
        Assert.assertNotNull(admin);
    }

}
