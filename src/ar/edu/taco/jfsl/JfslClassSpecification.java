/**
 *
 */
package ar.edu.taco.jfsl;

import ar.uba.dc.rfm.alloy.ast.formulas.AlloyFormula;

import java.util.Vector;

public class JfslClassSpecification {
    public final Vector<AlloyFormula> invariant = new Vector<AlloyFormula>();
    public final Vector<JfslSpecField> spec_fields = new Vector<JfslSpecField>();
}