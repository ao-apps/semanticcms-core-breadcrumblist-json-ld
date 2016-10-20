/*
 * semanticcms-core-breadcrumblist-json-ld - BreadcrumbList for SemanticCMS in JSON-LD format.
 * Copyright (C) 2016  AO Industries, Inc.
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
 * along with semanticcms-core-breadcrumblist-json-ld.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.semanticcms.core.breadcrumblist.jsonld;

import com.semanticcms.core.servlet.SemanticCMS;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener("Registers the BreadcrumbListJsonLd component in SemanticCMS.")
public class BreadcrumbListJsonLdContextListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent event) {
		SemanticCMS.getInstance(event.getServletContext()).addComponent(new BreadcrumbListJsonLd());
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		// Do nothing
	}
}
