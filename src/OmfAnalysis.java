
public class OmfAnalysis
{

	public static void main(String[] args)
	{
		Long t0 = System.currentTimeMillis();
		OmfFile f = new OmfFile ();
		f.initialize(args[0]);
		Long t1 = System.currentTimeMillis();
		System.out.printf ("File size %d Bytes, CPU-time %g ms\n", f.len(), ((double) (t1-t0))/((double) 1000));

		if (f.valid)
		{
			try
			{
				DisassembledListing lst = new DisassembledListing(f);
				lst.transcode("LTE.CPP", "I:\\work\\opencppcoverage\\OpenCppCoverage-release-0.9.7.0\\OpenCppCoverage-release-0.9.7.0\\omf386symtab\\rttlsrc\\SRC\\lte.cpp");
//				lst.transcode("LTE.CPP", "U:\\TGMT_WCU_SW\\TRA\\SRC\\lte.cpp");
				Long t2 = System.currentTimeMillis();
				System.out.printf ("CPU-time %g ms\n", ((double) (t2-t0))/((double) 1000));
			} 
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
