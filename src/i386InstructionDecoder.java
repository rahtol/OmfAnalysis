import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

public class i386InstructionDecoder
{
	Vector<Integer> instructionData;
	
	// instruction layout according to figure 17-1
	int instructionPrefixPresent;		// meaning: 0=no 1=yes  or the number of bytes covered by this instruction part
	int adressSizePrefixPresent;		// meaning: 0=no 1=yes
	int operandSizePrefixPresent;		// meaning: 0=no 1=yes
	int segmentOverridePrefixPresent;	// meaning: 0=no 1=yes
	int opcodeSize;
	int modrmPresent;
	int sibPresent; 
	int displacementSize;               // possible values: -1 (no displacement), 1 (8 bit), 2 (16 bit) or 4 (32 bit)
	int immediateSize;

	int adressSize;    // meaning: 0=16-bit 1=32-bit
	int operandSize;   // meaning: 0=16-bit 1=32-bit
	i386OpcodeMap opcodeMap;
	i386Instruction instruction;
	
	int modrm; // value
	int sib;
	int displacement;
	int immediate;
	
	static boolean isInstructionPrefix (int b)
	{
		return     (b == 0xf3)		// REP/REPE/REPZ
				|| (b == 0xf2)		// REPNE/REPNZ
				|| (b == 0xf0);		// LOCK
	}

	static boolean isAdressSizePrefix (int b)
	{
		return     (b == 0x67);
	}

	static boolean isOperandSizePrefix (int b)
	{
		return     (b == 0x66);
	}

	static boolean isSegmentOverridePrefix (int b)
	{
		return     (b == 0x2e)		// CS
				|| (b == 0x36)		// SS
				|| (b == 0x3e)		// DS
				|| (b == 0x26)		// ES
				|| (b == 0x64)		// FS
				|| (b == 0x65);		// GS
	}

	i386InstructionDecoder()
	{
		resetState();
	}
	
	void resetState()
	{
		instructionData = new Vector<Integer>();
		instructionPrefixPresent = 0;
		adressSizePrefixPresent = 0;
		operandSizePrefixPresent = 0;
		segmentOverridePrefixPresent = 0;
		adressSize = 1;  // defaulting to 32-bit
		operandSize = 1; // defaulting to 32-bit
		opcodeSize = 0;
		opcodeMap = null;
		modrmPresent = 0;
		modrm = 0;
		sibPresent = 0;
		sib = 0;
		displacementSize = -1;
		displacement = 0;
		immediateSize = -1;
		immediate = 0;
	}
	
	public int getUInt8(InputStream is) throws Exception
	{
		int b = is.read();
		check(b!=-1, "Incomplete i386Instruction. Unexpected end of stream detected.");
		instructionData.add(b);
		return b;
	}

	public int getUInt16(InputStream is) throws Exception
	{
		int b0 = is.read();
		int b1 = is.read();
		check(b1!=-1, "Incomplete i386Instruction. Unexpected end of stream detected.");
		instructionData.add(b0);
		instructionData.add(b1);
		return b0 + 256*b1;
	}

	public int getUInt32(InputStream is) throws Exception
	{
		int b0 = is.read();
		int b1 = is.read();
		int b2 = is.read();
		int b3 = is.read();
		check(b3!=-1, "Incomplete i386Instruction. Unexpected end of stream detected.");
		instructionData.add(b0);
		instructionData.add(b1);
		instructionData.add(b2);
		instructionData.add(b3);
		return b0 + 256*b1 + 65536*b2 + 256*65536*b3;
	}

	void check(boolean b, String errmsg) throws Exception
	{
		if (!b)
			throw new Exception(errmsg);
	}
	
	public int getReg() throws Exception
	{
		check(modrmPresent == 1,"Internal error in getReg().");
		return (modrm >> 3) & 7;
	}

	public int getMod() throws Exception
	{
		check(modrmPresent == 1,"Internal error in getReg().");
		return (modrm >> 6) & 3;
	}

	public int getRm() throws Exception
	{
		check(modrmPresent == 1,"Internal error in getReg().");
		return modrm & 7;
	}

	void decodeModRM (InputStream is) throws Exception
	{
		if (modrmPresent == 1)
			return;  // ModRm has already been decoded. 
		             // Multiple request are allowed, 
					 // i.e. from opcode and one operand or from two operands.
		
		modrmPresent = 1;
		modrm = getUInt8(is);
		int rm = getRm(); 
		int mod = getMod(); 
		
		if (operandSize==0)
		{
			// table 17-2: 16-Bit Addressing Forms with the the ModR/M byte applies
			switch (mod) {
				case 0: if (rm==6) decodeDisplacement(is, 2); break;
				case 1: decodeDisplacement(is, 1); break;
				case 2: decodeDisplacement(is, 2); break;
				case 3: /* no displacement */ break;
			}
		}
		else 
		{
			// table 17-3: 32-Bit Addressing Forms with the the ModR/M byte applies
			sibPresent = (mod != 3 && rm == 5 ? 1 : 0);
			if (sibPresent == 1)
				sib = getUInt8(is);

			switch (mod) {
				case 0: if (rm==6) decodeDisplacement(is, 4); break;
				case 1: decodeDisplacement(is, 1); break;
				case 2: decodeDisplacement(is, 4); break;
				case 3: /* no displacement */ break;
			}
		}
	}
	
	public void decodeDisplacement(InputStream is, int requiresDisplacement) throws Exception
	{
		check(this.displacementSize==-1, "Multiple use of DISPLACEMENT in instruction.");

		if (requiresDisplacement==-1)             // -1 = displacement present, operandSize decides between 16-bit and 32-bit
			displacementSize = (operandSize+1)*2; // 0->2, 1->4;
		else if (requiresDisplacement==-2)        // -2 = displacement present, adressSize decides between 16-bit and 32-bit
			displacementSize = (adressSize+1)*2;  // 0->2, 1->4;
		else if (requiresDisplacement==2)
			displacementSize = 2;
		else if (requiresDisplacement==4)
			displacementSize = 4;
		else
			this.check(false, "Illegal displacementSize.");
		
		if (displacementSize == 2)
			displacement = getUInt16(is);
		else
			displacement = getUInt32(is);
	}

	public void decodeImmediate(InputStream is, int requiresImmediate) throws Exception
	{
		// requiresImmediate: same semantics as above
		if (requiresImmediate==-1)
			immediateSize = (operandSize+1)*2;
		else if (requiresImmediate==-2)
			immediateSize = (adressSize+1)*2;
		else if (requiresImmediate==2)
			immediateSize = 2;
		else if (requiresImmediate==4)
			immediateSize = 4;
		else
			this.check(false, "Illegal immediateSize.");
		
		if (immediateSize == 2)
			immediate = getUInt16(is);
		else
			immediate = getUInt32(is);
	}

	void decode (InputStream is) throws Exception
	{
		resetState();
		int b = getUInt8(is);

		// prefix parsing according to figure 17-1
		// assuming: (1) order is fixed as given by 17-1
		// assuming: (2) each prefix occurs at least once due to "0 or 1" in 17-1 (REP and LOCK are candidates where this isn't obvious)
		if (isInstructionPrefix (b)) {
			instructionPrefixPresent = 1;
			b = getUInt8(is);
		}
		if (isAdressSizePrefix (b)) {
			adressSizePrefixPresent = 1;
			b = getUInt8(is);
			adressSize = (adressSize + 1) % 2;  // toggle between 32-bit and 16-bit
		}
		if (isOperandSizePrefix (b)) {
			operandSizePrefixPresent = 1;
			b = getUInt8(is);
			operandSize = (operandSize + 1) % 2;  // toggle between 32-bit and 16-bit
		}
		if (isSegmentOverridePrefix (b)) {
			segmentOverridePrefixPresent = 1;
			b = getUInt8(is);
		}
		
		opcodeSize = 1;
		opcodeMap = i386OpcodeMap.getOneByteInstructionMap();
		instruction = opcodeMap.get(b);
		instruction = instruction.opcode.decode(this, is, instruction);
		
		for (int i=0; i<instruction.operands.length; i++)
			instruction.operands[i].decode(this, is, instruction);

		instruction.opcode.check(this);
		for (int i=0; i<instruction.operands.length; i++)
			instruction.operands[i].check(this);
	}
	
	public final String regs8[] = {
			"al",
			"cl",
			"dl",
			"bl",
			"ah",
			"ch",
			"dh",
			"bh",
	};
	
	public final String regs16[] = {
			"ax",
			"cx",
			"dx",
			"bx",
			"sp",
			"bp",
			"si",
			"di",
	};
	
	public final String regs32[] = {
			"eax",
			"ecx",
			"edx",
			"ebx",
			"esp",
			"ebp",
			"esi",
			"edi",
	};
	
	public final String controlregister[] = {
			"cr0",
			"cr?1?",
			"cr2",
			"cr3",
			"cr?4?",
			"cr?5?",
			"cr?6?",
			"cr?7?",
	};
	
	public final String debugregister[] = {
			"dr0",
			"dr1",
			"dr2",
			"dr3",
			"dr?4?",
			"dr?5?",
			"dr6",
			"dr7",
	};

	public final String testregister[] = {
			"tr?0?",
			"tr?1?",
			"tr?2?",
			"tr?3?",
			"tr?4?",
			"tr?5?",
			"tr6",
			"tr7",
	};
	
	public String displStr(int displSize)
	{
		return String.format("%08x", displacement); // TODO
	}
	
	private String sibStr(int mod)
	{
		// precondition for call: sibPresent==1
		int ss = (sib >> 6) & 3;
		int index = (sib >> 3) & 7;
		int base = sib & 7;

		String baseStr = (base==5 && mod==0 ? displStr(4) : "[" + regs32[base] + "]");

		String fmt = (index==5 ? "": (ss==0 ? "[%s]" : "[%s*%d]"));
		String indexStr = String.format(fmt, regs32[index], ss*2);

		return baseStr + indexStr;
	}
	
	public String modRMtoEffectiveAddressString (int opsize)
	{
		String s = null;

		int rm = modrm & 7; 
		int mod = (modrm >> 6) & 3; 
		
		if (mod == 3)
		{
			// modrm specifies a register
			s = (opsize==1 ? regs8[rm] : (opsize==2 ? regs16[rm] : regs32 [rm]));
		}
		else if (this.adressSize==0)
		{
			// effective address according to table 17-2
			
			final String effregs[] = {
					"[bx+si]",
					"[bx+di]",
					"[bp+si]",
					"[bd+di]",
					"[si]",
					"[di]",
					"[bp]",
					"[bx]",
			};
			
			switch (mod) {
			case 0: 
				s = (rm==6 ? displStr(2) : effregs[rm]);
				break;
			case 1:
				s = String.format("%s[%s]", displStr(1), effregs[rm]); 
				break;
			case 2:
				s = String.format("%s[%s]", displStr(2), effregs[rm]); 
				break;
			case 3:
				// already handled above
				break;
			}
		}
		else
		{
			switch (mod) {
			case 0: 
				s = (rm==6 ? displStr(4) : "") + (rm==5 ? sibStr(mod) : (rm==6 ? "" : regs32[rm]));
				break;
			case 1:
				s = displStr(1) + (rm==5 ? sibStr(mod) : regs32[rm]); 
				break;
			case 2:
				s = displStr(4) + (rm==5 ? sibStr(mod) : regs32[rm]); 
				break;
			case 3:
				// already handled above
				break;
			}
		};
		
		return s;
	}

	public String toString()
	{
		return this.instruction.toString (this);
	}

}
