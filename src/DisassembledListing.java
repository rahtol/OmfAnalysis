import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.NavigableMap;

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
	
	static String hexdump (byte data[])
	{
		String s="";
		for (int i=0; i<data.length; i++)
		{
			int d = ((int) data[i]) & 0xff;
			s += String.format("%02x", d);
		}
		return s;
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
				outf.write("---- " + hexdump(linecode) + "\n");
			}
		}
		outf.close();
		inf.close();
	}
}
