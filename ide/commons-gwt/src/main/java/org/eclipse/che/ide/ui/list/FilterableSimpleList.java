/**
 * Copyright (c) 2012-2017 Red Hat, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.ide.ui.list;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.FocusPanel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FilterableSimpleList<M> extends FocusPanel {
  private SimpleList<M> simpleList;
  private HasSelection<M> selectionModel;

  private StringBuilder stringBuilder;
  private List<M> items;
  private Map<String, M> map;

  public interface Delegate {
    void onFilterChanged(String filter);
  }

  public interface FilterableItem {
    String getName();
  }

  public FilterableSimpleList(
      SimpleList.View view,
      SimpleList.Css css,
      Css thisCss,
      SimpleList.ListItemRenderer<M> itemRenderer,
      SimpleList.ListEventDelegate<M> eventDelegate,
      Delegate delegate) {
    super();
    simpleList = SimpleList.create(view, css, itemRenderer, eventDelegate);
    addStyleName(thisCss.container());
    addKeyDownHandler(
        new KeyDownHandler() {
          @Override
          public void onKeyDown(KeyDownEvent keyDownEvent) {
            if (keyDownEvent.getNativeEvent().getKeyCode() == 8) {
              stringBuilder.deleteCharAt(stringBuilder.length() - 1);
              delegate.onFilterChanged(stringBuilder.toString());
              update();
            }
          }
        });
    addKeyPressHandler(
        new KeyPressHandler() {
          @Override
          public void onKeyPress(KeyPressEvent keyPressEvent) {
            stringBuilder.append(String.valueOf(keyPressEvent.getCharCode()));
            delegate.onFilterChanged(stringBuilder.toString());
            update();
          }
        });
    add(simpleList);
    stringBuilder = new StringBuilder();
  }

  public void render(Map<String, M> map) {
    this.map = map;
    simpleList.render(new ArrayList<>(map.values()));
  }

  public void update() {
    List<M> collect =
        this.map
            .keySet()
            .stream()
            .filter(name -> name.startsWith(stringBuilder.toString()))
            .map(name -> map.get(name))
            .collect(Collectors.toList());
    simpleList.render(collect);
  }

  public HasSelection getSelectionModel() {
    selectionModel = simpleList.getSelectionModel();
    return selectionModel;
  }

  /** Item style selectors for a simple list item. */
  public interface Css extends CssResource {
    String container();
  }

  public interface Resources extends ClientBundle {
    @Source("org/eclipse/che/ide/ui/list/FilterableSimpleList.css")
    Css defaultFilterableListCss();
  }
}
