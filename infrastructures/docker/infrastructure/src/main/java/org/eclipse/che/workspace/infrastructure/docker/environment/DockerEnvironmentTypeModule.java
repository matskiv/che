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
package org.eclipse.che.workspace.infrastructure.docker.environment;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import org.eclipse.che.api.workspace.server.spi.InternalEnvironmentFactory;
import org.eclipse.che.workspace.infrastructure.docker.environment.compose.ComposeInternalEnvironmentFactory;
import org.eclipse.che.workspace.infrastructure.docker.environment.dockerfile.DockerfileEnvironmentParser;
import org.eclipse.che.workspace.infrastructure.docker.environment.dockerfile.DockerfileInternalEnvironmentFactory;
import org.eclipse.che.workspace.infrastructure.docker.environment.dockerimage.DockerImageEnvironmentParser;
import org.eclipse.che.workspace.infrastructure.docker.environment.dockerimage.DockerimageInternalEnvironmentFactory;

/** @author Alexander Garagatyi */
public class DockerEnvironmentTypeModule extends AbstractModule {
  @Override
  protected void configure() {
    // Environment type
    MapBinder<String, InternalEnvironmentFactory> environmentFactories =
        MapBinder.newMapBinder(binder(), String.class, InternalEnvironmentFactory.class);
    environmentFactories.addBinding("compose").to(ComposeInternalEnvironmentFactory.class);
    environmentFactories
        .addBinding(DockerfileEnvironmentParser.TYPE)
        .to(DockerfileInternalEnvironmentFactory.class);
    environmentFactories
        .addBinding(DockerImageEnvironmentParser.TYPE)
        .to(DockerimageInternalEnvironmentFactory.class);
  }
}
