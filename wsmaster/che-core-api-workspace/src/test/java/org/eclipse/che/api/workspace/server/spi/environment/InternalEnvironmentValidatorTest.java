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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.che.api.core.ValidationException;
import org.eclipse.che.api.installer.server.model.impl.InstallerImpl;
import org.eclipse.che.api.workspace.server.WsAgentMachineFinderUtil;
import org.eclipse.che.api.workspace.server.model.impl.ServerConfigImpl;
import org.eclipse.che.api.workspace.shared.Constants;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Tests {@link InternalEnvironmentValidator}.
 *
 * @author Sergii Leshchenko
 */
@Listeners(MockitoTestNGListener.class)
public class InternalEnvironmentValidatorTest {

  private static final String MACHINE_NAME = "machine1";

  @Mock private InternalEnvironment environment;
  @Mock private InternalMachineConfig machineConfig;
  @Mock private InstallerImpl installer;
  @Mock private ServerConfigImpl server;

  private InternalEnvironmentValidator<InternalEnvironment> environmentValidator;

  @BeforeMethod
  public void setUp() throws Exception {
    environmentValidator = new TestEnvironmentValidator();

    when(environment.getMachines()).thenReturn(singletonMap(MACHINE_NAME, machineConfig));
    when(machineConfig.getInstallers()).thenReturn(singletonList(installer));
    when(installer.getId()).thenReturn(WsAgentMachineFinderUtil.WS_AGENT_INSTALLER);
    when(server.getPort()).thenReturn("8080/tcp");
    when(server.getPath()).thenReturn("/some/path");
    when(server.getProtocol()).thenReturn("https");
  }

  @Test(
    expectedExceptions = ValidationException.class,
    expectedExceptionsMessageRegExp = "Environment should contain at least 1 machine"
  )
  public void shouldFailIfMachinesListIsNull() throws Exception {
    // given
    when(environment.getMachines()).thenReturn(null);

    // when
    environmentValidator.validate(environment);
  }

  @Test(
    expectedExceptions = ValidationException.class,
    expectedExceptionsMessageRegExp =
        "Machine '.*' in environment contains server conf '.*' with invalid port '.*'"
  )
  public void shouldFailIfServerPortInMachineIsInvalid() throws Exception {
    // given
    when(server.getPort()).thenReturn("aaaaa");
    when(machineConfig.getServers())
        .thenReturn(singletonMap(Constants.SERVER_WS_AGENT_HTTP_REFERENCE, server));

    // when
    environmentValidator.validate(environment);
  }

  @Test(
    expectedExceptions = ValidationException.class,
    expectedExceptionsMessageRegExp =
        "Machine '.*' in environment contains server conf '.*' with invalid protocol '.*'"
  )
  public void shouldFailIfServerProtocolInMachineIsInvalid() throws Exception {
    // given
    when(machineConfig.getServers()).thenReturn(singletonMap("server1", server));
    when(server.getProtocol()).thenReturn("0");

    // when
    environmentValidator.validate(environment);
  }

  @Test(
    expectedExceptions = ValidationException.class,
    expectedExceptionsMessageRegExp =
        "Environment should contain exactly 1 machine with wsagent, but contains '.*'. All machines with this agent: .*"
  )
  public void shouldFailIfThereIsMoreThan1MachineWithWsAgent() throws Exception {
    // TODO Revert removing checking installers
    // given
    doReturn(
            ImmutableMap.of(
                "machine1", machineMockWithServers(Constants.SERVER_WS_AGENT_HTTP_REFERENCE),
                "machine2", machineMockWithServers(Constants.SERVER_WS_AGENT_HTTP_REFERENCE)))
        .when(environment)
        .getMachines();

    // when
    environmentValidator.validate(environment);
  }

  private static class TestEnvironmentValidator
      extends InternalEnvironmentValidator<InternalEnvironment> {

    @Override
    protected void doValidate(InternalEnvironment env) throws ValidationException {
      // Do nothing
    }
  }

  private static InternalMachineConfig machineMock() {
    InternalMachineConfig mock = mock(InternalMachineConfig.class);
    when(mock.getServers()).thenReturn(emptyMap());
    when(mock.getInstallers()).thenReturn(emptyList());
    return mock;
  }

  private static InternalMachineConfig machineMockWithServers(String... servers) {
    InternalMachineConfig mock = machineMock();
    when(mock.getServers())
        .thenReturn(
            Arrays.stream(servers)
                .collect(
                    Collectors.toMap(
                        Function.identity(), s -> new ServerConfigImpl("8080", "http", "/"))));
    return mock;
  }
}
