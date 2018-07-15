
public class AbstxtRecord
{
	long start_address;           // virtual address under windows
	long end_address;             // virtual address under windows
	long length;
	long location;				  // offset in OMF file
	
	AbstxtRecord(long start_address, long length, long location)
	{
		this.start_address = start_address;
		this.length = length;
		this.end_address = start_address + length;
		this.location = location;
	}
}
