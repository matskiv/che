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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertEqualsNoOrder;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import org.eclipse.che.api.core.ValidationException;
import org.eclipse.che.workspace.infrastructure.docker.environment.compose.model.ComposeService;
import org.testng.annotations.Test;

/**
 * @author Alexander Garagatyi
 * @author Alexander Andrienko
 */
public class ComposeServicesStartStrategyTest {
  private ComposeServicesStartStrategy strategy = new ComposeServicesStartStrategy();
  // TODO Take a look
  @Test
  public void shouldOrderServicesWithDependenciesWhereOrderIsStrict() throws Exception {
    // given
    ComposeEnvironment composeEnvironment =
        new ComposeEnvironment("", new HashMap<>(), null, null, null);
    composeEnvironment
        .getServices()
        .put("second", new ComposeService().withDependsOn(singletonList("first")));
    composeEnvironment
        .getServices()
        .put("third", new ComposeService().withDependsOn(asList("first", "second")));
    composeEnvironment.getServices().put("first", new ComposeService().withDependsOn(emptyList()));
    composeEnvironment
        .getServices()
        .put("forth", new ComposeService().withDependsOn(singletonList("third")));
    composeEnvironment
        .getServices()
        .put("fifth", new ComposeService().withDependsOn(asList("forth", "first")));
    List<String> expected = asList("first", "second", "third", "forth", "fifth");

    // when
    List<String> actual = strategy.order(composeEnvironment);

    // then
    assertEquals(actual, expected);
  }

  @Test
  public void shouldOrderServicesWithDependenciesWhereOrderIsStrict2() throws Exception {
    // given
    ComposeEnvironment composeEnvironment =
        new ComposeEnvironment("", new HashMap<>(), null, null, null);
    composeEnvironment
        .getServices()
        .put("web", new ComposeService().withDependsOn(asList("db", "redis")));
    composeEnvironment
        .getServices()
        .put("redis", new ComposeService().withDependsOn(singletonList("dev-machine")));
    composeEnvironment
        .getServices()
        .put("db", new ComposeService().withDependsOn(singletonList("redis")));
    composeEnvironment
        .getServices()
        .put("dev-machine", new ComposeService().withDependsOn(emptyList()));

    List<String> expected = asList("dev-machine", "redis", "db", "web");

    // when
    List<String> actual = strategy.order(composeEnvironment);

    // then
    assertEquals(actual, expected);
  }

  @Test
  public void testOrderingOfServicesWithoutDependencies() throws Exception {
    // given
    ComposeEnvironment composeEnvironment =
        new ComposeEnvironment("", new HashMap<>(), null, null, null);
    composeEnvironment.getServices().put("second", new ComposeService());
    composeEnvironment.getServices().put("third", new ComposeService());
    composeEnvironment.getServices().put("first", new ComposeService());
    String[] expected = new String[] {"first", "second", "third"};

    // when
    String[] actual =
        strategy
            .order(composeEnvironment)
            .toArray(new String[composeEnvironment.getServices().size()]);

    // then
    assertEqualsNoOrder(actual, expected);
  }

  @Test
  public void shouldOrderServicesWithDependenciesWhereOrderIsNotStrict() throws Exception {
    // given
    ComposeEnvironment composeEnvironment =
        new ComposeEnvironment("", new HashMap<>(), null, null, null);
    composeEnvironment
        .getServices()
        .put("second", new ComposeService().withDependsOn(singletonList("first")));
    composeEnvironment
        .getServices()
        .put("third", new ComposeService().withDependsOn(singletonList("second")));
    composeEnvironment.getServices().put("first", new ComposeService().withDependsOn(emptyList()));
    composeEnvironment
        .getServices()
        .put("forth", new ComposeService().withDependsOn(singletonList("second")));
    composeEnvironment
        .getServices()
        .put("fifth", new ComposeService().withDependsOn(singletonList("second")));

    // when
    List<String> actual = strategy.order(composeEnvironment);

    // then
    assertEquals(actual.get(0), "first");
    assertEquals(actual.get(1), "second");
    assertTrue(actual.contains("third"));
    assertTrue(actual.contains("forth"));
    assertTrue(actual.contains("fifth"));
  }

  @Test(
    expectedExceptions = ValidationException.class,
    expectedExceptionsMessageRegExp =
        "Launch order of services '.*, .*' can't be evaluated. Circular dependency."
  )
  public void shouldFailIfCircularDependencyFound() throws Exception {
    // given
    ComposeEnvironment composeEnvironment =
        new ComposeEnvironment("", new HashMap<>(), null, null, null);
    composeEnvironment
        .getServices()
        .put("second", new ComposeService().withDependsOn(singletonList("third")));
    composeEnvironment
        .getServices()
        .put("third", new ComposeService().withDependsOn(singletonList("second")));
    composeEnvironment.getServices().put("first", new ComposeService());

    // when
    strategy.order(composeEnvironment);
  }

  @Test(
    expectedExceptions = ValidationException.class,
    expectedExceptionsMessageRegExp = "A service can not link to itself: .*"
  )
  public void shouldFailIfMachineLinksByItSelf() throws Exception {
    // given
    ComposeEnvironment composeEnvironment =
        new ComposeEnvironment("", new HashMap<>(), null, null, null);
    composeEnvironment
        .getServices()
        .put("first", new ComposeService().withLinks(singletonList("first")));

    // when
    strategy.order(composeEnvironment);
  }

  @Test(
    expectedExceptions = ValidationException.class,
    expectedExceptionsMessageRegExp = "A service can not depend on itself: .*"
  )
  public void shouldFailIfMachineDependsOnByItSelf() throws Exception {
    // given
    ComposeEnvironment composeEnvironment =
        new ComposeEnvironment("", new HashMap<>(), null, null, null);
    composeEnvironment
        .getServices()
        .put("first", new ComposeService().withDependsOn(singletonList("first")));

    // when
    strategy.order(composeEnvironment);
  }

  @Test(
    expectedExceptions = ValidationException.class,
    expectedExceptionsMessageRegExp = "A service can not contain 'volumes_from' to itself:.*"
  )
  public void shouldFailIfMachineContainsVolumesFromByItSelf() throws Exception {
    // given
    ComposeEnvironment composeEnvironment =
        new ComposeEnvironment("", new HashMap<>(), null, null, null);
    composeEnvironment
        .getServices()
        .put("first", new ComposeService().withVolumesFrom(singletonList("first")));

    // when
    strategy.order(composeEnvironment);
  }

  @Test
  public void shouldOrderServicesWithLinks() throws Exception {
    // given
    ComposeEnvironment composeEnvironment =
        new ComposeEnvironment("", new HashMap<>(), null, null, null);
    composeEnvironment
        .getServices()
        .put("second", new ComposeService().withLinks(singletonList("first:alias")));
    composeEnvironment
        .getServices()
        .put("third", new ComposeService().withLinks(asList("first", "second")));
    composeEnvironment.getServices().put("first", new ComposeService().withLinks(emptyList()));
    composeEnvironment
        .getServices()
        .put("forth", new ComposeService().withLinks(singletonList("third")));
    composeEnvironment
        .getServices()
        .put("fifth", new ComposeService().withLinks(asList("forth:alias", "first:alias")));
    List<String> expected = asList("first", "second", "third", "forth", "fifth");

    // when
    List<String> actual = strategy.order(composeEnvironment);

    // then
    assertEquals(actual, expected);
  }

  @Test
  public void shouldOrderServicesWithVolumesFrom() throws Exception {
    // given
    ComposeEnvironment composeEnvironment =
        new ComposeEnvironment("", new HashMap<>(), null, null, null);
    composeEnvironment
        .getServices()
        .put("second", new ComposeService().withVolumesFrom(singletonList("first")));
    composeEnvironment
        .getServices()
        .put("third", new ComposeService().withVolumesFrom(asList("first", "second")));
    composeEnvironment
        .getServices()
        .put("first", new ComposeService().withVolumesFrom(emptyList()));
    composeEnvironment
        .getServices()
        .put("forth", new ComposeService().withVolumesFrom(singletonList("third")));
    composeEnvironment
        .getServices()
        .put("fifth", new ComposeService().withVolumesFrom(asList("forth", "first")));
    List<String> expected = asList("first", "second", "third", "forth", "fifth");

    // when
    List<String> actual = strategy.order(composeEnvironment);

    // then
    assertEquals(actual, expected);
  }

  @Test
  public void shouldOrderServicesWithMixedDependenciesInDependsOnVolumesFromAndLinks()
      throws Exception {
    // given
    ComposeEnvironment composeEnvironment =
        new ComposeEnvironment("", new HashMap<>(), null, null, null);
    composeEnvironment
        .getServices()
        .put("second", new ComposeService().withDependsOn(singletonList("first")));
    composeEnvironment
        .getServices()
        .put("third", new ComposeService().withVolumesFrom(asList("first", "second")));
    composeEnvironment.getServices().put("first", new ComposeService().withLinks(emptyList()));
    composeEnvironment
        .getServices()
        .put("forth", new ComposeService().withLinks(singletonList("third")));
    composeEnvironment
        .getServices()
        .put("fifth", new ComposeService().withDependsOn(asList("forth", "first")));
    List<String> expected = asList("first", "second", "third", "forth", "fifth");

    // when
    List<String> actual = strategy.order(composeEnvironment);

    // then
    assertEquals(actual, expected);
  }

  @Test
  public void shouldOrderServicesWithTheSameDependenciesInDependsOnVolumesFromAndLinks()
      throws Exception {
    // given
    ComposeEnvironment composeEnvironment =
        new ComposeEnvironment("", new HashMap<>(), null, null, null);
    composeEnvironment
        .getServices()
        .put(
            "second",
            new ComposeService()
                .withVolumesFrom(singletonList("first"))
                .withDependsOn(singletonList("first"))
                .withLinks(singletonList("first:alias")));
    composeEnvironment
        .getServices()
        .put(
            "third",
            new ComposeService()
                .withVolumesFrom(asList("first", "second"))
                .withDependsOn(asList("first", "second"))
                .withLinks(asList("first", "second")));
    composeEnvironment.getServices().put("first", new ComposeService());
    composeEnvironment
        .getServices()
        .put(
            "forth",
            new ComposeService()
                .withVolumesFrom(singletonList("third"))
                .withDependsOn(singletonList("third"))
                .withLinks(singletonList("third")));
    composeEnvironment
        .getServices()
        .put(
            "fifth",
            new ComposeService()
                .withVolumesFrom(asList("forth", "first"))
                .withDependsOn(asList("forth", "first"))
                .withLinks(asList("forth:alias", "first")));
    List<String> expected = asList("first", "second", "third", "forth", "fifth");

    // when
    List<String> actual = strategy.order(composeEnvironment);

    // then
    assertEquals(actual, expected);
  }

  @Test
  public void shouldOrderServicesWithComplementaryDependenciesInDependsOnLinksAndVolumesFrom()
      throws Exception {
    // given
    ComposeEnvironment composeEnvironment =
        new ComposeEnvironment("", new HashMap<>(), null, null, null);
    composeEnvironment
        .getServices()
        .put("second", new ComposeService().withVolumesFrom(singletonList("first")));
    composeEnvironment
        .getServices()
        .put(
            "third",
            new ComposeService()
                .withVolumesFrom(singletonList("second"))
                .withDependsOn(singletonList("first")));
    composeEnvironment.getServices().put("first", new ComposeService());
    composeEnvironment
        .getServices()
        .put(
            "forth",
            new ComposeService()
                .withVolumesFrom(singletonList("third"))
                .withDependsOn(singletonList("second"))
                .withLinks(singletonList("first:alias")));
    composeEnvironment
        .getServices()
        .put(
            "fifth",
            new ComposeService()
                .withVolumesFrom(singletonList("first"))
                .withLinks(singletonList("forth"))
                .withDependsOn(singletonList("second")));
    List<String> expected = asList("first", "second", "third", "forth", "fifth");

    // when
    List<String> actual = strategy.order(composeEnvironment);

    // then
    assertEquals(actual, expected);
  }

  @Test(
    expectedExceptions = ValidationException.class,
    expectedExceptionsMessageRegExp =
        "Dependency 'fifth' in service 'second' points to unknown service."
  )
  public void shouldFailIfDependsOnFieldContainsNonExistingService() throws Exception {
    // given
    ComposeEnvironment composeEnvironment =
        new ComposeEnvironment("", new HashMap<>(), null, null, null);
    composeEnvironment
        .getServices()
        .put("second", new ComposeService().withDependsOn(singletonList("fifth")));
    composeEnvironment.getServices().put("third", new ComposeService());
    composeEnvironment.getServices().put("first", new ComposeService());

    // when
    strategy.order(composeEnvironment);
  }

  @Test(
    expectedExceptions = ValidationException.class,
    expectedExceptionsMessageRegExp =
        "Dependency 'fifth' in service 'third' points to unknown service."
  )
  public void shouldFailIfVolumesFromFieldContainsNonExistingService() throws Exception {
    // given
    ComposeEnvironment composeEnvironment =
        new ComposeEnvironment("", new HashMap<>(), null, null, null);
    composeEnvironment.getServices().put("second", new ComposeService());
    composeEnvironment
        .getServices()
        .put("third", new ComposeService().withVolumesFrom(singletonList("fifth")));
    composeEnvironment.getServices().put("first", new ComposeService());

    // when
    strategy.order(composeEnvironment);
  }

  @Test(
    expectedExceptions = ValidationException.class,
    expectedExceptionsMessageRegExp = "Service volumes_from '.*' is invalid"
  )
  public void shouldFailIfVolumesFromFieldHasIllegalFormat() throws Exception {
    // given
    ComposeEnvironment composeEnvironment =
        new ComposeEnvironment("", new HashMap<>(), null, null, null);
    composeEnvironment
        .getServices()
        .put(
            "second",
            new ComposeService().withVolumesFrom(singletonList("first:broken:dependency")));
    composeEnvironment.getServices().put("third", new ComposeService());
    composeEnvironment.getServices().put("first", new ComposeService());

    // when
    strategy.order(composeEnvironment);
  }

  @Test(
    expectedExceptions = ValidationException.class,
    expectedExceptionsMessageRegExp =
        "Dependency 'fifth' in service 'second' points to unknown service."
  )
  public void shouldFailIfLinksFieldContainsNonExistingService() throws Exception {
    // given
    ComposeEnvironment composeEnvironment =
        new ComposeEnvironment("", new HashMap<>(), null, null, null);
    composeEnvironment
        .getServices()
        .put("second", new ComposeService().withLinks(singletonList("fifth")));
    composeEnvironment.getServices().put("third", new ComposeService());
    composeEnvironment.getServices().put("first", new ComposeService());

    // when
    strategy.order(composeEnvironment);
  }

  @Test(
    expectedExceptions = ValidationException.class,
    expectedExceptionsMessageRegExp = "Service link '.*' is invalid"
  )
  public void shouldFailIfLinksFieldHasIllegalFormat() throws Exception {
    // given
    ComposeEnvironment composeEnvironment =
        new ComposeEnvironment("", new HashMap<>(), null, null, null);
    composeEnvironment
        .getServices()
        .put("second", new ComposeService().withLinks(singletonList("first:broken:dependency")));
    composeEnvironment.getServices().put("third", new ComposeService());
    composeEnvironment.getServices().put("first", new ComposeService());

    // when
    strategy.order(composeEnvironment);
  }
}
