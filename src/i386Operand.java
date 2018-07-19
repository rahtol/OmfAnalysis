import java.io.InputStream;

public enum i386Operand
{
	_Eb(1,0,0)
	{
		public String toString(i386InstructionDecoder decoder) throws Exception
		{
			int mod = decoder.getMod();
			return (mod==3 ? "" : "byte ptr ") + decoder.modRMtoEffectiveAddressStr(1);
		}
	},
	_Ew(1,0,0)
	{
		public String toString(i386InstructionDecoder decoder) throws Exception
		{
			int mod = decoder.getMod();
			return (mod==3 ? "" : "word ptr ") + decoder.modRMtoEffectiveAddressStr(2);
		}
	},
	_Ev(1,0,0)
	{
		public String toString(i386InstructionDecoder decoder) throws Exception
		{
			int mod = decoder.getMod();
			return (mod==3 ? "" : (decoder.operandSize==0 ? "word ptr " : "dword ptr ")) + decoder.modRMtoEffectiveAddressStr((decoder.operandSize==1?4:2));
		}
	},
	_Ep(1,0,0)  // used with: CALL JMP
	{
		// TODO: register possible ? -> CALL [EAX] also CALL [AX]
		public String toString(i386InstructionDecoder instructionDecoder)
		{
			return instructionDecoder.modRMtoEffectiveAddressStr((4)); 
		}
	},
	_Gb(1,0,0)
	{
		public String toString(i386InstructionDecoder instructionDecoder) throws Exception
		{
			int reg = instructionDecoder.getReg();
			return instructionDecoder.regs8[reg]; 
		}
	},
	_Gw(1,0,0)
	{
		public String toString(i386InstructionDecoder decoder) throws Exception
		{
			int reg = decoder.getReg();
			return decoder.regs16[reg]; 
		}
	},
	_Gv(1,0,0)
	{
		public String toString(i386InstructionDecoder decoder) throws Exception
		{
			int reg = decoder.getReg();
			return (decoder.operandSize==1 ? decoder.regs32[reg] : decoder.regs16[reg]); 
		}
	},
	_Ib(0,0,1)
	{
		public String toString(i386InstructionDecoder decoder) throws Exception
		{
			return decoder.immediateStr(1); 
		}
	},
	_Iw(0,0,2)
	{
		public String toString(i386InstructionDecoder decoder) throws Exception
		{
			return decoder.immediateStr(2); 
		}
	},
	_Iv(0,0,-1) // =2 or =4 depending on operandSize
	{
		public String toString(i386InstructionDecoder decoder) throws Exception
		{
			return decoder.immediateStr(decoder.operandSize==0 ? 2 : 4); 
		}
	},
	_Jb(0,1,0)
	{
		public String toString(i386InstructionDecoder decoder) throws Exception
		{
			return "$" + decoder.displStr(1, true) + String.format("  (%08x)", decoder.virtualaddress+decoder.instructionData.size()+decoder.get_displacement()); 
		}
	},
	_Jw(0,2,0)
	{
		public String toString(i386InstructionDecoder decoder) throws Exception
		{
			return "$" + decoder.displStr(2, true) + String.format("  (%08x)", decoder.virtualaddress+decoder.instructionData.size()+decoder.get_displacement()); 
		}
	},
	_Jv(0,-1,0)
	{
		public String toString(i386InstructionDecoder decoder) throws Exception
		{
			return "$" + decoder.displStr((decoder.adressSize==0 ? 2 : 4), true) + String.format("  (%08x)", decoder.virtualaddress+decoder.instructionData.size()+decoder.get_displacement()); 
		}
	},
	_M(1,0,0)  // mod != 3  memory reference only, exclusively used in "LEA _Gv, _M"
	{
		public void check (i386InstructionDecoder decoder) throws Exception
		{
			int mod = decoder.getMod();
			decoder.check(mod!=3, "_M must not refer to a register.");
		}
		
		public String toString(i386InstructionDecoder decoder) throws Exception
		{
			return decoder.modRMtoEffectiveAddressStr((0)); // parameter doesn't matter since mod!=3
		}
	},
	_Mp(1,0,0) // mod != 3  memory reference only  far pointer, i.e. 32 bit or 48 bit depending on operand size, exclusively used in "L?S, _Gv, _Mp"
	{
		public void check (i386InstructionDecoder decoder) throws Exception
		{
			int mod = decoder.getMod();
			decoder.check(mod!=3, "_M must not refer to a register.");
		}
		
		public String toString(i386InstructionDecoder decoder) throws Exception
		{
			return "far ptr " + decoder.modRMtoEffectiveAddressStr((0)); // parameter doesn't matter since mod!=3 
		}
	},
	_Ma(1,0,0) // mod != 3  memory reference only  pointer to two words or doublewords depending on operand size, exclusively used in BOUND
	{
		public void check (i386InstructionDecoder decoder) throws Exception
		{
			int mod = decoder.getMod();
			decoder.check(mod!=3, "_M must not refer to a register.");
		}
		
		public String toString(i386InstructionDecoder decoder) throws Exception
		{
			return (decoder.operandSize==1 ? "dword[2] ptr " : "word[2] ptr ") + decoder.modRMtoEffectiveAddressStr((0)); // parameter doesn't matter since mod!=3
		}
	},
	_Ms(1,0,0) // mod != 3  memory reference only  pointer to 6 byte pseudo descriptor
	{
		public void check (i386InstructionDecoder decoder) throws Exception
		{
			int mod = decoder.getMod();
			decoder.check(mod!=3, "_M must not refer to a register.");
		}
		
		public String toString(i386InstructionDecoder decoder) throws Exception
		{
			return decoder.modRMtoEffectiveAddressStr((0)); // parameter doesn't matter since mod!=3
		}
	},
	_Rw(1,0,0)  // mod == 3  register reference only
	{
		public void check (i386InstructionDecoder decoder) throws Exception
		{
			int mod = decoder.getMod();
			decoder.check(mod==3, "_Rw must not refer to memeory.");
		}
		
		public String toString(i386InstructionDecoder decoder) throws Exception
		{
			int reg = decoder.getRm();
			return decoder.regs16[reg]; 
		}
	},
	_Rd // register reference only
	{
		public void check (i386InstructionDecoder decoder) throws Exception
		{
			int mod = decoder.getMod();
			decoder.check(mod==3, "_Rw must not refer to memeory.");
		}
		
		public String toString(i386InstructionDecoder decoder) throws Exception
		{
			int reg = decoder.getRm();
			return decoder.regs32[reg]; 
		}
	},
	_Cd // control register CR0 CR2 CR3  ref. 6.2.3.8
	{
		public void check (i386InstructionDecoder decoder) throws Exception
		{
			int reg = decoder.getReg();
			decoder.check(reg==0||reg==2||reg==3, String.format("Illegal control register specification: %d.", reg));
		}
		
		public String toString(i386InstructionDecoder decoder) throws Exception
		{
			int reg = decoder.getReg();
			return decoder.controlregister[reg]; 
		}
	},
	_Dd // debug register DR0 DR1 DR2 DR3 DR6 DR7   ref. 6.2.3.8
	{
		public void check (i386InstructionDecoder decoder) throws Exception
		{
			int reg = decoder.getReg();
			decoder.check(reg!=4 && reg!=5, String.format("Illegal debug register specification: %d.", reg));
		}
		
		public String toString(i386InstructionDecoder decoder) throws Exception
		{
			int reg = decoder.getReg();
			return decoder.debugregister[reg]; 
		}
	},
	_Td // test register TR6 TR7  ref. 6.2.3.8
	{
		public void check (i386InstructionDecoder decoder) throws Exception
		{
			int reg = decoder.getReg();
			decoder.check(reg>=6, String.format("Illegal test register specification: %d.", reg));
		}
		
		public String toString(i386InstructionDecoder decoder) throws Exception
		{
			int reg = decoder.getReg();
			return decoder.testregister[reg]; 
		}
	},
	_Sw(1,0,0)
	{
		public void check (i386InstructionDecoder decoder) throws Exception
		{
			int reg = decoder.getReg();
			decoder.check(reg<=5, String.format("Illegal segment register specification: %d.", reg));
		}
		
		public String toString(i386InstructionDecoder decoder) throws Exception
		{
			int reg = decoder.getReg();
			return decoder.segmentregister[reg]; 
		}
	},
	_Ap(0,2,-1)  // immediate far address 32 or 48 bit depending on operand size
	{
		public String toString(i386InstructionDecoder instructionDecoder)
		{
			String fmt = (instructionDecoder.operandSize==2 ? "%04x:%04x" : "%04x:%08x");
			return "far " + String.format(fmt, instructionDecoder.displacement, instructionDecoder.immediate);
		}
	},
	__Xb, // DS:SI pointer to byte, not displayed
	__Xv, // DS:SI pointer to word or doubleword, not displayed
	__Yb, // ES:DI pointer to byte, not displayed
	__Yv, // ES:DI pointer to word or doubleword, not displayed
	__Fv, // Flag register, not displayed
	_Ob(0,-2,0),  // word or doubleword depending on adresssize used as displacement in MOV (A0-A3)
	_Ov(0,-2,0),  // word or doubleword depending on adresssize used as displacement in MOV (A0-A3)
	_AL,
	_CL,
	_DL,
	_BL,
	_AH,
	_CH,
	_DH,
	_BH,
	_DX,
	_eAX,
	_eCX,
	_eDX,
	_eBX,
	_eSP,
	_eBP,
	_eSI,
	_eDI,
	_DS,
	_ES,
	_FS,
	_GS,
	_CS,
	_SS,
	_1,  // SHL AX, 1
	_3;  // INT 3
	
	final int requiresModRm;
	final int requiresDisplacement;
	final int requiresImmediate;
	
	i386Operand ()
	{
		requiresModRm = 0;
		requiresDisplacement = 0;
		requiresImmediate = 0;
	}
	
	i386Operand (int requiresModRm, int requiresDisplacement, int requiresImmediate)
	{
		this.requiresModRm = requiresModRm;
		this.requiresDisplacement = requiresDisplacement;
		this.requiresImmediate = requiresImmediate;
	}
	
	public void check (i386InstructionDecoder decoder) throws Exception
	{
		// nothing to check by default
	}
	
	public void decode(i386InstructionDecoder instructionDecoder, InputStream is, i386Instruction instruction) throws Exception
	{
		if (requiresModRm!=0) {
			instructionDecoder.decodeModRM(is);
		};
		
		if (requiresDisplacement!=0) {
			instructionDecoder.decodeDisplacement(is, requiresDisplacement);
		};
		
		if (requiresImmediate!=0) {
			instructionDecoder.decodeImmediate(is, requiresImmediate);
		};
	}
	
	public String toString(i386InstructionDecoder instructionDecoder) throws Exception
	{
		String s = name();

		// redundant operands like __F 
		if(s.startsWith("__")) 
			return "";
	
		// special handling for _eAX, _eCX, ...
		if(s.startsWith("_e"))
		{
			s = s.substring(2);  // remove "_e"
			s = (instructionDecoder.operandSize==0 ? "_" : "_E") + s;
		}
			
		return s.substring(1).toLowerCase(); // remove leading "_" and convert to lower case
	}

}
