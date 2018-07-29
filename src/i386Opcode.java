import java.io.InputStream;

public enum i386Opcode
{
	_ADD,
	_ADC,
	_PUSH,
	_PUSHA,
	_PUSHF,
	_POP,
	_POPA,
	_POPF,
	_INC,
	_DEC,
	_MOV,
	_MOVZX,
	_MOVSX,
	_MOVSB,
	_MOVSWD,
	_CMP,
	_CMPSB,
	_CMPSWD,
	_SUB,
	_SBB,
	_AND,
	_OR,
	_XOR,
	_RET,
	_RETF,
	_INT,
	_IRET,
	_INTO,
	_MUL,
	_IMUL,
	_DIV,
	_IDIV,
	_TEST,
	_XCHG,
	_NOP,
	_NOT,
	_NEG,
	_LES,
	_LDS,
	_LFS,
	_LGS,
	_LSS,
	_AAM,
	_AAD,
	_DAA,
	_AAA,
	_DAS,
	_AAS,
	_LEA,
	_CBW
	{
		public String toString(i386InstructionDecoder decoder)
		{
			return (decoder.operandSize==0 ? "cbw" : "cwde");
		}
	},
	_CWD
	{
		public String toString(i386InstructionDecoder decoder)
		{
			return (decoder.operandSize==0 ? "cwd" : "cdq");
		}
	},
	_CALL,
	_WAIT,
	_SAHF,
	_LAHF,
	_IN,
	_INSB,
	_INSWD,
	_OUT,
	_OUTSB,
	_OUTSWD,
	_STOSB,
	_STOSWD,
	_LODSB,
	_LODSWD,
	_SCASB,
	_SCASWD,
	_LOOPNE,
	_LOOPE,
	_LOOP,
	_JCXZ,
	_HLT,
	_CMC,
	_JO,
	_JNO,
	_JB,
	_JNB,
	_JZ,
	_JNZ,
	_JBE,
	_JNBE,
	_JS,
	_JNS,
	_JP,
	_JNP,
	_JL,
	_JNL,
	_JLE,
	_JNLE,
	_JMP,
	_CLC,
	_STC,
	_CLI,
	_STI,
	_CLD,
	_STD,
	_BOUND,
	_ARPL,
	_ENTER,
	_LEAVE,
	_XLAT,
	_ROL,
	_ROR,
	_RCL,
	_RCR,
	_SHL,
	_SHR,
	_SAL,
	_SAR,
	_LAR,
	_LSL,
	_CLTS,
	_SETO,
	_SETNO,
	_SETB,
	_SETNB,
	_SETZ,
	_SETNZ,
	_SETBE,
	_SETNBE,
	_SETS,
	_SETNS,
	_SETP,
	_SETNP,
	_SETL,
	_SETNL,
	_SETLE,
	_SETNLE,
	_BT,
	_BTR,
	_BTS,
	_BTC,
	_BSF,
	_BSR,
	_SHLD,
	_SHRD,
	_SLDT,
	_SGDT,
	_SIDT,
	_LLDT,
	_LGDT,
	_LIDT,
	_STR,
	_LTR,
	_VERR,
	_VERW,
	_SMSW,
	_LMSW,
	__grp1
	{
		public i386Instruction decode(i386InstructionDecoder instructionDecoder, InputStream is, i386Instruction instruction) throws Exception
		{
			instructionDecoder.decodeModRM(is);
			int reg = instructionDecoder.getReg();
			return new i386Instruction((instruction.id<<8)+reg, i386OpcodeMap.__grp1_Opcodes[reg], instruction.operands);
		}
	},
	__grp2
	{
		public i386Instruction decode(i386InstructionDecoder instructionDecoder, InputStream is, i386Instruction instruction) throws Exception
		{
			instructionDecoder.decodeModRM(is);
			int reg = instructionDecoder.getReg();
			return new i386Instruction((instruction.id<<8)+reg, i386OpcodeMap.__grp2_Opcodes[reg], instruction.operands);
		}
	},
	__grp3
	{
		public i386Instruction decode(i386InstructionDecoder instructionDecoder, InputStream is, i386Instruction instruction) throws Exception
		{
			instructionDecoder.decodeModRM(is);
			int reg = instructionDecoder.getReg();
			i386Instruction instruction2 = null;
			switch (instruction.operands[0]) {
			case _Eb:
				instruction2 = i386OpcodeMap.__grp3_v_Instructions[reg];
				break;
			case _Ev:
				instruction2 = i386OpcodeMap.__grp3_v_Instructions[reg];
				break;
			default:
				instructionDecoder.check(false, "Interal ERROR __grp3");
				break;
			}
			return instruction2;
		}
	},
	__grp4
	{
		public i386Instruction decode(i386InstructionDecoder instructionDecoder, InputStream is, i386Instruction instruction) throws Exception
		{
			instructionDecoder.decodeModRM(is);
			int reg = instructionDecoder.getReg();
			return new i386Instruction((instruction.id<<8)+reg, i386OpcodeMap.__grp4_Opcodes[reg], instruction.operands);
		}
	},
	__grp5
	{
		public i386Instruction decode(i386InstructionDecoder instructionDecoder, InputStream is, i386Instruction instruction) throws Exception
		{
			instructionDecoder.decodeModRM(is);
			int reg = instructionDecoder.getReg();
			return i386OpcodeMap.__grp5_Instructions[reg];
		}
	},
	__grp6
	{
		public i386Instruction decode(i386InstructionDecoder instructionDecoder, InputStream is, i386Instruction instruction) throws Exception
		{
			instructionDecoder.decodeModRM(is);
			int reg = instructionDecoder.getReg();
			return new i386Instruction((instruction.id<<8)+reg, i386OpcodeMap.__grp6_Opcodes[reg], instruction.operands);
		}
	},
	__grp7
	{
		public i386Instruction decode(i386InstructionDecoder instructionDecoder, InputStream is, i386Instruction instruction) throws Exception
		{
			instructionDecoder.decodeModRM(is);
			int reg = instructionDecoder.getReg();
			return i386OpcodeMap.__grp7_Instructions[reg];
		}
	},
	__grp8
	{
		public i386Instruction decode(i386InstructionDecoder instructionDecoder, InputStream is, i386Instruction instruction) throws Exception
		{
			instructionDecoder.decodeModRM(is);
			int reg = instructionDecoder.getReg();
			return new i386Instruction((instruction.id<<8)+reg, i386OpcodeMap.__grp8_Opcodes[reg], instruction.operands);
		}
	},
	__twoByteEscape
	{
		public i386Instruction decode(i386InstructionDecoder instructionDecoder, InputStream is, i386Instruction instruction) throws Exception
		{
			instructionDecoder.opcodeSize = 2;
			instructionDecoder.opcodeMap = i386OpcodeMap.getTwoByteInstructionMap();
			int b = instructionDecoder.getUInt8(is);
			i386Instruction instruction2 = instructionDecoder.opcodeMap.get(b);
			return instruction2.opcode.decode(instructionDecoder, is, instruction2);
		}
	},
	__invalidOpcode
	{
		public i386Instruction decode(i386InstructionDecoder i386InstructionDecoder, InputStream is, i386Instruction instruction) throws Exception
		{
			i386InstructionDecoder.check(false, "Invalid Opcode.");
			return instruction; // we'll never get here, just satisfy the compiler by returning something 
		}
	};
	
	i386Opcode()
	{
	}
	
	public i386Instruction decode(i386InstructionDecoder decoder, InputStream is, i386Instruction instruction) throws Exception
	{
		// the default implementation just returns the original instruction
		return instruction;
	}
	
	public String toString(i386InstructionDecoder decoder)
	{
		String s = name();
		
		// special handling for STOSWD, LODSWD, SCASWD, INSWD, OUSTSW, MOVSWD, CMPSWD
		if(s.endsWith("SWD"))
		{
			s = s.substring(0,s.length()-2);  // remove "WD"
			s = s + (decoder.operandSize==0 ? "W" : "D");
		}
		
		return s.substring(1).toLowerCase(); // remove leading "_" and convert to lower case
	}

	public void check(i386InstructionDecoder decoder) throws Exception
	{
		// nothing to check by default
	}
}
