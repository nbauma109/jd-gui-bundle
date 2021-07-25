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
package jd.core.model.classfile.attribute;

public class ElementValueEnumConstValue extends ElementValue
{
    private final int typeNameIndex;
    private final int constNameIndex;

    public ElementValueEnumConstValue(byte tag,
                                      int typeNameIndex,
                                      int constNameIndex)
    {
        super(tag);
        this.typeNameIndex = typeNameIndex;
        this.constNameIndex = constNameIndex;
    }

	public int getTypeNameIndex() {
		return typeNameIndex;
	}

	public int getConstNameIndex() {
		return constNameIndex;
	}
}
