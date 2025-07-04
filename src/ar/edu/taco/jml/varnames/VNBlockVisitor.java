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
package ar.edu.taco.jml.varnames;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmlspecs.checker.JmlAssertStatement;
import org.jmlspecs.checker.JmlAssignmentStatement;
import org.jmlspecs.checker.JmlAssumeStatement;
import org.jmlspecs.checker.JmlLoopInvariant;
import org.jmlspecs.checker.JmlLoopStatement;
import org.jmlspecs.checker.JmlPredicate;
import org.jmlspecs.checker.JmlSpecExpression;
import org.jmlspecs.checker.JmlVariableDefinition;
import org.jmlspecs.checker.JmlVariantFunction;
import org.jmlspecs.jmlrac.JavaAndJmlPrettyPrint2;
import org.multijava.mjc.CClassType;
import org.multijava.mjc.CExpressionContextType;
import org.multijava.mjc.CType;
import org.multijava.mjc.JAssertStatement;
import org.multijava.mjc.JBlock;
import org.multijava.mjc.JCastExpression;
import org.multijava.mjc.JExpression;
import org.multijava.mjc.JExpressionStatement;
import org.multijava.mjc.JIfStatement;
import org.multijava.mjc.JLocalVariableExpression;
import org.multijava.mjc.JMethodDeclaration;
import org.multijava.mjc.JReturnStatement;
import org.multijava.mjc.JStatement;
import org.multijava.mjc.JThrowStatement;
import org.multijava.mjc.JVariableDeclarationStatement;
import org.multijava.mjc.JVariableDefinition;
import org.multijava.mjc.JWhileStatement;
import org.multijava.util.compiler.FastStringBuffer;
import org.multijava.util.compiler.PositionedError;
import org.multijava.util.compiler.UnpositionedError;

import ar.edu.taco.jml.utils.ASTUtils;
import ar.edu.taco.utils.jml.JmlAstClonerStatementVisitor;

public class VNBlockVisitor extends JmlAstClonerStatementVisitor {
    private static int variableNameIndex = 0;

    public String createNewName(String originalName) {
        variableNameIndex++;
        String s = "var_" + variableNameIndex + "_" + originalName;
        return s;
    }

    private Map<String, String> variableMapping;

    public VNBlockVisitor() {
        variableMapping = new HashMap<String, String>();
    }

    public VNBlockVisitor(Map<String, String> variableMapping) {
        this.variableMapping = variableMapping;
    }

    @Override
    public void visitMethodDeclaration(JMethodDeclaration arg0) {
        super.visitMethodDeclaration(arg0);
        variableMapping = new HashMap<String, String>();
    }

    @Override
    public void visitBlockStatement(JBlock self) {
        Map<String, String> variableMappingOld = this.variableMapping;
        variableMapping = new HashMap<String, String>(this.variableMapping);

        List<JStatement> declarationList = new ArrayList<JStatement>();
        List<JStatement> statementList = new ArrayList<JStatement>();

        for (int i = 0; i < self.body().length; i++) {
            JStatement statement = self.body()[i];
            {
                VNBlockVisitor visitor = new VNBlockVisitor(variableMapping);
                statement.accept(visitor);

                JStatement aStatement = (JStatement) visitor.getStack().pop();

                // If the statement is a Local variable declaration, we are
                // going to skip it.
                if (!(aStatement instanceof JExpressionStatement) || !(((JExpressionStatement) aStatement).expr() instanceof JLocalVariableExpression)) {
                    statementList.add(aStatement);
                }
            }
        }

        JStatement[] statements = new JStatement[declarationList.size() + statementList.size()];
        int i = 0;
        for (JStatement statement : declarationList) {
            assert (statement != null);

            statements[i] = statement;
            i++;
        }

        for (JStatement statement : statementList) {
            assert (statement != null);
            statements[i] = statement;
            i++;
        }

        for (int j = 0; j < statements.length; j++) {
            JStatement statement = statements[j];
            assert (statement != null);
        }

        assert (statements != null);
        JBlock newSelf = new JBlock(self.getTokenReference(), statements, self.getComments());
        this.getStack().push(newSelf);

        JavaAndJmlPrettyPrint2 prettyPrinter = new JavaAndJmlPrettyPrint2();
        newSelf.accept(prettyPrinter);

        // super.visitBlockStatement(new JBlock(self.getTokenReference(),
        // statements, self.getComments()));

        this.variableMapping = variableMappingOld;
    }

    // BEGIN - ESStatementVisitor

    @Override
    public void visitIfStatement(/* @non_null */JIfStatement self) {

        self.thenClause().accept(this);
        JStatement newThen = (JStatement) this.getStack().pop();
        JStatement newElse = null;
        if (self.elseClause() != null) {
            self.elseClause().accept(this);
            newElse = (JStatement) this.getStack().pop();
        }

        VNExpressionVisitor conditionSimplifierVisitor = new VNExpressionVisitor(variableMapping);
        self.cond().accept(conditionSimplifierVisitor);
        JExpression condition = conditionSimplifierVisitor.getArrayStack().pop();

        JIfStatement newIfStatement = ASTUtils.createIfStatement(condition, newThen, newElse, self.getComments());

        this.getStack().push(newIfStatement);
    }


    @Override
    public void visitAssertStatement(JAssertStatement self){
        VNExpressionVisitor conditionSimplifierVisitor = new VNExpressionVisitor(variableMapping);
        self.predicate().accept(conditionSimplifierVisitor);
        JExpression condition = conditionSimplifierVisitor.getArrayStack().pop();

        JAssertStatement newAssertStatement = new JAssertStatement(self.getTokenReference(), condition, self.getComments());

        this.getStack().push(newAssertStatement);

    }

    @Override
    public void visitJmlLoopStatement(JmlLoopStatement self) {
        JmlLoopInvariant[] newJmlLoopInvariants = new JmlLoopInvariant[self.loopInvariants().length];
        for (int x = 0; x < self.loopInvariants().length; x++) {
            JmlLoopInvariant aJmlLoopInvariant = self.loopInvariants()[x];
            VNExpressionVisitor exprSimplifierVisitor = new VNExpressionVisitor(variableMapping);

            aJmlLoopInvariant.predicate().specExpression().expression().accept(exprSimplifierVisitor);
            JExpression expr = exprSimplifierVisitor.getArrayStack().pop();
            JmlPredicate newJmlPredicate = new JmlPredicate(new JmlSpecExpression(expr));

            JmlLoopInvariant newJmlLoopInvariant = new JmlLoopInvariant(aJmlLoopInvariant.getTokenReference(), aJmlLoopInvariant.isRedundantly(), newJmlPredicate);
            newJmlLoopInvariants[x] = newJmlLoopInvariant;
        }


        JmlVariantFunction[] newJmlLoopVariants = new JmlVariantFunction[self.variantFunctions().length];
        for (int x = 0; x < self.variantFunctions().length; x++) {
            JmlVariantFunction aJmlLoopVariant = self.variantFunctions()[x];
            VNExpressionVisitor exprSimplifierVisitor = new VNExpressionVisitor(variableMapping);

            aJmlLoopVariant.specExpression().expression().accept(exprSimplifierVisitor);
            JExpression expr = exprSimplifierVisitor.getArrayStack().pop();
            JmlSpecExpression newJmlExpression = new JmlSpecExpression(expr);

            JmlVariantFunction newJmlLoopVariant = new JmlVariantFunction(self.getTokenReference(), self.acceptsBreak(), newJmlExpression);
            newJmlLoopVariants[x] = newJmlLoopVariant;
        }

        VNBlockVisitor visitor = new VNBlockVisitor(variableMapping);
        self.stmt().accept(visitor);

        JStatement newStatement = (JStatement) visitor.getStack().pop();

        JmlLoopStatement newJmlLoopStatement = new JmlLoopStatement(self.getTokenReference(), newJmlLoopInvariants, newJmlLoopVariants, newStatement, self.getComments());

        this.getStack().push(newJmlLoopStatement);
    }

    @Override
    public void visitWhileStatement(JWhileStatement self) {
        self.body().accept(this);
        JStatement newBody = (JStatement) this.getStack().pop();

        VNExpressionVisitor conditionSimplifierVisitor = new VNExpressionVisitor(variableMapping);
        self.cond().accept(conditionSimplifierVisitor);
        JExpression condition = conditionSimplifierVisitor.getArrayStack().pop();

        JWhileStatement newJWhileStatement = new JWhileStatement(self.getTokenReference(), condition, newBody, self.getComments());

        this.getStack().push(newJWhileStatement);
    }

    @Override
    public void visitVariableDeclarationStatement(JVariableDeclarationStatement self) {

        JVariableDefinition[] newVars = new JVariableDefinition[self.getVars().length];
        for (int i = 0; i < self.getVars().length; i++) {
            JVariableDefinition variableDefinition = self.getVars()[i];
            variableDefinition.accept(this);
            newVars[i] = (JVariableDefinition) getStack().pop();
        }

        JVariableDeclarationStatement newSelf = new JVariableDeclarationStatement(self.getTokenReference(), newVars, self.getComments());
        this.getStack().push(newSelf);
    }

    @Override
    public void visitJmlVariableDefinition(JmlVariableDefinition self) {
        VNExpressionVisitor conditionSimplifierVisitor = new VNExpressionVisitor(variableMapping);
        self.expr().accept(conditionSimplifierVisitor);

        JmlVariableDefinition newSelf = new JmlVariableDefinition(self.getTokenReference(), self.modifiers(), self.getType(), self.ident(),
                conditionSimplifierVisitor.getArrayStack().pop());
        getStack().push(newSelf);

    }

    @Override
    public void visitVariableDefinition(JVariableDefinition self) {

        String newIdent = createNewName(self.ident());

        this.variableMapping.put(self.ident(), newIdent);

        VNExpressionVisitor conditionSimplifierVisitor = new VNExpressionVisitor(variableMapping);
        JExpression newExpr = null;
        if (self.expr() != null) {
            self.expr().accept(conditionSimplifierVisitor);
            newExpr = conditionSimplifierVisitor.getArrayStack().pop();
        }
        JVariableDefinition newSelf = new JVariableDefinition(self.getTokenReference(), self.modifiers(), self.getType(), newIdent, newExpr);
        getStack().push(newSelf);

    }

    @Override
    public void visitJmlAssignmentStatement(JmlAssignmentStatement self) {
        /*
         * ESExpressionVisitor conditionSimplifierVisitor = new
         * ESExpressionVisitor(); JExpression newExpr = null; if (self. != null)
         * { self.expr().accept(conditionSimplifierVisitor); newExpr =
         * conditionSimplifierVisitor.getArrayStack().pop(); }
         */
        self.assignmentStatement().accept(this);
        JExpressionStatement newExpressionStatement = (JExpressionStatement) this.getStack().pop();
        JmlAssignmentStatement newAssignamentStatement = new JmlAssignmentStatement(newExpressionStatement);
        getStack().push(newAssignamentStatement);

        /*
         * JExpression newExpression = visitor.getArrayStack().pop();
         * JExpressionStatement newAssignamentStatement = new
         * JmlAssignmentStatement(self.getTokenReference(), newExpression,
         * self.getComments());
         * 
         * 
         * 
         * 
         * 
         * this.getDeclarationStatements().addAll(visitor.getDeclarationStatements
         * ()); this.getNewStatements().addAll(visitor.getNewStatements());
         * getStack().push(newAssignamentStatement);
         */

    }

    @Override
    public void visitExpressionStatement(JExpressionStatement self) {
        VNExpressionVisitor visitor = new VNExpressionVisitor(variableMapping);
        self.expr().accept(visitor);
        JExpression newExpression = visitor.getArrayStack().pop();
        JExpressionStatement newExpressionStatement = new JExpressionStatement(self.getTokenReference(), newExpression, self.getComments());

        getStack().push(newExpressionStatement);

    }

    @Override
    public void visitReturnStatement(JReturnStatement self) {
        VNExpressionVisitor exprSimplifierVisitor = new VNExpressionVisitor(variableMapping);
        JExpression expr = null;

        if (self.expr() != null) {
            self.expr().accept(exprSimplifierVisitor);
            expr = exprSimplifierVisitor.getArrayStack().pop();
        }

        JReturnStatement newSelf = new JReturnStatement(self.getTokenReference(), expr, self.getComments());

        this.getStack().push(newSelf);

    }

    @Override
    public void visitThrowStatement(JThrowStatement self) {
        VNExpressionVisitor exprSimplifierVisitor = new VNExpressionVisitor(variableMapping);

        self.expr().accept(exprSimplifierVisitor);
        JExpression newExpression = exprSimplifierVisitor.getArrayStack().pop();
        JThrowStatement newExpressionStatement = new JThrowStatement(self.getTokenReference(), newExpression, self.getComments());

        this.getStack().push(newExpressionStatement);
    }

    // @Override
    // public void visitAssertStatement(JAssertStatement self) {
    // VNExpressionVisitor exprSimplifierVisitor = new
    // VNExpressionVisitor(variableMapping);
    // JExpression expr = null;
    //
    // self.predicate().accept(exprSimplifierVisitor);
    // expr = exprSimplifierVisitor.getArrayStack().pop();
    //
    // JAssertStatement newSelf = new JAssertStatement(self.getTokenReference(),
    // expr, self.getComments());
    //
    // this.getStack().push(newSelf);
    // }

    @Override
    public void visitJmlAssertStatement(JmlAssertStatement self) {
        VNExpressionVisitor exprSimplifierVisitor = new VNExpressionVisitor(variableMapping);
        JExpression expr = null;

        self.predicate().specExpression().expression().accept(exprSimplifierVisitor);
        expr = exprSimplifierVisitor.getArrayStack().pop();
        JmlPredicate jmlPredicate = new JmlPredicate(new JmlSpecExpression(expr));
        JmlAssertStatement newSelf = new JmlAssertStatement(self.getTokenReference(), self.isRedundantly(), jmlPredicate, self.throwMessage(),
                self.getComments());

        this.getStack().push(newSelf);
    }

    @Override
    public void visitJmlAssumeStatement(JmlAssumeStatement self) {
        VNExpressionVisitor exprSimplifierVisitor = new VNExpressionVisitor(variableMapping);
        JExpression expr = null;

        self.predicate().specExpression().expression().accept(exprSimplifierVisitor);
        expr = exprSimplifierVisitor.getArrayStack().pop();
        JmlPredicate jmlPredicate = new JmlPredicate(new JmlSpecExpression(expr));
        JmlAssumeStatement newSelf = new JmlAssumeStatement(self.getTokenReference(), self.isRedundantly(), jmlPredicate, self.throwMessage(),
                self.getComments());

        this.getStack().push(newSelf);
    }

    @Override
    public void visitJmlLoopInvariant(JmlLoopInvariant self) {
        VNExpressionVisitor exprSimplifierVisitor = new VNExpressionVisitor(variableMapping);
        JExpression expr = null;

        self.predicate().specExpression().expression().accept(exprSimplifierVisitor);

        expr = exprSimplifierVisitor.getArrayStack().pop();

        JmlPredicate jmlPredicate = new JmlPredicate(new JmlSpecExpression(expr));

        JmlLoopInvariant newSelf = new JmlLoopInvariant(self.getTokenReference(), self.isRedundantly(), jmlPredicate);

        this.getStack().push(newSelf);
    }

    // END - ESStatementVisitor

}
