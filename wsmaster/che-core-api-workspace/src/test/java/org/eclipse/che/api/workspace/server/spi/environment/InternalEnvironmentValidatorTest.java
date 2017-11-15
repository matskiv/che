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
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Map;
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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Tests {@link InternalEnvironmentValidator}.
 *
 * @author Alexander Garagatyi
 * @author Sergii Leshchenko
 */
@Listeners(MockitoTestNGListener.class)
public class InternalEnvironmentValidatorTest {

  private static final String MACHINE_NAME = "machine1";

  @Mock private InternalEnvironment environment;
  private InternalMachineConfig machineConfig;

  private InternalEnvironmentValidator<InternalEnvironment> environmentValidator;

  @BeforeMethod
  public void setUp() throws Exception {
    environmentValidator = spy(new TestEnvironmentValidator());

    machineConfig = machineMockWithServers(Constants.SERVER_WS_AGENT_HTTP_REFERENCE);
    when(environment.getMachines()).thenReturn(singletonMap(MACHINE_NAME, machineConfig));
  }

  @Test
  public void shouldInvokeDoValidateIsEnvironmentIsValid() throws Exception {
    // when
    environmentValidator.validate(environment);

    // then
    verify(environmentValidator).doValidate(environment);
  }

  @Test(
    expectedExceptions = ValidationException.class,
    expectedExceptionsMessageRegExp = "Environment should contain at least 1 machine"
  )
  public void shouldFailIfMachinesIsNull() throws Exception {
    // given
    when(environment.getMachines()).thenReturn(null);

    // when
    environmentValidator.validate(environment);
  }

  @Test(
    expectedExceptions = ValidationException.class,
    expectedExceptionsMessageRegExp = "Environment should contain at least 1 machine"
  )
  public void shouldFailIfMachinesIsEmpty() throws Exception {
    // given
    when(environment.getMachines()).thenReturn(emptyMap());

    // when
    environmentValidator.validate(environment);
  }

  @Test(
    expectedExceptions = ValidationException.class,
    expectedExceptionsMessageRegExp = "Name of machine '.*' in environment is invalid",
    dataProvider = "invalidMachineNames"
  )
  public void shouldFailIfMachinesNameAreInvalid(String machineName) throws Exception {
    // given
    when(environment.getMachines()).thenReturn(singletonMap(machineName, machineConfig));

    // when
    environmentValidator.validate(environment);
  }

  @DataProvider(name = "invalidMachineNames")
  public Object[][] invalidMachineNames() {
    return new Object[][] {
      {""}, {"-123"}, {"123-"}, {"-123-"}, {"/123-"}, {"/123"}, {"123/"}, {"123_"}, {"!asdd/"},
    };
  }

  @Test(dataProvider = "validMachineNames")
  public void shouldNotFailIfMachinesNameAreValid(String machineName) throws Exception {
    // given
    when(environment.getMachines()).thenReturn(singletonMap(machineName, machineConfig));

    // when
    environmentValidator.validate(environment);
  }

  @DataProvider(name = "validMachineNames")
  public Object[][] validMachineNames() {
    return new Object[][] {
      {"machine"}, {"machine123"}, {"machine-123"}, {"app/db"}, {"app_db"},
    };
  }

  @Test(
    expectedExceptions = ValidationException.class,
    expectedExceptionsMessageRegExp =
        "Machine '.*' in environment contains server conf '.*' with invalid port '.*'",
    dataProvider = "invalidServerPorts"
  )
  public void shouldFailIfServerPortIsInvalid(String servicePort) throws Exception {
    // given
    ServerConfigImpl server = new ServerConfigImpl(servicePort, "https", "/some/path");
    when(machineConfig.getServers())
        .thenReturn(singletonMap(Constants.SERVER_WS_AGENT_HTTP_REFERENCE, server));

    // when
    environmentValidator.validate(environment);
  }

  @DataProvider(name = "invalidServerPorts")
  public Object[][] invalidServerPorts() {
    return new Object[][] {
      {"aaa"}, {"123aaa"}, {"8080/tpc2"}, {"8080/TCP"}, {"123udp"}, {""}, {"/123"},
    };
  }

  @Test(dataProvider = "validServerPorts")
  public void shouldNotFailIfServerPortIsValid(String servicePort) throws Exception {
    // given
    ServerConfigImpl server = new ServerConfigImpl(servicePort, "https", "/some/path");
    when(machineConfig.getServers())
        .thenReturn(singletonMap(Constants.SERVER_WS_AGENT_HTTP_REFERENCE, server));

    // when
    environmentValidator.validate(environment);
  }

  @DataProvider(name = "validServerPorts")
  public Object[][] validServerPorts() {
    return new Object[][] {
      {"1"}, {"12"}, {"8080"}, {"8080/tcp"}, {"8080/udp"},
    };
  }

  @Test(
    expectedExceptions = ValidationException.class,
    expectedExceptionsMessageRegExp =
        "Machine '.*' in environment contains server conf '.*' with invalid protocol '.*'",
    dataProvider = "invalidServerProtocols"
  )
  public void shouldFailIfServerProtocolIsInvalid(String serviceProtocol) throws Exception {
    // given
    ServerConfigImpl server = new ServerConfigImpl("8080", serviceProtocol, "/some/path");
    when(machineConfig.getServers())
        .thenReturn(singletonMap(Constants.SERVER_WS_AGENT_HTTP_REFERENCE, server));

    // when
    environmentValidator.validate(environment);
  }

  @DataProvider(name = "invalidServerProtocols")
  public Object[][] invalidServerProtocols() {
    return new Object[][] {{"0"}, {"0sds"}, {"TCP"}, {"UDP"}, {"http@"}};
  }

  @Test(dataProvider = "validServerProtocols")
  public void shouldNotFailIfServerProtocolIsValid(String serviceProtocol) throws Exception {
    // given
    ServerConfigImpl server = new ServerConfigImpl("8080", serviceProtocol, "/some/path");
    when(machineConfig.getServers())
        .thenReturn(singletonMap(Constants.SERVER_WS_AGENT_HTTP_REFERENCE, server));

    // when
    environmentValidator.validate(environment);
  }

  @DataProvider(name = "validServerProtocols")
  public Object[][] validServerProtocols() {
    return new Object[][] {{"a"}, {"http"}, {"tcp"}, {"tcp2"}};
  }

  @Test(
    expectedExceptions = ValidationException.class,
    expectedExceptionsMessageRegExp =
        "Environment should contain exactly 1 machine with wsagent, but contains '.*'. All machines with this agent: .*",
    dataProvider = "severalWsAgentsProvider"
  )
  public void shouldFailIfThereIsMoreThan1MachineWithWsAgent(
      Map<String, InternalMachineConfig> machines) throws Exception {
    // given
    when(environment.getMachines()).thenReturn(machines);

    // when
    environmentValidator.validate(environment);
  }

  @DataProvider(name = "severalWsAgentsProvider")
  public static Object[][] severalWsAgentsProvider() {
    return new Object[][] {
      {
        ImmutableMap.of(
            "machine1", machineMockWithServers(Constants.SERVER_WS_AGENT_HTTP_REFERENCE),
            "machine2", machineMockWithServers(Constants.SERVER_WS_AGENT_HTTP_REFERENCE))
      },
      {
        ImmutableMap.of(
            "machine1", machineMockWithInstallers(WsAgentMachineFinderUtil.WS_AGENT_INSTALLER),
            "machine2", machineMockWithServers(Constants.SERVER_WS_AGENT_HTTP_REFERENCE))
      },
      {
        ImmutableMap.of(
            "machine1", machineMockWithInstallers(WsAgentMachineFinderUtil.WS_AGENT_INSTALLER),
            "machine2", machineMockWithInstallers(WsAgentMachineFinderUtil.WS_AGENT_INSTALLER))
      }
    };
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

  private static InternalMachineConfig machineMockWithInstallers(String... servers) {
    InternalMachineConfig mock = machineMock();
    when(mock.getInstallers())
        .thenReturn(
            Arrays.stream(servers)
                .map(s -> new InstallerImpl().withId(s))
                .collect(Collectors.toList()));
    return mock;
  }
}
