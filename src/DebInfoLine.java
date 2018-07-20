
public class DebInfoLine
{
	long start_address;           // virtual address under windows
	long end_address;             // virtual address under windows
	long length;
	int line;
	DebInfoProcedure procedure;
	DebInfoModule module;
	long start_line_and_column;
	long end_line_and_column;
	long srcline;
	
	static class LineNo implements Comparable<LineNo>
	{
		DebInfoModule module;  // 1st order criteria
		long srcline;		   // 2nd order criteria
		long start_address;    // 3rd order criteria
		
		LineNo(DebInfoModule module, long srcline)
		{
			this.module = module;
			this.srcline = srcline;
			this.start_address = -1;
		}

		LineNo(DebInfoModule module, long srcline, long start_address)
		{
			this.module = module;
			this.srcline = srcline;
			this.start_address = start_address;
		}

		@Override
		public int compareTo(LineNo lineid)
		{
			int result = 0;
			
			if (this.module == lineid.module)
				if (this.srcline == lineid.srcline)
					result = Long.compare(this.start_address, lineid.start_address);
				else
					result = Long.compare(this.srcline, lineid.srcline);
			else
				result = Long.compare(this.module.start_address, lineid.module.start_address);
						
			return result;
		}
		
	}
	
	DebInfoLine(int line, long start_address, DebInfoProcedure procedure)
	{
		this.line = line;
		this.start_address = start_address;
		this.procedure = procedure;
		this.module = procedure.module;
		end_address = 0; // to be updated later during set_end_address
		length = 0; // to be updated later during set_end_address
	}
	
	void set_end_address (long end_address)
	{
		this.end_address = end_address;
		this.length = end_address - start_address;
	}
	
	void set_srcline (long start_line_and_column, long end_line_and_column)
	{
		final int NLR_FLAG = (1<<31);		 // new line record flag
		final int NLR_START_LINE_BOFF = 10;  // line number at bit offset 10
		final int NLR_START_LINE_BSIZE = 21; // line number 21 bits wide
		
		this.start_line_and_column = start_line_and_column;
		this.end_line_and_column = end_line_and_column;
		this.srcline = ((start_line_and_column >> NLR_START_LINE_BOFF) & ((1<<NLR_START_LINE_BSIZE)-1));
	}

}
