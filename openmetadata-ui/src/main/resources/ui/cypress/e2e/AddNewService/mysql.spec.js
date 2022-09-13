/*
 *  Copyright 2021 Collate
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import { deleteCreatedService, editOwnerforCreatedService, goToAddNewServicePage, testServiceCreationAndIngestion, uuid } from '../../common/common';
import { SERVICE_TYPE } from '../../constants/constants';

const serviceType = 'Mysql';
const serviceName = `${serviceType}-ct-test-${uuid()}`;

describe('MySQL Ingestion', () => {
  it('add and ingest data', () => {
    goToAddNewServicePage(SERVICE_TYPE.Database);
    const connectionInput = () => {
      cy.get('#root_username').type('openmetadata_user');
      cy.get('#root_password').type('openmetadata_password');
      cy.get('#root_hostPort').type('172.16.239.10:3306');
      cy.get('#root_databaseSchema').type('openmetadata_db');
    };

    const addIngestionInput = () => {
      cy.get('[data-testid="schema-filter-pattern-checkbox"]').check();
      cy.get('[data-testid="filter-pattern-includes-schema"]')
        .should('be.visible')
        .type('openmetadata_db');
    };

    testServiceCreationAndIngestion(
      serviceType,
      connectionInput,
      addIngestionInput,
      serviceName
    );
  });

  it('Edit and validate owner', () => {
    editOwnerforCreatedService(SERVICE_TYPE.Database, serviceName);
  });

  it('delete created service', () => {
    deleteCreatedService(SERVICE_TYPE.Database, serviceName);
  });
});
