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
package org.eclipse.che.api.workspace.server.spi.environment;

import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.che.api.core.ValidationException;
import org.eclipse.che.api.core.model.workspace.config.ServerConfig;
import org.eclipse.che.api.installer.server.InstallerRegistry;
import org.eclipse.che.api.workspace.server.model.impl.ServerConfigImpl;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Tests {@link InternalEnvironmentFactory}.
 *
 * @author Sergii Leshchenko
 */
@Listeners(MockitoTestNGListener.class)
public class InternalEnvironmentFactoryTest {

  @Mock private InstallerRegistry installerRegistry;
  @Mock private RecipeRetriever recipeRetriever;
  @Mock private InternalEnvironmentValidator<InternalEnvironment> envValidator;

  private InternalEnvironmentFactory<InternalEnvironment> environmentFactory;

  @BeforeMethod
  public void setUp() throws Exception {
    environmentFactory =
        spy(new TestEnvironmentFactory(installerRegistry, recipeRetriever, envValidator));
  }

  // TODO Cover all use cases

  @Test
  public void normalizeServersProtocols() throws InfrastructureException {
    ServerConfigImpl serverWithoutProtocol = new ServerConfigImpl("8080", "http", "/api");
    ServerConfigImpl udpServer = new ServerConfigImpl("8080/udp", "http", "/api");
    ServerConfigImpl normalizedServer = new ServerConfigImpl("8080/tcp", "http", "/api");

    Map<String, ServerConfig> servers = new HashMap<>();
    servers.put("serverWithoutProtocol", serverWithoutProtocol);
    servers.put("udpServer", udpServer);

    Map<String, ServerConfig> normalizedServers = environmentFactory.normalizeServers(servers);

    assertEquals(
        normalizedServers,
        ImmutableMap.of("serverWithoutProtocol", normalizedServer, "udpServer", udpServer));
  }

  private static class TestEnvironmentFactory
      extends InternalEnvironmentFactory<InternalEnvironment> {

    private TestEnvironmentFactory(
        InstallerRegistry installerRegistry,
        RecipeRetriever recipeRetriever,
        InternalEnvironmentValidator<InternalEnvironment> envValidator) {
      super(installerRegistry, recipeRetriever, envValidator);
    }

    @Override
    protected InternalEnvironment doCreate(InternalRecipe recipe, Map machines, List list)
        throws InfrastructureException, ValidationException {
      return null;
    }
  }
}
