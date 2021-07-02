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

public class LocalVariable
    implements Comparable<LocalVariable>
{
	public int start_pc;
    public int length;
    public int nameIndex;
    public int signature_index;
    public final int index;
    public boolean exceptionOrReturnAddress;
    // Champ de bits utilisé pour determiner le type de la variable (byte, char,
    // short, int).
    public int typesBitField;
    // Champs utilisé lors de la génération des déclarations de variables
    // locales (FastDeclarationAnalyzer.Analyze).
    public boolean declarationFlag = false;

    public boolean finalFlag = false;

    private boolean toBeRemoved;

    public LocalVariable(
            int start_pc, int length, int nameIndex, int signature_index,
            int index)
    {
        this(start_pc, length, nameIndex, signature_index, index, false, 0);
    }

    public LocalVariable(
            int start_pc, int length, int nameIndex, int signature_index,
            int index, int typesBitSet)
    {
        this(start_pc, length, nameIndex, signature_index, index, false,
             typesBitSet);
    }

    public LocalVariable(
            int start_pc, int length, int nameIndex, int signature_index,
            int index, boolean exception)
    {
        this(start_pc, length, nameIndex, signature_index, index, exception, 0);
    }

    protected LocalVariable(
        int start_pc, int length, int nameIndex, int signature_index,
        int index, boolean exceptionOrReturnAddress, int typesBitField)
    {
        this.start_pc = start_pc;
        this.length = length;
        this.nameIndex = nameIndex;
        this.signature_index = signature_index;
        this.index = index;
        this.exceptionOrReturnAddress = exceptionOrReturnAddress;
        this.declarationFlag = exceptionOrReturnAddress;
        this.typesBitField = typesBitField;
    }

    public void updateRange(int offset)
    {
        if (offset < this.start_pc)
        {
            this.length += (this.start_pc - offset);
            this.start_pc = offset;
        }

        if (offset >= this.start_pc+this.length)
        {
            this.length = offset - this.start_pc + 1;
        }
    }

    public void updateSignatureIndex(int signatureIndex)
    {
        this.signature_index = signatureIndex;
    }

    @Override
    public String toString()
    {
        return
            "LocalVariable{start_pc=" + start_pc +
            ", length=" + length +
            ", nameIndex=" + nameIndex +
            ", signature_index=" + signature_index +
            ", index=" + index +
            "}";
    }

    @Override
    public int compareTo(LocalVariable other)
    {
        if (other == null)
            return -1;

        if (this.nameIndex != other.nameIndex)
            return this.nameIndex - other.nameIndex;

        if (this.length != other.length)
            return this.length - other.length;

        if (this.start_pc != other.start_pc)
            return this.start_pc - other.start_pc;

        return this.index - other.index;
    }

	public boolean isToBeRemoved() {
		return toBeRemoved;
	}

	public void setToBeRemoved(boolean toBeRemoved) {
		this.toBeRemoved = toBeRemoved;
	}
}
