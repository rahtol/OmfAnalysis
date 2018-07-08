
public class OmfFile extends BinaryFile
{
	class File_header
	{
		long file_type;
		
		public void read (Pos pos) throws Exception
		{
			file_type = pos.readUInt8();
			check (file_header.file_type == 0xb2, String.format("Illegal file_type: %02x",file_header.file_type));
		}
	}
	
	class Bootloadable_module_header
	{
		long total_space;
		String date;
		String time;
		String module_creator;
		long GDT_limit;
		long GDT_base;
		long IDT_limit;
		long IDT_base;
		long TSS_selector;
		
		public void read (Pos pos) throws Exception
		{
			total_space = pos.readUInt32();
			date = pos.readStr(8);
			time = pos.readStr(8);
			module_creator = pos.readStr(41);
			GDT_limit = pos.readUInt16();
			GDT_base = pos.readUInt32();
			IDT_limit = pos.readUInt16();
			IDT_base = pos.readUInt32();
			TSS_selector = pos.readUInt16();
		}
		
	}
	
	class Partition_1
	{
		class Table_of_contents
		{
			long ABSTXT_location;
			long DEBTXT_location;
			long last_location;
			long next_partition;
			long reserved;
			
			public void read (Pos pos) throws Exception
			{
				ABSTXT_location = pos.readUInt32();
				DEBTXT_location = pos.readUInt32();
				last_location = pos.readUInt32();
				next_partition = pos.readUInt32();
				reserved = pos.readUInt32();
				check (ABSTXT_location < image.length-1, String.format("ABSTXT_location out of range: ABSTXT_location=%d imagelen=%d", ABSTXT_location, image.length));
				check (DEBTXT_location < image.length-1, String.format("DEBTXT_location out of range: DEBTXT_location=%d imagelen=%d", DEBTXT_location, image.length));
				check (last_location == image.length-1, String.format("last_location unexpected: last_location=%d imagelen=%d", last_location, image.length));
				check (next_partition == 0, String.format("next_partition expected to be zero: next_partition=%d", next_partition));
			}
			
		}
		
		Pos start_pos;
		Table_of_contents table_of_contents;
		
		Partition_1 ()
		{
			table_of_contents = new Table_of_contents ();
		}
		
		public void read (Pos pos) throws Exception
		{
			start_pos = new Pos (pos.pos);
			table_of_contents.read(pos);
		}
	}
	
	File_header file_header;
	Bootloadable_module_header bootloadable_module_header;
	Partition_1 partition_1;
	Absolute_text_section abstxt;
	Debug_loadable_text_section debtxt;
	
	OmfFile ()
	{
		file_header = new File_header();
		bootloadable_module_header = new Bootloadable_module_header();
		partition_1 = new Partition_1();
		abstxt = new Absolute_text_section();
		debtxt = new Debug_loadable_text_section();
		
	}
	
	public void initialize (String path)
	{
		try 
		{
			super.initialize (path);
			Pos pos = new Pos (0);
			file_header.read(pos);
			bootloadable_module_header.read(pos);
			partition_1.read(pos);
			
			// TODO check checksum ...
			
			if (partition_1.table_of_contents.ABSTXT_location < partition_1.table_of_contents.DEBTXT_location)
			{
				long lenabstxt = partition_1.table_of_contents.DEBTXT_location - partition_1.table_of_contents.ABSTXT_location;
				long lendebtxt = partition_1.table_of_contents.last_location - partition_1.table_of_contents.DEBTXT_location;
				abstxt.initialize(this, partition_1.table_of_contents.ABSTXT_location, lenabstxt);
				debtxt.initialize(this, partition_1.table_of_contents.DEBTXT_location, lendebtxt);
			}
			else
			{
				check (false, "Expecting ABSTXT before DEBTXT");
			}
		}
		catch (Exception ex)
		{
			valid = false;
            ex.printStackTrace();
		}
	}

}
