package ar.edu.taco.regresion.sbp.regresion.arithmetic;

import ar.edu.taco.regresion.sbp.regresion.GenericTestBase;
import ar.uba.dc.rfm.dynalloy.visualization.VizException;

public class TestMultipleCallMultiplication extends GenericTestBase {

	@Override
	protected String getClassToCheck() {
		return "ar.edu.taco.arithmetic.MultipleCallMultiplication";
	}

	public void test_entry_point() throws VizException {
		setConfigKeyRelevantClasses("ar.edu.taco.arithmetic.MultipleCallMultiplication");
		setConfigKeyRelevancyAnalysis(true);
		setConfigKeyUseJavaArithmetic(true);
		setConfigKeyRemoveQuantifiers(true);

		runAndCheck(GENERIC_PROPERTIES, "entry_point_0", true);
	}

}
