import java.io.File;

public class OmfAnalysis
{

	public static void main(String[] args)
	{
		System.out.println("OmfAnalysis v1.00, 20.07.2018\n");
		
		if (args.length != 2)
		{
			System.out.println("Usage: OmfAnalysis <OMF-File> <CPP-Path>\n");
		}
		
		String omfFname = args[0];
		String cppDir = args[1];
		
		Long t0 = System.currentTimeMillis();
		OmfFile fomf = new OmfFile ();
		fomf.initialize(omfFname);
		Long t1 = System.currentTimeMillis();

		if (fomf.valid)
		{
			System.out.printf ("OMF-File: %s ok.\nSize=%d Bytes, CPU-time %g ms\n", omfFname, fomf.len(), ((double) (t1-t0))/((double) 1000));

			for (DebInfoModule module : fomf.debtxt.modulesByAddress.values())
			{
				String cppFname = cppDir + "\\" + module.toc.module_name;
				File fcpp = new File(cppFname);
				if (fcpp.exists() && fcpp.isFile())
				{
					System.out.printf ("CPP-file=%s, ", module.toc.module_name);

					try
					{
						DisassembledListing lst = new DisassembledListing(fomf);
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
		else {
			System.out.printf ("Error reading OMF, fname=%s, CPU-time %g ms\n", omfFname, ((double) (t1-t0))/((double) 1000));
		}
	}

}
