/*
 * TACO: Translation of Annotated COde
 * Copyright (c) 2010 Universidad de Buenos Aires
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA,
 * 02110-1301, USA
 */
package ar.edu.taco.regresion.simplifier;

import ar.edu.taco.regresion.GenericTestBase;
import ar.uba.dc.rfm.dynalloy.visualization.VizException;

public class NonNullAnnotationTest extends GenericTestBase {

	@Override
	protected String getClassToCheck() {
		return "ar.edu.taco.simplifier.NonNullAnnotation";
	}

	public void test_non_null_return_value() throws VizException {
		runAndCheck(GENERIC_PROPERTIES,"non_null_return_value_0", true);
	}
	
	public void test_non_null_argument() throws VizException {
		runAndCheck(GENERIC_PROPERTIES,"non_null_argument_0", false);
	}
	
}