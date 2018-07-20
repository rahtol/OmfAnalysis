
public class DebInfoProcedure
{
	DebInfoModule module;		  // enclosing module
	long start_address;           // virtual address under windows
	long end_address;             // virtual address under windows
	long length;
	String name;
	long symbol_record_type;	  // possible values: 1(no SRCLINE information) or 14(including SRCLINE information)

	long start_line_col;
	long type_index;
	long procedure_type;
	long ebp_offset;
	

	DebInfoProcedure (DebInfoModule module)
	{
		this.module = module;
		start_address = 0;
		end_address = 0;
		length = 0;
		name = null;
		start_line_col = -1;
		symbol_record_type = -1;
	}

	public void read (OmfFile omf, OmfFile.Pos pos, long symbol_record_type) throws Exception
	{
		this.symbol_record_type = symbol_record_type;
		
		// symbol_record_type=1 without line number
		// symbol_record_type=14 with line number
		if (symbol_record_type==14)
		{
			start_line_col = pos.readUInt32();
		}
		start_address = pos.readUInt32();
		type_index = pos.readUInt16();
		procedure_type = pos.readUInt8();
		ebp_offset = pos.readUInt32();
		length = pos.readUInt32()+1; // TODO: REVIEW: +1 workaround since "ret" is systematically missing in debug information. probably the start address of last instruction is used to determine length 
		name = pos.readStr();
		end_address = start_address + length;
	}

}
