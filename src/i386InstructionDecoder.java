import java.io.InputStream;
import java.util.Vector;

public class i386InstructionDecoder
{
	final long virtualaddress; // context information: virtual address where the instruction is located 
	final int operandSizeDefault;  // context information: operandSize from segment selector
	final int adressSizeDefault;  // context information: addressSize from segment selector
	
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
	long displacement;
	long immediate;
	int segmentOverridePrefix;
	
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
	
	public final String segmentregister[] = {
			"es",
			"cs",
			"ss",
			"ds",
			"fs",
			"gs",
			"?6?",
			"?7?",
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

	i386InstructionDecoder(long virtualaddress, int operandSizeDefault, int adressSizeDefault)
	{
		this.virtualaddress = virtualaddress;
		this.operandSizeDefault = operandSizeDefault;
		this.adressSizeDefault = adressSizeDefault;
		
		resetState();
	}
	
	void resetState()
	{
		instructionData = new Vector<Integer>();
		instructionPrefixPresent = 0;
		adressSizePrefixPresent = 0;
		operandSizePrefixPresent = 0;
		segmentOverridePrefixPresent = 0;
		adressSize = operandSizeDefault;
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
		segmentOverridePrefix = -1;
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

	public long getUInt32(InputStream is) throws Exception
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
		return b0 + 256*b1 + 65536*b2 + 256*65536*(long)b3;
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
		
		if (adressSize==0)
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
			sibPresent = (mod != 3 && rm == 4 ? 1 : 0);
			int base = 0;
			if (sibPresent == 1)
			{
				sib = getUInt8(is);
				base = sib & 7;
			}

			switch (mod) {
				case 0: if ((rm==5)||(rm==4 && base==5)) decodeDisplacement(is, 4); break;
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
		else if (requiresDisplacement==1)
			displacementSize = 1;
		else if (requiresDisplacement==2)
			displacementSize = 2;
		else if (requiresDisplacement==4)
			displacementSize = 4;
		else
			this.check(false, "Illegal displacementSize.");
		
		if (displacementSize == 1)
			displacement = getUInt8(is);
		else if (displacementSize == 2)
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
		else if (requiresImmediate==1)
			immediateSize = 1;
		else if (requiresImmediate==2)
			immediateSize = 2;
		else if (requiresImmediate==4)
			immediateSize = 4;
		else
			this.check(false, "Illegal immediateSize.");
		
		if (immediateSize == 1)
			immediate = getUInt8(is);
		else if (immediateSize == 2)
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
			segmentOverridePrefix = b;
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
	
	private String itoa(long val, int siz, boolean forceSign)
	{
		long max = (1L << (siz << 3));
		long min = max >> 1;
		String sign = (forceSign ? "+" : "");
		
		if (val > min)
		{
			sign = "-";
			val = max - val;
		}
		
		return String.format("%s%d", sign, val);
	}
	
	public long get_displacement()
	{
		long max = (1L << (displacementSize << 3));
		long min = max >> 1;
		return (displacement > min ? -max + displacement : displacement);
	}
	
	public String displStr(int displSize)
	{
		
		return itoa(displacement, displSize, false);
	}
	
	public String displStr(int displSize, boolean forceSign)
	{
		
		return itoa(displacement, displSize, forceSign);
	}
	
	public String immediateStr(int displSize)
	{
		
		return itoa(immediate, displSize, false);
	}
	
	private String sibStr(int mod)
	{
		// precondition for call: sibPresent==1
		// reference: table 17-4,
		// base part horizontal, index part vertical, 
		// displacement added by caller except when mod==0 && base==5
		
		int ss = (sib >> 6) & 3;
		int index = (sib >> 3) & 7;
		int base = sib & 7;

		String baseStr = (base==5 && mod==0 ? displStr(4) : regs32[base]);

		String fmt = (index==5 ? "": (ss==0 ? "%s" : "%s*%d"));
		String indexStr = String.format(fmt, regs32[index], ss*2);

		return (base==5 && mod==0 ? indexStr + "+" + baseStr : baseStr + "+" + indexStr);
	}
	
	public String segmentOverridePrefixStr()
	{
		if (segmentOverridePrefixPresent == 0)
			return "";
		
		switch (segmentOverridePrefix) {
			case 0x2e: return "cs:"; 
			case 0x36: return "ss:"; 
			case 0x3e: return "ds:"; 
			case 0x26: return "es:"; 
			case 0x64: return "fs:"; 
			case 0x65: return "gs:";
		}
		
		return "";
	}
	
	public String modRMtoEffectiveAddressStr (int opsize)
	{
		String s = null;

		int rm = modrm & 7; 
		int mod = (modrm >> 6) & 3; 
		
		if (mod == 3)
		{
			// modrm specifies a register
			s = (opsize==1 ? regs8[rm] : (opsize==2 ? regs16[rm] : regs32 [rm]));
			return s; // no brackets and no segment override prefix
		}
		else if (this.adressSize==0)
		{
			// effective address according to table 17-2
			
			final String effregs[] = {
					"bx+si",
					"bx+di",
					"bp+si",
					"bd+di",
					"si",
					"di",
					"bp",
					"bx",
			};
			
			switch (mod) {
			case 0: 
				s = (rm==6 ? displStr(2) : effregs[rm]);
				break;
			case 1:
				s = effregs[rm] + displStr(1, true); 
				break;
			case 2:
				s = effregs[rm] + displStr(2, true); 
				break;
			case 3:
				// already handled above
				break;
			}
		}
		else
		{
			// reference: table 17-3
			
			switch (mod) {
			case 0: 
				s = (rm==4 ? sibStr(mod) : (rm==5 ? displStr(4) : regs32[rm]));
				break;
			case 1:
				s = (rm==4 ? sibStr(mod) : regs32[rm]) + displStr(1, true); 
				break;
			case 2:
				s = (rm==4 ? sibStr(mod) : regs32[rm]) + displStr(4, true); 
				break;
			case 3:
				// already handled above
				break;
			}
		};
		
		return segmentOverridePrefixStr() + "[" + s + "]";
	}

	public String toString()
	{
		String s = null;
		try 
		{
			s = this.instruction.toString (this);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			s = "? Exception in toString() ?";
		}
		return s;
	}
	
	public boolean isJmpOnTableUsingCS()
	{
		// checks for jmp cs:[eax*4+x] for any x
		return instructionData.size()==8
				&& instructionData.get(0) == 0x2e
				&& instructionData.get(1) == 255
				&& instructionData.get(2) == 36
				&& instructionData.get(3) == 133;
	}

}
