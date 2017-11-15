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
package org.eclipse.che.ide.ext.git.client.compare.branchlist;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import elemental.dom.Element;
import elemental.html.TableCellElement;
import elemental.html.TableElement;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import org.eclipse.che.api.git.shared.Branch;
import org.eclipse.che.ide.FontAwesome;
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.ide.ext.git.client.GitResources;
import org.eclipse.che.ide.ui.list.FilterableSimpleList;
import org.eclipse.che.ide.ui.list.SimpleList;
import org.eclipse.che.ide.ui.window.Window;
import org.eclipse.che.ide.util.dom.Elements;
import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * The implementation of {@link BranchListView}.
 *
 * @author Igor Vinokur
 */
@Singleton
public class BranchListViewImpl extends Window implements BranchListView {
  interface BranchViewImplUiBinder extends UiBinder<Widget, BranchListViewImpl> {}

  private static BranchViewImplUiBinder uiBinder = GWT.create(BranchViewImplUiBinder.class);

  Button btnClose;
  Button btnCompare;
  @UiField ScrollPanel branchesPanel;
  @UiField Label searchFilterLabel;
  @UiField Label searchFilterIcon;

  @UiField(provided = true)
  final GitResources res;

  private ActionDelegate delegate;
  private final GitLocalizationConstant locale;
  private final FilterableSimpleList<Branch> branchesList;

  @Inject
  protected BranchListViewImpl(
      GitResources resources,
      GitLocalizationConstant locale,
      org.eclipse.che.ide.Resources coreRes) {
    this.res = resources;
    this.locale = locale;
    this.ensureDebugId("git-compare-branch-window");

    Widget widget = uiBinder.createAndBindUi(this);

    this.setTitle(locale.compareWithBranchTitle());
    this.searchFilterIcon.getElement().setInnerHTML(FontAwesome.SEARCH);
    this.setWidget(widget);

    TableElement branchElement = Elements.createTableElement();
    branchElement.setAttribute("style", "width: 100%");
    SimpleList.ListEventDelegate<Branch> listBranchesDelegate =
        new SimpleList.ListEventDelegate<Branch>() {
          public void onListItemClicked(Element itemElement, Branch itemData) {
            branchesList.getSelectionModel().setSelectedItem(itemData);
            delegate.onBranchSelected(itemData);
          }

          public void onListItemDoubleClicked(Element listItemBase, Branch itemData) {
            delegate.onCompareClicked();
          }
        };
    SimpleList.ListItemRenderer<Branch> listBranchesRenderer =
        new SimpleList.ListItemRenderer<Branch>() {
          @Override
          public void render(Element itemElement, Branch itemData) {
            TableCellElement label = Elements.createTDElement();

            SafeHtmlBuilder sb = new SafeHtmlBuilder();

            sb.appendHtmlConstant("<table><tr><td>");
            sb.appendHtmlConstant(
                "<div id=\""
                    + UIObject.DEBUG_ID_PREFIX
                    + "git-compare-branch-"
                    + itemData.getDisplayName()
                    + "\">");
            sb.appendEscaped(itemData.getDisplayName());
            sb.appendHtmlConstant("</td>");

            if (itemData.isActive()) {
              SVGResource icon = res.currentBranch();
              sb.appendHtmlConstant("<td><img src=\"" + icon.getSafeUri().asString() + "\"></td>");
            }

            sb.appendHtmlConstant("</tr></table>");

            label.setInnerHTML(sb.toSafeHtml().asString());

            itemElement.appendChild(label);
          }

          @Override
          public Element createElement() {
            return Elements.createTRElement();
          }
        };
    branchesList =
        FilterableSimpleList.create(
            (SimpleList.View) branchElement,
            coreRes.defaultSimpleListCss(),
            coreRes.defaultFilterableListCss(),
            listBranchesRenderer,
            listBranchesDelegate,
            this::onFilterChanged);
    this.branchesPanel.add(branchesList);

    createButtons();
  }

  private void onFilterChanged(String filter) {
    if (branchesList.getSelectionModel().getSelectedItem() == null) {
      delegate.onBranchUnselected();
    }
    delegate.onFilterChanged(filter);
  }

  @Override
  public void setDelegate(ActionDelegate delegate) {
    this.delegate = delegate;
  }

  @Override
  public void setBranches(@NotNull List<Branch> branches) {
    branchesList.render(
        branches.stream().collect(Collectors.toMap(Branch::getDisplayName, branch -> branch)));
    if (branchesList.getSelectionModel().getSelectedItem() == null) {
      delegate.onBranchUnselected();
    }
  }

  @Override
  public void setEnableCompareButton(boolean enabled) {
    btnCompare.setEnabled(enabled);
  }

  @Override
  public void close() {
    this.hide();
  }

  @Override
  public void showDialog() {
    this.show();
    searchFilterIcon.setVisible(false);
    branchesList.setFocus(true);
  }

  @Override
  public void setFilterContent(String content) {
    searchFilterIcon.setVisible(!content.isEmpty());
    searchFilterLabel.setText(content);
  }

  @Override
  public void clearFilter() {
    branchesList.clearFilter();
    searchFilterIcon.setVisible(false);
  }

  private void createButtons() {
    btnClose =
        createButton(locale.buttonClose(), "git-compare-branch-close", event -> delegate.onClose());
    addButtonToFooter(btnClose);

    btnCompare =
        createButton(
            locale.buttonCompare(),
            "git-compare-branch-compare",
            event -> delegate.onCompareClicked());
    addButtonToFooter(btnCompare);
  }

  @Override
  public void onClose() {
    delegate.onClose();
  }
}
