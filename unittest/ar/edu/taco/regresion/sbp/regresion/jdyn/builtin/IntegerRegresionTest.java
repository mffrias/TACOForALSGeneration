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
package ar.edu.taco.regresion.sbp.regresion.jdyn.builtin;

import ar.edu.taco.regresion.sbp.regresion.GenericTestBase;
import ar.uba.dc.rfm.dynalloy.visualization.VizException;

public class IntegerRegresionTest extends GenericTestBase {

	@Override
	protected String getClassToCheck() {
		return "ar.edu.taco.builtin.IntegerTest";
	}

	public void testRunAndCheck_valueOf_String_OK() throws VizException {
		runAndCheck(GENERIC_PROPERTIES,"valueOf_String_OK_0", false);		
	}

	public void testRunAndCheck_valueOf_String_Count() throws VizException {
		setConfigKeyRemoveQuantifiers(true);
		runAndCheck(GENERIC_PROPERTIES,"valueOf_String_Count_0", true);		
	}
	
	public void testRunAndCheck_valueOf_Int_OK() throws VizException {
		runAndCheck(GENERIC_PROPERTIES,"valueOf_Int_OK_0", false);		
	}

	public void testRunAndCheck_valueOf_Int_Count() throws VizException {
		setConfigKeyRemoveQuantifiers(true);
		runAndCheck(GENERIC_PROPERTIES,"valueOf_Int_Count_0", true);		
	}
	
	public void testRunAndCheck_toString_OK() throws VizException {
		runAndCheck(GENERIC_PROPERTIES,"toString_OK_0", false);		
	}

	public void testRunAndCheck_toString_Count() throws VizException {
		setConfigKeyRemoveQuantifiers(true);
		runAndCheck(GENERIC_PROPERTIES,"toString_Count_0", true);		
	}

	public void testRunAndCheck_equals_OK() throws VizException {
		runAndCheck(GENERIC_PROPERTIES,"equals_OK_0", true);		
	}

	public void testRunAndCheck_equals_Count() throws VizException {
		runAndCheck(GENERIC_PROPERTIES,"equals_Count_0", true);		
	}

	public void testRunAndCheck_hashCode_OK() throws VizException {
		runAndCheck(GENERIC_PROPERTIES,"hashCode_OK_0", false);		
	}

	public void testRunAndCheck_hashCode_Count() throws VizException {
		runAndCheck(GENERIC_PROPERTIES,"hashCode_Count_0", true);		
	}
}
