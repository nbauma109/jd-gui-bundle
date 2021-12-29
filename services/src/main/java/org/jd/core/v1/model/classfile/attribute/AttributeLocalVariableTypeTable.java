/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.classfile.attribute;

public class AttributeLocalVariableTypeTable implements Attribute {
    private final LocalVariableType[] localVariableTypeTable;

    public AttributeLocalVariableTypeTable(LocalVariableType[] localVariableTypeTable) {
        this.localVariableTypeTable = localVariableTypeTable;
    }

    public LocalVariableType[] getLocalVariableTypeTable() {
        return localVariableTypeTable;
    }
}
