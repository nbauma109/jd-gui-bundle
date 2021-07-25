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

public class ElementValuePrimitiveType extends ElementValue
{
    /*
     * type = {'B', 'D', 'F', 'I', 'J', 'S', 'Z', 'C', 's'}
     */
    private final byte type;
    private final int constValueIndex;

    public ElementValuePrimitiveType(byte tag, byte type, int constValueIndex)
    {
        super(tag);
        this.type = type;
        this.constValueIndex = constValueIndex;
    }

	public byte getType() {
		return type;
	}

	public int getConstValueIndex() {
		return constValueIndex;
	}
}
