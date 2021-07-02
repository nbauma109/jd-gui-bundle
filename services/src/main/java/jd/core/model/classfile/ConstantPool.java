/*******************************************************************************
 * Copyright (C) 2007-2019 Emmanuel Dupuy GPLv3
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package jd.core.model.classfile;

import org.jd.core.v1.model.classfile.constant.*;
import org.jd.core.v1.util.StringConstants;

import java.util.ArrayList;
import java.util.List;

import jd.core.util.IndexToIndexMap;
import jd.core.util.StringToIndexMap;

public class ConstantPool
{
    private List<Constant> listOfConstants;
    private StringToIndexMap constantUtf8ToIndex;
    private IndexToIndexMap constantClassToIndex;

    public final int instanceConstructorIndex;
    public final int classConstructorIndex;
    public final int internalDeprecatedSignatureIndex;
    public final int toStringIndex;
    public final int valueOfIndex;
    public final int appendIndex;

    public final int objectClassIndex;

    public final int objectClassNameIndex;
    public final int stringClassNameIndex;
    public final int stringBufferClassNameIndex;
    public final int stringBuilderClassNameIndex;

    public final int objectSignatureIndex;

    public final int thisLocalVariableNameIndex;

    public final int annotationDefaultAttributeNameIndex;
    public final int codeAttributeNameIndex;
    public final int constantValueAttributeNameIndex;
    public final int deprecatedAttributeNameIndex;
    public final int enclosingMethodAttributeNameIndex;
    public final int exceptionsAttributeNameIndex;
    public final int innerClassesAttributeNameIndex;
    public final int lineNumberTableAttributeNameIndex;
    public final int localVariableTableAttributeNameIndex;
    public final int localVariableTypeTableAttributeNameIndex;
    public final int runtimeInvisibleAnnotationsAttributeNameIndex;
    public final int runtimeVisibleAnnotationsAttributeNameIndex;
    public final int runtimeInvisibleParameterAnnotationsAttributeNameIndex;
    public final int runtimeVisibleParameterAnnotationsAttributeNameIndex;
    public final int signatureAttributeNameIndex;
    public final int sourceFileAttributeNameIndex;
    public final int syntheticAttributeNameIndex;

    public ConstantPool(Constant[] constants)
    {
        this.listOfConstants = new ArrayList<>();
        this.constantUtf8ToIndex = new StringToIndexMap();
        this.constantClassToIndex = new IndexToIndexMap();

        for (int i=0; i<constants.length; i++)
        {
            Constant constant = constants[i];

            int index = this.listOfConstants.size();
            this.listOfConstants.add(constant);

            if (constant instanceof ConstantUtf8)
            {
                this.constantUtf8ToIndex.put(
                        ((ConstantUtf8)constant).getValue(), index);
            }
            if (constant instanceof ConstantClass)
            {
                this.constantClassToIndex.put(
                        ((ConstantClass)constant).getNameIndex(), index);
            }
        }

        // Add instance constructor
        this.instanceConstructorIndex =
            addConstantUtf8(StringConstants.INSTANCE_CONSTRUCTOR);

        // Add class constructor
        this.classConstructorIndex =
            addConstantUtf8(StringConstants.CLASS_CONSTRUCTOR);

        // Add internal deprecated signature
        this.internalDeprecatedSignatureIndex =
            addConstantUtf8(StringConstants.INTERNAL_DEPRECATED_SIGNATURE);

        // -- Add method names --------------------------------------------- //
        // Add 'toString'
        this.toStringIndex = addConstantUtf8(StringConstants.TOSTRING_METHOD_NAME);

        // Add 'valueOf'
        this.valueOfIndex = addConstantUtf8(StringConstants.VALUEOF_METHOD_NAME);

        // Add 'append'
        this.appendIndex = addConstantUtf8(StringConstants.APPEND_METHOD_NAME);

        // -- Add class names ---------------------------------------------- //
        // Add 'Object'
        this.objectClassNameIndex =
            addConstantUtf8(StringConstants.JAVA_LANG_OBJECT);
        this.objectClassIndex =
            addConstantClass(this.objectClassNameIndex);
        this.objectSignatureIndex =
            addConstantUtf8(StringConstants.INTERNAL_OBJECT_SIGNATURE);

        // Add 'String'
        this.stringClassNameIndex =
            addConstantUtf8(StringConstants.JAVA_LANG_STRING);

        // Add 'StringBuffer'
        this.stringBufferClassNameIndex =
            addConstantUtf8(StringConstants.JAVA_LANG_STRING_BUFFER);

        // Add 'StringBuilder'
        this.stringBuilderClassNameIndex =
            addConstantUtf8(StringConstants.JAVA_LANG_STRING_BUILDER);

        // Add 'this'
        this.thisLocalVariableNameIndex =
            addConstantUtf8(StringConstants.THIS_LOCAL_VARIABLE_NAME);

        // -- Add attribute names ------------------------------------------ //
        this.annotationDefaultAttributeNameIndex =
            addConstantUtf8(StringConstants.ANNOTATIONDEFAULT_ATTRIBUTE_NAME);

        this.codeAttributeNameIndex =
            addConstantUtf8(StringConstants.CODE_ATTRIBUTE_NAME);

        this.constantValueAttributeNameIndex =
            addConstantUtf8(StringConstants.CONSTANTVALUE_ATTRIBUTE_NAME);

        this.deprecatedAttributeNameIndex =
            addConstantUtf8(StringConstants.DEPRECATED_ATTRIBUTE_NAME);

        this.enclosingMethodAttributeNameIndex =
            addConstantUtf8(StringConstants.ENCLOSINGMETHOD_ATTRIBUTE_NAME);

        this.exceptionsAttributeNameIndex =
            addConstantUtf8(StringConstants.EXCEPTIONS_ATTRIBUTE_NAME);

        this.innerClassesAttributeNameIndex =
            addConstantUtf8(StringConstants.INNERCLASSES_ATTRIBUTE_NAME);

        this.lineNumberTableAttributeNameIndex =
            addConstantUtf8(StringConstants.LINENUMBERTABLE_ATTRIBUTE_NAME);

        this.localVariableTableAttributeNameIndex =
            addConstantUtf8(StringConstants.LOCALVARIABLETABLE_ATTRIBUTE_NAME);

        this.localVariableTypeTableAttributeNameIndex =
            addConstantUtf8(StringConstants.LOCALVARIABLETYPETABLE_ATTRIBUTE_NAME);

        this.runtimeInvisibleAnnotationsAttributeNameIndex =
            addConstantUtf8(StringConstants.RUNTIMEINVISIBLEANNOTATIONS_ATTRIBUTE_NAME);

        this.runtimeVisibleAnnotationsAttributeNameIndex =
            addConstantUtf8(StringConstants.RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE_NAME);

        this.runtimeInvisibleParameterAnnotationsAttributeNameIndex =
            addConstantUtf8(StringConstants.RUNTIMEINVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE_NAME);

        this.runtimeVisibleParameterAnnotationsAttributeNameIndex =
            addConstantUtf8(StringConstants.RUNTIMEVISIBLEPARAMETERANNOTATIONS_ATTRIBUTE_NAME);

        this.signatureAttributeNameIndex =
            addConstantUtf8(StringConstants.SIGNATURE_ATTRIBUTE_NAME);

        this.sourceFileAttributeNameIndex =
            addConstantUtf8(StringConstants.SOURCEFILE_ATTRIBUTE_NAME);

        this.syntheticAttributeNameIndex =
            addConstantUtf8(StringConstants.SYNTHETIC_ATTRIBUTE_NAME);

    }

    public Constant get(int i)
    {
        return this.listOfConstants.get(i);
    }

    public int size()
    {
        return this.listOfConstants.size();
    }

    // -- Constants -------------------------------------------------------- //

    public int addConstantUtf8(String s)
    {
        if (s == null)
            throw new IllegalArgumentException("Constant string is null");

        if (s.startsWith("L[")) {
            throw new IllegalArgumentException("Constant string starts with L[");
        }

        int index = this.constantUtf8ToIndex.get(s);

        if (index == -1)
        {
            ConstantUtf8 cutf8 =
                new ConstantUtf8(s);
            index = this.listOfConstants.size();
            this.listOfConstants.add(cutf8);
            this.constantUtf8ToIndex.put(s, index);
        }

        return index;
    }

    public int addConstantClass(int nameIndex)
    {
        String internalName = getConstantUtf8(nameIndex);
        if ((internalName == null) ||
            (internalName.isEmpty()) ||
            (internalName.charAt(internalName.length()-1) == ';'))
            System.err.println("ConstantPool.addConstantClass: invalid name index");

        int index = this.constantClassToIndex.get(nameIndex);

        if (index == -1)
        {
            ConstantClass cc =
                new ConstantClass(nameIndex);
            index = this.listOfConstants.size();
            this.listOfConstants.add(cc);
            this.constantClassToIndex.put(nameIndex, index);
        }

        return index;
    }

    public int addConstantNameAndType(int nameIndex, int descriptorIndex)
    {
        int index = this.listOfConstants.size();

        while (--index > 0)
        {
            Constant constant = this.listOfConstants.get(index);

            if (!(constant instanceof ConstantNameAndType))
                continue;

            ConstantNameAndType cnat = (ConstantNameAndType)constant;

            if ((cnat.getNameIndex() == nameIndex) &&
                (cnat.getDescriptorIndex() == descriptorIndex))
                return index;
        }

        ConstantNameAndType cnat = new ConstantNameAndType(nameIndex, descriptorIndex);
        index = this.listOfConstants.size();
        this.listOfConstants.add(cnat);

        return index;
    }

    public int addConstantFieldref(int class_index, int name_and_type_index)
    {
        int index = this.listOfConstants.size();

        while (--index > 0)
        {
            Constant constant = this.listOfConstants.get(index);

            if (!(constant instanceof ConstantFieldref))
                continue;

            ConstantFieldref cfr = (ConstantFieldref)constant;

            if ((cfr.getClassIndex() == class_index) &&
                (cfr.getNameAndTypeIndex() == name_and_type_index))
                return index;
        }

        ConstantFieldref cfr = new ConstantFieldref(class_index, name_and_type_index);
        index = this.listOfConstants.size();
        this.listOfConstants.add(cfr);

        return index;
    }

    public int addConstantMethodref(int class_index, int name_and_type_index)
    {
        return addConstantMethodref(
            class_index, name_and_type_index, null, null);
    }

    public int addConstantMethodref(
        int class_index, int name_and_type_index,
        List<String> listOfParameterSignatures, String returnedSignature)
    {
        int index = this.listOfConstants.size();

        while (--index > 0)
        {
            Constant constant = this.listOfConstants.get(index);

            if (!(constant instanceof ConstantMethodref))
                continue;

            ConstantMethodref cmr = (ConstantMethodref)constant;

            if ((cmr.getClassIndex() == class_index) &&
                (cmr.getNameAndTypeIndex() == name_and_type_index))
                return index;
        }

        ConstantMethodref cfr = new ConstantMethodref(class_index, name_and_type_index,
            listOfParameterSignatures, returnedSignature);
        index = this.listOfConstants.size();
        this.listOfConstants.add(cfr);

        return index;
    }

    public String getConstantUtf8(int index)
    {
        ConstantUtf8 cutf8 = (ConstantUtf8)this.listOfConstants.get(index);
        return cutf8.getValue();
    }

    public String getConstantClassName(int index)
    {
        ConstantClass cc = (ConstantClass)this.listOfConstants.get(index);
        ConstantUtf8 cutf8 = (ConstantUtf8)this.listOfConstants.get(cc.getNameIndex());
        return cutf8.getValue();
    }

    public ConstantClass getConstantClass(int index)
    {
        return (ConstantClass)this.listOfConstants.get(index);
    }

    public ConstantFieldref getConstantFieldref(int index)
    {
        return (ConstantFieldref)this.listOfConstants.get(index);
    }

    public ConstantNameAndType getConstantNameAndType(int index)
    {
        return (ConstantNameAndType)this.listOfConstants.get(index);
    }

    public ConstantMethodref getConstantMethodref(int index)
    {
        return (ConstantMethodref)this.listOfConstants.get(index);
    }

    public ConstantInterfaceMethodref getConstantInterfaceMethodref(int index)
    {
        return (ConstantInterfaceMethodref)this.listOfConstants.get(index);
    }

    public Constant getConstantValue(int index)
    {
        return this.listOfConstants.get(index);
    }

    public ConstantInteger getConstantInteger(int index)
    {
        return (ConstantInteger)this.listOfConstants.get(index);
    }
}
