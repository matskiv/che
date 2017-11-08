/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.selenium.dashboard.organization;

import static org.eclipse.che.commons.lang.NameGenerator.generate;
import static org.eclipse.che.selenium.pageobject.dashboard.NavigationBar.MenuItem.ORGANIZATIONS;
import static org.testng.Assert.assertEquals;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eclipse.che.multiuser.organization.shared.dto.OrganizationDto;
import org.eclipse.che.selenium.core.client.TestOrganizationServiceClient;
import org.eclipse.che.selenium.core.user.TestUser;
import org.eclipse.che.selenium.core.utils.WaitUtils;
import org.eclipse.che.selenium.pageobject.dashboard.ConfirmDialog;
import org.eclipse.che.selenium.pageobject.dashboard.Dashboard;
import org.eclipse.che.selenium.pageobject.dashboard.NavigationBar;
import org.eclipse.che.selenium.pageobject.dashboard.organization.OrganizationListPage;
import org.eclipse.che.selenium.pageobject.dashboard.organization.OrganizationPage;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test validates organization deletion.
 *
 * @author Ann Shumilova
 */
public class DeleteOrganizationTest {
  private static final String PARENT_ORG_NAME = generate("organization", 5);
  private static final String CHILD_ORG_NAME = generate("organization", 5);

  private OrganizationDto parentOrganization;
  private OrganizationDto childOrganization;

  @Inject
  @Named("admin")
  private TestOrganizationServiceClient testOrganizationServiceClient;

  @Inject private OrganizationListPage organizationListPage;
  @Inject private OrganizationPage organizationPage;
  @Inject private NavigationBar navigationBar;
  @Inject private ConfirmDialog confirmDialog;
  @Inject private Dashboard dashboard;
  @Inject private TestUser testUser;

  @BeforeClass
  public void setUp() throws Exception {
    parentOrganization = testOrganizationServiceClient.create(PARENT_ORG_NAME);
    childOrganization = testOrganizationServiceClient.create(CHILD_ORG_NAME);
    testOrganizationServiceClient.addAdmin(parentOrganization.getId(), testUser.getId());
    testOrganizationServiceClient.addAdmin(childOrganization.getId(), testUser.getId());

    dashboard.open(testUser.getName(), testUser.getPassword());
  }

  @AfterClass
  public void tearDown() throws Exception {
    for (OrganizationDto organization : testOrganizationServiceClient.getAll())
      testOrganizationServiceClient.deleteById(organization.getId());
  }

  @Test
  public void testSubOrganizationDelete() {
    navigationBar.waitNavigationBar();
    navigationBar.clickOnMenu(ORGANIZATIONS);
    organizationListPage.waitForOrganizationsToolbar();
    organizationListPage.waitForOrganizationsList();
    organizationListPage.clickOnOrganization(childOrganization.getQualifiedName());
    organizationPage.waitOrganizationName(childOrganization.getName());

    // Delete organization
    organizationPage.clickDeleteOrganizationButton();
    confirmDialog.waitOpened();
    assertEquals(confirmDialog.getTitle(), "Delete organization");
    assertEquals(
        confirmDialog.getMessage(),
        "Would you like to delete organization '" + childOrganization.getName() + "'?");
    assertEquals(confirmDialog.getConfirmButtonTitle(), "Delete");
    confirmDialog.clickConfirm();
    confirmDialog.waitClosed();

    // Test that organization deleted
    WaitUtils.sleepQuietly(1);
    organizationListPage.waitForOrganizationsList();
    organizationListPage.waitForOrganizationIsRemoved(childOrganization.getQualifiedName());
    assertEquals(navigationBar.getMenuCounterValue(ORGANIZATIONS), 1);
    assertEquals(organizationListPage.getOrganizationListItemCount(), 1);
  }

  @Test(priority = 1)
  public void testParentOrganizationDeletion() {
    navigationBar.waitNavigationBar();
    navigationBar.clickOnMenu(ORGANIZATIONS);
    organizationListPage.waitForOrganizationsToolbar();
    organizationListPage.waitForOrganizationsList();
    organizationListPage.clickOnOrganization(parentOrganization.getName());
    organizationPage.waitOrganizationName(parentOrganization.getName());
    organizationPage.clickDeleteOrganizationButton();
    confirmDialog.waitOpened();

    assertEquals(confirmDialog.getTitle(), "Delete organization");
    assertEquals(
        confirmDialog.getMessage(),
        "Would you like to delete organization '" + parentOrganization.getName() + "'?");
    assertEquals(confirmDialog.getConfirmButtonTitle(), "Delete");

    confirmDialog.clickConfirm();
    confirmDialog.waitClosed();

    organizationListPage.waitForOrganizationsEmptyList();
    assertEquals(navigationBar.getMenuCounterValue(ORGANIZATIONS), 0);
  }
}
