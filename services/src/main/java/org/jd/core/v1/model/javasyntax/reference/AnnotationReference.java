/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.reference;

import org.jd.core.v1.model.javasyntax.type.ObjectType;

public class AnnotationReference implements BaseAnnotationReference {
    protected final ObjectType type;
    protected final BaseElementValue elementValue;
    protected final BaseElementValuePair elementValuePairs;

    public AnnotationReference(ObjectType type) {
        this(type, null, null);
    }

    public AnnotationReference(ObjectType type, BaseElementValue elementValue) {
        this(type, elementValue, null);
    }

    public AnnotationReference(ObjectType type, BaseElementValuePair elementValuePairs) {
        this(type, null, elementValuePairs);
    }

    protected AnnotationReference(ObjectType type, BaseElementValue elementValue, BaseElementValuePair elementValuePairs) {
        this.type = type;
        this.elementValue = elementValue;
        this.elementValuePairs = elementValuePairs;
    }

    public ObjectType getType() {
        return type;
    }

    public BaseElementValue getElementValue() {
        return elementValue;
    }

    public BaseElementValuePair getElementValuePairs() {
        return elementValuePairs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AnnotationReference that)) {
            return false;
        }

        if (elementValue != null ? !elementValue.equals(that.elementValue) : that.elementValue != null) {
            return false;
        }
        if (elementValuePairs != null ? !elementValuePairs.equals(that.elementValuePairs) : that.elementValuePairs != null) {
            return false;
        }
        return type.equals(that.type);
    }

    @Override
    public int hashCode() {
        int result = 970748295 + type.hashCode();
        result = 31 * result + (elementValue != null ? elementValue.hashCode() : 0);
        result = 31 * result + (elementValuePairs != null ? elementValuePairs.hashCode() : 0);
        return result;
    }

    @Override
    public void accept(ReferenceVisitor visitor) {
        visitor.visit(this);
    }
}
