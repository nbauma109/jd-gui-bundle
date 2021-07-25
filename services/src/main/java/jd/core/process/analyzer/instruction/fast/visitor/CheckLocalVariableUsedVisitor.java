/**
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
 */
package jd.core.process.analyzer.instruction.fast.visitor;

import org.apache.bcel.Const;

import java.util.List;

import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.*;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.model.instruction.fast.instruction.*;
import jd.core.model.instruction.fast.instruction.FastTry.FastCatch;

public class CheckLocalVariableUsedVisitor
{
    private CheckLocalVariableUsedVisitor() {
    }
        public static boolean visit(
        LocalVariables localVariables, int maxOffset, Instruction instruction)
    {
        switch (instruction.getOpcode())
        {
        case Const.ARRAYLENGTH:
            return visit(
                localVariables, maxOffset, ((ArrayLength)instruction).getArrayref());
        case Const.AASTORE:
        case ByteCodeConstants.ARRAYSTORE:
            {
                ArrayStoreInstruction asi = (ArrayStoreInstruction)instruction;
                return visit(localVariables, maxOffset, asi.getIndexref()) || visit(localVariables, maxOffset, asi.getValueref());
            }
        case Const.ATHROW:
            return visit(localVariables, maxOffset, ((AThrow)instruction).getValue());
        case ByteCodeConstants.UNARYOP:
            return visit(
                localVariables, maxOffset,
                ((UnaryOperatorInstruction)instruction).getValue());
        case ByteCodeConstants.BINARYOP:
            {
                BinaryOperatorInstruction boi =
                    (BinaryOperatorInstruction)instruction;
                return visit(localVariables, maxOffset, boi.getValue1()) || visit(localVariables, maxOffset, boi.getValue2());
            }
        case Const.CHECKCAST:
            return visit(
                localVariables, maxOffset, ((CheckCast)instruction).getObjectref());
        case ByteCodeConstants.LOAD:
        case Const.ALOAD:
        case Const.ILOAD:
            {
                LoadInstruction li = (LoadInstruction)instruction;
                LocalVariable lv =
                    localVariables.getLocalVariableWithIndexAndOffset(
                        li.getIndex(), li.getOffset());
                return lv != null && maxOffset <= lv.getStartPc();
            }
        case ByteCodeConstants.STORE:
        case Const.ASTORE:
        case Const.ISTORE:
            {
                StoreInstruction si = (StoreInstruction)instruction;
                LocalVariable lv =
                    localVariables.getLocalVariableWithIndexAndOffset(
                        si.getIndex(), si.getOffset());
                return (lv != null && maxOffset <= lv.getStartPc()) || visit(localVariables, maxOffset, si.getValueref());
            }
        case ByteCodeConstants.DUPSTORE:
            return visit(
                localVariables, maxOffset, ((DupStore)instruction).getObjectref());
        case ByteCodeConstants.CONVERT:
        case ByteCodeConstants.IMPLICITCONVERT:
            return visit(
                localVariables, maxOffset,
                ((ConvertInstruction)instruction).getValue());
        case ByteCodeConstants.IFCMP:
            {
                IfCmp ifCmp = (IfCmp)instruction;
                return visit(localVariables, maxOffset, ifCmp.getValue1()) || visit(localVariables, maxOffset, ifCmp.getValue2());
            }
        case ByteCodeConstants.IF:
        case ByteCodeConstants.IFXNULL:
            return visit(
                localVariables, maxOffset, ((IfInstruction)instruction).getValue());
        case ByteCodeConstants.COMPLEXIF:
            {
                List<Instruction> branchList =
                    ((ComplexConditionalBranchInstruction)instruction).getInstructions();
                for (int i=branchList.size()-1; i>=0; --i)
                {
                    if (visit(localVariables, maxOffset, branchList.get(i))) {
                        return true;
                    }
                }
                return false;
            }
        case Const.INSTANCEOF:
            return visit(
                localVariables, maxOffset, ((InstanceOf)instruction).getObjectref());
        case Const.INVOKEINTERFACE:
        case Const.INVOKESPECIAL:
        case Const.INVOKEVIRTUAL:
            if (visit(
                    localVariables, maxOffset,
                    ((InvokeNoStaticInstruction)instruction).getObjectref())) {
                return true;
            }
            // intended fall through
        case Const.INVOKESTATIC:
            {
                List<Instruction> list = ((InvokeInstruction)instruction).getArgs();
                for (int i=list.size()-1; i>=0; --i)
                {
                    if (visit(localVariables, maxOffset, list.get(i))) {
                        return true;
                    }
                }
                return false;
            }
        case ByteCodeConstants.INVOKENEW:
            {
                List<Instruction> list = ((InvokeNew)instruction).getArgs();
                for (int i=list.size()-1; i>=0; --i)
                {
                    if (visit(localVariables, maxOffset, list.get(i))) {
                        return true;
                    }
                }
                return false;
            }
        case Const.LOOKUPSWITCH:
            return visit(
                localVariables, maxOffset, ((LookupSwitch)instruction).getKey());
        case Const.MONITORENTER:
            return visit(
                localVariables, maxOffset,
                ((MonitorEnter)instruction).getObjectref());
        case Const.MONITOREXIT:
            return visit(
                localVariables, maxOffset,
                ((MonitorExit)instruction).getObjectref());
        case Const.MULTIANEWARRAY:
            {
                Instruction[] dimensions = ((MultiANewArray)instruction).getDimensions();
                for (int i=dimensions.length-1; i>=0; --i)
                {
                    if (visit(localVariables, maxOffset, dimensions[i])) {
                        return true;
                    }
                }
                return false;
            }
        case Const.NEWARRAY:
            return visit(
                localVariables, maxOffset,
                ((NewArray)instruction).getDimension());
        case Const.ANEWARRAY:
            return visit(
                localVariables, maxOffset,
                ((ANewArray)instruction).getDimension());
        case Const.POP:
            return visit(
                localVariables, maxOffset,
                ((Pop)instruction).getObjectref());
        case Const.PUTFIELD:
            {
                PutField putField = (PutField)instruction;
                return visit(localVariables, maxOffset, putField.getObjectref()) || visit(localVariables, maxOffset, putField.getValueref());
            }
        case Const.PUTSTATIC:
            return visit(
                localVariables, maxOffset,
                ((PutStatic)instruction).getValueref());
        case ByteCodeConstants.XRETURN:
            return visit(
                localVariables, maxOffset,
                ((ReturnInstruction)instruction).getValueref());
        case Const.TABLESWITCH:
            return visit(
                localVariables, maxOffset,
                ((TableSwitch)instruction).getKey());
        case ByteCodeConstants.TERNARYOPSTORE:
            return visit(
                localVariables, maxOffset,
                ((TernaryOpStore)instruction).getObjectref());
        case ByteCodeConstants.TERNARYOP:
            {
                TernaryOperator to = (TernaryOperator)instruction;
                return visit(localVariables, maxOffset, to.getValue1()) || visit(localVariables, maxOffset, to.getValue2());
            }
        case ByteCodeConstants.ASSIGNMENT:
            {
                AssignmentInstruction ai = (AssignmentInstruction)instruction;
                return visit(localVariables, maxOffset, ai.getValue1()) || visit(localVariables, maxOffset, ai.getValue2());
            }
        case ByteCodeConstants.ARRAYLOAD:
            {
                ArrayLoadInstruction ali = (ArrayLoadInstruction)instruction;
                return visit(localVariables, maxOffset, ali.getArrayref()) || visit(localVariables, maxOffset, ali.getIndexref());
            }
        case ByteCodeConstants.PREINC:
        case ByteCodeConstants.POSTINC:
            return visit(
                localVariables, maxOffset,
                ((IncInstruction)instruction).getValue());
        case Const.GETFIELD:
            return visit(
                localVariables, maxOffset,
                ((GetField)instruction).getObjectref());
        case ByteCodeConstants.INITARRAY:
        case ByteCodeConstants.NEWANDINITARRAY:
            {
                InitArrayInstruction iai = (InitArrayInstruction)instruction;
                return visit(localVariables, maxOffset, iai.getNewArray()) || (iai.getValues() != null && visit(localVariables, maxOffset, iai.getValues()));
            }
        case FastConstants.FOR:
            {
                FastFor ff = (FastFor)instruction;
                return (ff.getInit() != null && visit(localVariables, maxOffset, ff.getInit())) || (ff.getInc() != null && visit(localVariables, maxOffset, ff.getInc()));
            }
        case FastConstants.WHILE:
        case FastConstants.DO_WHILE:
        case FastConstants.IF_SIMPLE:
            {
                Instruction test = ((FastTestList)instruction).getTest();
                return test != null && visit(localVariables, maxOffset, test);
            }
        case FastConstants.INFINITE_LOOP:
            {
                List<Instruction> instructions =
                        ((FastList)instruction).getInstructions();
                return instructions != null && visit(localVariables, maxOffset, instructions);
            }
        case FastConstants.FOREACH:
            {
                FastForEach ffe = (FastForEach)instruction;
                return visit(localVariables, maxOffset, ffe.getVariable()) || visit(localVariables, maxOffset, ffe.getValues()) || visit(localVariables, maxOffset, ffe.getInstructions());
            }
        case FastConstants.IF_ELSE:
            {
                FastTest2Lists ft2l = (FastTest2Lists)instruction;
                return visit(localVariables, maxOffset, ft2l.getTest()) || visit(localVariables, maxOffset, ft2l.getInstructions()) || visit(localVariables, maxOffset, ft2l.getInstructions2());
            }
        case FastConstants.IF_CONTINUE:
        case FastConstants.IF_BREAK:
        case FastConstants.IF_LABELED_BREAK:
        case FastConstants.GOTO_CONTINUE:
        case FastConstants.GOTO_BREAK:
        case FastConstants.GOTO_LABELED_BREAK:
            {
                FastInstruction fi = (FastInstruction)instruction;
                return fi.getInstruction() != null && visit(localVariables, maxOffset, fi.getInstruction());
            }
        case FastConstants.SWITCH:
        case FastConstants.SWITCH_ENUM:
        case FastConstants.SWITCH_STRING:
            {
                FastSwitch fs = (FastSwitch)instruction;
                if (visit(localVariables, maxOffset, fs.getTest())) {
                    return true;
                }
                FastSwitch.Pair[] pairs = fs.getPairs();
                List<Instruction> instructions;
                for (int i=pairs.length-1; i>=0; --i)
                {
                    instructions = pairs[i].getInstructions();
                    if (instructions != null && visit(localVariables, maxOffset, instructions)) {
                        return true;
                    }
                }
                return false;
            }
        case FastConstants.TRY:
            {
                FastTry ft = (FastTry)instruction;
                if (visit(localVariables, maxOffset, ft.getInstructions()) || (ft.getFinallyInstructions() != null && visit(localVariables, maxOffset, ft.getFinallyInstructions()))) {
                    return true;
                }
                List<FastCatch> catchs = ft.getCatches();
                for (int i=catchs.size()-1; i>=0; --i) {
                    if (visit(localVariables, maxOffset, catchs.get(i).getInstructions())) {
                        return true;
                    }
                }
                return false;
            }
        case FastConstants.SYNCHRONIZED:
            {
                FastSynchronized fsd = (FastSynchronized)instruction;
                return visit(localVariables, maxOffset, fsd.getMonitor()) || visit(localVariables, maxOffset, fsd.getInstructions());
            }
        case FastConstants.LABEL:
            {
                FastLabel fl = (FastLabel)instruction;
                return fl.getInstruction() != null && visit(localVariables, maxOffset, fl.getInstruction());
            }
        case FastConstants.DECLARE:
            {
                FastDeclaration fd = (FastDeclaration)instruction;
                return fd.getInstruction() != null && visit(localVariables, maxOffset, fd.getInstruction());
            }
        case Const.GETSTATIC:
        case ByteCodeConstants.OUTERTHIS:
        case Const.ACONST_NULL:
        case Const.BIPUSH:
        case ByteCodeConstants.ICONST:
        case ByteCodeConstants.LCONST:
        case ByteCodeConstants.FCONST:
        case ByteCodeConstants.DCONST:
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
        case ByteCodeConstants.DUPLOAD:
            return false;
        default:
            System.err.println(
                    "Can not find local variable used in " +
                    instruction.getClass().getName() +
                    ", opcode=" + instruction.getOpcode());
            return false;
        }
    }

    private static boolean visit(
        LocalVariables localVariables, int maxOffset,
        List<Instruction> instructions)
    {
        for (int i=instructions.size()-1; i>=0; --i) {
            if (visit(localVariables, maxOffset, instructions.get(i))) {
                return true;
            }
        }
        return false;
    }
}
