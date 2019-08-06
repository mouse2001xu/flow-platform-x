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

package com.flowci.core.credential.dao;

import com.flowci.core.credential.domain.Credential;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * @author yang
 */
@Repository
public interface CredentialDao extends MongoRepository<Credential, String>, CustomCredentialDao {

    List<Credential> findAllByCreatedByOrderByCreatedAt(String createdBy);

    Credential findByName(String name);

    Credential findByNameAndCreatedBy(String name, String createdBy);

}
