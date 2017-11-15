/**
 * Copyright (c) 2012-2017 Red Hat, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.ide.ui.list;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.FocusPanel;

import java.util.Map;
import java.util.stream.Collectors;

public class FilterableSimpleList<M> extends FocusPanel {
  private Map<String, M> map;
  private SimpleList<M> simpleList;
  private StringBuilder stringBuilder;

  public interface Delegate {
    void onFilterChanged(String filter);
  }

  private FilterableSimpleList(
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
        keyDownEvent -> {
          int keyCode = keyDownEvent.getNativeEvent().getKeyCode();
          if (keyCode == 8) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            delegate.onFilterChanged(stringBuilder.toString());
            filter();
          } else if (keyCode == 27 && !stringBuilder.toString().isEmpty()) {
            stringBuilder.delete(0, stringBuilder.length() + 1);
            keyDownEvent.stopPropagation();
            delegate.onFilterChanged("");
            filter();
          }
        });
    addKeyPressHandler(
        keyPressEvent -> {
          stringBuilder.append(String.valueOf(keyPressEvent.getCharCode()));
          delegate.onFilterChanged(stringBuilder.toString());
          filter();
        });
    add(simpleList);
    stringBuilder = new StringBuilder();
  }

  public static <M> FilterableSimpleList<M> create(
      SimpleList.View view,
      SimpleList.Css simpleListCss,
      Css filterableListCss,
      SimpleList.ListItemRenderer<M> itemRenderer,
      SimpleList.ListEventDelegate<M> eventDelegate,
      Delegate delegate) {
    return new FilterableSimpleList<>(
        view, simpleListCss, filterableListCss, itemRenderer, eventDelegate, delegate);
  }

  public void render(Map<String, M> map) {
    this.map = map;
    filter();
  }

  private void filter() {
    simpleList.render(
        map.keySet()
            .stream()
            .filter(name -> name.startsWith(stringBuilder.toString()))
            .map(name -> map.get(name))
            .collect(Collectors.toList()));
  }

  public void clearFilter() {
    stringBuilder.delete(0, stringBuilder.length() + 1);
  }

  public HasSelection<M> getSelectionModel() {
    return simpleList.getSelectionModel();
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
