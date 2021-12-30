/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.model.javasyntax.declaration;

import org.jd.core.v1.model.javasyntax.expression.Expression;

public class ExpressionVariableInitializer implements VariableInitializer {
    private Expression expression;

    public ExpressionVariableInitializer(Expression expression) {
        this.expression = expression;
    }

    @Override
    public Expression getExpression() {
        return expression;
    }

    @Override
    public int getLineNumber() {
        return expression.getLineNumber();
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExpressionVariableInitializer that)) {
            return false;
        }

        return expression != null ? expression.equals(that.expression) : that.expression == null;
    }

    @Override
    public int hashCode() {
        return 25107399 + (expression != null ? expression.hashCode() : 0);
    }

    @Override
    public boolean isExpressionVariableInitializer() { return true; }

    @Override
    public void accept(DeclarationVisitor visitor) {
        visitor.visit(this);
    }
}
