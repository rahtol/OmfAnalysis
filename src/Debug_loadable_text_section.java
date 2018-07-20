import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

public class Debug_loadable_text_section 
{
	final static int idxMODULES = 0;
	final static int idxSYMBOLS = 2;
	final static int idxLINES = 3;
	final static int idxSRCLINES = 6;
	
	class Table_of_contents
	{
		int num_debug_segments;
		long length;
		long location [];  // relative to start of file
		
		public void read (OmfFile.Pos pos) throws Exception
		{
			num_debug_segments = (int) pos.readUInt16();
			omf.check(num_debug_segments > 6, String.format("Expecting at least 6 DEBSEGs, num_debug_segments=%d", num_debug_segments));
			length = pos.readUInt16();
			omf.check(length == 4* num_debug_segments, String.format("Unexpected length in table of contents of DEBTXT: length=%d, num_debug_segments=%d", length, num_debug_segments));
			
			location = new long [num_debug_segments+1]; // one extra entry "last"
			for (int i=0; i < num_debug_segments; i++)
			{
				location[i] = pos.readUInt32(); 
			}
			location [num_debug_segments] = offs+len; // allows to determine length of last segment consistently 
			
			long prev_location = pos.pos;
			for (int i=1; i <= num_debug_segments; i++)
			{
				if (location[i] == 0) continue;
				omf.check(location[i] > prev_location, String.format("Debug segments not ascending in table of contens of DEBTXT: i=%d, location[i]=%d, prev_location=%d", i, location[i], prev_location));
				prev_location = location[i];
			}
			omf.check(location[idxMODULES]!=0, String.format("DEBTXT: TOC entry MODULES should not be zero."));
			omf.check(location[idxSYMBOLS]!=0, String.format("DEBTXT: TOC entry SYMBOLS should not be zero."));
			omf.check(location[idxLINES]!=0, String.format("DEBTXT: TOC entry LINES should not be zero."));
			omf.check(location[idxSRCLINES]!=0, String.format("DEBTXT: TOC entry SRCLINES should not be zero."));
		}
		
		long debsegLocation (int idx)
		{
			return location [idx];
		}
		
		long debsegLen (int idx)
		{
			int idx2 = idx+1;
			while (location[idx2]==0) idx2++;
			return location[idx2]-location[idx];
		}
	}

	OmfFile omf;
	int offs;
	int len;
	Table_of_contents table_of_contents;
	NavigableMap<Long,DebInfoModule> modulesByAddress;
	NavigableMap<String,DebInfoModule> modulesByName;
	NavigableMap<Long,DebInfoProcedure> proceduresByAddress;
	NavigableMap<String,DebInfoProcedure> proceduresByName;
	NavigableMap<Long,DebInfoLine> linesByAddress;
	NavigableMap<DebInfoLine.LineNo,DebInfoLine> linesByNo;
	
	Debug_loadable_text_section ()
	{
		omf = null;
		offs = 0;
		len = 0;
		table_of_contents = new Table_of_contents();
		modulesByAddress = new TreeMap<Long,DebInfoModule>();
		modulesByName = new TreeMap<String,DebInfoModule>();
		proceduresByAddress = new TreeMap<Long,DebInfoProcedure>();
		proceduresByName = new TreeMap<String,DebInfoProcedure>();
		linesByAddress = new TreeMap<Long,DebInfoLine>();
		linesByNo = new TreeMap<DebInfoLine.LineNo,DebInfoLine>();
	}
	
	long [] debsegLen ()
	{
		long l[] = new long [table_of_contents.num_debug_segments];
		for (int i=0; i<l.length;i++)
			l[i] = table_of_contents.debsegLen(i);
		return l;
	}
	
	public void read_modules_section (long location, long len) throws Exception
	{
		OmfFile.Pos pos = omf.new Pos((int) location);
		
		DebInfoModule prev_module = null;
		@SuppressWarnings("unused")
		int n = 0;
		while (pos.pos < location+len)
		{
			n++;
			DebInfoModule cur_module = new DebInfoModule (table_of_contents);
			cur_module.read(omf, pos, location);
			if (prev_module != null)
			{
				prev_module.set_end_location_offs (omf, cur_module.start_location_offs);
			}
			prev_module = cur_module;
			modulesByAddress.put(cur_module.start_address, cur_module);
			modulesByName.put(cur_module.toc.module_name, cur_module);
//			System.out.printf("%3d %s\n", n, cur_module.toc.module_name);
		}
		prev_module.set_end_location_offs (omf, debsegLen());
		omf.check(pos.pos == location+len, String.format("MODULES: length mismatch, pos=%d, offs=%d, len=%d", pos.pos, location, len));
	}
	
	public void read_symbols_for_module (DebInfoModule module) throws Exception
	{
		final long symbol_record_len [] = {  // VAL mod 1000 -> length of fixed part of record in bytes, VAL > 1000 -> varname follows the fixed part
			 1008,		//  0: block start 
			 1015,		//  1: procedure start 
			    0,      //  2: block end
			    6,		//  3: symbol base
			    0,		//  4: BP relative base
			 1006,		//  5: symbol   
			 1006,		//  6: offset based symbol   
			 1006,		//  7: pointer based symbol   
			 1006,		//  8: selector bases symbol   
			 1006,		//  9:    
			 1006,		// 10:    
			 1006,		// 11: register symbol    
			 1006,		// 12:
			    0,		// 13: ESP relative base
			 1019,		// 14: procedure start with line number 
			 1012,		// 15: block start with line number
			    4       // 16: block end with line number
		};
		
		long start_location = table_of_contents.debsegLocation(idxSYMBOLS) + module.start_location_offs[idxSYMBOLS];
		long end_location = table_of_contents.debsegLocation(idxSYMBOLS) + module.end_location_offs[idxSYMBOLS];
		int indent = 0;
		OmfFile.Pos pos = omf.new Pos((int) start_location);
		int n = 0;
		while (pos.pos < end_location)
		{
			n++;
			long symbol_record_type = pos.readUInt8();
			omf.check(symbol_record_type<=symbol_record_len.length, String.format("Unexpectd symbol_record_type=%d at location=%d in module %s", symbol_record_type, pos.pos-1, module.toc.module_name));
			
			switch ((int)symbol_record_type) {
			case 0:
			case 15:
				indent++;
				break;
			case 1:
			case 14:
				omf.check(indent==1, String.format("SYMBOLS: Procedure record not at indentation level one at location=%d in module %s", pos.pos-1, module.toc.module_name));
				indent++;
				OmfFile.Pos pos2 = omf.new Pos(pos.pos); // use fresh read position as pos will be advanced below for all record types
				DebInfoProcedure cur_procedure = new DebInfoProcedure (module);
				cur_procedure.read(omf, pos2, symbol_record_type);
				proceduresByAddress.put(cur_procedure.start_address, cur_procedure);
				proceduresByName.put(cur_procedure.name, cur_procedure);
				break;
			case 2:
			case 16:
				omf.check(indent>0, String.format("Block at at indentation level zero at location=%d in module %s", pos.pos-1, module.toc.module_name));
				indent--;
				break;
			default:
				break;
			}
			
			// advance pos to next record
			pos.pos += symbol_record_len[(int) symbol_record_type] % 1000;  // skip fixed part of record
			if (symbol_record_len[(int) symbol_record_type] >= 1000)
			{
				// skip varname, if existent in record
				pos.readStr();
			}
		}
		omf.check(indent==0, String.format("Indentation level not zero at end of SYMBOLS at location=%d in module %s", pos.pos-1, module.toc.module_name));
		omf.check(pos.pos==end_location, String.format("SYMBOLS: length mismatch for module=%s, pos=%d, offs=%d", module.toc.module_name, pos.pos, end_location));
	}

	public Vector<DebInfoLine> read_lines_for_module (DebInfoModule module) throws Exception
	{
		Vector<DebInfoLine> veclines = new Vector<DebInfoLine>();
		long start_location = table_of_contents.debsegLocation(idxLINES) + module.start_location_offs[idxLINES];
		long end_location = table_of_contents.debsegLocation(idxLINES) + module.end_location_offs[idxLINES];
		OmfFile.Pos pos = omf.new Pos((int) start_location);
		int line = 0;
		long last_line_address = 0;
		while (pos.pos < end_location)
		{
			line++;
			long line_address = pos.readUInt32() + module.start_address;
			omf.check(line_address<module.end_address, String.format("LINES: address not in range for module=%s, pos=%08x, line_address=%08x, last_address=%08x", module.toc.module_name, pos.pos, line_address, module.end_address));
			omf.warn(line_address>last_line_address, String.format("LINES: not strictly ascending for module=%s, pos=%08x, line_address=%08x, last_line_address=%08x", module.toc.module_name, pos.pos, line_address, last_line_address));
			last_line_address = line_address;
			Map.Entry<Long,DebInfoProcedure> procentry = proceduresByAddress.floorEntry(line_address);
			omf.check(procentry!=null, String.format("LINES: address not in range of a procedure for module=%s, pos=%08x, line_address=%08x", module.toc.module_name, pos.pos, line_address));
			DebInfoLine lineinfo = new DebInfoLine (line, line_address, procentry.getValue());
			linesByAddress.put(line_address, lineinfo);
			veclines.add(lineinfo);
		}
		omf.check(pos.pos==end_location, String.format("LINES: length mismatch for module=%s, pos=%d, offs=%d", module.toc.module_name, pos.pos, end_location));
		module.num_lines = line;
		return veclines;
	}
	
	public void read_srclines_for_module (DebInfoModule module, Vector<DebInfoLine> veclines) throws Exception
	{
		final int NLR_FLAG = (1<<31);		 // new line record flag
		
		long start_location = table_of_contents.debsegLocation(idxSRCLINES) + module.start_location_offs[idxSRCLINES];
		long end_location = table_of_contents.debsegLocation(idxSRCLINES) + module.end_location_offs[idxSRCLINES];
		OmfFile.Pos pos = omf.new Pos((int) start_location);
		int line = 0;
		long count = 0;
		if (pos.pos != end_location)
		{
			String fname = pos.readStr();
			count = pos.readUInt16();
			while (pos.pos < end_location)
			{
				line++;
				long start_line_and_column = pos.readUInt32();
				long end_line_and_column = 0;
				if ((start_line_and_column & NLR_FLAG) != 0)
				{
					end_line_and_column = pos.readUInt32();
				}
				veclines.elementAt(line-1).set_srcline(start_line_and_column, end_line_and_column);
			}
		}
		omf.check(pos.pos==end_location, String.format("SRCLINES: length mismatch for module=%s, pos=%d, offs=%d", module.toc.module_name, pos.pos, end_location));
		omf.check(line==count, String.format("SRCLINES: count different from line for module=%s, count=%d, srcline=%d", module.toc.module_name, count, line));
		omf.check(module.num_lines==line, String.format("SRCLINES: line count different from num_lines for module=%s, count=%d, srcline=%d", module.toc.module_name, line, module.num_lines));
		module.num_srclines = line;
	}
	
	private Long get_line_end_address (DebInfoLine lineinfo, Iterator<Entry<Long, DebInfoLine>> li)
	{
		Entry<Long, DebInfoLine> e = this.linesByAddress.tailMap(lineinfo.start_address, false).firstEntry();
		if (e==null)
			return lineinfo.procedure.end_address;
		DebInfoLine lineinfo2 = e.getValue();
		if (lineinfo.procedure == lineinfo2.procedure)
			return lineinfo2.start_address;
		else
			return lineinfo.procedure.end_address;
	}
	 
	public void read_symbols_lines_srclines() throws Exception
	{
		for(Map.Entry<Long,DebInfoModule> entry : modulesByAddress.entrySet())
		{
			  DebInfoModule module = entry.getValue();
			  read_symbols_for_module (module);
			  Vector<DebInfoLine> veclines = read_lines_for_module (module);
			  read_srclines_for_module (module, veclines);

//			  System.out.printf("%08x  %08x  %08x  %s\n", module.start_address, module.end_address, module.block_length, module.toc.module_name);
		}

		// TODO: REVIEW: the solution below
		// add a SRCLINE for each procedure (type=14, i.e. with SRCLINE) start address
		for(Entry<Long, DebInfoProcedure> procedureEntry :proceduresByAddress.entrySet())
		{
			DebInfoProcedure procedure = procedureEntry.getValue();
			if (procedure.symbol_record_type==14)
			{
				// makes sense only with valid SRCLINE information
				DebInfoLine srcline = new DebInfoLine(0, procedure.start_address, procedure);
				srcline.set_srcline(procedure.start_line_col, 0);
				
				DebInfoLine prev = linesByAddress.get(srcline.start_address);
				if (prev==null)
					linesByAddress.put(srcline.start_address, srcline);
//				omf.check(prev==null, String.format("Duplicate SRCLINE for proc begin: procedure=%s, srcline=%d", procedure.name, srcline.srcline));
			}
		}
		
		Iterator<Entry<Long, DebInfoLine>> li = linesByAddress.entrySet().iterator();
		while (li.hasNext())
		{
			DebInfoLine lineinfo = li.next().getValue();
			lineinfo.set_end_address(get_line_end_address(lineinfo, li));
			DebInfoLine prev = linesByNo.put(new DebInfoLine.LineNo(lineinfo.module, lineinfo.srcline, lineinfo.start_address), lineinfo);
			omf.check(prev==null, String.format("Duplicate SRCLINE for module=%s, srcline=%d", lineinfo.module.toc.module_name, lineinfo.srcline));
		}
	}
	
	public void initialize (OmfFile omf, long DEBTXT_location, long lendebtxt) throws Exception
	{
		this.omf = omf;
		this.offs =  (int) DEBTXT_location;
		this.len = (int) lendebtxt;
		OmfFile.Pos pos = omf.new Pos (offs);
		table_of_contents.read(pos);
		read_modules_section (table_of_contents.debsegLocation(idxMODULES), table_of_contents.debsegLen(idxMODULES));
		read_symbols_lines_srclines ();
	}

}
