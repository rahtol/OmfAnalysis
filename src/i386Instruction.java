import java.io.IOException;
import java.io.InputStream;

public class i386Instruction
{
	int id;
	i386Opcode opcode;
	i386Operand operands [];
	
	i386Instruction(int id, i386Opcode opcode)
	{
		this.id = id;
		this.opcode = opcode;
		this.operands = new i386Operand [] {};
	}

	i386Instruction(int id, i386Opcode opcode, i386Operand operand1)
	{
		this.id = id;
		this.opcode = opcode;
		this.operands = new i386Operand [] {operand1};
	}

	i386Instruction(int id, i386Opcode opcode, i386Operand operand1, i386Operand operand2)
	{
		this.id = id;
		this.opcode = opcode;
		this.operands = new i386Operand [] {operand1, operand2};
	}

	i386Instruction(int id, i386Opcode opcode, i386Operand operand1, i386Operand operand2, i386Operand operand3)
	{
		this.id = id;
		this.opcode = opcode;
		this.operands = new i386Operand [] {operand1, operand2, operand3};
	}
	
	public i386Instruction(int id, i386Opcode opcode, i386Operand[] operands)
	{
		this.id = id;
		this.opcode = opcode;
		this.operands = operands;
	}

	public String toString(i386InstructionDecoder instructionDecoder) throws Exception
	{
		String s = this.opcode.toString(instructionDecoder);
		String sep = " ";
		for (int i=0; i<this.operands.length;i++)
		{
			s = s + sep + this.operands[i].toString(instructionDecoder);
			sep = ", ";
		}
		return s;
	}

}
