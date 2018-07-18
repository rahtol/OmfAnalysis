import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Vector;

public class DisassembledListing
{

	String cppfname;
	String cppasmfname;
	String modulename;
	OmfFile omf;
	
	DisassembledListing(OmfFile omf)
	{
		this.omf = omf;
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
		DebInfoModule module = omf.debtxt.modulesByName.get(modulename);
		omf.check(module!=null, String.format("Module %s not found error!", modulename));
//		NavigableMap<Long, DebInfoLine> lineinfos = omf.debtxt.lines.subMap(module.start_address, true, module.end_address, false);
		BufferedReader inf = new BufferedReader(new FileReader(cppfname));
		BufferedWriter outf = new BufferedWriter(new FileWriter(cppfname+".asl"));
		DebInfoLine.LineNo lineno = new DebInfoLine.LineNo(module,0);
		while (inf.ready())
		{
			lineno.srcline++;
			String line = inf.readLine();
			outf.write(String.format("%4d %s\n", lineno.srcline, line));
			DebInfoLine l = omf.debtxt.linesByNo.get(lineno);
			if (l != null)
			{
				byte linecode [] = omf.abstxt.getRange(l.start_address, l.end_address);
				long virtualaddress = l.start_address;
				ByteArrayInputStream stream = new ByteArrayInputStream(linecode);
				Vector<i386InstructionDecoder> instructions = new Vector<i386InstructionDecoder>();
				while (stream.available() > 0)
				{
					i386InstructionDecoder instruction = new i386InstructionDecoder(virtualaddress, 1, 1);   // assuming 32-bit operand- and address size here
					instruction.decode(stream);
					instructions.add(instruction);
					virtualaddress += instruction.instructionData.size();
					if(instruction.isJmpOnTableUsingCS() && virtualaddress==instruction.displacement)
					{
						// TODO: if jmp cs:[eax*4+x] instruction where x=virtualaddress then break
						break;
					}
				}
				for (i386InstructionDecoder instruction : instructions)
				{
					outf.write("---- " + hexdump(instruction.instructionData, 24) + instruction.toString() + "\n");
				}
			}
		}
		outf.close();
		inf.close();
	}
}
