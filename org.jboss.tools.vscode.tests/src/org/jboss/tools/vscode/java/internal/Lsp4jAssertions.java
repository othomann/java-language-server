/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies range distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.vscode.java.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

/**
 * A set of assertion methods about LSP4J entities
 *
 * @author Fred Bricon
 *
 */
public class Lsp4jAssertions {

	public static void assertRange(int expectedLine, int expectedStart, int expectedEnd, Range range) {
		assertNotNull("Range is null", range);
		assertPosition(expectedLine, expectedStart, range.getStart());
		assertPosition(expectedLine, expectedEnd, range.getEnd());
	}

	public static void assertPosition(int expectedLine, int expectedChar, Position position) {
		assertNotNull("Position is null", position);
		assertEquals("Unexpected line position from "+position, expectedLine, position.getLine());
		assertEquals("Unexpected character position from "+position, expectedChar, position.getCharacter());
	}

	public static void assertTextEdit(int expectedLine, int expectedStart, int expectedEnd, String expectedText, TextEdit edit){
		assertNotNull("TextEdit is null");
		assertEquals(expectedText, edit.getNewText());
		assertRange(expectedLine, expectedStart, expectedEnd, edit.getRange());

	}

}
