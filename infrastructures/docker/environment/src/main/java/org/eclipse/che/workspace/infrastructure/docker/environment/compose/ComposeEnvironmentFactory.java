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

import static java.lang.String.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.che.api.core.ValidationException;
import org.eclipse.che.api.core.model.workspace.Warning;
import org.eclipse.che.api.installer.server.InstallerRegistry;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.environment.InternalEnvironmentFactory;
import org.eclipse.che.api.workspace.server.spi.environment.InternalMachineConfig;
import org.eclipse.che.api.workspace.server.spi.environment.InternalRecipe;
import org.eclipse.che.api.workspace.server.spi.environment.RecipeRetriever;
import org.eclipse.che.workspace.infrastructure.docker.environment.compose.model.ComposeRecipe;

/**
 * TODO Add java doc
 *
 * @author Sergii Leshchenko
 */
@Singleton
public class ComposeEnvironmentFactory extends InternalEnvironmentFactory<ComposeEnvironment> {

  private static final ObjectMapper YAML_PARSER = new ObjectMapper(new YAMLFactory());

  private final ComposeServicesStartStrategy startStrategy;

  @Inject
  public ComposeEnvironmentFactory(
      InstallerRegistry installerRegistry,
      RecipeRetriever recipeRetriever,
      ComposeEnvironmentValidator composeValidator,
      ComposeServicesStartStrategy startStrategy) {
    super(installerRegistry, recipeRetriever, composeValidator);
    this.startStrategy = startStrategy;
  }

  @Override
  protected ComposeEnvironment doCreate(
      InternalRecipe recipe, Map<String, InternalMachineConfig> machines, List<Warning> warnings)
      throws InfrastructureException, ValidationException {
    // TODO Cover with test
    String contentType = recipe.getContentType();
    String recipeContent = recipe.getContent();
    checkNotNull(contentType, "Recipe content type should not be null");

    if (!ComposeEnvironment.TYPE.equals(recipe.getType())) {
      throw new ValidationException(
          format("Compose environment parser doesn't support recipe type '%s'", recipe.getType()));
    }

    ComposeRecipe composeRecipe;
    switch (contentType) {
      case "application/x-yaml":
      case "text/yaml":
      case "text/x-yaml":
        composeRecipe = doParse(recipeContent);
        break;
      default:
        throw new ValidationException(
            "Provided environment recipe content type '"
                + contentType
                + "' is unsupported. Supported values are: "
                + "application/x-yaml, text/yaml, text/x-yaml");
    }
    ComposeEnvironment composeEnv =
        new ComposeEnvironment(
            composeRecipe.getVersion(), composeRecipe.getServices(), recipe, machines, warnings);

    // check that containers start order can be resolved
    // NOTE: it should be performed before dockerEnvironmentNormalizer.normalize because
    // normalization
    // changes links, volumes from which will fail order evaluation
    // It can be changed after reimplementing strategy to respect normalization

    // TODO mb make startStrategy modify environment
    List<String> containersOrder = startStrategy.order(composeEnv);

    composeEnv.setContainersOrder(containersOrder);

    return composeEnv;
  }

  @VisibleForTesting
  ComposeRecipe doParse(String recipeContent) throws ValidationException {
    ComposeRecipe composeRecipe;
    try {
      composeRecipe = YAML_PARSER.readValue(recipeContent, ComposeRecipe.class);
    } catch (IOException e) {
      throw new ValidationException(
          "Parsing of environment configuration failed. " + e.getLocalizedMessage());
    }
    return composeRecipe;
  }

  private static void checkNotNull(
      Object object, String errorMessageTemplate, Object... errorMessageParams)
      throws ValidationException {
    if (object == null) {
      throw new ValidationException(format(errorMessageTemplate, errorMessageParams));
    }
  }
}
