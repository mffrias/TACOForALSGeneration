// === BinomialHeapExtractTest.java ===
package roops.core.objects;

import ar.edu.taco.regresion.CollectionTesetBase;
import ar.uba.dc.rfm.dynalloy.visualization.VizException;

/**
 * JUnit driver for TACO/JML testing: only extractMin() and generateInv().
 */
public class BinomialHeapExtractTest.java extends CollectionTestBase {

    @Override
    protected String getClassToCheck() {
        return "roops.core.objects.BinomialHeap";
    }

    public void test_extractMin() throws VizException {
        // same configuration as before
        setConfigKeyRelevantClasses("roops.core.objects.BinomialHeap,roops.core.objects.BinomialHeapNode");
        setConfigKeyRelevancyAnalysis(true);
        setConfigKeyCheckNullDereference(true);
        setConfigKeyUseJavaArithmetic(false);
        setConfigKeyInferScope(true);
        setConfigKeyObjectScope(0);
        setConfigKeyIntBithwidth(5);
        setConfigKeyLoopUnroll(4);
        setConfigKeySkolemizeInstanceInvariant(true);
        setConfigKeySkolemizeInstanceAbstraction(false);
        setConfigKeyGenerateUnitTestCase(true);
        setConfigKeyAttemptToCorrectBug(false);
        setConfigKeyMaxStrykerMethodsPerFile(1);
        setConfigKeyRemoveQuantifiers(true);
        setConfigKeyUseJavaSBP(true);
        setConfigKeyUseTightUpperBounds(true);
        setConfigKeyTypeScopes("roops.core.objects.BinomialHeap:1,roops.core.objects.BinomialHeapNode:13");
        check(GENERIC_PROPERTIES, "extractMin()", false);
    }

    public void test_generateInv() throws VizException {
        // same configuration; just change the target to generateInv()
        setConfigKeyRelevantClasses("roops.core.objects.BinomialHeap,roops.core.objects.BinomialHeapNode");
        setConfigKeyRelevancyAnalysis(true);
        setConfigKeyCheckNullDereference(true);
        setConfigKeyUseJavaArithmetic(false);
        setConfigKeyInferScope(true);
        setConfigKeyObjectScope(0);
        setConfigKeyIntBithwidth(5);
        setConfigKeyLoopUnroll(4);
        setConfigKeySkolemizeInstanceInvariant(true);
        setConfigKeySkolemizeInstanceAbstraction(false);
        setConfigKeyGenerateUnitTestCase(true);
        setConfigKeyAttemptToCorrectBug(false);
        setConfigKeyMaxStrykerMethodsPerFile(1);
        setConfigKeyRemoveQuantifiers(true);
        setConfigKeyUseJavaSBP(true);
        setConfigKeyUseTightUpperBounds(true);
        setConfigKeyTypeScopes("roops.core.objects.BinomialHeap:1,roops.core.objects.BinomialHeapNode:13");
        check(GENERIC_PROPERTIES, "generateInv()", false);
    }
}
