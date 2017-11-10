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
package org.eclipse.che.workspace.infrastructure.openshift;

import static java.lang.String.format;
import static org.eclipse.che.workspace.infrastructure.openshift.Constants.CHE_ORIGINAL_NAME_LABEL;
import static org.eclipse.che.workspace.infrastructure.openshift.Constants.MACHINE_NAME_ANNOTATION_FMT;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import java.util.Map;
import org.eclipse.che.commons.lang.NameGenerator;

/**
 * Helps to work with OpenShift objects names.
 *
 * @author Sergii Leshchenko
 */
public class Names {

  static final char WORKSPACE_ID_PREFIX_SEPARATOR = '.';

  static final String ROUTE_PREFIX = "route";
  static final int ROUTE_PREFIX_SIZE = 8;

  /**
   * Returns machine name that has the following format `{POD_NAME}/{CONTAINER_NAME}` or machine
   * name from configuration when it's not composite.
   */
  public static String machineName(Pod pod, Container container) {
    final Map<String, String> annotations = pod.getMetadata().getAnnotations();
    String machineName;
    final String containerName = container.getName();
    if (annotations != null
        && (machineName = annotations.get(format(MACHINE_NAME_ANNOTATION_FMT, containerName)))
            != null) {
      return machineName;
    }
    final Map<String, String> labels = pod.getMetadata().getLabels();
    if (labels != null && (machineName = labels.get(CHE_ORIGINAL_NAME_LABEL)) != null) {
      return machineName + '/' + containerName;
    }
    return pod.getMetadata().getName() + '/' + containerName;
  }

  /** Return pod name that will be unique for a whole namespace. */
  public static String uniquePodName(String originalPodName, String workspaceId) {
    return workspaceId + WORKSPACE_ID_PREFIX_SEPARATOR + originalPodName;
  }

  /** Returns route name that will be unique whole a namespace. */
  public static String uniqueRouteName() {
    return NameGenerator.generate(ROUTE_PREFIX, ROUTE_PREFIX_SIZE);
  }
}
