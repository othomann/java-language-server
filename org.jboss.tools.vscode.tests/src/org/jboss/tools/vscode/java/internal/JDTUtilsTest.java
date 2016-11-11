/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.vscode.java.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.vscode.java.internal.managers.ProjectsManager;
import org.junit.Test;

/**
 * @author Fred Bricon
 *
 */
public class JDTUtilsTest extends AbstractWorkspaceTest {

	@Test
	public void testGetPackageNameString() throws Exception {
		String content = "package foo.bar.internal";
		assertEquals("foo.bar.internal", JDTUtils.getPackageName(null, content));

		content = "some junk";
		assertEquals("", JDTUtils.getPackageName(null, content));

		content = "";
		assertEquals("", JDTUtils.getPackageName(null, content));

		assertEquals("", JDTUtils.getPackageName(null, (String)null));
	}

	@Test
	public void testGetPackageNameURI() throws Exception {
		URI src = Paths.get("projects", "eclipse", "hello", "src", "java", "Foo.java").toUri();
		String packageName = JDTUtils.getPackageName(null, src);
		assertEquals("java", packageName);
	}

	@Test
	public void testResolveStandaloneCompilationUnit() throws Exception {
		Path helloSrcRoot = Paths.get("projects", "eclipse", "hello", "src").toAbsolutePath();
		URI uri = helloSrcRoot.resolve(Paths.get("java", "Foo.java")).toUri();
		ICompilationUnit cu = JDTUtils.resolveCompilationUnit(uri.toString());
		assertNotNull("Could not find compilation unit for " + uri, cu);
		assertEquals(ProjectsManager.DEFAULT_PROJECT_NAME, cu.getResource().getProject().getName());
		IJavaElement[] elements = cu.getChildren();
		assertEquals(2, elements.length);
		assertTrue(IPackageDeclaration.class.isAssignableFrom(elements[0].getClass()));
		assertTrue(IType.class.isAssignableFrom(elements[1].getClass()));

		IResource[] resources = ResourcesPlugin.getWorkspace().getRoot().findContainersForLocationURI(uri);
		assertEquals(1, resources.length);

		uri = helloSrcRoot.resolve("NoPackage.java").toUri();
		cu = JDTUtils.resolveCompilationUnit(uri.toString());
		assertNotNull("Could not find compilation unit for " + uri, cu);
		assertEquals(ProjectsManager.DEFAULT_PROJECT_NAME, cu.getResource().getProject().getName());
		elements = cu.getChildren();
		assertEquals(1, elements.length);
		assertTrue(IType.class.isAssignableFrom(elements[0].getClass()));
	}

	@Test
	public void testUnresolvableCompilationUnits() {
		assertNull(JDTUtils.resolveCompilationUnit(null));
		assertNull(JDTUtils.resolveCompilationUnit("foo/bar/Clazz.java"));
		assertNull(JDTUtils.resolveCompilationUnit("file:///foo/bar/Clazz.java"));
	}

}
