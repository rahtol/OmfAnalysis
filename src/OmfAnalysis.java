import java.io.File;

public class OmfAnalysis
{

	public static void main(String[] args)
	{
		System.out.println("OmfAnalysis v1.03, 24.07.2018");
		
		if (args.length < 3)
		{
			System.out.println("Usage: OmfAnalysis <OMF-File> <code_segment_start_address> <CPP-Path>+\n");
			return;
		}
		
		String omfFname = args[0];
		long code_segment_start_address = Long.parseLong(args[1]);
		String cppDirs [] = new String [args.length-2];
		System.arraycopy(args, 2, cppDirs, 0, cppDirs.length);
		
		Long t0 = System.currentTimeMillis();
		OmfFile fomf = new OmfFile ();
		fomf.initialize(omfFname);
		Long t1 = System.currentTimeMillis();

		if (fomf.valid)
		{
			System.out.printf ("OMF-File: %s ok.\nSize=%d Bytes, CPU-time %g ms\n", omfFname, fomf.len(), ((double) (t1-t0))/((double) 1000));

			for (DebInfoModule module : fomf.debtxt.modulesByAddress.values())
			{
				for (String cppDir : cppDirs)
				{
					String cppFname = cppDir + "\\" + module.toc.module_name;
					File fcpp = new File(cppFname);
					if (fcpp.exists() && fcpp.isFile())
					{
						System.out.printf ("CPP-file=%s, ", module.toc.module_name);
	
						try
						{
							DisassembledListing lst = new DisassembledListing(fomf, code_segment_start_address);
							lst.transcode(module.toc.module_name, cppFname);
							t1 = System.currentTimeMillis();
							System.out.printf ("CPU-time %g ms\n", ((double) (t1-t0))/((double) 1000));
						} 
						catch (Exception e)
						{
							t1 = System.currentTimeMillis();
							System.out.printf ("Error.\n");
							e.printStackTrace(System.out);
						}
					}
				}
			}
		}
		else {
			System.out.printf ("Error reading OMF, fname=%s, CPU-time %g ms\n", omfFname, ((double) (t1-t0))/((double) 1000));
		}
	}

}
