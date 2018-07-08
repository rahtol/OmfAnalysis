
public class DebInfoModule
{
	
	class Table_of_contents
	{
		long ldt_selector;
		long code_segment;
		long code_segment_selector;
		long location_offs [];        // relative to start of corresponding DEBSEG 
		long location_selector [];    // no relevance in FLAT memory model
		long first_line_number;
		long format;
		long translator_id;
		long version_number;
		String module_name;
		
		public void read (OmfFile omf, OmfFile.Pos pos) throws Exception
		{
			location_offs = new long [toc_debtxt.num_debug_segments];
			location_selector = new long [toc_debtxt.num_debug_segments];
			
			ldt_selector = pos.readUInt16();
			code_segment = pos.readUInt32();
			code_segment_selector = pos.readUInt16();
			
			for (int i=1; i < toc_debtxt.num_debug_segments; i++)
			{
				location_offs [i] = pos.readUInt32();
				location_selector [i] = pos.readUInt16();
			}
			
			first_line_number = pos.readUInt16();
			format = pos.readUInt8();
			translator_id = pos.readUInt8();
			version_number = pos.readUInt32();
			module_name = pos.readStr();
			
		}
	}
	
	Debug_loadable_text_section.Table_of_contents toc_debtxt;
	Table_of_contents toc;
	long start_location_offs [];  // relative to corresponding DEBSEG
	long end_location_offs [];    // relative to corresponding DEBSEG
	long start_address;           // virtual address under windows
	long end_address;             // virtual address under windows
	long block_length;
	int num_lines;
	int num_srclines;
	
	DebInfoModule (Debug_loadable_text_section.Table_of_contents toc_debtxt)
	{
		this.toc_debtxt = toc_debtxt;
		this.toc = new Table_of_contents();
		start_location_offs = new long [toc_debtxt.num_debug_segments];
		end_location_offs = new long [toc_debtxt.num_debug_segments];
		num_lines = 0;
		num_srclines = 0;
	}
	
	public void read (OmfFile omf, OmfFile.Pos pos, long debseg_modules_location) throws Exception
	{
		long module_location_offs = pos.pos - debseg_modules_location;
		toc.read(omf, pos);

		start_location_offs[0] = module_location_offs;
		System.arraycopy(toc.location_offs, 1, start_location_offs, 1, toc.location_offs.length-1);
		
		start_address = toc.code_segment;
		end_address = 0; // to be defined later after length of all debseg are known, see set_end_address
	}

	public void set_end_location_offs(OmfFile omf, long[] end_location_offs) throws Exception
	{
		assert(end_location_offs.length==this.end_location_offs.length);
		assert(end_location_offs.length==toc_debtxt.num_debug_segments);
		System.arraycopy(end_location_offs, 0, this.end_location_offs, 0, end_location_offs.length);
		
		for (int i=0; i < end_location_offs.length; i++)
			omf.check(end_location_offs[i]>=start_location_offs[i], String.format("Debseg info not ascending module=%s, debsegidx=%d start_location_offs=%d end_location_offs=%d", toc.module_name, i, start_location_offs[i], end_location_offs[i]));
		
		omf.check(end_location_offs[Debug_loadable_text_section.idxMODULES]>start_location_offs[Debug_loadable_text_section.idxMODULES], String.format("DEBTXT: MODULES segment should not be zero for module=%s", toc.module_name));
// commented checks do not hold in general, i.e. there are module with empty SYMBOLS, LINES and/or SRCLINES debseg
//		omf.check(end_location_offs[Debug_loadable_text_section.idxSYMBOLS]>start_location_offs[Debug_loadable_text_section.idxSYMBOLS], String.format("DEBTXT: SYMBOLS segment should not be zero for module=%s", toc.module_name));
//		omf.check(end_location_offs[Debug_loadable_text_section.idxLINES]>start_location_offs[Debug_loadable_text_section.idxLINES], String.format("DEBTXT: Lines segment should not be zero for module=%s", toc.module_name));
//		omf.check(end_location_offs[Debug_loadable_text_section.idxSRCLINES]>start_location_offs[Debug_loadable_text_section.idxSRCLINES], String.format("DEBTXT: Symbols segment should not be zero for module=%s", toc.module_name));
		set_end_address (omf);

// not really sure whether to keep this check
		omf.check(toc.first_line_number<=1, String.format("Expecting first_line_number to be zero or one in module=%s, first_line_number=%s", toc.module_name, toc.first_line_number));
	}
	
	public void set_end_address (OmfFile omf) throws Exception
	{
		if (end_location_offs[Debug_loadable_text_section.idxSYMBOLS]>start_location_offs[Debug_loadable_text_section.idxSYMBOLS])
		{
			// non-empty SYMBOLS debseg for this module
			long len = end_location_offs[Debug_loadable_text_section.idxSYMBOLS]-start_location_offs[Debug_loadable_text_section.idxSYMBOLS];
			omf.check(len > 10, String.format("SYMBOLS for module %s unexpected short, len=%d", toc.module_name, len));
			OmfFile.Pos pos = omf.new Pos((int) (start_location_offs[Debug_loadable_text_section.idxSYMBOLS]+toc_debtxt.location[Debug_loadable_text_section.idxSYMBOLS]));
			long block_start_code = pos.readUInt8();
			long block_start_address = pos.readUInt32();
			block_length = pos.readUInt32();
			String block_name = pos.readStr();
			omf.check(len > 10 + block_name.length(), String.format("SYMBOLS for module %s unexpected short #2, len=%d %s", toc.module_name, len, block_name));
			omf.check(block_start_code==0, String.format("Expecting block_start_code zero for module_name=%s, %d", toc.module_name, block_start_code));
			omf.check(block_start_address==start_address, String.format("Expecting block_start_address equal to start_address for module_name=%s, %d %d", toc.module_name, block_start_address, start_address));
// commented checks do not hold in general, e.g block_name "iosfwd" occurs with some "__ct_..." module_name
//			omf.check(block_name.toUpperCase().equals(toc.module_name.toUpperCase()), String.format("Expecting block_name equal to module_name module_name=%s, %s", toc.module_name, block_name));
			end_address = start_address + block_length;
			
// expecting also LINES and SRCLINES debseg as SYMBOLS debseg is present
// does not hold, there are symbols without lines even when SYMBOLS is present
//			omf.check(end_location_offs[Debug_loadable_text_section.idxLINES]>start_location_offs[Debug_loadable_text_section.idxLINES], String.format("DEBTXT: Lines segment should not be zero for module=%s", toc.module_name));
//			omf.check(end_location_offs[Debug_loadable_text_section.idxSRCLINES]>start_location_offs[Debug_loadable_text_section.idxSRCLINES], String.format("DEBTXT: Symbols segment should not be zero for module=%s", toc.module_name));
		}
		else {
			omf.check(start_address==0, String.format("Expecting start_address zero for module without block module=%s, start_address=%d", toc.module_name, start_address));
		}
		
	}
	
	public boolean has_debug_info ()
	{
		return end_location_offs[Debug_loadable_text_section.idxSYMBOLS]>start_location_offs[Debug_loadable_text_section.idxSYMBOLS];
		// alternative: start_address != 0 which is eqivalent according to above check
	}
	
}
