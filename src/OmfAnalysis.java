
public class OmfAnalysis {

	public static void main(String[] args)
	{
		OmfFile f = new OmfFile ();
		f.initialize(args[0]);
		System.out.printf ("%d\n", f.len());

	}

}
