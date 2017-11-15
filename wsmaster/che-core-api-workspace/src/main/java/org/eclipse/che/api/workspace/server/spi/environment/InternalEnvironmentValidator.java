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

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import com.google.common.base.Joiner;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.eclipse.che.api.core.ValidationException;
import org.eclipse.che.api.core.model.workspace.config.ServerConfig;
import org.eclipse.che.api.workspace.server.WsAgentMachineFinderUtil;

/**
 * Validates {@link InternalEnvironment}.
 *
 * @author Alexander Garagatyi
 * @author Sergii Leshchenko
 */
public abstract class InternalEnvironmentValidator<T extends InternalEnvironment> {

  protected static final String MACHINE_NAME_REGEXP = "[a-zA-Z0-9]+([a-zA-Z0-9_/-]*[a-zA-Z0-9])?";
  protected static final Pattern MACHINE_NAME_PATTERN =
      Pattern.compile("^" + MACHINE_NAME_REGEXP + "$");

  protected static final Pattern SERVER_PORT_PATTERN =
      Pattern.compile("^[1-9]+[0-9]*(/(tcp|udp))?$");
  protected static final Pattern SERVER_PROTOCOL_PATTERN = Pattern.compile("^[a-z][a-z0-9-+.]*$");

  /**
   * Validates the specified environment.
   *
   * @param env environment to validate
   * @throws ValidationException when the specified environment is not valid
   */
  public void validate(T env) throws ValidationException {
    validateMachines(env.getMachines());

    doValidate(env);
  }

  /**
   * Performs implementations specific validation.
   *
   * <p>Common internal environment validation like machines validation is dome by {@link
   * #validate(InternalEnvironment)}.
   *
   * @param env environment to perform implementations specific validation
   * @throws ValidationException when the specified environment is not valid
   */
  protected abstract void doValidate(T env) throws ValidationException;

  private void validateMachines(Map<String, InternalMachineConfig> machines)
      throws ValidationException {
    checkArgument(
        machines != null && !machines.isEmpty(), "Environment should contain at least 1 machine");

    for (Entry<String, InternalMachineConfig> machineConfigEntry : machines.entrySet()) {
      validateMachine(machineConfigEntry.getKey(), machineConfigEntry.getValue());
    }

    List<String> machinesWithWsagentServer =
        machines
            .entrySet()
            .stream()
            .filter(
                entry ->
                    WsAgentMachineFinderUtil.containsWsAgentServerOrInstaller(entry.getValue()))
            .map(Map.Entry::getKey)
            .collect(toList());

    checkArgument(
        machinesWithWsagentServer.size() == 1,
        "Environment should contain exactly 1 machine with wsagent, but contains '%s'. "
            + "All machines with this agent: %s",
        machinesWithWsagentServer.size(),
        Joiner.on(", ").join(machinesWithWsagentServer));
  }

  private void validateMachine(String machineName, InternalMachineConfig machineConfig)
      throws ValidationException {

    checkArgument(
        MACHINE_NAME_PATTERN.matcher(machineName).matches(),
        "Name of machine '%s' in environment is invalid",
        machineName);

    if (machineConfig.getServers() != null) {
      for (Map.Entry<String, ? extends ServerConfig> serverEntry :
          machineConfig.getServers().entrySet()) {
        String serverName = serverEntry.getKey();
        ServerConfig server = serverEntry.getValue();

        checkArgument(
            server.getPort() != null && SERVER_PORT_PATTERN.matcher(server.getPort()).matches(),
            "Machine '%s' in environment contains server conf '%s' with invalid port '%s'",
            machineName,
            serverName,
            server.getPort());
        checkArgument(
            server.getProtocol() == null
                || SERVER_PROTOCOL_PATTERN.matcher(server.getProtocol()).matches(),
            "Machine '%s' in environment contains server conf '%s' with invalid protocol '%s'",
            machineName,
            serverName,
            server.getProtocol());
      }
    }
  }

  protected static void checkArgument(boolean expression, String error) throws ValidationException {
    if (!expression) {
      throw new ValidationException(error);
    }
  }

  protected static void checkArgument(
      boolean expression, String errorMessageTemplate, Object... errorMessageParams)
      throws ValidationException {
    if (!expression) {
      throw new ValidationException(format(errorMessageTemplate, errorMessageParams));
    }
  }
}
