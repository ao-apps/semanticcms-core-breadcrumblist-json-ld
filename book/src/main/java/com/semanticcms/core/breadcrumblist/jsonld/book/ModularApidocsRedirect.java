/*
 * semanticcms-core-breadcrumblist-json-ld - BreadcrumbList for SemanticCMS in JSON-LD format.
 * Copyright (C) 2021, 2022  AO Industries, Inc.
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

package com.semanticcms.core.breadcrumblist.jsonld.book;

import com.aoapps.net.URIParametersUtils;
import com.aoapps.servlet.http.HttpServletUtil;
import java.io.IOException;
import java.util.Objects;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Performs redirects for conversion to modular apidocs.
 */
@WebServlet("/core/breadcrumblist-json-ld/apidocs/com/*")
public class ModularApidocsRedirect extends HttpServlet {

  private static final long serialVersionUID = 1L;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    HttpServletUtil.sendRedirect(
        HttpServletResponse.SC_MOVED_PERMANENTLY, req, resp,
        "/core/breadcrumblist-json-ld/apidocs/com.semanticcms.core.breadcrumblist.jsonld/com" + Objects.toString(req.getPathInfo(), ""),
        URIParametersUtils.of(req.getQueryString()), true, false
    );
  }
}
