/*
 * semanticcms-core-breadcrumblist-json-ld - BreadcrumbList for SemanticCMS in JSON-LD format.
 * Copyright (C) 2016, 2017, 2019, 2020, 2021, 2022, 2023  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of semanticcms-core-breadcrumblist-json-ld.
 *
 * semanticcms-core-breadcrumblist-json-ld is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * semanticcms-core-breadcrumblist-json-ld is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with semanticcms-core-breadcrumblist-json-ld.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.semanticcms.core.breadcrumblist.jsonld;

import static com.aoapps.encoding.TextInJavaScriptEncoder.textInLdJsonEncoder;

import com.aoapps.encoding.LdJsonWriter;
import com.aoapps.encoding.MediaEncoder;
import com.aoapps.encoding.MediaType;
import com.aoapps.encoding.servlet.EncodingContextEE;
import com.aoapps.html.servlet.DocumentEE;
import com.aoapps.net.URIEncoder;
import com.semanticcms.core.controller.Book;
import com.semanticcms.core.controller.PageUtils;
import com.semanticcms.core.controller.SemanticCMS;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.model.PageRef;
import com.semanticcms.core.model.ParentRef;
import com.semanticcms.core.renderer.html.Component;
import com.semanticcms.core.renderer.html.ComponentPosition;
import com.semanticcms.core.renderer.html.HtmlRenderer;
import com.semanticcms.core.renderer.html.HtmlRendererUtils;
import com.semanticcms.core.renderer.html.View;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * Adds a BreadcrumbsList in JSON-LD format script just before head end.
 * This is not a visual element for the user to navigate by, but is only
 * the script in JSON-LD.
 * </p>
 * <p>
 * This is applied to all {@link View views} and all {@link Page pages}, even those that are "noindex".
 * </p>
 * <p>
 * All links within the BreadcrumbList are within the current view and keep the view setting.
 * Thus, different views might have different BreadcrumbLists depending on the applicability
 * of a view to the parent pages.
 * </p>
 * <p>
 * Generates one BreadcrumbList for each unique path through parent pages.
 * This is done with a depth-first search through the parents DAG, with each leaf
 * node constituting a starting point for a BreadcrumbList.
 * </p>
 * <p>
 * Parents in missing books are skipped.
 * </p>
 * <p>
 * All parents are verified as applicable to the given view.
 * The search stops when there are no parents applicable to the given view.
 * The parent's parents will not be checked in this case.
 * </p>
 * <p>
 * Parents are always handled in-order, so the ordering of the resulting BreadcrumbList is
 * determined by the ordering the parents are declared.
 * </p>
 * <p>
 * When the first item of the BreadcrumbList is the site's root ({@link Book#getContentRoot() contentRoot}
 * of the {@link SemanticCMS#getRootBook() rootBook}), which will typically be the case, the root
 * is excluded from the list.
 * </p>
 * <p>
 * With the above constraints, should two paths be found to be duplicates, only the first is added.
 * </p>
 * <p>
 * The current page is always included in the BreadcrumbList, unless it is the contentRoot itself.
 * </p>
 * <p>
 * {@link ParentRef#getShortTitle() shortTitle} is used for each list entry, with the parent
 * in the list as the shortTitle context.  This was the original motivation for making shortTitle
 * be configurable on a per-parent basis.  When the site root is excluded from the BreadcrumbList,
 * it is still used as the parent context for shortTitle.
 * See {@link PageUtils#getShortTitle(com.semanticcms.core.model.PageRef, com.semanticcms.core.model.Page)} for details.
 * </p>
 * <p>
 * The URLs contained within breadcrumbLists are generated as absolute URLs, similar to how the sitemaps are generated.
 * </p>
 * <p>
 * The URLs within BreadcrumbLists are not rewritten by the current site export.  A more advanced export
 * in the future may allow for this.
 * </p>
 * See also:
 * <ul>
 *   <li><a href="https://developers.google.com/search/docs/appearance/structured-data/breadcrumb#json-ld">Google Search - Breadcrumbs</a></li>
 *   <li><a href="https://search.google.com/structured-data/testing-tool">Google Structure Data Testing Tool</a></li>
 *   <li><a href="https://audisto.com/insights/guides/2/">Breadcrumb Navigation Guide - Usability &amp; SEOM</a></li>
 *   <li><a href="https://webmaster.yandex.com/tools/microtest/">Yandex validator</a></li>
 * </ul>
 */
public class BreadcrumbListJsonLd implements Component {

  /**
   * Enables multiple <code><a href="https://schema.org/BreadcrumbList">BreadcrumbList</a></code>, defaults to
   * {@code true}.
   */
  private static final String ENABLE_MULTI_BREADCRUMB_INIT_PARAM = BreadcrumbListJsonLd.class.getName() + ".enableMultiBreadcrumb";

  /**
   * Minimum number of ListItem required in a BreadcrumbList.
   * <p>
   * <a href="https://developers.google.com/search/docs/appearance/structured-data/breadcrumb#structured-data-type-definitions">Google Search - Breadcrumbs</a>
   * states "that contains at least two ListItems".  However, Google Search Console is showing one-item breadcrumb lists
   * as valid.  In fact, these are the only ones currently showing under "Breadcrumbs" on one of our smaller sites.
   * On this same site, however, we can see that breadcrumbs are still in the index by inspecting the URL.
   * Perhaps single-item breadcrumbs are still meaningful because we do not include the home page as an entry?
   * </p>
   * <p>
   * We have reactivated single-item breadcrumbs, since their presence may inform search engines about the short title
   * of the page.
   * </p>
   */
  private static final int MIN_LIST_ITEMS = 1;

  /**
   * Registers the {@link BreadcrumbListJsonLd} component in {@link HtmlRenderer}.
   */
  @WebListener("Registers the BreadcrumbListJsonLd component in HtmlRenderer.")
  public static class Initializer implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
      ServletContext servletContext = event.getServletContext();
      // Defaults to true
      boolean enableMultiBreadcrumb = !"false".equalsIgnoreCase(servletContext.getInitParameter(ENABLE_MULTI_BREADCRUMB_INIT_PARAM));
      HtmlRenderer.getInstance(event.getServletContext()).addComponent(new BreadcrumbListJsonLd(enableMultiBreadcrumb));
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
      // Do nothing
    }
  }

  private final boolean enableMultiBreadcrumb;

  private BreadcrumbListJsonLd(boolean enableMultiBreadcrumb) {
    this.enableMultiBreadcrumb = enableMultiBreadcrumb;
  }

  @Override
  public void doComponent(
      ServletContext servletContext,
      HttpServletRequest request,
      HttpServletResponse response,
      DocumentEE document,
      View view,
      Page page,
      ComponentPosition position
  ) throws ServletException, IOException {
    if (
        view != null
            && page != null
            && position == ComponentPosition.HEAD_END
    ) {
      PageRef contentRoot = SemanticCMS.getInstance(servletContext).getRootBook().getContentRoot();
      // Find each distinct list, these will be in reverse order as a consequence of the traversal
      Set<List<Page>> distinctLists = new LinkedHashSet<>();
      findDistinctListsRecursive(
          enableMultiBreadcrumb,
          servletContext,
          request,
          response,
          view,
          contentRoot,
          distinctLists,
          new ArrayList<>(),
          page
      );
      if (!distinctLists.isEmpty()) {
        EncodingContextEE encodingContext = new EncodingContextEE(servletContext, request, response);
        // This JSON-LD is embedded in the XHTML page, use encoder
        try (
            @SuppressWarnings("deprecation")
            LdJsonWriter jsonOut = new LdJsonWriter(
                encodingContext,
                MediaEncoder.getInstance(encodingContext, MediaType.LD_JSON, MediaType.XHTML),
                document.unsafe()
            )
            ) {
          jsonOut.writePrefix();
          if (distinctLists.size() > 1) {
            jsonOut.write('[');
          }
          boolean didOne = false;
          for (List<Page> list : distinctLists) {
            if (didOne) {
              jsonOut.write(",\n");
            } else {
              didOne = true;
            }
            jsonOut.write("{\n"
                + "  \"@context\": \"https://schema.org\",\n"
                + "  \"@type\": \"BreadcrumbList\",\n"
                + "  \"itemListElement\": [");
            for (int size = list.size(), i = size - 1; i >= 0; i--) {
              final Page item = list.get(i);
              jsonOut.write("{\n"
                  + "    \"@type\": \"ListItem\",\n"
                  + "    \"position\": ");
              jsonOut.write(Integer.toString(size - i));
              jsonOut.write(",\n");
              String title = item.getTitle();
              // The parent used for shortTitle resolution, if any
              PageRef parentPageRef;
              if (i < (size - 1)) {
                parentPageRef = list.get(i + 1).getPageRef();
              } else {
                // If there is one, and only one, parent that is applicable to the given view, use it as shortTitle context
                Set<Page> applicableParents = HtmlRendererUtils.getApplicableParents(servletContext, request, response, view, item);
                if (applicableParents.size() == 1) {
                  parentPageRef = applicableParents.iterator().next().getPageRef();
                } else {
                  parentPageRef = null;
                }
              }
              String shortTitle = PageUtils.getShortTitle(parentPageRef, item);
              // ListItem.name is based on parent-relative shortTitle, only added when different than the page title
              if (!title.equals(shortTitle)) {
                jsonOut.write("    \"name\": ");
                jsonOut.text(shortTitle);
                jsonOut.write(",\n");
              }
              jsonOut.write("    \"item\": {\n"
                  + "      \"@id\": \"");
              // Write US-ASCII always per https://www.w3.org/TR/microdata/#terminology
              URIEncoder.encodeURI(
                  view.getCanonicalUrl(servletContext, request, response, item),
                  textInLdJsonEncoder,
                  jsonOut
              );
              jsonOut.write("\",\n"
                  + "      \"name\": ");
              jsonOut.text(title);
              jsonOut.write("\n"
                  + "    }\n"
                  + "  }");
              if (i != 0) {
                jsonOut.write(',');
              }
            }
            jsonOut.write("]\n"
                + "}");
          }
          if (distinctLists.size() > 1) {
            jsonOut.write(']');
          }
          jsonOut.write('\n');
          jsonOut.writeSuffix(false);
        }
      }
    }
  }


  private static void findDistinctListsRecursive(
      boolean enableMultiBreadcrumb,
      ServletContext servletContext,
      HttpServletRequest request,
      HttpServletResponse response,
      View view,
      PageRef contentRoot,
      Set<List<Page>> distinctLists,
      List<Page> currentList,
      Page currentPage
  ) throws ServletException, IOException {
    // The contentRoot is never added to the list
    boolean isContentRoot = currentPage.getPageRef().equals(contentRoot);
    if (!isContentRoot) {
      currentList.add(currentPage);
    }
    // Find all parents that are not in missing books and apply to the current view
    Set<Page> applicableParents = HtmlRendererUtils.getApplicableParents(servletContext, request, response, view, currentPage);
    if (!applicableParents.isEmpty()) {
      // Recurse further, still not at leaf
      for (Page parent : applicableParents) {
        findDistinctListsRecursive(
            enableMultiBreadcrumb,
            servletContext,
            request,
            response,
            view,
            contentRoot,
            distinctLists,
            currentList,
            parent
        );
        if (!enableMultiBreadcrumb && !distinctLists.isEmpty()) {
          break;
        }
      }
    } else {
      if (currentList.size() >= MIN_LIST_ITEMS && !distinctLists.contains(currentList)) {
        // Add copy, since currentList will continue to be altered during traversal
        distinctLists.add(new ArrayList<>(currentList));
      }
    }
    if (!isContentRoot) {
      currentList.remove(currentList.size() - 1);
    }
  }
}
