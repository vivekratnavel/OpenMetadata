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

const serviceType = 'Snowflake';
const serviceName = `${serviceType}-ct-test-${uuid()}`;

describe('Snowflake Ingestion', () => {
  it('add and ingest data', { defaultCommandTimeout: 8000 }, () => {
    goToAddNewServicePage(SERVICE_TYPE.Database);
    const connectionInput = () => {
      cy.get('#root_username').type(Cypress.env('snowflakeUsername'));
      cy.get('#root_password').type(Cypress.env('snowflakePassword'));
      cy.get('#root_account').type(Cypress.env('snowflakeAccount'));
      cy.get('#root_database').type(Cypress.env('snowflakeDatabase'));
      cy.get('#root_warehouse').type(Cypress.env('snowflakeWarehouse'));
    };

    const addIngestionInput = () => {
      cy.get('[data-testid="schema-filter-pattern-checkbox"]').check();
      cy.get('[data-testid="filter-pattern-includes-schema"]')
        .should('be.visible')
        .type('public');
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
