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
package jd.core.process.analyzer.classfile.visitor;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.jd.core.v1.model.classfile.constant.ConstantMethodref;

import java.util.List;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.*;

/*
 * Search :
        public int indexOf(int ch)
        public int indexOf(int ch, int fromIndex)
        public int lastIndexOf(int ch)
        public int lastIndexOf(int ch, int fromIndex)
 */
public class SetConstantTypeInStringIndexOfMethodsVisitor
{
    protected ConstantPool constants;

    public SetConstantTypeInStringIndexOfMethodsVisitor(ConstantPool constants)
    {
        this.constants = constants;
    }

    public void visit(Instruction instruction)
    {
        switch (instruction.getOpcode())
        {
        case Const.ARRAYLENGTH:
            visit(((ArrayLength)instruction).getArrayref());
            break;
        case Const.AASTORE:
        case ByteCodeConstants.ARRAYSTORE:
            visit(((ArrayStoreInstruction)instruction).getArrayref());
            break;
        case ByteCodeConstants.ASSERT:
            {
                AssertInstruction ai = (AssertInstruction)instruction;
                visit(ai.getTest());
                if (ai.getMsg() != null)
                    visit(ai.getMsg());
            }
            break;
        case Const.ATHROW:
            visit(((AThrow)instruction).getValue());
            break;
        case ByteCodeConstants.UNARYOP:
            visit(((UnaryOperatorInstruction)instruction).getValue());
            break;
        case ByteCodeConstants.BINARYOP:
        case ByteCodeConstants.ASSIGNMENT:
            {
                BinaryOperatorInstruction boi =
                    (BinaryOperatorInstruction)instruction;
                visit(boi.getValue1());
                visit(boi.getValue2());
            }
            break;
        case Const.CHECKCAST:
            visit(((CheckCast)instruction).getObjectref());
            break;
        case ByteCodeConstants.STORE:
        case Const.ASTORE:
        case Const.ISTORE:
            visit(((StoreInstruction)instruction).getValueref());
            break;
        case ByteCodeConstants.DUPSTORE:
            visit(((DupStore)instruction).getObjectref());
            break;
        case ByteCodeConstants.CONVERT:
        case ByteCodeConstants.IMPLICITCONVERT:
            visit(((ConvertInstruction)instruction).getValue());
            break;
        case ByteCodeConstants.IFCMP:
            {
                IfCmp ifCmp = (IfCmp)instruction;
                visit(ifCmp.getValue1());
                visit(ifCmp.getValue2());
            }
            break;
        case ByteCodeConstants.IF:
        case ByteCodeConstants.IFXNULL:
            visit(((IfInstruction)instruction).getValue());
            break;
        case ByteCodeConstants.COMPLEXIF:
            {
                List<Instruction> branchList =
                    ((ComplexConditionalBranchInstruction)instruction).getInstructions();
                for (int i=branchList.size()-1; i>=0; --i)
                    visit(branchList.get(i));
            }
            break;
        case Const.INSTANCEOF:
            visit(((InstanceOf)instruction).getObjectref());
            break;
        case Const.INVOKEVIRTUAL:
            {
                Invokevirtual iv = (Invokevirtual)instruction;
                ConstantMethodref cmr =
                    this.constants.getConstantMethodref(iv.getIndex());
                ConstantClass cc = this.constants.getConstantClass(cmr.getClassIndex());

                if (cc.getNameIndex() == this.constants.getStringClassNameIndex())
                {
                    int nbrOfParameters = iv.getArgs().size();

                    if ((1 <= nbrOfParameters) && (nbrOfParameters <= 2))
                    {
                        int opcode = iv.getArgs().get(0).getOpcode();

                        if (((opcode==Const.BIPUSH) ||
                             (opcode==Const.SIPUSH)) &&
                             cmr.getReturnedSignature().equals("I") &&
                             cmr.getListOfParameterSignatures().get(0).equals("I"))
                        {
                            ConstantNameAndType cnat =
                                this.constants.getConstantNameAndType(
                                    cmr.getNameAndTypeIndex());
                            String name =
                                this.constants.getConstantUtf8(cnat.getNameIndex());

                            if ("indexOf".equals(name) ||
                                "lastIndexOf".equals(name))
                            {
                                // Change constant type
                                IConst ic = (IConst)iv.getArgs().get(0);
                                ic.setReturnedSignature("C");
                                break;
                            }
                        }
                    }
                }
            }
            // intended fall through
        case Const.INVOKEINTERFACE:
        case Const.INVOKESPECIAL:
            visit(((InvokeNoStaticInstruction)instruction).getObjectref());
            // intended fall through
        case Const.INVOKESTATIC:
        case ByteCodeConstants.INVOKENEW:
            {
                List<Instruction> list = ((InvokeInstruction)instruction).getArgs();
                for (int i=list.size()-1; i>=0; --i)
                    visit(list.get(i));
            }
            break;
        case Const.LOOKUPSWITCH:
            visit(((LookupSwitch)instruction).getKey());
            break;
        case Const.MONITORENTER:
            visit(((MonitorEnter)instruction).getObjectref());
            break;
        case Const.MONITOREXIT:
            visit(((MonitorExit)instruction).getObjectref());
            break;
        case Const.MULTIANEWARRAY:
            {
                Instruction[] dimensions = ((MultiANewArray)instruction).getDimensions();
                for (int i=dimensions.length-1; i>=0; --i)
                    visit(dimensions[i]);
            }
            break;
        case Const.NEWARRAY:
            visit(((NewArray)instruction).getDimension());
            break;
        case Const.ANEWARRAY:
            visit(((ANewArray)instruction).getDimension());
            break;
        case Const.POP:
            visit(((Pop)instruction).getObjectref());
            break;
        case Const.PUTFIELD:
            {
                PutField putField = (PutField)instruction;
                visit(putField.getObjectref());
                visit(putField.getValueref());
            }
            break;
        case Const.PUTSTATIC:
            visit(((PutStatic)instruction).getValueref());
            break;
        case ByteCodeConstants.XRETURN:
            visit(((ReturnInstruction)instruction).getValueref());
            break;
        case Const.TABLESWITCH:
            visit(((TableSwitch)instruction).getKey());
            break;
        case ByteCodeConstants.TERNARYOPSTORE:
            visit(((TernaryOpStore)instruction).getObjectref());
            break;
        case ByteCodeConstants.PREINC:
        case ByteCodeConstants.POSTINC:
            visit(((IncInstruction)instruction).getValue());
            break;
        case Const.GETFIELD:
            visit(((GetField)instruction).getObjectref());
            break;
        case ByteCodeConstants.INITARRAY:
        case ByteCodeConstants.NEWANDINITARRAY:
            {
                InitArrayInstruction iai = (InitArrayInstruction)instruction;
                visit(iai.getNewArray());
                if (iai.getValues() != null)
                    visit(iai.getValues());
            }
            break;
        case Const.ACONST_NULL:
        case ByteCodeConstants.ARRAYLOAD:
        case ByteCodeConstants.LOAD:
        case Const.ALOAD:
        case Const.ILOAD:
        case Const.BIPUSH:
        case ByteCodeConstants.ICONST:
        case ByteCodeConstants.LCONST:
        case ByteCodeConstants.FCONST:
        case ByteCodeConstants.DCONST:
        case ByteCodeConstants.DUPLOAD:
        case Const.GETSTATIC:
        case ByteCodeConstants.OUTERTHIS:
        case Const.GOTO:
        case Const.IINC:
        case Const.JSR:
        case Const.LDC:
        case Const.LDC2_W:
        case Const.NEW:
        case Const.NOP:
        case Const.SIPUSH:
        case Const.RET:
        case Const.RETURN:
        case ByteCodeConstants.EXCEPTIONLOAD:
        case ByteCodeConstants.RETURNADDRESSLOAD:
            break;
        default:
            System.err.println(
                    "Can not search String.indexOf in " +
                    instruction.getClass().getName() +
                    ", opcode=" + instruction.getOpcode());
        }
    }

    public void visit(List<Instruction> instructions)
    {
        for (int i=instructions.size()-1; i>=0; --i)
            visit(instructions.get(i));
    }
}