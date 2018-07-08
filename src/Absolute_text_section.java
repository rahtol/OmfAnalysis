
public class Absolute_text_section // ABSTXT
{
	OmfFile omf;
	int offs;
	int len;
	
	Absolute_text_section ()
	{
		omf = null;
		offs = 0;
		len = 0;
	}
	
	public void initialize (OmfFile omf, long ABSTXT_location, long lenabstxt) throws Exception
	{
		this.omf = omf;
		this.offs =  (int) ABSTXT_location;
		this.len = (int) lenabstxt;
		OmfFile.Pos pos = omf.new Pos (offs);
		int n = 0; // counts text blocks in ABSTXT section
		
		while (pos.pos < offs + len)
		{
			long real_address = pos.readUInt32();
			long length = pos.readUInt32();
			omf.check(length > 0, String.format("ABSTXT: text_length zero at offs=%08x", pos.pos));
			int text_offs = pos.pos;
//			System.out.printf("ABSTXT(%04d): real_address=%08x, length=%08x, text_offs=%08x\n", ++n, real_address, length, text_offs);
			// TODO ... store in TREE
			pos.pos += length;
		}
		omf.check(pos.pos == offs+len, String.format("ABSTXT mismatch: pos=%d, offs=%d, len=%d", pos.pos, offs, len));
	}

}
