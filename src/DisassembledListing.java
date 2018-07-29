import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.Vector;

public class DisassembledListing
{

	String cppfname;
	String cppasmfname;
	String modulename;
	OmfFile omf;
	long code_segment_start_address; // 0x0 for WCU_ATP, 0x400 for OBCU_ATP
	
	DisassembledListing(OmfFile omf, long code_segment_start_address)
	{
		this.omf = omf;
		this.code_segment_start_address = code_segment_start_address;  // TODO: can this value be calculated somehow from OMF
	}
	
	private long readUInt32(ByteArrayInputStream stream)
	{
		long b0 = ((long) stream.read()) & 255;
		long b1 = ((long) stream.read()) & 255;
		long b2 = ((long) stream.read()) & 255;
		long b3 = ((long) stream.read()) & 255;
		return b0 + 256*b1 + 65536*b2 + 256*65536*b3;
	}

	static String hexdump (byte data[], int len)
	{
		String s="";
		for (int i=0; i<data.length; i++)
		{
			int d = ((int) data[i]) & 0xff;
			s += String.format("%02x", d);
		}
		return (s + "                         ").substring(0, len);
	}
	
	static String hexdump (Vector<Integer> data, int len)
	{
		String s="";
		for (int i=0; i<data.size(); i++)
		{
			int d = ((int) data.get(i)) & 0xff;
			s += String.format("%02x", d);
		}
		return (s + "                         ").substring(0, len);
	}
	
	void transcode (String modulename, String cppfname) throws Exception
	{
		BufferedReader inf = new BufferedReader(new FileReader(cppfname));
		BufferedWriter outf = new BufferedWriter(new FileWriter(cppfname+".asl"));
		
		try {
			DebInfoModule module = omf.debtxt.modulesByName.get(modulename);
			omf.check(module!=null, String.format("Module %s not found error!", modulename));
			DebInfoLine.LineNo lineno0 = new DebInfoLine.LineNo(module,0);
			DebInfoLine.LineNo lineno1 = new DebInfoLine.LineNo(module,1);
			while (inf.ready())
			{
				lineno0.srcline++;
				lineno1.srcline++;
				String line = inf.readLine();
				outf.write(String.format("%4d %s\n", lineno0.srcline, line));
				Iterator<DebInfoLine> ll = omf.debtxt.linesByNo.subMap(lineno0, lineno1).values().iterator();
				int i = 0;
				while (ll.hasNext())
				{
					DebInfoLine l = ll.next();
					i++;
					byte linecode [] = omf.abstxt.getRange(l.start_address+code_segment_start_address, l.end_address+code_segment_start_address);
					long virtualaddress = l.start_address+code_segment_start_address;
					ByteArrayInputStream stream = new ByteArrayInputStream(linecode);
					Vector<i386InstructionDecoder> instructions = new Vector<i386InstructionDecoder>();
					boolean ok = false;
					while (stream.available() > 0)
					{
						i386InstructionDecoder instruction = new i386InstructionDecoder(virtualaddress, 1, 1);   // assuming 32-bit operand- and address size here
						instruction.decode(stream);
						instructions.add(instruction);
						virtualaddress += instruction.instructionData.size();
						outf.write((i==1?"---- ":"++-- ") + String.format("%08x: ", instruction.virtualaddress) + hexdump(instruction.instructionData, 24) + instruction.toString() + "\n");
						// TODO: REVIEW: the following if is very specialized code:
						// if jmp cs:[eax*4+x] instruction where x=virtualaddress (i.e. the jmp-table is located immediately after the indirect jmp-instruction) then break
						// it accounts for table data in the codesegment (switch-statement, jump-table)
						if(instruction.isJmpOnTableUsingCS() && virtualaddress==instruction.displacement+code_segment_start_address)
						{
//							omf.warn(stream.available() % 4 == 0, String.format("Size of jmp-table not divisible by four at module=%s, srcline=%d", l.module.toc.module_name, l.srcline));
							long minJmpAddress = l.end_address+code_segment_start_address;
							for (i386InstructionDecoder instruction2 : instructions)
							{
								// consider any jump-target address already parsed on this srcline as possible end address of the following jmp table
								long addr = instruction2.jmpTargetAddress;
								if (addr > virtualaddress && addr <= minJmpAddress)
								{
									minJmpAddress = addr;
									ok = true;
								}
							}
							while (virtualaddress+3 < minJmpAddress) // at least four bytes are available
							{
								long dd = readUInt32 (stream)+code_segment_start_address;
								outf.write("DD-- " + String.format("%08x: %08x\n", virtualaddress, dd));
								virtualaddress += 4;
								if (dd < minJmpAddress)
									minJmpAddress = dd;
								ok |= dd == minJmpAddress;
							}
							omf.warn(ok, String.format("Jmp-table incosistent at module=%s, srcline=%d", l.module.toc.module_name, l.srcline));
						}
					}
				}
			}
			outf.close();
			inf.close();
		} 
		catch (Exception e)
		{
			outf.close();
			inf.close();
			throw e;
		}
	}
}
