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
package org.eclipse.che.workspace.infrastructure.docker.environment.compose;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toList;

import com.google.common.base.Joiner;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.che.api.core.ValidationException;
import org.eclipse.che.api.workspace.server.spi.environment.InternalEnvironmentValidator;
import org.eclipse.che.workspace.infrastructure.docker.environment.compose.model.ComposeService;

/** @author Alexander Garagatyi */
public class ComposeEnvironmentValidator extends InternalEnvironmentValidator<ComposeEnvironment> {
  // DockerContainer syntax patterns
  /**
   * Examples:
   *
   * <ul>
   *   <li>8080/tcp
   *   <li>8080/udp
   *   <li>8080
   *   <li>8/tcp
   *   <li>8
   * </ul>
   */
  private static final Pattern EXPOSE_PATTERN = Pattern.compile("^[1-9]+[0-9]*(/(tcp|udp))?$");
  /**
   * Examples:
   *
   * <ul>
   *   <li>service1
   *   <li>service1:alias1
   * </ul>
   */
  private static final Pattern LINK_PATTERN =
      Pattern.compile(
          "^(?<containerName>" + MACHINE_NAME_REGEXP + ")(:" + MACHINE_NAME_REGEXP + ")?$");

  private static final Pattern VOLUME_FROM_PATTERN =
      Pattern.compile("^(?<containerName>" + MACHINE_NAME_REGEXP + ")(:(ro|rw))?$");

  @Override
  protected void doValidate(ComposeEnvironment env) throws ValidationException {
    checkArgument(
        env.getServices() != null && !env.getServices().isEmpty(),
        "Environment should contain at least 1 service");

    List<String> missingServices =
        env.getMachines()
            .keySet()
            .stream()
            .filter(machineName -> !env.getServices().containsKey(machineName))
            .collect(toList());
    checkArgument(
        missingServices.isEmpty(),
        "Environment contains machines that are missing in environment recipe: %s",
        Joiner.on(", ").join(missingServices));

    // needed to validate different kinds of dependencies in containers to other containers
    Set<String> containersNames = env.getServices().keySet();

    for (Map.Entry<String, ComposeService> serviceEntry : env.getServices().entrySet()) {
      validateMachine(serviceEntry.getKey(), serviceEntry.getValue(), containersNames);
    }
  }

  private void validateMachine(
      String machineName, ComposeService service, Set<String> servicesNames)
      throws ValidationException {

    checkArgument(
        MACHINE_NAME_PATTERN.matcher(machineName).matches(),
        "Name of machine '%s' in environment is invalid",
        machineName);

    checkArgument(
        !isNullOrEmpty(service.getImage())
            || (service.getBuild() != null
                && (!isNullOrEmpty(service.getBuild().getContext())
                    || !isNullOrEmpty(service.getBuild().getDockerfile()))),
        "Field 'image' or 'build.context' is required in machine '%s' in environment",
        machineName);

    checkArgument(
        service.getBuild() == null
            || (isNullOrEmpty(service.getBuild().getContext())
                != isNullOrEmpty(service.getBuild().getDockerfile())),
        "Machine '%s' in environment contains mutually exclusive dockerfile content and build context.",
        machineName);

    for (String expose : service.getExpose()) {
      checkArgument(
          EXPOSE_PATTERN.matcher(expose).matches(),
          "Exposed port '%s' in machine '%s' in environment is invalid",
          expose,
          machineName);
    }

    for (String link : service.getLinks()) {
      Matcher matcher = LINK_PATTERN.matcher(link);

      checkArgument(
          matcher.matches(),
          "Link '%s' in machine '%s' in environment is invalid",
          link,
          machineName);

      String containerFromLink = matcher.group("containerName");
      checkArgument(
          !machineName.equals(containerFromLink),
          "Container '%s' has illegal link to itself",
          machineName);
      checkArgument(
          servicesNames.contains(containerFromLink),
          "Machine '%s' in environment contains link to non existing machine '%s'",
          machineName,
          containerFromLink);
    }

    for (String depends : service.getDependsOn()) {
      checkArgument(
          MACHINE_NAME_PATTERN.matcher(depends).matches(),
          "Dependency '%s' in machine '%s' in environment is invalid",
          depends,
          machineName);

      checkArgument(
          !machineName.equals(depends),
          "Container '%s' has illegal dependency to itself",
          machineName);
      checkArgument(
          servicesNames.contains(depends),
          "Machine '%s' in environment contains dependency to non existing machine '%s'",
          machineName,
          depends);
    }

    for (String volumesFrom : service.getVolumesFrom()) {
      Matcher matcher = VOLUME_FROM_PATTERN.matcher(volumesFrom);

      checkArgument(
          matcher.matches(),
          "Machine name '%s' in field 'volumes_from' of machine '%s' in environment is invalid",
          volumesFrom,
          machineName);

      String containerFromVolumesFrom = matcher.group("containerName");
      checkArgument(
          !machineName.equals(containerFromVolumesFrom),
          "Container '%s' can not mount volume from itself",
          machineName);
      checkArgument(
          servicesNames.contains(containerFromVolumesFrom),
          "Machine '%s' in environment contains non existing machine '%s' in 'volumes_from' field",
          machineName,
          containerFromVolumesFrom);
    }

    checkArgument(
        service.getPorts() == null || service.getPorts().isEmpty(),
        "Ports binding is forbidden but found in machine '%s' of environment",
        machineName);

    checkArgument(
        service.getVolumes() == null || service.getVolumes().isEmpty(),
        "Volumes binding is forbidden but found in machine '%s' of environment",
        machineName);

    checkArgument(
        service.getNetworks() == null || service.getNetworks().isEmpty(),
        "Networks configuration is forbidden but found in machine '%s' of environment",
        machineName);
  }
}
