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

const uuid = () => Cypress._.random(0, 1e6);

const testServiceCreationAndIngestion = (
  serviceType,
  connectionInput,
  addIngestionInput
) => {
  const serviceName = `${serviceType}-ci-test-${uuid()}`;

  // Select Service in step 1
  cy.get(`[data-testid="${serviceType}"]`).should('exist').click();
  cy.get('[data-testid="next-button"]').should('exist').click();

  // Enter service name in step 2
  cy.get('[data-testid="service-name"]').should('exist').type(serviceName);
  cy.get('[data-testid="next-button"]').click();

  // Connection Details in step 3
  cy.get('[data-testid="add-new-service-container"]')
    .parent()
    .parent()
    .scrollTo('top', {
      ensureScrollable: false,
    });
  cy.contains('Connection Details').should('be.visible');

  connectionInput();

  cy.get('[data-testid="test-connection-btn"]').should('exist');
  cy.get('[data-testid="test-connection-btn"]').click();

  cy.contains('Connection test was successful').should('exist');
  cy.get('[data-testid="submit-btn"]').should('exist').click();

  // check success
  cy.get('[data-testid="success-line"]').should('be.visible');
  cy.contains(`"${serviceName}"`).should('be.visible');
  cy.contains('has been created successfully').should('be.visible');

  cy.get('[data-testid="add-ingestion-button"]').should('be.visible');
  cy.get('[data-testid="add-ingestion-button"]').click();

  // Add ingestion page
  cy.get('[data-testid="schema-filter-pattern-checkbox"]').should('be.visible');
  cy.get('[data-testid="add-ingestion-container"]').should('be.visible');

  addIngestionInput();
  cy.get('[data-testid="next-button"]').should('exist').click();

  // Configure DBT Model
  cy.contains('Configure DBT Model').should('be.visible');
  cy.get('[data-testid="dbt-source"]').should('be.visible').select('');

  cy.get('[data-testid="submit-btn"]').should('be.visible').click();

  // Schedule & Deploy
  cy.contains('Schedule for Ingestion').should('be.visible');
  cy.get('[data-testid="deploy-button"]').should('be.visible');
  cy.get('[data-testid="deploy-button"]').click();

  // check success
  cy.get('[data-testid="success-line"]').should('be.visible');
  cy.contains(`"${serviceName}_metadata"`).should('be.visible');
  cy.contains('has been created and deployed successfully').should(
    'be.visible'
  );
  // On the Right panel
  cy.contains('Metadata Ingestion Added & Deployed Successfully').should(
    'be.visible'
  );

  // wait for ingestion to run
  cy.clock();
  cy.wait(30000);

  cy.get('[data-testid="view-service-button"]').should('be.visible');
  cy.get('[data-testid="view-service-button"]').click();

  // ingestions page
  const testIngestionsTab = () => {
    cy.get('[data-testid="Ingestions"]').should('be.visible');
    cy.get('[data-testid="Ingestions"] >> [data-testid="filter-count"]').should(
      'have.text',
      1
    );
    cy.get('[data-testid="Ingestions"]').click();
    cy.get('[data-testid="add-new-ingestion-button"]').should('be.visible');
  };

  const retryTimes = 10;
  let retryCount = 0;
  const checkSuccessState = () => {
    testIngestionsTab();
    retryCount++;
    // the latest run should be success
    cy.get('.tableBody-row > :nth-child(4)').then(($ingestionStatus) => {
      if ($ingestionStatus.text() === 'Running' && retryCount <= retryTimes) {
        // retry after waiting for 30 seconds
        cy.wait(30000);
        cy.reload();
        checkSuccessState();
      } else {
        cy.get('.tableBody-row > :nth-child(4)').should('have.text', 'Success');
      }
    });
  };

  checkSuccessState();
};

describe('login with SSO', () => {
  beforeEach(() => {
    cy.visit('http://localhost:8585');
  });

  // Secure Mode
  // it('displays sign in page', () => {
  //   cy.get('.tw-signin-button').should('have.length', 1);
  //   cy.get('.tw-signin-button span').should('have.text', 'Sign in with Google');
  // });

  describe('test services', () => {
    beforeEach(() => {
      cy.visit('http://localhost:8585');
      // cy.loginByGoogleApi();
      cy.get('[data-testid="WhatsNewModalFeatures"]').should('be.visible');
      cy.get('[data-testid="closeWhatsNew"]').click();
      cy.get('[data-testid="WhatsNewModalFeatures"]').should('not.exist');
    });

    it('shows homepage', () => {
      cy.get('[data-testid="tables"]').should('be.visible');
    });

    describe('add new service', () => {
      beforeEach(() => {
        cy.get('[data-testid="menu-button"]').should('be.visible');
        cy.get('[data-testid="menu-button"]').first().click();
        cy.get('[data-testid="menu-item-Services"]')
          .should('be.visible')
          .click();

        // Services page
        cy.contains('Services').should('be.visible');
        cy.get('.activeCategory > .tw-py-px').then(($databaseServiceCount) => {
          if ($databaseServiceCount.text() === '0') {
            cy.get('[data-testid="add-service-button"]')
              .should('be.visible')
              .click();
          } else {
            cy.get('[data-testid="add-new-service-button"]')
              .should('be.visible')
              .click();
          }
        });

        // Add new service page
        cy.url().should('include', 'databaseServices/add-service');
        cy.get('[data-testid="header"]').should('be.visible');
        cy.contains('Add New Service').should('be.visible');
        cy.get('[data-testid="service-category"]').should('be.visible');
      });

      it('mysql', () => {
        const connectionInput = () => {
          cy.get('#root_username').type('openmetadata_user');
          cy.get('#root_password').type('openmetadata_password');
          cy.get('#root_hostPort').type('172.16.239.10:3306');
          cy.get('#root_database').type('openmetadata_db');
        };

        const addIngestionInput = () => {
          cy.get('[data-testid="schema-filter-pattern-checkbox"]').check();
          cy.get('[data-testid="filter-pattern-includes-schema"]')
            .should('be.visible')
            .type('openmetadata_db');
        };

        testServiceCreationAndIngestion(
          'Mysql',
          connectionInput,
          addIngestionInput
        );
      });

      it('snowflake', () => {
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
            .type('testschema');
        };

        testServiceCreationAndIngestion(
          'Snowflake',
          connectionInput,
          addIngestionInput
        );
      });
    });
  });
});
