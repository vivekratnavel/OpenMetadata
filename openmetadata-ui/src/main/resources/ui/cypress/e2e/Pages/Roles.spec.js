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

import { uuid } from '../../common/common';

const roles = {
  dataConsumer: 'Data Consumer',
  dataSteward: 'Data Steward',
};

const policies = {
  dataConsumerPolicy: 'Data Consumer Policy',
  dataStewardPolicy: 'Data Steward Policy',
  organizationPolicy: 'Organization Policy',
};

const errorMessageValidation = {
  ifPolicyNotSelected: 'At least one policy is required!',
  ifNameNotEntered: 'invalid name',
  lastPolicyCannotBeRemoved: 'At least one policy is required in a role',
};

const roleName = `Role-test-${uuid()}`;
const description = `This is ${roleName} description`;

const removePolicyFromRole = (policyName) => {
  //Clicking on remove action for added policy
  cy.get(`[data-testid="remove-action-${policyName}"]`)
    .should('be.visible')
    .click();

  cy.get('.ant-modal-body')
    .should('be.visible')
    .should(
      'have.text',
      `Are you sure you want to remove the ${policyName} from ${roleName}?`
    );

  cy.get('[type="button"]').contains('Confirm').should('be.visible').click();
};

describe('Roles page should work properly', () => {
  beforeEach(() => {
    cy.goToHomePage();
    cy.intercept('GET', '*api/v1/roles*').as('getRoles');

    cy.get('[data-testid="appbar-item-settings"]').should('be.visible').click();

    cy.get('[data-menu-id*="roles"]').should('be.visible').click();

    cy.wait('@getRoles', { timeout: 15000 })
      .its('response.statusCode')
      .should('equal', 200);

    cy.url().should('eq', 'http://localhost:8585/settings/access/roles');
  });

  it('Default Role and Policies should be displayed', () => {
    //Verifying the default roles and policies are present

    Object.values(roles).forEach((role) => {
      cy.get('[data-testid="role-name"]')
        .should('contain', role)
        .should('be.visible');
    });

    //Validate policy
    cy.get('[data-testid="policy-link"]')
      .should('contain', policies.dataConsumerPolicy)
      .should('be.visible');
    cy.get('[data-testid="policy-link"]')
      .should('contain', policies.dataStewardPolicy)
      .should('be.visible');
  });

  it('Add new role and check all tabs data', () => {
    cy.get('[data-testid="add-role"]').contains('Add Role').should('be.visible').click();

    //Asserting navigation
    cy.get('[data-testid="inactive-link"]')
      .should('contain', 'Add New Role')
      .should('be.visible');
    //Entering name
    cy.get('#name').should('be.visible').type(roleName);
    //Entering description
    cy.get(
      '.toastui-editor-md-container > .toastui-editor > .ProseMirror'
    ).type(description);
    //Select the policies
    cy.get('.ant-select').should('be.visible').click();

    cy.get('[title="Data Consumer Policy"]')
      .scrollIntoView()
      .should('be.visible')
      .click();

    cy.get('[title="Data Steward Policy"]')
      .scrollIntoView()
      .should('be.visible')
      .click();
    //Clicking outside to close the dropdown
    cy.get('.ant-card-body').click();
    //Save the role
    cy.get('[data-testid="submit-btn"]')
      .scrollIntoView()
      .should('be.visible')
      .click();

    //Verify the role is added successfully
    cy.url().should(
      'eq',
      `http://localhost:8585/settings/access/roles/${roleName}`
    );
    cy.get('[data-testid="inactive-link"]').should('contain', roleName);

    //Verify added description
    cy.get('[data-testid="description"] > [data-testid="viewer-container"]')
      .should('be.visible')
      .should('contain', description);

    // click on the policies tab
    cy.get('[role="tab"]').contains("Policies").should("be.visible").click()

    //Verifying the added policies
    cy.get('.ant-table-cell')
      .should('contain', policies.dataConsumerPolicy)
      .should('be.visible')
      .and('contain', policies.dataStewardPolicy)
      .should('be.visible');
    
    // click on the teams tab
    cy.get('[role="tab"]').contains("Teams").should("be.visible").click()

    // check for empty table
    cy.get("table").should("be.visible")
    cy.get(".ant-empty").should("be.visible")

    // click on the users tab
    cy.get('[role="tab"]').contains("Users").should("be.visible").click()

    // check for empty table
    cy.get("table").should("be.visible")
    cy.get(".ant-empty").should("be.visible")

    //Navigating to roles tab to verify the added role
    cy.get('[data-testid="breadcrumb-link"]').first().click();
    cy.get('table').should('be.visible').should('contain', roleName);
    cy.get('[data-testid="plus-more-count"]').should("be.visible").contains("+1 more").click()

    // second policy should be visible on tooltip
    cy.get('[role="tooltip"]').should("be.visible").contains(policies.dataStewardPolicy)
  });

  it('Add new role without selecting data', () => {
    cy.get('[data-testid="add-role"]').contains('Add Role').should('be.visible').click();

    //Asserting navigation
    cy.get('[data-testid="inactive-link"]')
      .should('contain', 'Add New Role')
      .should('be.visible');
    //Entering name
    cy.get('#name').should('be.visible').type(roleName);
    //Entering description
    cy.get(
      '.toastui-editor-md-container > .toastui-editor > .ProseMirror'
    ).type(description);
    //Do not Select the policies
    //Save the role
    cy.get('[data-testid="submit-btn"]').scrollIntoView().click();

    //Verify the error message that is displayed
    cy.get('[role="alert"]').should(
      'contain',
      errorMessageValidation.ifPolicyNotSelected
    );
  });

  it('Edit created role', () => {
    //Edit description
    cy.get('[data-testid="role-name"]')
      .contains(roleName)
      .should('be.visible')
      .click();
    cy.get('[data-testid="edit-description"]').should('be.visible').click();

    cy.get('.toastui-editor-md-container > .toastui-editor > .ProseMirror')
      .clear()
      .type(`${description}-updated`);
    cy.get('[data-testid="save"]').should('be.visible').click();

    cy.get('[data-testid="inactive-link"]').should('be.visible');
    //Asserting updated description
    cy.get('[data-testid="description"] > [data-testid="viewer-container"]')
      .should('be.visible')
      .should('contain', `${description}-updated`);
  });

  it('Add new policy to created role', () => {
    cy.get('[data-testid="role-name"]')
      .contains(roleName)
      .should('be.visible')
      .click();
    //Asserting navigation
    cy.get('[data-testid="inactive-link"]')
      .should('contain', roleName)
      .should('be.visible');

    cy.get('[data-testid="add-policy"]').should('be.visible').click();
    //Checking the added policy is selected in the add policy modal
    cy.get('[data-testid="policy-row"]')
      .should('contain', policies.dataConsumerPolicy)
      .should('have.class', 'selected');
    cy.get('[data-testid="policy-row"]')
      .should('contain', policies.dataStewardPolicy)
      .should('have.class', 'selected');

    //Add policy
    cy.get('[data-testid="policy-row"]')
      .contains(policies.organizationPolicy)
      .click();

    cy.get('[data-testid="policy-row"]')
      .should('contain', policies.organizationPolicy)
      .should('have.class', 'selected');

    cy.get('[type="button"]').contains('Submit').should('be.visible').click();

    cy.get('[data-testid="entity-name"]')
      .should('contain', policies.organizationPolicy)
      .should('be.visible');
  });

  it('Remove added policy from created role', () => {
    cy.get('[data-testid="role-name"]')
      .contains(roleName)
      .should('be.visible')
      .click();
    //Asserting navigation
    cy.get('[data-testid="inactive-link"]')
      .should('contain', roleName)
      .should('be.visible');

    removePolicyFromRole(policies.organizationPolicy);

    //Validating if the policy is removed successfully
    cy.get('[data-testid="entity-name"]').should(
      'not.contain',
      policies.organizationPolicy
    );
  });

  it('Check if last policy is not removed', () => {
    cy.intercept({ method: 'GET', url: `/api/v1/roles/name/${roleName}*` }).as(
      'getSelectedRole'
    );

    cy.get('[data-testid="role-name"]')
      .contains(roleName)
      .should('be.visible')
      .click();

    //Asserting navigation
    cy.get('[data-testid="inactive-link"]')
      .should('contain', roleName)
      .should('be.visible');

    cy.wait('@getSelectedRole').its('response.statusCode').should('eq', 200);

    cy.intercept({ method: 'PATCH', url: '/api/v1/roles/*' }).as(
      'checkDeletedRole'
    );
    //Removing second policy from the role
    removePolicyFromRole(policies.dataStewardPolicy);

    //Validating if the policy is removed successfully
    cy.get('[data-testid="entity-name"]').should(
      'not.contain',
      policies.dataStewardPolicy
    );

    cy.wait('@checkDeletedRole').its('response.statusCode').should('eq', 200);

    //Removing the last policy and validating the error message
    removePolicyFromRole(policies.dataConsumerPolicy);

    cy.get('.Toastify__toast-body')
      .should('be.visible')
      .should('contain', errorMessageValidation.lastPolicyCannotBeRemoved);

    cy.get('[data-testid="entity-name"]').should(
      'contain',
      policies.dataConsumerPolicy
    );
  });

  it('Delete created Role', () => {
    cy.get(`[data-testid="delete-action-${roleName}"]`)
      .should('be.visible')
      .click();

    cy.get('[data-testid="confirmation-text-input"]')
      .should('be.visible')
      .type('DELETE');

    cy.get('[data-testid="confirm-button"]').should('be.visible').click();

    //Validate deleted role
    cy.get('[data-testid="role-name"]').should('not.contain', roleName);
  });
});
