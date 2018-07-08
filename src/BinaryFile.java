import java.io.*;
import java.nio.file.*;

public class BinaryFile {

	public byte [] image;
	boolean valid;
	
	BinaryFile ()
	{
		image = null;
		valid = false;
	}
	
	public void initialize (String path)
	{
		try {
			valid = true;
			image = Files.readAllBytes(Paths.get(path));	
		} 
		catch (IOException ex)
		{
			valid = false;
            ex.printStackTrace();
        }
	}
	
	public int len ()
	{
		return image.length;
	}
	
	public void check (boolean b, String errmsg) throws Exception
	{
		if (!b)
			throw new Exception(errmsg);
	}
	
	public void warn (boolean b, String errmsg) throws Exception
	{
		if (!b)
			System.err.println(errmsg);
	}
	
	class Pos
	{
		int pos;
		
		Pos (int pos)
		{
			this.pos = pos;
		}
		
		public void checkavail (int n) throws Exception
		{
			check (pos+n <= image.length, String.format("File length error at: pos=%s, n=%d, imagelen=%d", pos, n, image.length));
		}
		
		public long readUInt8 () throws Exception
		{
			checkavail(1);
			long b0 = ((long) image[pos++]) & 255;
			return b0;
		};
		
		public long readUInt16 () throws Exception
		{
			checkavail(2);
			long b0 = ((long) image[pos++]) & 255;
			long b1 = ((long) image[pos++]) & 255;
			return b0 + 256*b1;
		};
		
		public long readUInt32 () throws Exception
		{
			checkavail(4);
			long b0 = ((long) image[pos++]) & 255;
			long b1 = ((long) image[pos++]) & 255;
			long b2 = ((long) image[pos++]) & 255;
			long b3 = ((long) image[pos++]) & 255;
			return b0 + 256*b1 + 65536*b2 + 256*65536*b3;
		};
		
		public String readStr (int len) throws Exception
		{
			checkavail(len);
			String s = new String (image, pos, len);
			pos += len;
			return s;
		}

		public String readStr () throws Exception
		{
			int len = (int) readUInt8();
			if (len == 255)
			{
				len = (int) readUInt16();
			}
			return readStr(len);
		}
	}


}
