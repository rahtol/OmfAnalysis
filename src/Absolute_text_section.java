import java.util.NavigableMap;
import java.util.TreeMap;

public class Absolute_text_section // ABSTXT
{
	OmfFile omf;
	int location;  // location always means an offset in OMF file counting from begin of file
	int len;

	NavigableMap<Long,AbstxtRecord> abstxtrecords;
	
	
	Absolute_text_section ()
	{
		omf = null;
		location = 0;
		len = 0;
		abstxtrecords = new TreeMap<Long,AbstxtRecord>();

	}
	
	public void initialize (OmfFile omf, long ABSTXT_location, long lenabstxt) throws Exception
	{
		this.omf = omf;
		this.location =  (int) ABSTXT_location;
		this.len = (int) lenabstxt;
		OmfFile.Pos pos = omf.new Pos (location);
		int n = 0; // counts text blocks in ABSTXT section
		
		while (pos.pos < location + len)
		{
			long real_address = pos.readUInt32();
			long length = pos.readUInt32();
			omf.check(length > 0, String.format("ABSTXT: text_length zero at offs=%08x", pos.pos));
			int text_location = pos.pos;
//			System.out.printf("ABSTXT(%04d): real_address=%08x, length=%08x, text_offs=%08x\n", ++n, real_address, length, text_offs);
			abstxtrecords.put(real_address, new AbstxtRecord(real_address, length, text_location));
			pos.pos += length;
		}
		omf.check(pos.pos == location+len, String.format("ABSTXT mismatch: pos=%d, location=%d, len=%d", pos.pos, location, len));
	}

	public byte [] getRange(long start_address, long end_address) throws Exception
	{
		AbstxtRecord txt = null;
		int len = (int) (end_address-start_address);
		byte[] data = new byte [len];
		int i = 0;
		int txtlen = 0;
		while (len>i)
		{
			if (txtlen==0)
			{
				txt = abstxtrecords.floorEntry(start_address+i).getValue();
				omf.check(txt!=null, String.format("ABSTXT: Address not in reange %08x", start_address+i));
				omf.check(start_address+i<txt.end_address, String.format("ABSTXT: Address not in reange %08x %08x %08x", start_address+i, txt.start_address, txt.end_address));
				txtlen = (int) (txt.end_address-start_address+i);
			}
			int txtoffs = (int) (start_address+i-txt.start_address+txt.location);
			data [i] = omf.image[txtoffs];
			i++;
			txtlen--;
		}
		return data;
	}

}
